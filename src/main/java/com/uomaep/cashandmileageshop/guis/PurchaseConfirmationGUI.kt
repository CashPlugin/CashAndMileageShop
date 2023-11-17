package com.uomaep.cashandmileageshop.guis

import com.uomaep.cashandmileageshop.dto.CashItemDTO
import com.uomaep.cashandmileageshop.dto.CashShopDTO
import com.uomaep.cashandmileageshop.listeners.ShopItemClickEvent
import com.uomaep.cashandmileageshop.utils.DatabaseManager
import com.uomaep.cashandmileageshop.utils.ItemUtil
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack

class PurchaseConfirmationGUI: InventoryHolder {
    private val inventory: Inventory
    var ogEvent: InventoryClickEvent? = null
    var ogCashItemDTO: CashItemDTO? = null
    var OgshopItemClickEvent: ShopItemClickEvent? = null
    var ogCashShopDTO: CashShopDTO? = null

    companion object{
        val name: String = "구매 확인"
        val cancelSlot = hashSetOf(0, 1, 2, 9, 10, 11, 18, 19, 20)
        val blockingSlot = hashSetOf(3, 4, 5, 12, 13, 14, 21, 22, 23)
        val buySlot = hashSetOf(6, 7, 8, 15, 16, 17, 24, 25, 26)
    }

    constructor(sender: CommandSender,
                e: InventoryClickEvent,
                cashItemDTO: CashItemDTO,
                cashShopDTO: CashShopDTO,
                shopItemClickEvent: ShopItemClickEvent
    ) {
        this.inventory = Bukkit.createInventory(this, 27, name)
        ogEvent = e
        ogCashItemDTO = cashItemDTO
        OgshopItemClickEvent = shopItemClickEvent
        ogCashShopDTO = cashShopDTO

        // item set logic
        val item: ItemStack = ItemUtil.deserialize(cashItemDTO.itemInfo)

        val lore = item.itemMeta.lore ?: mutableListOf()
        val infoLore = mutableListOf(
            "",
            "§7--------------------------------------------",
            "",
            CashShopGUI.PRICE.replace("%price%", cashItemDTO.price.toString())
        )

        if (cashItemDTO.maxBuyableCnt != -1){
            //로그 뒤져서 남은 구매가능 횟수
            val uuid = (sender as Player).uniqueId
            val sql = "select count(*) cnt from cash_log where user_id = (select id from user where uuid = '${uuid}') " +
                    "and cash_shop_id = ${cashItemDTO.cashShopId} and cash_item_id = ${cashItemDTO.cashItemId};"
            val result = DatabaseManager.select(sql)!!

            result.next()
            val curBuyCnt = result.getInt("cnt")
            infoLore.add(CashShopGUI.SERVER_LIMITED
                .replace("%server_remain%", (cashItemDTO.maxBuyableCnt - curBuyCnt).toString())
                .replace("%server_purchases_limited%", cashItemDTO.maxBuyableCnt.toString()))
        }
        if (cashItemDTO.maxBuyableCntServer != -1){
            val sql = "select count(*) cnt from cash_log where " +
                    "cash_shop_id = ${cashItemDTO.cashShopId} and cash_item_id = ${cashItemDTO.cashItemId};"
            val result = DatabaseManager.select(sql)!!

            result.next()
            val curServerBuyCnt = result.getInt("cnt")
            infoLore.add(CashShopGUI.USER_LIMITED
                .replace("%user_remain%", (cashItemDTO.maxBuyableCntServer - curServerBuyCnt).toString())
                .replace("%user_purchases_limited%", cashItemDTO.maxBuyableCntServer.toString()))
        }

        infoLore.add("")
        infoLore.add("§7--------------------------------------------")

        // Combine the existing lore with the new lore
        val combinedLore = lore + infoLore

        // Update the item's lore with the combined lore
        val itemMeta = item.itemMeta
        itemMeta?.lore = combinedLore
        item.itemMeta = itemMeta

        val green = ItemStack(Material.LIME_STAINED_GLASS_PANE)
        val red = ItemStack(Material.RED_STAINED_GLASS_PANE)
        val white = ItemStack(Material.WHITE_STAINED_GLASS_PANE)

        val gName = green.itemMeta
        gName.setDisplayName("구매")
        green.itemMeta = gName

        val rName = red.itemMeta
        rName.setDisplayName("구매취소")
        red.itemMeta = rName

        val wName = white.itemMeta
        wName.setDisplayName("*")
        white.itemMeta = wName

        for (i in cancelSlot){
            this.inventory.setItem(i, red)
        }
        for (i in buySlot){
            this.inventory.setItem(i, green)
        }
        for (i in blockingSlot){
            this.inventory.setItem(i, white)
        }

        this.inventory.setItem(13, item)
    }

    override fun getInventory(): Inventory = inventory
}
