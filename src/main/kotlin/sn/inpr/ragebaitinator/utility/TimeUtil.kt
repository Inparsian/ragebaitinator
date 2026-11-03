package sn.inpr.ragebaitinator.utility

import java.lang.management.ManagementFactory
import java.lang.management.ThreadMXBean
import java.text.SimpleDateFormat
import java.util.Date

object TimeUtil {
    private const val MINUTE_NANOS: Long = 60_000_000_000L
    private const val HOUR_NANOS: Long = 3_600_000_000_000L
    private const val DAY_NANOS: Long = 86_400_000_000_000L

    private val bean: ThreadMXBean get() = ManagementFactory.getThreadMXBean()
    val cpuTimeSupported: Boolean get() = bean.isCurrentThreadCpuTimeSupported
    /**
     * A CPU time value that will fall back to nano time if the JVM does not support CPU time
     */
    val cpuTime: Long get() = if (cpuTimeSupported) bean.currentThreadCpuTime else System.nanoTime()

    fun formatNano(nanoTime: Long): String = String.format("%s%s%s%s",
        if (nanoTime >= DAY_NANOS) "${nanoTime / DAY_NANOS}d " else "",
        if (nanoTime >= HOUR_NANOS) "${(nanoTime / HOUR_NANOS) % 24}h " else "",
        if (nanoTime >= MINUTE_NANOS) "${(nanoTime / MINUTE_NANOS) % 60}m " else "",
        formatNanoInSeconds(nanoTime)
    )

    fun formatNanoInSeconds(nanoTime: Long): String = when {
        nanoTime >= 1_000_000_000 -> "${nanoTime / 1_000_000_000}s"
        nanoTime >= 1_000_000 -> "${nanoTime / 1_000_000}ms"
        nanoTime >= 1_000 -> "${nanoTime / 1_000}us"
        else -> "0ns"
    }

    fun epochToDate(epoch: Long): String = SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(Date(epoch))

    /**
     * Returns the time in nanoseconds it took to run the given Runnable
     */
    fun timeTask(runnable: Runnable): Long {
        val preRun = cpuTime
        runnable.run()
        return cpuTime - preRun
    }
}