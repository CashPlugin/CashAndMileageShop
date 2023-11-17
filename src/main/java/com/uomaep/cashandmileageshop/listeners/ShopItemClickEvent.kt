package com.uomaep.cashandmileageshop.listeners

import com.uomaep.cashandmileageshop.dto.CashItemDTO
import com.uomaep.cashandmileageshop.dto.CashShopDTO
import com.uomaep.cashandmileageshop.guis.CashShopGUI
import com.uomaep.cashandmileageshop.guis.PurchaseConfirmationGUI
import com.uomaep.cashandmileageshop.utils.DatabaseManager
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent

class ShopItemClickEvent : Listener {
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onShopItemClick(e: InventoryClickEvent) {
        val player = e.whoClicked

        if (e.isCancelled) {
            return
        }

        val inventoryHolder = e.inventory.holder ?: return
        //캐시샵을 클릭한 것이 아님 -> 리스너가 작동하지 않아야 함.
        if (inventoryHolder !is CashShopGUI) {
            return
        }

        //gui밖을 클릭한 경우
        val clickedInventoryHolder = e.clickedInventory?.holder ?: run {
            e.isCancelled = true
            return
        }

        //클릭한 홀더가 유저의 것인지 상점인지 확인
        if (clickedInventoryHolder is CashShopGUI) {//구매 로직
            val currentItem = e.currentItem
            if (currentItem == null) {
                println("[캐시상점]: 빈 슬롯을 클릭")
                e.isCancelled = true
                return
            }

            if (player.inventory.firstEmpty() == -1) {
                player.sendMessage("[캐시상점]: 인벤토리가 가득 찼습니다.")
                e.isCancelled = true
                return
            }

            val itemSlotNum = e.slot
            val cashShopName = inventoryHolder.name

            val sql3 = "select * from cash_shop where name = '$cashShopName' and state = 2;"
            val result3 = DatabaseManager.select(sql3)!!
            if (!result3.next()) {
                println("존재하지 않는 캐시상점입니다.")
            }

            val cashShopDTO = CashShopDTO(
                result3.getInt("id"),
                result3.getString("name"),
                result3.getInt("line_num"),
                result3.getInt("state")
            )

            val sql2 = "select * " +
                    "from (select cash_item.id, max_buyable_cnt, price, item_id, cash_shop_id, max_buyable_cnt_server, slot_num, state " +
                    "from (select id from cash_shop where name = '$cashShopName') as cashShopId " +
                    "join cash_item on cash_shop_id = cashShopId.id and slot_num = $itemSlotNum and state = 1) as cashItem " +
                    "join item on item.id = cashItem.item_id;"
            val result2 = DatabaseManager.select(sql2)!!

            if (!result2.next()) {
                println("존재하지 않는 아이템입니다.")
                e.isCancelled = true
                return
            }

            val cashItemDTO = CashItemDTO(
                cashItemId = result2.getInt("cashItem.id"),
                maxBuyableCnt = result2.getInt("max_buyable_cnt"),
                price = result2.getInt("price"),
                itemId = result2.getInt("item_id"),
                cashShopId = result2.getInt("cash_shop_id"),
                maxBuyableCntServer = result2.getInt("max_buyable_cnt_server"),
                slotNum = result2.getInt("slot_num"),
                state = result2.getInt("state"),
                itemInfo = result2.getString("\u202Aitem_info"),
                name = result2.getString("name")
            )

            val purchaseConfirmationGUI = PurchaseConfirmationGUI(player, e, cashItemDTO, cashShopDTO, this)
            player.openInventory(purchaseConfirmationGUI.inventory)
            e.isCancelled = true
        } else {//꼼수 막기
            e.isCancelled = true
        }
    }
}
