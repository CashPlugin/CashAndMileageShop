package com.uomaep.mileageandmileageshop.listeners

import com.uomaep.Mileageandmileageshop.dto.MileageItemDTO
import com.uomaep.cashandmileageshop.dto.MileageShopDTO
import com.uomaep.cashandmileageshop.utils.DatabaseManager
import com.uomaep.mileageandmileageshop.guis.MileageShopGUI
import com.uomaep.mileageandmileageshop.guis.MileageShopPurchaseConfirmationGUI
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent

class MileageShopItemClickEvent : Listener {
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onShopItemClick(e: InventoryClickEvent) {
        val player = e.whoClicked

        if (e.isCancelled) {
            return
        }

        val inventoryHolder = e.inventory.holder ?: return
        //마일리지샵을 클릭한 것이 아님 -> 리스너가 작동하지 않아야 함.
        if (inventoryHolder !is MileageShopGUI) {
            return
        }

        //gui밖을 클릭한 경우
        val clickedInventoryHolder = e.clickedInventory?.holder ?: run {
            e.isCancelled = true
            return
        }

        //클릭한 홀더가 유저의 것인지 상점인지 확인
        if (clickedInventoryHolder is MileageShopGUI) {//구매 로직
            val currentItem = e.currentItem
            if (currentItem == null) {
                println("[마일리지상점]: 빈 슬롯을 클릭")
                e.isCancelled = true
                return
            }

            if (player.inventory.firstEmpty() == -1) {
                player.sendMessage("[마일리지상점]: 인벤토리가 가득 찼습니다.")
                e.isCancelled = true
                return
            }

            val itemSlotNum = e.slot
            val mileageShopName = inventoryHolder.name

            val sql3 = "select * from mileage_shop where name = '$mileageShopName' and state = 2;"
            val result3 = DatabaseManager.select(sql3)!!
            if (!result3.next()) {
                println("존재하지 않는 마일리지상점입니다.")
            }

            val mileageShopDTO = MileageShopDTO(
                result3.getInt("id"),
                result3.getString("name"),
                result3.getInt("line_num"),
                result3.getInt("state")
            )

            val sql2 = "select * " +
                    "from (select mileage_item.id, max_buyable_cnt, price, item_id, mileage_shop_id, max_buyable_cnt_server, slot_num, state " +
                    "from (select id from mileage_shop where name = '$mileageShopName') as mileageShopId " +
                    "join mileage_item on mileage_shop_id = mileageShopId.id and slot_num = $itemSlotNum and state = 1) as mileageItem " +
                    "join item on item.id = mileageItem.item_id;"
            val result2 = DatabaseManager.select(sql2)!!

            if (!result2.next()) {
                println("존재하지 않는 아이템입니다.")
                e.isCancelled = true
                return
            }

            val mileageItemDTO = MileageItemDTO(
                mileageItemId = result2.getInt("mileageItem.id"),
                maxBuyableCnt = result2.getInt("max_buyable_cnt"),
                price = result2.getInt("price"),
                itemId = result2.getInt("item_id"),
                mileageShopId = result2.getInt("mileage_shop_id"),
                maxBuyableCntServer = result2.getInt("max_buyable_cnt_server"),
                slotNum = result2.getInt("slot_num"),
                state = result2.getInt("state"),
                itemInfo = result2.getString("\u202Aitem_info"),
                name = result2.getString("name")
            )

            val mileageShopPurchaseConfirmationGUI =
                MileageShopPurchaseConfirmationGUI(player, e, mileageItemDTO, mileageShopDTO, this)
            player.openInventory(mileageShopPurchaseConfirmationGUI.inventory)
            e.isCancelled = true
        } else {//꼼수 막기
            e.isCancelled = true
        }
    }
}
