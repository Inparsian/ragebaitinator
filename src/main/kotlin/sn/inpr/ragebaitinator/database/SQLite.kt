package sn.inpr.ragebaitinator.database

import org.bukkit.Bukkit
import sn.inpr.ragebaitinator.Main
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