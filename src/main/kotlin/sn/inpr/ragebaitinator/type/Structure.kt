package sn.inpr.ragebaitinator.type

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.util.Vector
import sn.inpr.ragebaitinator.database.Blocks

class Structure {
    private val blocks: MutableList<Pair<Material, Vector>> = mutableListOf()

    val height: Int get() = blocks.maxOfOrNull { it.second.y.toInt() } ?: 0
    val vectors: List<Vector> get() = blocks.map { it.second }

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

    fun build(owner: String, location: Location) {
        Blocks.place(owner, blocks.map { [material, vector] -> location.clone().add(vector).block to material })
    }

    fun rollback(owner: String) = Blocks.release(owner)
}
