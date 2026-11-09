@file:Suppress("UsePropertyAccessSyntax")

package sn.inpr.ragebaitinator

import com.github.retrooper.packetevents.PacketEvents
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.plugin.java.JavaPlugin
import sn.inpr.ragebaitinator.command.CommandManager
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.database.Blocks
import sn.inpr.ragebaitinator.database.SQLite
import sn.inpr.ragebaitinator.gui.GuiHandler
import sn.inpr.ragebaitinator.gui.def.Menu
import sn.inpr.ragebaitinator.integrations.PAPI
import sn.inpr.ragebaitinator.integrations.PAPIExpansion
import sn.inpr.ragebaitinator.listeners.BukkitEventListener
import sn.inpr.ragebaitinator.listeners.PacketListener
import sn.inpr.ragebaitinator.type.RandomTimer
import sn.inpr.ragebaitinator.utility.LogUtil.info
import sn.inpr.ragebaitinator.utility.TimeUtil

class Main : JavaPlugin() {
    companion object {
        var configuration = Config()
        var language: LanguageConfig = LanguageConfig()

        fun instance() = getPlugin(Main::class.java)

        fun makeKey(key: String) = NamespacedKey(instance(), key)

        fun runTask(runnable: Runnable)
        = instance().let { Bukkit.getScheduler().runTask(it, runnable) }

        fun runTaskAsync(runnable: Runnable)
        = instance().let { Bukkit.getScheduler().runTaskAsynchronously(it, runnable) }

        fun runTaskIfNotPrimaryThread(runnable: Runnable)
        = if (!Bukkit.isPrimaryThread()) runTask(runnable) else runnable.run()

        fun runTaskLater(runnable: Runnable, delay: Long)
        = instance().let { Bukkit.getScheduler().runTaskLater(it, runnable, delay) }

        fun runTaskTimer(runnable: Runnable, delay: Long, period: Long)
        = instance().let { Bukkit.getScheduler().runTaskTimer(it, runnable, delay, period) }

        fun runTaskRandomTimer(runnable: Runnable, delay: Long, minTicks: Long, maxTicks: Long)
        = RandomTimer(instance(), runnable, minTicks, maxTicks, delay)
    }

    override fun onLoad() {
        configuration = Config("config.yml")
        language = LanguageConfig("language.yml")
        PacketEvents.getAPI().eventManager.registerListener(PacketListener())
    }

    override fun onEnable() {
        if (!TimeUtil.cpuTimeSupported) {
            info("&6CPU time is not supported on this system, timing may be inaccurate.")
        }

        val time: String = TimeUtil.formatNano(TimeUtil.timeTask {
            synchronized(this) {
                Bukkit.getPluginManager().registerEvents(BukkitEventListener, this)
                Bukkit.getPluginManager().registerEvents(GuiHandler, this)

                if (SQLite.connect() != null)
                    info("&aSQLite connection established")
                else
                    info("&cFailed to connect to SQLite database")

                if (PAPI.present) {
                    PAPIExpansion().register()
                    info("&aPlaceholderAPI integration complete")
                }

                getCommand("ragebaitinator")?.setExecutor(CommandManager)
                CommandManager.registerCommands()
            }
        })

        info("&aPlugin initialized in $time")
    }

    override fun onDisable() {
        for (command in CommandManager.commands) {
            if (command is BaseToggleTroll)
                command.clearPlayers()
            command.onDisable()
        }

        Blocks.shutdown()

        // close all menus
        for (player in Bukkit.getOnlinePlayers()) {
            if (player.openInventory.topInventory.holder is Menu)
                player.closeInventory()
        }

        info("Goodbye!")
    }
}