package sn.inpr.ragebaitinator.database

import org.bukkit.permissions.Permissible
import sn.inpr.ragebaitinator.Main.Companion.runTaskAsync
import sn.inpr.ragebaitinator.database.SQLite.execute
import sn.inpr.ragebaitinator.database.SQLite.perform

object CommandHistory {
    data class CommandHistoryEntry(val playerName: String, val command: String, val timestamp: Long)

    private val history: MutableList<CommandHistoryEntry> = mutableListOf()
    val size: Int get() = history.size

    fun hasHistoryAccess(permissible: Permissible): Boolean = permissible.hasPermission("ragebaitinator.commands.history")

    fun initialize() {
        // assert table exists
        perform("CREATE TABLE IF NOT EXISTS command_history (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "player TEXT NOT NULL," +
                "command VARCHAR(255) NOT NULL," +
                "timestamp INTEGER NOT NULL" +
                ");")
        runTaskAsync(::sync)
    }

    @Synchronized
    fun sync() {
        val resultSet = execute("SELECT * FROM command_history") ?: return
        while (resultSet.next()) {
            history.add(CommandHistoryEntry(
                playerName = resultSet.getString("player"),
                command = resultSet.getString("command"),
                timestamp = resultSet.getLong("timestamp")
            ))
        }
        history.reverse()
    }

    @Synchronized
    fun addCommand(playerName: String, command: String) {
        perform("INSERT INTO command_history (player, command, timestamp) VALUES ('$playerName', '$command', ${System.currentTimeMillis()});")
        history.addFirst(CommandHistoryEntry(playerName, command, System.currentTimeMillis()))
    }

    // '!!' is console
    @Synchronized
    fun addConsoleCommand(command: String) = addCommand("!!", command)

    fun getHistory(forPlayerName: String): List<CommandHistoryEntry> = history.filter { it.playerName == forPlayerName }
    fun getConsoleHistory() = getHistory("!!")
    fun getAllHistory(): List<CommandHistoryEntry> = history.toList()
}