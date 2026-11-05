package sn.inpr.ragebaitinator.utility

import java.security.MessageDigest
import kotlin.random.Random

object TextTransform {
    private val sentenceRegex = Regex("""[^.!?]*[.!?]+\s*|[^.!?]+""")
    private val wordRegex = Regex("""[A-Za-z]+(?:'[A-Za-z]+)*""")

    fun md5(message: String, random: Random = Random.Default): String {
        val salt = (1..8).joinToString("") { "0123456789abcdef"[random.nextInt(16)].toString() }
        val digest = MessageDigest.getInstance("MD5").digest((message + salt).toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun uwu(message: String, chance: Double = 0.5, random: Random = Random.Default): String
        = sentenceRegex.replace(message) { match ->
            val trimmed = match.value.trimEnd()
            if (trimmed.isBlank())
                return@replace match.value

            val trailing = match.value.substring(trimmed.length)
            val suffix = when {
                random.nextDouble() >= chance -> ""
                random.nextBoolean() -> "~"
                else -> " UwU"
            }

            uwuify(trimmed) + suffix + trailing
        }

    private fun uwuify(text: String): String = text
        .replace(Regex("[nN][aeiouAEIOU]")) { it.value[0] + "y" + it.value[1] }
        .replace(Regex("ove"), "uv")
        .replace('r', 'w').replace('l', 'w')
        .replace('R', 'W').replace('L', 'W')

    fun pigLatin(message: String): String = wordRegex.replace(message) { pigLatinWord(it.value) }

    private fun pigLatinWord(word: String): String {
        // y counts as a vowel unless it opens the word
        var split = word.indexOfFirst { it.lowercaseChar() in "aeiou" }
            .let { vowel -> word.indexOfFirst { it.lowercaseChar() == 'y' }.takeIf { it > 0 && (vowel == -1 || it < vowel) } ?: vowel }
        if (split == 0)
            return word + "way"
        if (split == -1)
            return word + "ay"

        // the u in qu is a consonant
        if (word[split - 1].lowercaseChar() == 'q' && word[split].lowercaseChar() == 'u' && split + 1 < word.length)
            split++

        val head = word.substring(0, split)
        val tail = word.substring(split)
        return when {
            word.length > 1 && word.all { !it.isLowerCase() } -> (tail + head + "ay").uppercase()
            head.first().isUpperCase() -> tail.replaceFirstChar { it.uppercaseChar() } + head.lowercase() + "ay"
            else -> tail + head + "ay"
        }
    }

    fun garble(message: String, random: Random = Random.Default): String = message
        .split(' ')
        .filter { it.isNotEmpty() }
        .map { garbleWord(it, random) }
        .shuffled(random)
        .joinToString(" ")

    private fun garbleWord(word: String, random: Random): String {
        if (word.length < 4)
            return word

        val inside = word.substring(1, word.length - 1).toList()
        // a few extra tries so short insides don't shuffle back to themselves too often
        val shuffled = (1..4).map { inside.shuffled(random) }.firstOrNull { it != inside } ?: inside
        return word.first() + shuffled.joinToString("") + word.last()
    }

    fun typo(word: String, random: Random = Random.Default): String {
        if (word.length < 2)
            return word

        repeat(8) {
            val chars = word.toMutableList()
            val i = random.nextInt(chars.size - 1)
            when (random.nextInt(3)) {
                0 -> chars[i] = chars[i + 1].also { chars[i + 1] = chars[i] }
                1 -> chars.removeAt(i + 1)
                else -> chars.add(i, chars[i])
            }

            val result = chars.joinToString("")
            if (result != word)
                return result
        }

        return word + word.last()
    }
}
