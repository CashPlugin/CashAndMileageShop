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
import org.bukkit.inventory.ItemStack
import java.util.*

class ShopItemClickEvent: Listener {
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onShopItemClick(e: InventoryClickEvent) {
        val player = e.whoClicked

        if (e.isCancelled){
            return
        }

        val inventoryHolder = e.inventory.holder ?: return
        //캐시샵을 클릭한 것이 아님 -> 리스너가 작동하지 않아야 함.
        if (inventoryHolder !is CashShopGUI){
            return
        }

        //gui밖을 클릭한 경우
        val clickedInventoryHolder = e.clickedInventory?.holder ?: run {
            e.isCancelled = true
            return
        }

        //클릭한 홀더가 유저의 것인지 상점인지 확인
        if(clickedInventoryHolder is CashShopGUI) {//구매 로직
            val currentItem = e.currentItem
            if (currentItem == null){
                println("[캐시상점]: 빈 슬롯을 클릭")
                e.isCancelled = true
                return
            }

            if (player.inventory.firstEmpty()==-1){
                player.sendMessage("[캐시상점]: 인벤토리가 가득 찼습니다.")
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
            val uuid = player.uniqueId
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
                var logId: Long

                synchronized(this){

                    if(cashItemDTO.maxBuyableCntServer == -1 && cashItemDTO.maxBuyableCnt != -1) {
                        // 서버 수량 제한 X, 개인 수량 제한 O
                        if(cashItemDTO.maxBuyableCnt <= getUserBuyCnt(uuid, cashItemDTO)) {
                            // 구매 불가
                            player.sendMessage("[캐시상점]: 구매횟수초과")
                            e.isCancelled = true
                            return
                        }
                    }
                    else if(cashItemDTO.maxBuyableCntServer !=-1 && cashItemDTO.maxBuyableCnt == -1) {
                        // 서버 수량 제한 O, 개인 수량 제한 X
                        if(cashItemDTO.maxBuyableCntServer <= getServerBuyCnt(cashItemDTO)) {
                            player.sendMessage("[캐시상점]: 구매횟수초과")
                            e.isCancelled = true
                            return
                        }
                    }
                    else if(cashItemDTO.maxBuyableCntServer !=-1 && cashItemDTO.maxBuyableCnt != -1) {
                        // 서버 수량 제한 O, 개인 수량 제한 O
                        if(cashItemDTO.maxBuyableCntServer <= getServerBuyCnt(cashItemDTO) ||
                            cashItemDTO.maxBuyableCnt <= getUserBuyCnt(uuid, cashItemDTO)) {
                            player.sendMessage("[캐시상점]: 구매횟수초과")
                            e.isCancelled = true
                            return
                        }
                    }

                    logId = itemLogging(user.id, cashItemDTO.cashItemId, cashItemDTO.cashShopId, e)
                }
                //캐시 로그에 기록

                //유저의 캐시 차감
                val sql4 = "update user set cash = cash - ${cashItemDTO.price} where uuid = '${user.uuid}';"
                val result4 = DatabaseManager.update(sql4)
                if (!result4){
                    println("[캐시상점]: 캐시 차감에 실패했습니다.")
                    //실패시 로그 삭제
                    DatabaseManager.delete("delete from cash_log where id = ${logId};")
                    e.isCancelled = true
                    return
                }

                //아이템 지급
                player.inventory.addItem(ItemUtil.deserialize(cashItemDTO.itemInfo))

                //캐시 아이템 정보 다시 가져오기
                val item: ItemStack = getItemInfoById(cashItemDTO, user.uuid)

                //캐시샵의 구매한 아이템 정보 새로고침
                e.inventory.setItem(itemSlotNum, item)
            }
            else{
                player.sendMessage("[캐시상점]: 캐시가 부족합니다.")
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

    private fun getServerBuyCnt(cashItemDTO: CashItemDTO): Int {
        val sql = "select count(*) cnt from cash_log where " +
                "cash_shop_id = ${cashItemDTO.cashShopId} and cash_item_id = ${cashItemDTO.cashItemId};"
        val result = DatabaseManager.select(sql)!!

        result.next()
        return result.getInt("cnt")
    }

    private fun getUserBuyCnt(uuid: UUID, cashItemDTO: CashItemDTO): Int {
        val sql = "select count(*) cnt from cash_log where user_id = (select id from user where uuid = '${uuid}') " +
                "and cash_shop_id = ${cashItemDTO.cashShopId} and cash_item_id = ${cashItemDTO.cashItemId};"
        val result = DatabaseManager.select(sql)!!

        result.next()
        return result.getInt("cnt")
    }
}

fun itemLogging(userId: Int, itemId: Int, shopId: Int, e: InventoryClickEvent): Long {
    val statement = "insert into cash_log (user_id, cash_item_id, purchase_at, cash_shop_id) " +
            "values (${userId}, ${itemId}, NOW(), ${shopId});"
    val insertedId = DatabaseManager.insertAndGetGeneratedKey(statement)
    if (insertedId == -1L){
        println("[캐시상점]: 로깅 실패")
        e.isCancelled = true
        return -1
    }
    return insertedId!!
}
