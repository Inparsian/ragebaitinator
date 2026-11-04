package sn.inpr.ragebaitinator.utility

object TypeUtil {
    fun splitAtLength(string: String, length: Int, append: String?): MutableList<String> {
        // split between words
        val strings: MutableList<String> = mutableListOf()
        val words = string.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        var index = 0
        var lastIndex = 0

        for (word in words) {
            index += word.length + 1

            if (index > length * (strings.size + 1)) {
                strings.add(append + string.substring(lastIndex, index - word.length - 1))
                lastIndex = index - word.length - 1
            }
        }

        strings.add(append + string.substring(lastIndex))

        return strings
    }
}