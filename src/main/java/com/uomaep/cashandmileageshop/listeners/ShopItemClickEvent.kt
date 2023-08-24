package com.uomaep.cashandmileageshop.listeners

import com.uomaep.cashandmileageshop.DTO.CashItemDTO
import com.uomaep.cashandmileageshop.DTO.CashShopDTO
import com.uomaep.cashandmileageshop.DTO.UserDTO
import com.uomaep.cashandmileageshop.guis.CashShopGUI
import com.uomaep.cashandmileageshop.utils.DatabaseManager
import com.uomaep.kotlintestplugin.utils.ItemUtil
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent

class ShopItemClickEvent: Listener {
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onShopItemClick(e: InventoryClickEvent) {
        if (e.isCancelled){
            return
        }

        val inventoryHolder = e.inventory.holder!!
        //캐시샵을 클릭한 것이 아님 -> 리스너가 작동하지 않아야 함.
        if (inventoryHolder !is CashShopGUI){
            return
        }

        val clickedInventoryHolder = e.clickedInventory?.holder!!
        //클릭한 홀더가 유저의 것인지 상점인지 확인
        if(clickedInventoryHolder is CashShopGUI) {//구매 로직
            val currentItem = e.currentItem
            if (currentItem == null){
                println("[캐시상점]: 빈 슬롯을 클릭")
                e.isCancelled = true
                return
            }

            if (e.whoClicked.inventory.firstEmpty()==-1){
                e.whoClicked.sendMessage("[캐시상점]: 인벤토리가 가득 찼습니다.")
                e.isCancelled = true
                return
            }

            val itemSlotNum = e.slot
            val cashShopName = inventoryHolder.name

            val sql3 = "select * from cash_shop where name = '$cashShopName' and state = 2;"
            val result3 = DatabaseManager.select(sql3)!!
            if (!result3.next()){
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

            if (!result2.next()){
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

            //유저 정보 가져오기
            val uuid = e.whoClicked.uniqueId
            val sql1 = "select * from user where uuid = '$uuid';"
            val result1 = DatabaseManager.select(sql1)!!

            if (!result1.next()){
                println("[캐시상점]: 존재하지 않는 유저입니다.")
                e.isCancelled = true
                return
            }

            val user = UserDTO(
                id = result1.getInt("id"),
                uuid = result1.getString("uuid"),
                cash = result1.getInt("cash"),
                mileage = result1.getInt("mileage")
            )

            if (user.cash >= cashItemDTO.price){
                //서버 구매 가능 횟수 확인
                //개인 구매 가능 횟수 확인
                //존나 골치 아프네

                //유저의 캐시 차감
                val sql4 = "update user set cash = cash - ${cashItemDTO.price} where uuid = '${user.uuid}';"
                val result4 = DatabaseManager.update(sql4)
                if (!result4){
                    println("[캐시상점]: 캐시 차감에 실패했습니다.")
                }
                //캐시 로그에 기록
                val sql5 = "insert into cash_log (user_id, cash_item_id, purchase_at, cash_shop_id) " +
                        "values (${user.id}, ${cashItemDTO.cashItemId}, NOW(), ${cashShopDTO.id});"
                val result5 = DatabaseManager.insert(sql5)
                if (!result5){
                    println("[캐시상점]: 캐시 로그에 기록에 실패했습니다.")
                }
                //아이템 지급
                e.whoClicked.inventory.addItem(ItemUtil.deserialize(cashItemDTO.itemInfo))
            }
            else{
                e.whoClicked.sendMessage("[캐시상점]: 캐시가 부족합니다.")
            }

            e.isCancelled = true
        }
        else{//꼼수 막기
            e.isCancelled = true
        }

        //생각해줘야 할 것: 꼼수 막기. 캐시샵에서 아래 유저의 인벤토리 홀더?를 클릭했을 때는 어떻게 되는지
        //이 리스너가 실행되는지 여부부터 시작해서 만약 실행된다면 꼼수 막아야 함.

        //캐시샵 로직 짜고 e.isCancelled = true 부여
    }
}
