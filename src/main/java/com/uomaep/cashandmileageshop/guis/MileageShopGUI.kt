package com.uomaep.mileageandmileageshop.guis

import com.uomaep.cashandmileageshop.dto.MileageItemDTO
import com.uomaep.cashandmileageshop.dto.MileageShopDTO
import com.uomaep.cashandmileageshop.utils.DatabaseManager
import com.uomaep.cashandmileageshop.utils.ItemUtil
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack

class MileageShopGUI : InventoryHolder {
    val name: String
    private val inventory: Inventory

    companion object {
        lateinit var PRICE: String
        lateinit var SERVER_LIMITED: String
        lateinit var USER_LIMITED: String
    }

    constructor(mileageShopDTO: MileageShopDTO, sender: CommandSender) {
        this.name = mileageShopDTO.name
        this.inventory = Bukkit.createInventory(this, mileageShopDTO.lineNum * 9, name)

        val sql =
            "select * from (select * from mileage_item where mileage_shop_id = ${mileageShopDTO.id} and state = 1) as mileageItems join item on item.id = mileageItems.item_id"
        val result = DatabaseManager.select(sql)!!

        while (result.next()) {
            val mileageItemDTO = MileageItemDTO(
                mileageItemId = result.getInt("mileageItems.id"),
                maxBuyableCnt = result.getInt("max_buyable_cnt"),
                price = result.getInt("price"),
                itemId = result.getInt("item_id"),
                mileageShopId = result.getInt("mileage_shop_id"),
                maxBuyableCntServer = result.getInt("max_buyable_cnt_server"),
                slotNum = result.getInt("slot_num"),
                state = result.getInt("state"),
                itemInfo = result.getString("\u202Aitem_info"),
                name = result.getString("name")
            )
            val item: ItemStack = ItemUtil.deserialize(mileageItemDTO.itemInfo)

            val lore = item.itemMeta.lore ?: mutableListOf()
            val infoLore = mutableListOf(
                "",
                "§7§m                                                §7",
                "",
                PRICE.replace("%price%", mileageItemDTO.price.toString())
            )

            if (mileageItemDTO.maxBuyableCnt != -1) {
                val uuid = (sender as Player).uniqueId
                val sql =
                    "select count(*) cnt from mileage_log where user_id = (select id from user where uuid = '${uuid}') " +
                            "and mileage_shop_id = ${mileageItemDTO.mileageShopId} and mileage_item_id = ${mileageItemDTO.mileageItemId};"
                val result = DatabaseManager.select(sql)!!

                result.next()
                val curBuyCnt = result.getInt("cnt")
                infoLore.add(
                    SERVER_LIMITED
                        .replace("%server_remain%", (mileageItemDTO.maxBuyableCnt - curBuyCnt).toString())
                        .replace("%server_purchases_limited%", mileageItemDTO.maxBuyableCnt.toString())
                )
            }
            if (mileageItemDTO.maxBuyableCntServer != -1) {
                val sql = "select count(*) cnt from mileage_log where " +
                        "mileage_shop_id = ${mileageItemDTO.mileageShopId} and mileage_item_id = ${mileageItemDTO.mileageItemId};"
                val result = DatabaseManager.select(sql)!!

                result.next()
                val curServerBuyCnt = result.getInt("cnt")
                infoLore.add(
                    USER_LIMITED
                        .replace("%user_remain%", (mileageItemDTO.maxBuyableCntServer - curServerBuyCnt).toString())
                        .replace("%user_purchases_limited%", mileageItemDTO.maxBuyableCntServer.toString())
                )
            }

            infoLore.add("")
            infoLore.add("§7§m                                                §7")

            val combinedLore = lore + infoLore

            val itemMeta = item.itemMeta
            itemMeta?.lore = combinedLore
            item.itemMeta = itemMeta

            this.inventory.setItem(mileageItemDTO.slotNum, item)
        }
    }

    override fun getInventory(): Inventory = inventory
}
