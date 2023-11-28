package com.uomaep.cashandmileageshop.guis

import com.uomaep.cashandmileageshop.dto.CashItemDTO
import com.uomaep.cashandmileageshop.dto.CashShopDTO
import com.uomaep.cashandmileageshop.utils.DatabaseManager
import com.uomaep.cashandmileageshop.utils.ItemUtil
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack

class CashShopGUI : InventoryHolder {
    val name: String
    private val inventory: Inventory

    companion object {
        lateinit var PRICE: String
        lateinit var SERVER_LIMITED: String
        lateinit var USER_LIMITED: String
    }

    constructor(cashShopDTO: CashShopDTO, sender: CommandSender) {
        this.name = cashShopDTO.name
        this.inventory = Bukkit.createInventory(this, cashShopDTO.lineNum * 9, name)

        val sql =
            "select * from (select * from cash_item where cash_shop_id = ${cashShopDTO.id} and state = 1) as cashItems join item on item.id = cashItems.item_id"
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

            val lore = item.itemMeta.lore ?: mutableListOf()
            val infoLore = mutableListOf(
                "",
                "§7§m                                                §7",
                "",
                PRICE.replace("%price%", cashItemDTO.price.toString())
            )

            if (cashItemDTO.maxBuyableCnt != -1) {
                val uuid = (sender as Player).uniqueId
                val sql =
                    "select count(*) cnt from cash_log where user_id = (select id from user where uuid = '${uuid}') " +
                            "and cash_shop_id = ${cashItemDTO.cashShopId} and cash_item_id = ${cashItemDTO.cashItemId};"
                val result = DatabaseManager.select(sql)!!

                result.next()
                val curBuyCnt = result.getInt("cnt")
                infoLore.add(
                    USER_LIMITED
                        .replace("%user_remain%", (cashItemDTO.maxBuyableCnt - curBuyCnt).toString())
                        .replace("%user_purchases_limited%", cashItemDTO.maxBuyableCnt.toString())
                )
            }
            if (cashItemDTO.maxBuyableCntServer != -1) {
                val sql = "select count(*) cnt from cash_log where " +
                        "cash_shop_id = ${cashItemDTO.cashShopId} and cash_item_id = ${cashItemDTO.cashItemId};"
                val result = DatabaseManager.select(sql)!!

                result.next()
                val curServerBuyCnt = result.getInt("cnt")
                infoLore.add(
                    SERVER_LIMITED
                        .replace("%server_remain%", (cashItemDTO.maxBuyableCntServer - curServerBuyCnt).toString())
                        .replace("%server_purchases_limited%", cashItemDTO.maxBuyableCntServer.toString())
                )
            }

            infoLore.add("")
            infoLore.add("§7§m                                                §7")

            val combinedLore = lore + infoLore

            val itemMeta = item.itemMeta
            itemMeta?.lore = combinedLore
            item.itemMeta = itemMeta

            this.inventory.setItem(cashItemDTO.slotNum, item)
        }
    }

    override fun getInventory(): Inventory = inventory
}
