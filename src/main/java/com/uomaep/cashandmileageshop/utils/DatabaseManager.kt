package com.uomaep.cashandmileageshop.utils

import org.bukkit.Bukkit
import java.io.IOException
import java.io.InputStream
import java.sql.*
import java.util.*
import java.util.logging.Level

object DatabaseManager {
    private var con: Connection? = null

    fun connect(): Boolean {
        var properties: Properties?
        try {
            properties = PropertiesManager.getDatabaseProperties()
        } catch (e: NullPointerException) {
            Message.failureLogMessage(e.message ?: "알 수 없는 오류가 떴습니다.")
            return false
        } catch (e: Exception) {
            Message.failureLogMessage(e.message ?: "알 수 없는 오류가 떴습니다.")
            return false
        }

        val url = properties.getProperty("db.url")
        val user = properties.getProperty("db.user")
        val password = properties.getProperty("db.password")

        try {
            Class.forName("com.mysql.cj.jdbc.Driver")
            con = DriverManager.getConnection(url, user, password)
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
            return false
        } catch (e: SQLException) {
            e.printStackTrace()
            return false
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }

        return true
    }

    fun disconnect() {
        try {
            con?.close()
        } catch (e: SQLException) {
            e.printStackTrace()
        }
    }

    fun getConnection(): Connection? = con

    fun insert(statement: String): Boolean {
        val con = getConnection()
        if(con != null) {
            try {
                con.prepareStatement(statement).executeUpdate()
            } catch (e: SQLException) {
                Bukkit.getLogger().log(Level.SEVERE, "Error during INSERT operation", e)
                return false
            }
        } else {
            Bukkit.getLogger().severe("Connection is null")
            return false
        }
        return true
    }

    fun insertAndGetGeneratedKey(statement: String): Long? {
        val con = getConnection()
        if (con != null) {
            try {
                val preparedStatement = con.prepareStatement(statement, Statement.RETURN_GENERATED_KEYS)
                preparedStatement.executeUpdate()

                val generatedKeys = preparedStatement.generatedKeys
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1)
                }
            } catch (e: SQLException) {
                Bukkit.getLogger().log(Level.SEVERE, "Error during INSERT operation", e)
            }
        } else {
            Bukkit.getLogger().severe("Connection is null")
            return -1
        }
        return -1
    }



    fun select(statement: String): ResultSet? {
        val con = getConnection()
        if (con != null) {
            try {
                return con.prepareStatement(statement).executeQuery()
            } catch (e: SQLException) {
                Bukkit.getLogger().log(Level.SEVERE, "Error during SELECT operation", e)
                return null
            }
        } else {
            Bukkit.getLogger().severe("Connection is null")
        }
        return null
    }

    fun update(statement: String): Boolean {
        val con = getConnection()
        if (con != null) {
            try {
                con.prepareStatement(statement).executeUpdate()
            } catch (e: SQLException) {
                Bukkit.getLogger().log(Level.SEVERE, "Error during UPDATE operation", e)
                return false
            }
        } else {
            Bukkit.getLogger().severe("Connection is null")
            return false
        }
        return true
    }

    fun delete(statement: String): Boolean {
        val con = getConnection()
        if (con != null) {
            try {
                con.prepareStatement(statement).execute()
            } catch (e: SQLException) {
                Bukkit.getLogger().log(Level.SEVERE, "Error during DELETE operation", e)
                return false
            }
        } else {
            Bukkit.getLogger().severe("Connection is null")
            return false
        }
        return true
    }

    private fun loadDatabaseProperties(): Properties {
        val properties = Properties()
        try {
            val inputStream: InputStream? = javaClass.classLoader.getResourceAsStream("application-database.properties")
            inputStream?.use { properties.load(it) }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return properties
    }
}
