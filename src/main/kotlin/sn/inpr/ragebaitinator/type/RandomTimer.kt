package sn.inpr.ragebaitinator.type

import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitTask
import sn.inpr.ragebaitinator.utility.MathUtil

class RandomTimer(
    private val plugin: Plugin,
    private val runnable: Runnable,
    private val minTicks: Long,
    private val maxTicks: Long,
    delay: Long
) : Runnable, BukkitTask {
    @Volatile private var task: BukkitTask
    private val lock = Any()
    @Volatile private var cancelled = false

    init {
        task = Bukkit.getScheduler().runTaskLater(plugin, this, delay)
    }

    override fun run() {
        runnable.run()

        synchronized(lock) {
            if (!cancelled) {
                task = Bukkit.getScheduler().runTaskLater(plugin, this, MathUtil.random(minTicks, maxTicks))
            }
        }
    }

    override fun getTaskId(): Int {
        return task.taskId
    }

    override fun getOwner(): Plugin {
        return task.owner
    }

    override fun isSync(): Boolean {
        return task.isSync
    }

    override fun isCancelled(): Boolean {
        return cancelled || task.isCancelled
    }

    override fun cancel() {
        synchronized(lock) {
            cancelled = true
            task.cancel()
        }
    }
}