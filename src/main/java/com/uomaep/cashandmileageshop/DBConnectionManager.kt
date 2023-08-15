package com.uomaep.kotlintestplugin

import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.SQLException
import java.util.Properties

class DBConnectionManager {
    companion object {
        var con: Connection? = null

        fun connect() {
            val properties = Properties()

            val inputStream = javaClass.classLoader.getResourceAsStream("application-database.properties")
            properties.load(inputStream)
            inputStream.close()

            var url = properties.getProperty("db.url")
            val user = properties.getProperty("db.user")
            val password = properties.getProperty("db.password")

            try {
                Class.forName("com.mysql.cj.jdbc.Driver")
                con = DriverManager.getConnection(url, user, password)
            } catch (e: SQLException) {
                e.printStackTrace()
            }
        }

        fun disconnect() {
            try {
                con?.close()
            } catch (e: SQLException) {
                e.printStackTrace()
            }
        }

        fun getConnection(): Connection? {
            return con
        }

        fun insert(statement: String) {
            val con = getConnection()
            if(con != null) {
                try {
                    val stmt = con.prepareStatement(statement)
                    stmt.executeUpdate(statement)
                } catch (e: SQLException) {
                    throw SQLException(e.message)
                }
            } else {
                throw SQLException("Connection is null")
            }
        }

        fun select(statement: String): ResultSet? {
            val con = getConnection()
            var resultSet: ResultSet? = null
            if(con != null) {
                try {
                    val stmt = con.prepareStatement(statement)
                    resultSet = stmt.executeQuery(statement)
                } catch (e: SQLException) {
                    throw SQLException(e.message)
                }
                return resultSet
            } else {
                throw SQLException("Connection is null")
            }
        }

        fun update(statement: String) {
            val con = getConnection()
            if(con != null) {
                try {
                    val stmt = con.prepareStatement(statement)
                    stmt.executeUpdate(statement)
                } catch (e: SQLException) {
                    throw SQLException(e.message)
                }
            } else {
                throw SQLException("Connection is null")
            }
        }
    }
}