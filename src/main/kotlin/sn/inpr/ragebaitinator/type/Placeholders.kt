package sn.inpr.ragebaitinator.type

import sn.inpr.ragebaitinator.Main
import sn.inpr.ragebaitinator.Main.Companion.language

class Placeholders(val original: String) {
    companion object {
        private val placeholdersRegex = Regex("%([^%]+)%")
    }

    constructor(key: String, default: String) : this(language.stringOr(key, default))
    constructor(key: String, default: List<String>) : this(language.stringOr(key, default))

    private val placeholders = mutableMapOf<String, String>() //k,v

    // These will always be present
    init {
        put("prefix", language.prefix)
        put("version", Main.instance().description.version)
        put("plugin", Main.instance().description.name)
        put("c1", language.primaryColor)
        put("c2", language.secondaryColor)
        put("c3", language.tertiaryColor)
    }

    fun put(key: String, value: String): Placeholders {
        placeholders[key] = value
        return this
    }

    override fun toString(): String {
        return placeholdersRegex.replace(original) { match ->
            placeholders[match.groupValues[1]] ?: match.value
        }
    }
}