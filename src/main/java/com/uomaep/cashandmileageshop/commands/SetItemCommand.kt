package com.uomaep.cashandmileageshop.commands

import com.uomaep.cashandmileageshop.utils.DatabaseManager
import com.uomaep.cashandmileageshop.utils.ItemUtil
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class SetItemCommand : CommandExecutor {
    companion object {
        private fun sendFailMessage(player: Player, message: String) {
            player.sendMessage("§c[§e!§c] $message")
        }

        private fun sendSuccessMessage(player: Player, message: String) {
            player.sendMessage("§a[§e!§a] $message")
        }
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>?): Boolean {
        val player = sender as Player

        if (!player.isOp) {
            player.sendMessage("Unknown command. Type \"/help\" for help.")
            return false
        }

        if (args?.size == 0) {
            sendFailMessage(player, "아이템 이름을 입력해주세요.")
            return false
        }

        val item = player.inventory.itemInMainHand

        if (item.type == Material.AIR) {
            sendFailMessage(player, "손에 아이템을 들고 명령어를 사용해주세요.")
            return false
        }

        val itemName = args?.get(0)
        val serializedItem = ItemUtil.serialize(item)

        if (!DatabaseManager.insert("insert into item (name, `\u202Aitem_info`) values('${itemName}', '${serializedItem}');")) {
            sendFailMessage(player, "이미 등록된 이름의 아이템 이거나 등록 중 문제가 발생했습니다.")
            return false
        }

        sendSuccessMessage(player, "아이템이 등록되었습니다.")
        return true
    }
}
