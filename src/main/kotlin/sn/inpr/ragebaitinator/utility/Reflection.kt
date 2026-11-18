package sn.inpr.ragebaitinator.utility

import org.bukkit.Material
import sn.inpr.ragebaitinator.Main
import java.io.File
import java.util.jar.JarFile

object Reflection {
    private val materialCache: MutableMap<String, Material?> = HashMap()

    fun getClass(name: String?): Class<*>? = runCatching { Class.forName(name, false, Main::class.java.classLoader) }.getOrNull()

    fun getClassNamesInPackage(packageName: String): List<String> {
        val classes = mutableListOf<String>()
        val prefix = "${packageName.replace('.', '/')}/"
        val url = Main.Companion::class.java.protectionDomain.codeSource.location
        val file = File(url.toURI())
        if (file.isFile)
            JarFile(file).use {
                val jarEntries = it.entries()
                while (jarEntries.hasMoreElements()) {
                    val entry = jarEntries.nextElement()
                    if (entry.name.endsWith(".class") && entry.name.startsWith(prefix) && !entry.name.contains("$"))
                        classes.add(entry.name.replace('/', '.').removeSuffix(".class"))
                }
            }
        return classes
    }

    inline fun <reified T: Any> getClassesInPackage(packageName: String): List<Class<T>> {
        val classes = mutableListOf<Class<T>>()
        val names = getClassNamesInPackage(packageName)
        names.forEach {
            val clazz = getClass(it) ?: return@forEach
            if (T::class.java.isAssignableFrom(clazz))
                @Suppress("UNCHECKED_CAST")
                classes.add(clazz as Class<T>)
        }
        return classes.toList()
    }

    fun getOBClass(name: String?): Class<*>? = getClass("org.bukkit.$name")

    fun getMaterial(name: String): Material? {
        try {
            if (materialCache.containsKey(name))
                return materialCache[name]

            val clazz = getOBClass("Material")
            return if (clazz != null) {
                val material = clazz.getField(name).get(Material::class.java) as? Material?
                    ?: return null
                materialCache[name] = material
                material
            } else {
                null
            }
        } catch (exception: IllegalAccessException) {
            exception.printStackTrace()
        } catch (exception: NoSuchFieldException) {
            exception.printStackTrace()
        }

        // remember that this material doesn't exist/there was an error
        materialCache[name] = null

        return null
    }
}