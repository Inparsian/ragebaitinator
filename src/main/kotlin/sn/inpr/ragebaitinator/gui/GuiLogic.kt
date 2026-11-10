package sn.inpr.ragebaitinator.gui

import sn.inpr.ragebaitinator.command.def.ArgumentType
import sn.inpr.ragebaitinator.command.def.ArgumentTypes
import java.math.BigDecimal
import kotlin.reflect.KClass

sealed interface Tab {
    data object Active : Tab
    data object All : Tab
    data class Category(val key: String) : Tab
    data class Search(val query: String) : Tab
}

class TrollRow(
    val name: String,
    val description: String,
    val categoryKey: String,
    val categoryName: String,
    val active: Boolean,
)

fun browse(tab: Tab, rows: List<TrollRow>): List<TrollRow> = when (tab) {
    Tab.Active -> rows.filter { it.active }.sortedBy { it.name }
    Tab.All -> rows.sortedBy { it.name }
    is Tab.Category -> rows.filter { it.categoryKey == tab.key }.sortedBy { it.name }
    is Tab.Search -> rows
        .mapNotNull { row -> searchScore(tab.query, row)?.let { row to it } }
        .sortedWith(compareByDescending<Pair<TrollRow, Int>> { it.second }.thenBy { it.first.name })
        .map { it.first }
}

fun searchScore(query: String, row: TrollRow): Int? {
    val words = query.lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
    val name = row.name.lowercase()
    var score = 0
    for (word in words) {
        score += when {
            name == word -> 10
            name.startsWith(word) -> 6
            name.contains(word) -> 4
            row.categoryName.lowercase().contains(word) -> 2
            row.description.lowercase().contains(word) -> 1
            else -> return null
        }
    }
    return score
}

fun togglePlan(names: List<String>, enabledOn: Set<String>): List<String> =
    names.filter { it !in enabledOn }.ifEmpty { names }

fun commandLine(troll: String, targets: String, arguments: List<String> = emptyList()): String =
    (listOf("ragebaitinator", troll, targets) + arguments).joinToString(" ").trimEnd()

fun <T> pageOf(items: List<T>, page: Int, size: Int): List<T> = items.drop((page - 1) * size).take(size)

fun centeredSlots(count: Int, row: Int): List<Int> {
    val shown = count.coerceIn(0, 9)
    val start = row + (9 - shown) / 2
    return List(shown) { start + it }
}

private val wholeSteps = listOf(1.0, 2.0, 5.0, 10.0, 25.0, 50.0, 100.0, 250.0, 500.0, 1000.0, 2500.0, 5000.0, 10000.0)
private val decimalSteps = listOf(0.01, 0.025, 0.05, 0.1, 0.25, 0.5) + wholeSteps

fun stepFor(whole: Boolean, min: Double?, max: Double?): BigDecimal {
    if (min == null || max == null) return BigDecimal.ONE
    val target = (max - min) / (if (whole) 50.0 else 20.0)
    val steps = if (whole) wholeSteps else decimalSteps
    return BigDecimal.valueOf(steps.lastOrNull { it <= target } ?: steps.first())
}

fun stepNumber(type: KClass<*>, current: String, direction: Int, big: Boolean, min: Double? = null, max: Double? = null): String? {
    val whole = type == Int::class || type == Long::class
    if (!whole && type != Float::class && type != Double::class) return null

    val start = current.toBigDecimalOrNull() ?: BigDecimal.ZERO
    var next = start + stepFor(whole, min, max) * BigDecimal(direction) * (if (big) BigDecimal.TEN else BigDecimal.ONE)
    if (min != null) next = next.max(BigDecimal.valueOf(min))
    if (max != null) next = next.min(BigDecimal.valueOf(max))

    val value: Any = when (type) {
        Int::class -> next.toInt()
        Long::class -> next.toLong()
        Float::class -> next.toFloat()
        else -> next.toDouble()
    }
    @Suppress("UNCHECKED_CAST")
    return (ArgumentTypes.of(type) as ArgumentType<Any>).format(value)
}

fun cycle(values: List<String>, current: String, direction: Int): String {
    if (values.isEmpty()) return current
    val index = values.indexOfFirst { it.equals(current, true) }
    return when {
        index >= 0 -> values[Math.floorMod(index + direction, values.size)]
        direction > 0 -> values.first()
        else -> values.last()
    }
}
