package com.uomaep.cashandmileageshop.listeners

import com.uomaep.cashandmileageshop.dto.CashItemDTO
import com.uomaep.cashandmileageshop.dto.UserDTO
import com.uomaep.cashandmileageshop.guis.CashShopGUI
import com.uomaep.cashandmileageshop.guis.CashShopPurchaseConfirmationGUI
import com.uomaep.cashandmileageshop.utils.DatabaseManager
import com.uomaep.cashandmileageshop.utils.ItemUtil
import com.uomaep.cashandmileageshop.utils.UserUtil
import org.bukkit.entity.HumanEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import java.util.*

class CashShopPurchaseConfirmationEvent : Listener {
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onShopItemClick(e: InventoryClickEvent) {
        val player = e.whoClicked

        if (e.isCancelled) {
            return
        }

        val inventoryHolder = e.inventory.holder ?: return
        //캐시샵을 클릭한 것이 아님 -> 리스너가 작동하지 않아야 함.
        if (inventoryHolder !is CashShopPurchaseConfirmationGUI) {
            return
        }

        //gui밖을 클릭한 경우
        val clickedInventoryHolder = e.clickedInventory?.holder ?: run {
            e.isCancelled = true
            return
        }

        //클릭한 홀더가 유저의 것인지 상점인지 확인
        if (clickedInventoryHolder is CashShopPurchaseConfirmationGUI) {//구매 로직
            when (e.slot) {
                in CashShopPurchaseConfirmationGUI.buySlot -> {
                    buyItem(player, clickedInventoryHolder.ogEvent!!, clickedInventoryHolder.ogCashItemDTO!!)
                    e.clickedInventory!!.close()
                    val cashShopGUI = CashShopGUI(clickedInventoryHolder.ogCashShopDTO!!, player)
                    player.openInventory(cashShopGUI.inventory)
                    e.isCancelled = true
                    return
                }

                in CashShopPurchaseConfirmationGUI.blockingSlot -> {
                    e.isCancelled = true
                    return
                }

                in CashShopPurchaseConfirmationGUI.cancelSlot -> {
                    e.clickedInventory!!.close()
                    val cashShopGUI = CashShopGUI(clickedInventoryHolder.ogCashShopDTO!!, player)
                    player.openInventory(cashShopGUI.inventory)
                    e.isCancelled = true
                    return
                }

                else -> {
                    e.isCancelled = true
                    return
                }
            }
        } else {//꼼수 막기
            e.isCancelled = true
        }
    }

    private fun buyItem(
        player: HumanEntity,
        e: InventoryClickEvent,
        cashItemDTO: CashItemDTO
    ): Boolean {
        //유저 정보 가져오기
        val uuid = player.uniqueId
        val sql1 = "select * from user where uuid = '$uuid';"
        val result1 = DatabaseManager.select(sql1)!!

        if (!result1.next()) {
            println("[캐시상점]: 존재하지 않는 유저입니다.")
            e.isCancelled = true
            return false
        }

        val user = UserDTO(
            id = result1.getInt("id"),
            uuid = result1.getString("uuid"),
            cash = result1.getInt("cash"),
            mileage = result1.getInt("mileage")
        )
        if (user.cash >= cashItemDTO.price) {
            var logId: Long

            synchronized(this) {

                if (cashItemDTO.maxBuyableCntServer == -1 && cashItemDTO.maxBuyableCnt != -1) {
                    // 서버 수량 제한 X, 개인 수량 제한 O
                    if (cashItemDTO.maxBuyableCnt <= getUserBuyCnt(uuid, cashItemDTO)) {
                        // 구매 불가
                        player.sendMessage("[캐시상점]: 구매횟수초과")
                        e.isCancelled = true
                        return false
                    }
                } else if (cashItemDTO.maxBuyableCntServer != -1 && cashItemDTO.maxBuyableCnt == -1) {
                    // 서버 수량 제한 O, 개인 수량 제한 X
                    if (cashItemDTO.maxBuyableCntServer <= getServerBuyCnt(cashItemDTO)) {
                        player.sendMessage("[캐시상점]: 구매횟수초과")
                        e.isCancelled = true
                        return false
                    }
                } else if (cashItemDTO.maxBuyableCntServer != -1 && cashItemDTO.maxBuyableCnt != -1) {
                    // 서버 수량 제한 O, 개인 수량 제한 O
                    if (cashItemDTO.maxBuyableCntServer <= getServerBuyCnt(cashItemDTO) ||
                        cashItemDTO.maxBuyableCnt <= getUserBuyCnt(uuid, cashItemDTO)
                    ) {
                        player.sendMessage("[캐시상점]: 구매횟수초과")
                        e.isCancelled = true
                        return false
                    }
                }

                logId = itemLogging(user.id, cashItemDTO.cashItemId, cashItemDTO.cashShopId, e)
            }
            //캐시 로그에 기록

            //유저의 캐시 차감
            val sql4 = "update user set cash = cash - ${cashItemDTO.price} where uuid = '${user.uuid}';"
            val result4 = DatabaseManager.update(sql4)
            if (!result4) {
                println("[캐시상점]: 캐시 차감에 실패했습니다.")
                //실패시 로그 삭제
                DatabaseManager.delete("delete from cash_log where id = ${logId};")
                e.isCancelled = true
                return false
            }

            //아이템 지급
            player.inventory.addItem(ItemUtil.deserialize(cashItemDTO.itemInfo))
            UserUtil.playBuyCompleteSound(player)
            player.sendMessage("[캐시상점]: §f◇ 아이템 구매에 성공하였습니다.")

            //플레이어에게 캐시 아이템 가격의 10%만큼 마일리지 지급 db에 기록
            val bonusMileage = (cashItemDTO.price * 0.1).toInt()
            val sql5 = "update user set mileage = mileage + $bonusMileage where uuid = '${user.uuid}';"
            val result5 = DatabaseManager.update(sql5)
            if (!result5) {
                println("[캐시상점]: §f◇ 보너스 마일리지 지급에 실패했습니다.")
                e.isCancelled = true
                return false
            }
            player.sendMessage("[캐시상점]: §f◇ 보너스 마일리지 $bonusMileage 지급 완료")

            //캐시 아이템 정보 다시 가져오기
            val item: ItemStack = getItemInfoById(cashItemDTO, user.uuid)

            //캐시샵의 구매한 아이템 정보 새로고침
            e.inventory.setItem(cashItemDTO.slotNum, item)
        } else {
            player.sendMessage("[캐시상점]: §f◇ §c잔액이 부족합니다.")
        }
        return true
    }

    private fun getItemInfoById(
        cashItemDTO: CashItemDTO,
        uuid: String
    ): ItemStack {
        val sql5 =
            "select * from cash_item join item on cash_item.item_id = item.id where cash_item.id = ${cashItemDTO.cashItemId};"
        val result5 = DatabaseManager.select(sql5)!!

        result5.next()

        val reloadCashItemDTO = CashItemDTO(
            cashItemId = result5.getInt("cash_item.id"),
            maxBuyableCnt = result5.getInt("max_buyable_cnt"),
            price = result5.getInt("price"),
            itemId = result5.getInt("item_id"),
            cashShopId = result5.getInt("cash_shop_id"),
            maxBuyableCntServer = result5.getInt("max_buyable_cnt_server"),
            slotNum = result5.getInt("slot_num"),
            state = result5.getInt("state"),
            itemInfo = result5.getString("\u202Aitem_info"),
            name = result5.getString("name")
        )
        val item: ItemStack = ItemUtil.deserialize(reloadCashItemDTO.itemInfo)

        val lore = item.itemMeta.lore ?: mutableListOf()
        val infoLore = mutableListOf(
            "",
            "§7--------------------------------------------",
            "",
            "§f가격: §e${reloadCashItemDTO.price} §f캐시"
        )
        if (reloadCashItemDTO.maxBuyableCnt != -1) {
            //로그 뒤져서 남은 구매가능 횟수
            val sql =
                "select count(*) cnt from cash_log where user_id = (select id from user where uuid = '${uuid}') " +
                        "and cash_shop_id = ${reloadCashItemDTO.cashShopId} and cash_item_id = ${reloadCashItemDTO.cashItemId};"
            val result = DatabaseManager.select(sql)!!

            result.next()
            val curBuyCnt = result.getInt("cnt")
            infoLore.add("§f서버 한정: §e${reloadCashItemDTO.maxBuyableCnt - curBuyCnt}/${reloadCashItemDTO.maxBuyableCnt}")
        }
        if (reloadCashItemDTO.maxBuyableCntServer != -1) {
            val sql = "select count(*) cnt from cash_log where " +
                    "cash_shop_id = ${reloadCashItemDTO.cashShopId} and cash_item_id = ${reloadCashItemDTO.cashItemId};"
            val result = DatabaseManager.select(sql)!!

            result.next()
            val curServerBuyCnt = result.getInt("cnt")

            infoLore.add("§f개인 한정: §e${reloadCashItemDTO.maxBuyableCntServer - curServerBuyCnt}/${reloadCashItemDTO.maxBuyableCntServer}")
        }

        infoLore.add("")
        infoLore.add("§7--------------------------------------------")

        // Combine the existing lore with the new lore
        val combinedLore = lore + infoLore

        // Update the item's lore with the combined lore
        val itemMeta = item.itemMeta
        itemMeta?.lore = combinedLore
        item.itemMeta = itemMeta
        return item
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

    private fun itemLogging(userId: Int, itemId: Int, shopId: Int, e: InventoryClickEvent): Long {
        val statement = "insert into cash_log (user_id, cash_item_id, purchase_at, cash_shop_id) " +
                "values (${userId}, ${itemId}, NOW(), ${shopId});"
        val insertedId = DatabaseManager.insertAndGetGeneratedKey(statement)
        if (insertedId == -1L) {
            println("[캐시상점]: 로깅 실패")
            e.isCancelled = true
            return -1
        }
        return insertedId!!
    }
}
