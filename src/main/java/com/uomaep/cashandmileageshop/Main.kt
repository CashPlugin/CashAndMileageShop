package com.uomaep.cashandmileageshop

import com.uomaep.cashandmileageshop.commands.CashCommand
import com.uomaep.cashandmileageshop.commands.SetItemCommand
import com.uomaep.cashandmileageshop.commands.UUIDCommand
import com.uomaep.cashandmileageshop.commands.UserCashShopCommand
import com.uomaep.cashandmileageshop.listeners.SetUserAtFirstJoin
import com.uomaep.cashandmileageshop.utils.DatabaseManager
import com.uomaep.cashandmileageshop.utils.Message
import com.uomaep.kotlintestplugin.command.CashShopCommand
import com.uomaep.kotlintestplugin.command.MileageShopCommand
import com.uomaep.mileageandmileageshop.commands.MileageCommand
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class Main : JavaPlugin() {
    companion object {
        var pluginFolder: File? = null
    }

    override fun onEnable() {
        // Plugin startup logic
        createPluginFolder()

        if(DatabaseManager.connect()) {
            Message.successfulLogMessage("데이터베이스 연결에 성공했습니다.")
        } else {
            Message.failureLogMessage("데이터베이스 연결에 실패했습니다.")
            Message.failureLogMessage("플러그인을 종료합니다.")
            server.pluginManager.disablePlugin(this)
        }

        // 명령어 등록
        getCommand("캐시")?.setExecutor(CashCommand())
        getCommand("마일리지")?.setExecutor(MileageCommand())
        getCommand("아이템등록")?.setExecutor(SetItemCommand())
        getCommand("us")?.setExecutor(UUIDCommand())
        getCommand("캐시샵")?.setExecutor(CashShopCommand())
        getCommand("마일리지샵")?.setExecutor(MileageShopCommand())

        getCommand("캐시샵열기")?.setExecutor(UserCashShopCommand())

        // 이벤트 등록
        server.pluginManager.registerEvents(SetUserAtFirstJoin(), this)
    }

    override fun onDisable() {
        // Plugin shutdown logic

        DatabaseManager.disconnect()
    }

    fun createPluginFolder() {
        if(pluginFolder == null) {
            val folder = dataFolder
            if (!folder.exists()) {
                folder.mkdir()
            }
            pluginFolder = folder
        }
    }
}
