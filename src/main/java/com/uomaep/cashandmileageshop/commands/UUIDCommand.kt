package com.uomaep.cashandmileageshop.commands

import com.hj.rpgsharp.rpg.apis.rpgsharp.RPGSharpAPI
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.logging.Level

class UUIDCommand: CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>?): Boolean {
        val player = sender as Player
        if(!player.isOp) {
            player.sendMessage("\"Unknown command. Type \\\"/help\\\" for help.\"")
            return false
        }
        if(args?.size != 1) {
            player.sendMessage("/us <nickname>")
            return false
        }
        val nickname = args[0]
        val uuid = RPGSharpAPI.getRPGPlayerAPI().getUUID(nickname)

        if(uuid == null) {
            player.sendMessage("§e${nickname}§f님의 UUID를 찾을 수 없습니다.")
            return false
        }

        player.sendMessage("§e${nickname}§f님의 UUID는 §e${uuid}§f입니다.")

        Bukkit.getLogger().log(Level.INFO, String.format("%s's uuid = %s", nickname, uuid));
        Bukkit.getLogger().log(Level.INFO, String.format("%s's uuid = %s", nickname, uuid));
        Bukkit.getLogger().log(Level.INFO, String.format("%s's uuid = %s", nickname, uuid));
        return true
    }
}