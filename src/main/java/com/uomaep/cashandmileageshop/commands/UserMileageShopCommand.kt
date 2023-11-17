package com.uomaep.mileageandmileageshop.commands

import com.uomaep.cashandmileageshop.dto.MileageShopDTO
import com.uomaep.cashandmileageshop.utils.DatabaseManager
import com.uomaep.mileageandmileageshop.guis.MileageShopGUI
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class UserMileageShopCommand : CommandExecutor, TabCompleter {
    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>?
    ): List<String> {
        if (args!!.size == 1) {
            val sql = "SELECT * FROM mileage_shop WHERE state = 2;"
            val result = DatabaseManager.select(sql)!!
            val list = mutableListOf<String>()

            while (result.next()) {
                list.add(result.getString("name"))
            }

            return list
        }
        return listOf()
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>?): Boolean {
        if (args!!.isEmpty()) {
            sender.sendMessage("§e/마일리지샵열기 <상점이름>")
            return false
        }

        val shopName = args[0]
        val sql = "SELECT * FROM mileage_shop WHERE name = '${shopName}' and state = 2"
        val result = DatabaseManager.select(sql)!!

        if (!result.next()) {
            sender.sendMessage("§e${shopName}§f이라는 이름의 마일리지샵이 존재하지 않습니다.")
            return false
        }

        val mileageShopDTO = MileageShopDTO(
            result.getInt("id"),
            result.getString("name"),
            result.getInt("line_num"),
            result.getInt("state")
        )

        val mileageShopGUI = MileageShopGUI(mileageShopDTO, sender)
        (sender as Player).openInventory(mileageShopGUI.inventory)
        return true
    }
}
