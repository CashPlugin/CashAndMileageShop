package com.uomaep.cashandmileageshop.commands

import com.uomaep.cashandmileageshop.DTO.CashItemDTO
import com.uomaep.cashandmileageshop.DTO.CashShopDTO
import com.uomaep.cashandmileageshop.utils.DatabaseManager
import com.uomaep.kotlintestplugin.utils.ItemUtil
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack

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

        openCashShop(sender, cashShopDTO)
        return true
    }
}

fun openCashShop(sender: CommandSender, cashShopDTO: CashShopDTO) {
    val cashShop = Bukkit.createInventory(sender as InventoryHolder, cashShopDTO.lineNum * 9, cashShopDTO.name)
    val sql = "select * from (select * from cash_item where cash_shop_id = ${cashShopDTO.id} and state = 1) as cashItems join item on item.id = cashItems.item_id"
    val result = DatabaseManager.select(sql)!!

    while (result.next()) {
        val cashItemDTO = CashItemDTO(
            cashItemId = result.getInt("cashItems.id"),
            maxBuyableCnt = result.getInt("max_buyable_cnt"),
            price = result.getInt("price"),
            itemId = result.getInt("item_id"),
            cashShopId = result.getInt("cash_shop_id"),
            maxBuyableCntServer = result.getInt("max_buyable_cnt_server"),
            slotNum = result.getInt("slot_num"),
            state = result.getInt("state"),
            itemInfo = result.getString("\u202Aitem_info"),
            name = result.getString("name")
        )
        val item: ItemStack = ItemUtil.deserialize(cashItemDTO.itemInfo)
        cashShop.setItem(cashItemDTO.slotNum, item)
    }

    val player = sender as Player
    player.openInventory(cashShop)
}
