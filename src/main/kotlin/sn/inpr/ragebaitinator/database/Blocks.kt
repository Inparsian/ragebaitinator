package sn.inpr.ragebaitinator.database

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.TileState
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.world.WorldLoadEvent
import org.bukkit.util.BlockVector
import sn.inpr.ragebaitinator.Main.Companion.runTask
import sn.inpr.ragebaitinator.Main.Companion.runTaskTimer
import sn.inpr.ragebaitinator.utility.LogUtil.info
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.Types
import java.util.*
import kotlin.collections.ArrayDeque

object Blocks : Listener {
    // comfortably longer than the default autosave interval of 5 minutes
    private const val RELEASE_RETENTION_MILLIS = 15 * 60 * 1000L
    private const val PURGE_PERIOD_TICKS = 5 * 60 * 20L

    private data class Position(val world: UUID, val x: Int, val y: Int, val z: Int) {
        constructor(block: Block) : this(block.world.uid, block.x, block.y, block.z)

        fun block(): Block? = Bukkit.getWorld(world)?.getBlockAt(x, y, z)
        override fun toString() = "$x, $y, $z in $world"
    }

    private class Original(val blockData: String, val blockEntity: ByteArray?)
    private class Layer(val owner: String, val material: Material)
    private class Entry(val original: Original) {
        val layers = mutableListOf<Layer>()
        // what had to be taken away along with this block, and who it's held for
        val collateral = mutableListOf<Pair<String, Position>>()
    }

    private val FACES = listOf(BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST)

    private val entries = mutableMapOf<Position, Entry>()
    private val positionsByOwner = mutableMapOf<String, MutableSet<Position>>()
    private val worldsWithReleases = mutableSetOf<UUID>()
    private var connection: Connection? = null

    fun initialize() {
        connection = runCatching { SQLite.open() }.onFailure { it.printStackTrace() }.getOrNull()
        SQLite.transaction { connection ->
            connection.createStatement().use { statement ->
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS blocks (" +
                        "world TEXT NOT NULL," +
                        "x INTEGER NOT NULL," +
                        "y INTEGER NOT NULL," +
                        "z INTEGER NOT NULL," +
                        "block_data TEXT NOT NULL," +
                        "block_entity BLOB," +
                        "placed TEXT NOT NULL," +
                        "released_at INTEGER," +
                        "PRIMARY KEY (world, x, y, z)" +
                        ");")
            }
        }

        // with load: STARTUP, worlds don't exist yet and tasks only start running once they do
        runTask { Bukkit.getWorlds().forEach(::recover) }
        runTaskTimer(::purgeReleased, PURGE_PERIOD_TICKS, PURGE_PERIOD_TICKS)
    }

    @EventHandler
    fun onWorldLoad(event: WorldLoadEvent) {
        runTask { recover(event.world) }
    }

    fun place(owner: String, changes: List<Pair<Block, Material>>, applyPhysics: Boolean = true) {
        // originals are all captured first, as physics from one change can affect the next block
        val captured = changes.distinctBy { Position(it.first) }.mapNotNull { [block, material] ->
            val position = Position(block)
            val entry = entries[position] ?: Entry(capture(block) ?: return@mapNotNull null)
            Triple(block, material, position to entry)
        }
        if (captured.isEmpty()) return

        persist(captured.map { [_, material, tracked] -> Triple(tracked.first, tracked.second.original, material) })

        captured.forEach { [block, material, tracked] ->
            track(owner, tracked.first, tracked.second, material)
            block.setType(material, false)
        }

        if (!applyPhysics) return

        detachDependents(owner, captured.map { it.first })

        captured.forEach { [block, material, _] ->
            block.setType(if (material == Material.BARRIER) Material.STONE else Material.BARRIER, false)
            block.setType(material, true)
        }
    }

    private fun track(owner: String, position: Position, entry: Entry, material: Material) {
        entries[position] = entry
        entry.layers.removeIf { it.owner == owner }
        entry.layers.add(Layer(owner, material))
        positionsByOwner.getOrPut(owner) { mutableSetOf() }.add(position)
    }

    private fun persist(rows: List<Triple<Position, Original, Material>>) {
        SQLite.transaction { connection ->
            connection.prepareStatement(
                "INSERT INTO blocks (world, x, y, z, block_data, block_entity, placed) VALUES (?, ?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT (world, x, y, z) DO UPDATE SET " +
                "block_data = excluded.block_data, block_entity = excluded.block_entity, " +
                "placed = excluded.placed || ',' || placed, released_at = NULL"
            ).use { statement ->
                rows.forEach { [position, original, material] ->
                    statement.setPosition(position)
                    statement.setString(5, original.blockData)
                    if (original.blockEntity != null)
                        statement.setBytes(6, original.blockEntity)
                    else
                        statement.setNull(6, Types.BLOB)
                    statement.setString(7, material.name)
                    statement.addBatch()
                }
                statement.executeBatch()
            }
        }
    }

    fun <P : Any> findCollateral(
        changed: Collection<P>,
        neighbors: (P) -> Iterable<P>,
        cannotStay: (P) -> Boolean,
        remove: (P) -> Boolean,
    ): List<Pair<P, P>> {
        val taken = changed.toMutableSet()
        val refused = mutableSetOf<P>()
        val queue = ArrayDeque(changed)
        val found = mutableListOf<Pair<P, P>>()

        while (queue.isNotEmpty()) {
            val parent = queue.removeFirst()
            for (neighbor in neighbors(parent)) {
                // something that held up has not made a difference until it's gone, which is why a neighbor that could stay
                // is looked at again whenever something next to it goes
                if (neighbor in taken || neighbor in refused || !cannotStay(neighbor))
                    continue

                if (!remove(neighbor)) {
                    refused.add(neighbor)
                    continue
                }

                taken.add(neighbor)
                found.add(neighbor to parent)
                queue.add(neighbor)
            }
        }

        return found
    }

    private fun detachDependents(owner: String, changed: List<Block>) {
        val originals = mutableMapOf<Position, Original>()

        fun cannotStay(position: Position): Boolean {
            if (position in entries) return false
            val block = position.block() ?: return false
            return !block.type.isAir && !block.isLiquid && !block.blockData.isSupported(block)
        }

        fun remove(position: Position): Boolean {
            val block = position.block() ?: return false
            originals[position] = capture(block) ?: return false
            block.setType(Material.AIR, false)
            return true
        }

        val found = findCollateral(changed.map(::Position), ::neighborsOf, ::cannotStay, ::remove)
        if (found.isEmpty()) return

        val tracked = found.map { [position, parent] -> Triple(position, parent, Entry(originals.getValue(position))) }
        persist(tracked.map { [position, _, entry] -> Triple(position, entry.original, Material.AIR) })
        tracked.forEach { [position, parent, entry] ->
            track(owner, position, entry, Material.AIR)
            entries[parent]?.collateral?.add(owner to position)
        }
    }

    private fun neighborsOf(position: Position): List<Position> {
        val world = Bukkit.getWorld(position.world) ?: return emptyList()
        return FACES.mapNotNull { face ->
            val x = position.x + face.modX
            val z = position.z + face.modZ
            if (world.isChunkLoaded(x shr 4, z shr 4)) Position(position.world, x, position.y + face.modY, z) else null
        }
    }

    fun release(owner: String) {
        release(owner, positionsByOwner[owner]?.toList() ?: return)
    }

    fun release(owner: String, blocks: List<Block>) = release(owner, blocks.map(::Position))

    @JvmName("releasePositions")
    private fun release(owner: String, positions: List<Position>) {
        val restored = mutableListOf<Position>()

        for (position in positions) {
            val entry = entries[position] ?: continue
            val wasTop = entry.layers.lastOrNull()?.owner == owner
            if (!entry.layers.removeIf { it.owner == owner })
                continue

            positionsByOwner[owner]?.let { owned ->
                owned.remove(position)
                if (owned.isEmpty()) positionsByOwner.remove(owner)
            }

            val next = entry.layers.lastOrNull()
            if (next != null) {
                if (wasTop) position.block()?.setType(next.material, false)
                continue
            }

            entries.remove(position)
            // a world that isn't loaded keeps its row as active, to be recovered when it next loads
            val block = position.block() ?: continue
            if (restore(block, entry.original))
                restored.add(position)

            // what was taken away along with this block comes back now that there is something for it to stand on
            entry.collateral.forEach { [collateralOwner, dependent] -> release(collateralOwner, listOf(dependent)) }
        }

        markReleased(restored)
    }

    fun shutdown() {
        positionsByOwner.keys.toList().forEach(::release)

        for (uid in worldsWithReleases) {
            val world = Bukkit.getWorld(uid)?.takeIf { it.isAutoSave } ?: continue
            world.save()
            SQLite.transaction { connection ->
                connection.prepareStatement("DELETE FROM blocks WHERE world = ? AND released_at IS NOT NULL").use { statement ->
                    statement.setString(1, uid.toString())
                    statement.executeUpdate()
                }
            }
        }

        runCatching { connection?.close() }
        connection = null
    }

    private fun recover(world: World) {
        class Row(val position: Position, val original: Original, val placed: Set<String>, val released: Boolean)

        val rows = mutableListOf<Row>()
        val connection = connection ?: return
        connection.prepareStatement("SELECT * FROM blocks WHERE world = ?").use { statement ->
            statement.setString(1, world.uid.toString())
            statement.executeQuery().use { result ->
                while (result.next()) {
                    rows.add(Row(
                        Position(world.uid, result.getInt("x"), result.getInt("y"), result.getInt("z")),
                        Original(result.getString("block_data"), result.getBytes("block_entity")),
                        result.getString("placed").split(',').toSet(),
                        result.getObject("released_at") != null
                    ))
                }
            }
        }

        var restored = 0
        val settled = rows.filter { row ->
            // the world was reloaded in the middle of a session, these still belong to an active troll
            if (row.position in entries)
                return@filter false

            val block = world.getBlockAt(row.position.x, row.position.y, row.position.z)
            if (block.type.name in row.placed) {
                if (!restore(block, row.original))
                    return@filter false
                restored++
            } else if (!row.released && block.blockData.asString != row.original.blockData) {
                // the world rolled back to before the troll or something else changed this block since.
                // restoring would put a newer block in an older world, which can duplicate a container's items
                info("&6Skipped rollback at ${row.position}: expected ${row.placed} but found ${block.type}. Original was ${row.original.blockData}")
            }
            true
        }

        SQLite.transaction {
            it.prepareStatement("DELETE FROM blocks WHERE world = ? AND x = ? AND y = ? AND z = ?").use { statement ->
                settled.forEach { row ->
                    statement.setPosition(row.position)
                    statement.addBatch()
                }
                statement.executeBatch()
            }
        }

        if (restored > 0)
            info("&aRolled back $restored block(s) in ${world.name} left over from the last session")
    }

    private fun markReleased(positions: List<Position>) {
        if (positions.isEmpty()) return

        positions.mapTo(worldsWithReleases) { it.world }
        val now = System.currentTimeMillis()
        SQLite.transaction { connection ->
            connection.prepareStatement("UPDATE blocks SET released_at = $now WHERE world = ? AND x = ? AND y = ? AND z = ?").use { statement ->
                positions.forEach { position ->
                    statement.setPosition(position)
                    statement.addBatch()
                }
                statement.executeBatch()
            }
        }
    }

    private fun purgeReleased() {
        SQLite.transaction { connection ->
            connection.prepareStatement("DELETE FROM blocks WHERE released_at < ?").use { statement ->
                statement.setLong(1, System.currentTimeMillis() - RELEASE_RETENTION_MILLIS)
                statement.executeUpdate()
            }
        }
    }

    private fun PreparedStatement.setPosition(position: Position) {
        setString(1, position.world.toString())
        setInt(2, position.x)
        setInt(3, position.y)
        setInt(4, position.z)
    }

    // returns null if the block can't be saved, in which case it shouldn't be changed at all
    private fun capture(block: Block): Original? = runCatching {
        Original(block.blockData.asString, if (block.state is TileState) serializeBlockEntity(block) else null)
    }.onFailure {
        info("&cCouldn't save the block at ${Position(block)}, leaving it alone: $it")
    }.getOrNull()

    private fun restore(block: Block, original: Original): Boolean = runCatching {
        block.setBlockData(Bukkit.createBlockData(original.blockData), false)
        original.blockEntity?.let { placeBlockEntity(block, it) }
    }.onFailure {
        info("&cCouldn't restore ${original.blockData} at ${Position(block)}: $it")
    }.isSuccess

    private fun serializeBlockEntity(block: Block): ByteArray {
        val structures = Bukkit.getStructureManager()
        val structure = structures.createStructure()
        structure.fill(block.location, BlockVector(1, 1, 1), false)
        return ByteArrayOutputStream().use { output ->
            structures.saveStructure(output, structure)
            output.toByteArray()
        }
    }

    private fun placeBlockEntity(block: Block, bytes: ByteArray) {
        val structure = ByteArrayInputStream(bytes).use { Bukkit.getStructureManager().loadStructure(it) }
        // Structure.place runs shape updates, which would pop off a sign whose support isn't back yet
        structure.palettes.first().blocks.first().copy(block.location).update(true, false)
    }
}
