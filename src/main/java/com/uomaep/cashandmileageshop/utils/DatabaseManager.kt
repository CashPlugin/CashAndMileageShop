package com.uomaep.cashandmileageshop.utils

import org.bukkit.Bukkit
import java.io.IOException
import java.io.InputStream
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.SQLException
import java.util.*
import java.util.logging.Level

object DatabaseManager {
    private var con: Connection? = null

    fun connect(): Boolean {
        var properties: Properties? = null
        try {
            properties = PropertieManager.getDatabaseProperties()
        } catch (e: NullPointerException) {
            Bukkit.getLogger().severe(e.message)
            return false
        } catch (e: Exception) {
            Bukkit.getLogger().severe(e.message)
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