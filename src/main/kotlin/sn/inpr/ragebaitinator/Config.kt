package sn.inpr.ragebaitinator

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.configuration.serialization.ConfigurationSerializable
import org.bukkit.plugin.Plugin
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import kotlin.text.iterator

object Config {
    @Throws(IOException::class)
    fun update(plugin: Plugin, resourceName: String, toUpdate: File, ignoredSections: MutableList<String>) {
        val dumperOptions = DumperOptions()
        dumperOptions.splitLines = false
        if (resourceName.contains("language"))
            dumperOptions.defaultScalarStyle = DumperOptions.ScalarStyle.DOUBLE_QUOTED

        val resource = plugin.getResource(resourceName) ?: return
        val newLines = resource.bufferedReader(StandardCharsets.UTF_8).use { it.readLines() }

        val oldConfig: FileConfiguration =
            YamlConfiguration.loadConfiguration(toUpdate)
        val newConfig: FileConfiguration =
            YamlConfiguration.loadConfiguration(
                InputStreamReader(
                    plugin.getResource(resourceName)!!,
                    StandardCharsets.UTF_8
                )
            )

        val writer = BufferedWriter(OutputStreamWriter(FileOutputStream(toUpdate), StandardCharsets.UTF_8))
        val ignoredSectionsClone = ignoredSections.toMutableList()
        ignoredSectionsClone.removeIf { ignoredSection -> !newConfig.isConfigurationSection(ignoredSection) }

        val yaml = Yaml(dumperOptions)
        val comments: MutableMap<String?, String?> = parseComments(newLines.toMutableList(), ignoredSectionsClone, oldConfig, yaml)
        write(newConfig, oldConfig, comments, ignoredSectionsClone, writer, yaml)
    }

    // does some work
    @Throws(IOException::class)
    private fun write(
        newConfig: FileConfiguration,
        oldConfig: FileConfiguration,
        comments: MutableMap<String?, String?>,
        ignoredSections: MutableList<String>,
        writer: BufferedWriter,
        yaml: Yaml
    ) {
        for (key in newConfig.getKeys(true)) {
            comments.remove(key)?.let { writer.write(it) }
            if (ignoredSections.any { it == key })
                continue

            val actualKey = key.substringAfterLast('.')
            val prefixSpaces = " ".repeat(key.count { it == '.' } * 4)
            val newObj = newConfig.get(key)
            val oldObj = oldConfig.get(key)

            if (newObj is ConfigurationSection) {
                writeSection(writer, actualKey, prefixSpaces, (oldObj as? ConfigurationSection) ?: newObj)
            } else {
                write(oldObj ?: newObj, actualKey, prefixSpaces, yaml, writer)
            }
        }

        comments[null]?.let { writer.write(it) }
        writer.close()
    }

    // obj ver
    @Throws(IOException::class)
    private fun write(obj: Any?, actualKey: String?, prefixSpaces: String?, yaml: Yaml, writer: BufferedWriter) {
        var obj = obj
        when (obj) {
            is MutableList<*> -> writeList(obj, actualKey, prefixSpaces, yaml, writer)
            is ConfigurationSerializable ->
                writer.write("$prefixSpaces$actualKey: ${yaml.dump(obj.serialize())}")
            else -> {
                if (obj is String) {
                    obj = obj.replace("\n", "\\n")
                }

                writer.write("$prefixSpaces$actualKey: ${yaml.dump(obj)}")
            }
        }
    }

    private fun parseComments(
        lines: MutableList<String>,
        ignoredSections: MutableList<String>,
        oldConfig: FileConfiguration,
        yaml: Yaml?
    ): MutableMap<String?, String?> {
        val comments: MutableMap<String?, String?> = HashMap()
        val builder = java.lang.StringBuilder()
        val keyBuilder = java.lang.StringBuilder()
        var lastLineIndentCount = 0

        outer@ for (line in lines) {
            if (line.trim { it <= ' ' }.startsWith("-"))
                continue

            if ("" == line.trim { it <= ' ' } || "#".startsWith(line.trim { it <= ' ' })) {
                builder.append("$line\n")
            } else {
                lastLineIndentCount = setFullKey(keyBuilder, line, lastLineIndentCount)

                for (ignoredSection in ignoredSections) {
                    if (keyBuilder.toString() == ignoredSection) {
                        val value = oldConfig.get(keyBuilder.toString())

                        if (value is ConfigurationSection) appendSection(
                            builder,
                            value,
                            java.lang.StringBuilder(" ".repeat(lastLineIndentCount * 4)),
                            yaml!!
                        )

                        continue@outer
                    }
                }

                if (keyBuilder.isNotEmpty()) {
                    comments[keyBuilder.toString()] = builder.toString()
                    builder.setLength(0)
                }
            }
        }

        if (builder.isNotEmpty()) {
            comments[null] = builder.toString()
        }

        return comments
    }

    private fun appendSection(builder: StringBuilder, section: ConfigurationSection, prefixSpaces: StringBuilder, yaml: Yaml) {
        builder.append("$prefixSpaces${getKeyFromFullKey(section.currentPath)}:")
        val keys = section.getKeys(false)

        if (keys.isEmpty()) {
            builder.append(" {}\n")
        } else {
            builder.append("\n")
            prefixSpaces.append("  ")

            for (key in keys) {
                val value = section.get(key!!)
                val actualKey: String = getKeyFromFullKey(key)

                when (value) {
                    is ConfigurationSection -> {
                        appendSection(builder, value, prefixSpaces, yaml)
                        prefixSpaces.setLength(prefixSpaces.length - 2)
                    }
                    is MutableList<*> -> builder.append(getListAsString(value, actualKey, prefixSpaces.toString(), yaml))
                    else -> builder.append(prefixSpaces).append(actualKey).append(": ").append(yaml.dump(value))
                }
            }
        }
    }


    @Throws(IOException::class)
    private fun writeSection(
        writer: BufferedWriter,
        actualKey: String,
        prefixSpaces: String,
        section: ConfigurationSection
    ) {
        val v = if (section.getKeys(false).isEmpty()) " {}" else ""
        writer.write("$prefixSpaces$actualKey:$v\n")
    }

    @Throws(IOException::class)
    private fun writeList(
        list: MutableList<*>?,
        actualKey: String?,
        prefixSpaces: String?,
        yaml: Yaml?,
        writer: BufferedWriter
    ) {
        writer.write(getListAsString(list!!, actualKey, prefixSpaces!!, yaml!!))
    }

    private fun getListAsString(list: MutableList<*>, actualKey: String?, prefixSpaces: String, yaml: Yaml): String {
        val builder = StringBuilder(prefixSpaces)
            .append("$actualKey:")

        if (list.isEmpty()) {
            builder.append(" []\n")
        } else {
            builder.append("\n")

            for (i in list.indices) {
                when (val o: Any? = list[i]) {
                    is String, is Char -> builder.append("$prefixSpaces- \"$o\"")
                    is MutableList<*> -> builder.append("$prefixSpaces- ${yaml.dump(o)}")
                    else -> builder.append("$prefixSpaces- $o")
                }

                if (i != list.size - 1) {
                    builder.append("\n")
                }
            }
        }

        return builder.toString()
    }

    private fun setFullKey(keyBuilder: StringBuilder, configLine: String, lastLineIndentCount: Int): Int {
        val currentIndents = countIndents(configLine)
        val key = configLine.trim().substringBefore(' ')

        when {
            keyBuilder.isEmpty() -> keyBuilder.append(key)
            currentIndents > lastLineIndentCount -> keyBuilder.append(".$key")
            else -> {
                repeat(lastLineIndentCount - currentIndents + 1) {
                    removeLastKey(keyBuilder)
                }
                if (keyBuilder.isEmpty()) keyBuilder.append(key) else keyBuilder.append(".$key")
            }
        }

        return currentIndents
    }

    private fun removeLastKey(keyBuilder: StringBuilder) {
        val lastDot = keyBuilder.lastIndexOf('.')
        if (lastDot < 0)
            keyBuilder.clear()
        else keyBuilder.setLength(lastDot)
    }

    private fun getKeyFromFullKey(fullKey: String?): String {
        return fullKey?.substringAfterLast('.') ?: ""
    }

    private fun countIndents(s: String): Int {
        // divide by 2, one indent is 2 spaces
        var spaces = 0
        for (c in s) {
            if (c == ' ') spaces++ else break
        }
        return spaces / 2
    }
}