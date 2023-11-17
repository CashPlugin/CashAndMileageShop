package com.uomaep.cashandmileageshop.utils

import com.uomaep.cashandmileageshop.Main
import com.uomaep.cashandmileageshop.guis.CashShopGUI
import com.uomaep.mileageandmileageshop.guis.MileageShopGUI
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.util.*

object PropertiesManager {
    fun getCustomItemInfoProperties() {
        val configFile = File(Main.pluginFolder, "config.yml")
        val config = YamlConfiguration.loadConfiguration(configFile)

        val customItemInfoConfig = config.getConfigurationSection("custom_item_info")
        if (customItemInfoConfig == null) {
            val customItemInfoSection = config.createSection("custom_item_info")
            hashMapOf(
                "price" to "§f가격: §e%price% §f캐시",
                "server_limited" to "§f서버 한정: §e%server_remain%/%server_purchases_limited%",
                "user_limited" to "§f개인 한정: §e%user_remain%/%user_purchases_limited%"
            ).forEach { (key, value) -> customItemInfoSection.set(key, value) }
            try {
                config.save(configFile)
            } catch (e: Exception) {
                e.printStackTrace()
            }
//            throw IllegalArgumentException("config.yml 파일에 custom_item_info 섹션을 추가해주세요.")
            return
        }
        CashShopGUI.PRICE = customItemInfoConfig.getString("price")!!
        CashShopGUI.SERVER_LIMITED = customItemInfoConfig.getString("server_limited")!!
        CashShopGUI.USER_LIMITED = customItemInfoConfig.getString("user_limited")!!
        MileageShopGUI.PRICE = customItemInfoConfig.getString("price")!!
        MileageShopGUI.SERVER_LIMITED = customItemInfoConfig.getString("server_limited")!!
        MileageShopGUI.USER_LIMITED = customItemInfoConfig.getString("user_limited")!!
    }

    fun getServerCustomMessageProperties() {
        val configFile = File(Main.pluginFolder, "config.yml")
        val config = YamlConfiguration.loadConfiguration(configFile)

        val serverCustomMessageConfig = config.getConfigurationSection("server_custom_message")
        if (serverCustomMessageConfig == null) {
            val serverCustomMessageSection = config.createSection("server_custom_message")
            hashMapOf(
                "ServerErrorMessageHeader" to "§4",
                "ServerSuccessMessageHeader" to "§2"
            ).forEach { (key, value) -> serverCustomMessageSection.set(key, value) }
            try {
                config.save(configFile)
            } catch (e: Exception) {
                e.printStackTrace()
            }
//            throw IllegalArgumentException("config.yml 파일에 server_custom_message 섹션을 추가해주세요.")
            return
        }
        Message.successfulLogMessageHeader = serverCustomMessageConfig.getString("ServerSuccessMessageHeader")!!
        Message.failureLogMessageHeader = serverCustomMessageConfig.getString("ServerErrorMessageHeader")!!
    }

    fun getUserCustomMessageProperties() {
        val configFile = File(Main.pluginFolder, "config.yml")
        val config = YamlConfiguration.loadConfiguration(configFile)

        val userCustomMessageConfig = config.getConfigurationSection("user_custom_message")
        if (userCustomMessageConfig == null) {
            val userCustomMessageSection = config.createSection("user_custom_message")
            hashMapOf(
                "sendFailMessageHeader" to "§c◇§f",
                "sendSuccessMessageHeader" to "§a◇§f"
            ).forEach { (key, value) -> userCustomMessageSection.set(key, value) }
            try {
                config.save(configFile)
            } catch (e: Exception) {
                e.printStackTrace()
            }
//            throw IllegalArgumentException("config.yml 파일에 user_custom_message 섹션을 추가해주세요.")
            return
        }
        UserMessage.successMessageHeader = userCustomMessageConfig.getString("sendSuccessMessageHeader")!!
        UserMessage.failMessageHeader = userCustomMessageConfig.getString("sendFailMessageHeader")!!
    }

    fun getDatabaseProperties(): Properties {
        val configFile = File(Main.pluginFolder, "config.yml")
        val config = YamlConfiguration.loadConfiguration(configFile)
        val properties = Properties()

        val databaseConfig = config.getConfigurationSection("database")
        if (databaseConfig == null) {
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

        if (url == null || port == null || database == null || user == null || password == null) {
            throw NullPointerException("config.yml 파일에 database 섹션에 url, port, database-name, user, password를 추가해주세요.")
        }

        properties.setProperty("db.url", "jdbc:mysql://$url:$port/$database?autoReconnect=true")
        properties.setProperty("db.user", user)
        properties.setProperty("db.password", password)

        return properties
    }
}
