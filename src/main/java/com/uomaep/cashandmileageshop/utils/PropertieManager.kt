package com.uomaep.cashandmileageshop.utils

import com.uomaep.cashandmileageshop.Main
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.util.*

object PropertieManager {

    fun getDatabaseProperties(): Properties {
        val configFile = File(Main.pluginFolder, "config.yml")
        val config = YamlConfiguration.loadConfiguration(configFile)
        val properties = Properties()

        val databaseConfig = config.getConfigurationSection("database")
        if(databaseConfig == null) {
            val databaseSection = config.createSection("database")
            hashMapOf(
                "url" to "255.255.255.255",
                "port" to "3389",
                "database-name" to "serverDB",
                "user" to "test",
                "password" to "1234",
            ).forEach { (key, value) -> databaseSection.set(key, value) }
            try {
                config.save(configFile)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            throw NullPointerException("config.yml 파일에 database 섹션을 추가해주세요.")
        }

        val url = databaseConfig.getString("url")
        val port = databaseConfig.getString("port")
        val database = databaseConfig.getString("database-name")
        val user = databaseConfig.getString("user")
        val password = databaseConfig.getString("password")

        if(url == null || port == null || database == null || user == null || password == null) {
            throw NullPointerException("config.yml 파일에 database 섹션에 url, port, database-name, user, password를 추가해주세요.")
        }

        properties.setProperty("db.url", "jdbc:mysql://$url:$port/$database")
        properties.setProperty("db.user", user)
        properties.setProperty("db.password", password)

        return properties
    }
}