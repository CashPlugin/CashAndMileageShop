package com.uomaep.cashandmileageshop

import com.uomaep.cashandmileageshop.commands.CashCommand
import com.uomaep.cashandmileageshop.commands.SetItemCommand
import com.uomaep.cashandmileageshop.listeners.SetUserAtFirstJoin
import com.uomaep.cashandmileageshop.utils.DatabaseManager
import com.uomaep.mileageandmileageshop.commands.MileageCommand
import org.bukkit.plugin.java.JavaPlugin

class Main : JavaPlugin() {
    override fun onEnable() {
        // Plugin startup logic
        createPluginFolder()

        DatabaseManager.connect()

        // 명령어 등록
        getCommand("캐시")?.setExecutor(CashCommand())
        getCommand("마일리지")?.setExecutor(MileageCommand())
        getCommand("아이템등록")?.setExecutor(SetItemCommand())

        // 이벤트 등록
        server.pluginManager.registerEvents(SetUserAtFirstJoin(this), this)
    }

    override fun onDisable() {
        // Plugin shutdown logic

        DatabaseManager.disconnect()
    }

    fun createPluginFolder() {
        val pluginFolder = dataFolder
        if (!pluginFolder.exists()) {
            pluginFolder.mkdir()
        }
    }
}
