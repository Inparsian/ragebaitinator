package sn.inpr.ragebaitinator

import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

open class Config(fileName: String?) : YamlConfiguration() {
    constructor() : this(null)

    private var file: File? = null

    init {
        if (fileName != null) {
            file = File(Main.instance().dataFolder, fileName)
            default()
            reload()
        }
    }

    fun reload() {
        if (file == null) return
        try {
            load(file!!)
        } catch (exception: Exception) {
            exception.printStackTrace()
        }
    }

    private fun default() {
        if (file == null) return
        val plugin = Main.instance()
        try {
            val resource = plugin.getResource(file!!.name) ?: return
            super.setDefaults(loadConfiguration(InputStreamReader(resource, StandardCharsets.UTF_8)))
        } catch (exception: NullPointerException) {
            exception.printStackTrace()
        }

        if (!file!!.exists())
            plugin.saveResource(file!!.name, false)
    }

    fun stringOr(key: String, default: String): String {
        val list = getStringList(key)
        return if (list.isNotEmpty()) list.joinToString("\n") else getString(key) ?: default
    }
    fun stringOr(key: String, default: List<String>): String = stringOr(key, default.joinToString("\n"))
    fun intOr(key: String, default: Int): Int = getInt(key, default)
}

class LanguageConfig(fileName: String?) : Config(fileName) {
    constructor() : this(null)

    val prefix: String get() = stringOr("prefix", "&4&lRB »")
    val primaryColor: String get() = stringOr("primaryColor", "&c")
    val secondaryColor: String get() = stringOr("secondaryColor", "&f")
    val tertiaryColor: String get() = stringOr("tertiaryColor", "&7")
    val mustBeAPlayer: String get() = stringOr("mustBeAPlayer", "%prefix% &cYou must be a player to use this command")
    val bothFalse: String get() = stringOr("bothFalse", "%prefix% &cBoth options cannot be false")
    val playerNotFound: String get() = stringOr("noPlayersFound", "%prefix% &cPlayer(s) not found")
}