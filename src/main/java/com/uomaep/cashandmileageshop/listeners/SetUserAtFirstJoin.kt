package com.uomaep.cashandmileageshop.listeners

import com.uomaep.cashandmileageshop.utils.DatabaseManager
import org.bukkit.Bukkit
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.io.IOException
import java.time.LocalDateTime

class SetUserAtFirstJoin(val plugin: JavaPlugin): Listener {

    @EventHandler
    fun setUserToDatabase(e: PlayerJoinEvent) {
        val player = e.player
        if(player.hasPlayedBefore()) return

        if(!DatabaseManager.insert("insert into user (uuid, cash, mileage) values ('${player.uniqueId}', 0, 0);")) {
            Bukkit.getLogger().severe("데이터베이스에 새로운 플레이어의 정보를 추가하는데 실패했습니다.")
            if(!saveFailedUserDataToFile(player)) {
                Bukkit.getLogger().severe("플레이어의 정보를 파일에 저장하는데 실패했습니다.")
            }
        }
    }

    private fun saveFailedUserDataToFile(player: Player): Boolean {
        val fileName = "failedUserData.yml"
        val configFile = File(plugin.dataFolder, fileName)
        val config = YamlConfiguration()

        val failedUserData = hashMapOf(
            "nickname" to player.name,
            "uuid" to player.uniqueId.toString(),
            "timestamp" to LocalDateTime.now()
        )

        var userCount = 1
        while(config.contains(userCount.toString())) userCount++

        val userSection = config.createSection(userCount.toString())

        failedUserData.forEach { (key, value) ->
            userSection.set(key, value)
        }

        try {
            config.save(configFile)
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }

        return true
    }
}