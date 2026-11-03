package sn.inpr.ragebaitinator.utility

import org.bukkit.Material

object Reflection {
    private val materialCache: MutableMap<String, Material?> = HashMap()

    private fun getClass(name: String?): Class<*>? {
        return try {
            Class.forName(name)
        } catch (_: ClassNotFoundException) {
            null
        }
    }

    fun getOBClass(name: String?): Class<*>? {
        return getClass("org.bukkit.$name")
    }

    fun getMaterial(name: String): Material? {
        try {
            if (materialCache.containsKey(name))
                return materialCache[name]

            val clazz = getOBClass("Material")
            if (clazz != null) {
                val material = clazz.getField(name).get(Material::class.java) as? Material?
                    ?: return null

                materialCache[name] = material

                return material
            } else {
                return null
            }
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        } catch (_: NoSuchFieldException) {}

        // remember that this material doesn't exist/there was an error
        materialCache[name] = null

        return null
    }
}