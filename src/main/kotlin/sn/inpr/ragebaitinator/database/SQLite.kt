package sn.inpr.ragebaitinator.database

import org.bukkit.Bukkit
import sn.inpr.ragebaitinator.Main
import java.io.File
import java.sql.*

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

                connection = open()

                CommandHistory.initialize()
                // we attempt to init Blocks during the plugin's enable phase
                Bukkit.getPluginManager().registerEvents(Blocks, Main.instance())
                Blocks.initialize()

                return connection
            }
        }
        catch (exception: SQLException) { exception.printStackTrace() }
        catch (exception: ClassNotFoundException) { exception.printStackTrace() }

        return null
    }

    fun open(): Connection {
        val connection = DriverManager.getConnection("jdbc:sqlite:plugins/Ragebaitinator/sqlite.db")

        runCatching {
            connection.createStatement().use { statement ->
                statement.execute("PRAGMA journal_mode = WAL")
                statement.execute("PRAGMA synchronous = NORMAL")
                statement.execute("PRAGMA temp_store = MEMORY")
            }
        }.onFailure { it.printStackTrace() }

        return connection
    }

    fun prepare(statement: String, callback: (PreparedStatement) -> Unit): Unit = transaction { connection ->
        connection.prepareStatement(statement).use { callback(it) }
    }

    /** One shot for simple op */
    fun perform(statement: String): Boolean {
        try {
            return connection?.prepareStatement(statement)?.use { it.execute() } ?: false
        } catch (exception: SQLException) {
            exception.printStackTrace()
            return false
        }
    }

    fun execute(statement: String): ResultSet? {
        return try {
            connection?.prepareStatement(statement)?.executeQuery()
        } catch (exception: SQLException) {
            exception.printStackTrace()
            null
        }
    }

    fun transaction(body: (Connection) -> Unit) {
        val connection = connection ?: return
        try {
            connection.autoCommit = false
            body(connection)
            connection.commit()
        } catch (exception: SQLException) {
            exception.printStackTrace()
            runCatching { connection.rollback() }.onFailure { it.printStackTrace() }
        } finally {
            runCatching { connection.autoCommit = true }.onFailure { it.printStackTrace() }
        }
    }
}