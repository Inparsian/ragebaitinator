package sn.inpr.ragebaitinator.type

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.BlockState
import org.bukkit.util.Vector

class Structure {
    private val blocks: MutableList<Pair<Material, Vector>> = mutableListOf()
    private val former: MutableMap<Location, MutableList<Pair<Vector, BlockState>>> = mutableMapOf()

    val height: Int
        get() = blocks.maxOfOrNull { it.second.y.toInt() } ?: 0
    val vectors: List<Vector>
        get() = blocks.map { it.second }

    fun add(material: Material, vector: Vector): Structure {
        blocks.add(Pair(material, vector))
        return this
    }

    fun add(material: Material, x: Int, y: Int, z: Int): Structure {
        blocks.add(Pair(material, Vector(x, y, z)))
        return this
    }

    fun intersects(location: Location): Boolean
        = blocks.any { location.clone().add(it.second).block.type != Material.AIR }

    fun build(location: Location) {
        if (former.containsKey(location))
            return

        blocks.forEach { [material, vector] ->
            val block = location.clone().add(vector).block
            former.getOrPut(location) { mutableListOf() }.add(Pair(vector, block.state))
            block.type = material
        }
    }

    fun rollback(location: Location) {
        former[location]?.forEach { [_, state] -> state.update(true, false) }
        former.remove(location)
    }
}