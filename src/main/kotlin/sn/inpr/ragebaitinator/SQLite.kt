package sn.inpr.ragebaitinator

import sn.inpr.ragebaitinator.Main.Companion.runTaskAsync
import org.bukkit.permissions.Permissible
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.SQLException

object SQLite {
    var connection: Connection? = null

    init {
        connect()
    }

    fun connect(): Connection? {
        if (connection != null) return connection

        try {
            Class.forName("org.sqlite.JDBC")

            synchronized(SQLite) {
                // create directories if they are non-existent
                val file = File("plugins/Ragebaitinator")
                if (!file.exists()) file.mkdirs()

                connection = DriverManager.getConnection("jdbc:sqlite:plugins/Ragebaitinator/sqlite.db")

                runCatching {
                    val statement = connection?.createStatement() ?: return@runCatching
                    statement.executeUpdate("PRAGMA synchronous = OFF")
                    statement.executeUpdate("PRAGMA journal_mode = MEMORY")
                    statement.executeUpdate("PRAGMA temp_store = MEMORY")
                }

                CommandHistory.initialize()

                return connection
            }
        }
        catch (exception: SQLException) { exception.printStackTrace() }
        catch (exception: ClassNotFoundException) { exception.printStackTrace() }

        return null
    }

    fun perform(statement: String): Boolean {
        try {
            val preparedStatement = connection?.prepareStatement(statement) ?: return false
            return preparedStatement.execute()
        } catch (exception: SQLException) {
            exception.printStackTrace()
            return false
        }
    }

    fun execute(statement: String): ResultSet? {
        try {
            val preparedStatement = connection?.prepareStatement(statement) ?: return null
            return preparedStatement.executeQuery()
        } catch (exception: SQLException) {
            exception.printStackTrace()
            return null
        }
    }

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
}