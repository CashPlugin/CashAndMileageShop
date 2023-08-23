package com.uomaep.cashandmileageshop.commands

import com.uomaep.cashandmileageshop.DTO.CashShopDTO
import com.uomaep.cashandmileageshop.guis.CashShopGUI
import com.uomaep.cashandmileageshop.utils.DatabaseManager
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class UserCashShopCommand: CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>?): Boolean {
        if (sender !is Player || !sender.isOp) {
            sender.sendMessage("비정상적인 접근입니다.")
            return false
        }

        if (args!!.isEmpty()){
            sender.sendMessage("§e/캐시샵열기 <상점이름>")
            return false
        }

        val shopName = args[0]
        val sql = "SELECT * FROM cash_shop WHERE name = '${shopName}' and state = 2"
        val result = DatabaseManager.select(sql)!!

        if (!result.next()) {
            sender.sendMessage("§e${shopName}§f이라는 이름의 캐시샵이 존재하지 않습니다.")
            return false
        }

        val cashShopDTO = CashShopDTO(
            result.getInt("id"),
            result.getString("name"),
            result.getInt("line_num"),
            result.getInt("state")
        )

        val cashShopGUI = CashShopGUI(cashShopDTO, sender)
        (sender as Player).openInventory(cashShopGUI.inventory)
        return true
    }
}