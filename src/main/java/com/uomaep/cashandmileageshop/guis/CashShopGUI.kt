package com.uomaep.cashandmileageshop.guis

import com.uomaep.cashandmileageshop.DTO.CashItemDTO
import com.uomaep.cashandmileageshop.DTO.CashShopDTO
import com.uomaep.cashandmileageshop.utils.DatabaseManager
import com.uomaep.cashandmileageshop.utils.Message
import com.uomaep.kotlintestplugin.utils.ItemUtil
import net.kyori.adventure.text.Component
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.Bukkit
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack

class CashShopGUI: InventoryHolder {
    val name: String
    private val inventory: Inventory

    constructor(cashShopDTO: CashShopDTO) {
        this.name = cashShopDTO.name
        this.inventory = Bukkit.createInventory(this, cashShopDTO.lineNum * 9, name)

        // item set logic
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
//            val lore = (item.itemMeta.lore() ?: listOf<String>()) as List<String>
//            val newLore = listOf("가격: ${cashItemDTO.price}")
//            val x = lore.plus(newLore)
//            Message.failureLogMessage("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@\\n$x\\n@@@@@@@@@@@@@@@@@")
//            item.itemMeta.lore = x

            val lore = item.itemMeta.lore ?: mutableListOf()
            val infoLore = mutableListOf(
                "",
                "§7--------------------------------------------",
                "",
                "§f가격: §e${cashItemDTO.price} §f캐시"
            )
            if (cashItemDTO.maxBuyableCnt != -1){
                //로그 뒤져서 남은 구매가능 횟수
                infoLore.add("§f구매 가능 횟수: §e여기에 남은 횟수/${cashItemDTO.maxBuyableCnt}")
            }
            if (cashItemDTO.maxBuyableCntServer != -1){
                //여기도
                infoLore.add("§f한정판매: §e남은 수량/${cashItemDTO.maxBuyableCntServer}")
            }

            infoLore.add("")
            infoLore.add("§7--------------------------------------------")

            // Combine the existing lore with the new lore
            val combinedLore = lore + infoLore

            // Update the item's lore with the combined lore
            val itemMeta = item.itemMeta
            itemMeta?.lore = combinedLore
            item.itemMeta = itemMeta

            this.inventory.setItem(cashItemDTO.slotNum, item)


        }
    }

    override fun getInventory(): Inventory = inventory
}