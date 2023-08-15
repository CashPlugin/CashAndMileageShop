package com.uomaep.kotlintestplugin

import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.SQLException
import java.sql.Statement
import java.util.Properties

class DatabaseConfig{
    var url: String
    var user: String
    var password: String

    init {
        val properties = Properties()

        try {
            val inputStream = javaClass.classLoader.getResourceAsStream("application-database.properties")
            properties.load(inputStream)
            inputStream.close()

            url = properties.getProperty("db.url")
            user = properties.getProperty("db.user")
            password = properties.getProperty("db.password")
        } catch (e: Exception) {
            e.printStackTrace()
            // 파일을 읽을 수 없거나 예외가 발생했을 경우 기본 값을 설정하거나 오류 처리를 할 수 있습니다.
            url = "default-url"
            user = "default-user"
            password = "default-password"
        }
    }

    companion object {
        var con: Connection? = null

        fun connectToMySQL(): Connection {
            val properties = Properties()

            val url = properties.getProperty("db.url")
            val user = properties.getProperty("db.user")
            val password = properties.getProperty("db.password")
            try {
                Class.forName("com.mysql.cj.jdbc.Driver")
                con = DriverManager.getConnection(url, user, password)
            } catch (e: SQLException) {
                e.printStackTrace()
            }

            return con!!
        }
    }
}
