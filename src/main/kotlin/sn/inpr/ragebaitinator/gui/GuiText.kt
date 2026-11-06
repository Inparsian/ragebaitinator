package sn.inpr.ragebaitinator.gui

import org.bukkit.ChatColor
import sn.inpr.ragebaitinator.Main
import sn.inpr.ragebaitinator.Main.Companion.language
import sn.inpr.ragebaitinator.utility.TypeUtil

object GuiText {
    private val onlyAPlaceholder = Regex("^%([^%]+)%$")
    private val placeholder = Regex("%([^%]+)%")

    fun color(text: String): String = ChatColor.translateAlternateColorCodes('&', text)

    // the same ones that every other message has. these are only looked up when a message has them in it, since live
    // menus build every item they have again each tick
    private fun builtIn(name: String): String? = when (name) {
        "prefix" -> color(language.prefix)
        "c1" -> color(language.primaryColor)
        "c2" -> color(language.secondaryColor)
        "c3" -> color(language.tertiaryColor)
        "version" -> Main.instance().description.version
        "plugin" -> Main.instance().description.name
        else -> null
    }

    // the template is what gets its colors translated, and not what's put into it. a command or a name that happens to
    // have an & in it should be shown as it is
    private fun fill(template: String, values: Array<out Pair<String, String>>): String
        = placeholder.replace(color(template)) { match ->
            val name = match.groupValues[1]
            values.firstOrNull { it.first == name }?.second ?: builtIn(name) ?: match.value
        }

    fun text(key: String, vararg values: Pair<String, String>): String = fill(language.stringOr(key, ""), values)

    fun lines(key: String, vararg values: Pair<String, String>, multiline: Map<String, List<String>> = emptyMap()): MutableList<String> {
        val raw = language.stringOr(key, "")
        val result = mutableListOf<String>()
        if (raw.isEmpty())
            return result

        for (line in raw.split("\n")) {
            val expansion = onlyAPlaceholder.matchEntire(line.trim())?.groupValues?.get(1)?.let { multiline[it] }
            if (expansion != null)
                result.addAll(expansion)
            else
                result.add(fill(line, values))
        }

        return result
    }

    fun wrap(text: String, length: Int = 50): List<String>
        = TypeUtil.splitAtLength(color(text), length, color(language.secondaryColor))
}
