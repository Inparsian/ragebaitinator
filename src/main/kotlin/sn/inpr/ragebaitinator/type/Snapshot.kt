package sn.inpr.ragebaitinator.type

import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.util.Vector
import sn.inpr.ragebaitinator.utility.LogUtil.info
import java.lang.reflect.Method
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class PlayerSnapshot(val player: Player) {
    val inventory: List<ItemStack?> field = mutableListOf()
    val effects: List<PotionEffect> field = mutableListOf()

    init {
        capture()
    }

    fun capture() {
        inventory.addAll(player.inventory.contents)
        effects.addAll(player.activePotionEffects)
    }

    fun restore() {
        player.inventory.clear()
        inventory.forEachIndexed { index, item -> player.inventory.setItem(index, item) }
        player.activePotionEffects.forEach { player.removePotionEffect(it.type) }
        effects.forEach { player.addPotionEffect(it) }
    }
}

class EntitySnapshot(val entity: Entity) {
    data class Field(val entity: Entity, val value: Any?)
    data class FieldMethod(val name: String, val method: Method)

    companion object {
        val methodBlacklist = setOf(
            "Handle",
            "Velocity",
            "Health",
        )
        val entityGetMethodCache = ConcurrentHashMap<Class<out Entity>, List<FieldMethod>>()
        val entitySetMethodCache = ConcurrentHashMap<Class<out Entity>, List<FieldMethod>>()
    }

    private val fields = mutableMapOf<String, Field>()

    init {
        capture()
    }

    private fun isSnapshotable(type: Class<*>): Boolean = type.isPrimitive || type.isEnum || type in setOf(
        java.lang.String::class.java, //java-equivalent
        Vector::class.java
    )

    fun getGetterMethods(): List<FieldMethod> {
        // run anything starting with 'get' and 'is'
        // we care about the ones that have a corresponding 'set' method
        return if (entityGetMethodCache.containsKey(entity.javaClass)) {
            entityGetMethodCache[entity.javaClass]!!
        } else {
            val methods = entity.javaClass.methods.asSequence()
                .filter { method -> !method.isAnnotationPresent(Deprecated::class.java) }
                .filter { method -> isSnapshotable(method.returnType) }
                .filter { method -> method.parameterCount == 0 && method.name != "getClass" && listOf("get", "is").any { method.name.startsWith(it) }}
                .filter { method ->
                    val setterName = "set" + method.name.removePrefix("get").removePrefix("is")
                    entity.javaClass.methods.any {
                        it.name == setterName &&
                                it.parameterCount == 1 &&
                                it.parameterTypes[0] == method.returnType
                    }
                }
                .sortedBy { it.name }
                .map { method ->
                    // strip the 'get'/'is' and make sure the first character is uppercase
                    val name = method.name
                        .removePrefix("get")
                        .removePrefix("is")
                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
                    FieldMethod(name, method)
                }
                .filter { method -> methodBlacklist.none { it == method.name } }
                .toList()

            // cache them
            entityGetMethodCache[entity.javaClass] = methods
            methods
        }
    }

    fun getSetterMethods(): List<FieldMethod> {
        return if (entitySetMethodCache.containsKey(entity.javaClass)) {
            entitySetMethodCache[entity.javaClass]!!
        } else {
            val methods = entity.javaClass.methods.asSequence()
                .filter { method -> !method.isAnnotationPresent(Deprecated::class.java) }
                .filter { method -> method.parameterCount == 1 && method.name.startsWith("set") }
                .filter { method ->
                    val property = method.name.removePrefix("set")
                    val paramType = method.parameterTypes[0]
                    entity.javaClass.methods.any {
                        it.parameterCount == 0 &&
                                (it.name == "get$property" || it.name == "is$property") &&
                                isSnapshotable(it.returnType) &&
                                it.returnType == paramType
                    }
                }
                .sortedBy { it.name }
                .map { method ->
                    val name = method.name
                        .removePrefix("set")
                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
                    FieldMethod(name, method)
                }
                .filter { method -> methodBlacklist.none { it == method.name } }
                .toList()

            entitySetMethodCache[entity.javaClass] = methods
            methods
        }
    }

    fun capture() {
        val methods = getGetterMethods()

        methods.forEach { field -> runCatching {
            fields[field.name] = Field(entity,field.method.invoke(entity))
        }}
    }

    fun restoreOn(targetEntity: Entity) {
        val methods = getSetterMethods()

        methods.forEach { fieldMethod -> runCatching {
            fieldMethod.method.invoke(targetEntity, fields[fieldMethod.name]?.value)
        }}
    }

    fun restore() = restoreOn(entity)
}