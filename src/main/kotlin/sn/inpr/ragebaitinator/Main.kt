@file:Suppress("UsePropertyAccessSyntax")

package sn.inpr.ragebaitinator

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketListenerPriority
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.java.JavaPlugin
import sn.inpr.ragebaitinator.command.CommandManager
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.gui.GuiHandler
import sn.inpr.ragebaitinator.gui.Menu
import sn.inpr.ragebaitinator.listeners.BukkitEventListener
import sn.inpr.ragebaitinator.listeners.PacketListener
import sn.inpr.ragebaitinator.type.RandomTimer
import sn.inpr.ragebaitinator.utility.LogUtil.info
import sn.inpr.ragebaitinator.utility.TimeUtil

class Main : JavaPlugin() {
    companion object {
        var plugin: Plugin? = null

        fun makeKey(key: String) = NamespacedKey(plugin!!, key)

        fun runTask(runnable: Runnable)
        = plugin!!.let { Bukkit.getScheduler().runTask(it, runnable) }

        fun runTaskIfNotPrimaryThread(runnable: Runnable)
        = if (!Bukkit.isPrimaryThread()) runTask(runnable) else runnable.run()

        fun runTaskLater(runnable: Runnable, delay: Long)
        = plugin!!.let { Bukkit.getScheduler().runTaskLater(it, runnable, delay) }

        fun runTaskLaterAsync(runnable: Runnable, delay: Long)
        = plugin!!.let { Bukkit.getScheduler().runTaskLaterAsynchronously(it, runnable, delay) }

        fun runTaskTimer(runnable: Runnable, delay: Long, period: Long)
        = plugin!!.let { Bukkit.getScheduler().runTaskTimer(it, runnable, delay, period) }

        fun runTaskTimerAsync(runnable: Runnable, delay: Long, period: Long)
        = plugin!!.let { Bukkit.getScheduler().runTaskTimerAsynchronously(it, runnable, delay, period) }

        fun runTaskRandomTimer(runnable: Runnable, delay: Long, minTicks: Long, maxTicks: Long)
        = RandomTimer(plugin!!, runnable, minTicks, maxTicks, delay)
    }

    override fun onLoad() {
        plugin = this
        val events = PacketEvents.getAPI().eventManager
        events.registerListener(PacketListener(), PacketListenerPriority.HIGH)
    }

    override fun onEnable() {
        if (!TimeUtil.cpuTimeSupported) {
            info("&6CPU time is not supported on this system, timing may be inaccurate.")
        }

        val time: String = TimeUtil.formatNano(TimeUtil.timeTask({
            synchronized(this) {
                Bukkit.getPluginManager().registerEvents(BukkitEventListener, this)
                Bukkit.getPluginManager().registerEvents(GuiHandler, this)

                getCommand("ragebaitinator")?.setExecutor(CommandManager)
                CommandManager.registerCommands()
            }
        }))

        info("&aPlugin initialized in $time")
    }

    override fun onDisable() {
        for (command in CommandManager.commands) {
            if (command is BaseToggleTroll)
                command.clearPlayers()
            command.onDisable()
        }

        // close all menus
        for (player in Bukkit.getOnlinePlayers()) {
            if (player.openInventory.topInventory.holder is Menu)
                player.closeInventory()
        }

        info("Goodbye!")
    }
}
