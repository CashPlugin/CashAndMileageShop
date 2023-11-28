package com.uomaep.cashandmileageshop.listeners

import com.uomaep.cashandmileageshop.dto.MileageItemDTO
import com.uomaep.cashandmileageshop.dto.UserDTO
import com.uomaep.cashandmileageshop.guis.MileageShopPurchaseConfirmationGUI
import com.uomaep.cashandmileageshop.utils.DatabaseManager
import com.uomaep.cashandmileageshop.utils.ItemUtil
import com.uomaep.cashandmileageshop.utils.UserUtil
import com.uomaep.mileageandmileageshop.guis.MileageShopGUI
import org.bukkit.entity.HumanEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import java.util.*

class MileageShopPurchaseConfirmationEvent : Listener {
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onShopItemClick(e: InventoryClickEvent) {
        val player = e.whoClicked

        if (e.isCancelled) {
            return
        }

        val inventoryHolder = e.inventory.holder ?: return
        //마일리지샵을 클릭한 것이 아님 -> 리스너가 작동하지 않아야 함.
        if (inventoryHolder !is MileageShopPurchaseConfirmationGUI) {
            return
        }

        //gui밖을 클릭한 경우
        val clickedInventoryHolder = e.clickedInventory?.holder ?: run {
            e.isCancelled = true
            return
        }

        //클릭한 홀더가 유저의 것인지 상점인지 확인
        if (clickedInventoryHolder is MileageShopPurchaseConfirmationGUI) {//구매 로직
            when (e.slot) {
                in MileageShopPurchaseConfirmationGUI.buySlot -> {
                    buyItem(player, clickedInventoryHolder.ogEvent!!, clickedInventoryHolder.ogmileageItemDTO!!)
                    e.clickedInventory!!.close()
                    val mileageShopGUI = MileageShopGUI(clickedInventoryHolder.ogmileageShopDTO!!, player)
                    player.openInventory(mileageShopGUI.inventory)
                    e.isCancelled = true
                    return
                }

                in MileageShopPurchaseConfirmationGUI.blockingSlot -> {
                    e.isCancelled = true
                    return
                }

                in MileageShopPurchaseConfirmationGUI.cancelSlot -> {
                    e.clickedInventory!!.close()
                    val mileageShopGUI = MileageShopGUI(clickedInventoryHolder.ogmileageShopDTO!!, player)
                    player.openInventory(mileageShopGUI.inventory)
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
        mileageItemDTO: MileageItemDTO
    ): Boolean {
        //유저 정보 가져오기
        val uuid = player.uniqueId
        val sql1 = "select * from user where uuid = '$uuid';"
        val result1 = DatabaseManager.select(sql1)!!

        if (!result1.next()) {
            println("[마일리지상점]: 존재하지 않는 유저입니다.")
            e.isCancelled = true
            return false
        }

        val user = UserDTO(
            id = result1.getInt("id"),
            uuid = result1.getString("uuid"),
            mileage = result1.getInt("mileage"),
            cash = result1.getInt("cash")
        )
        if (user.mileage >= mileageItemDTO.price) {
            var logId: Long

            synchronized(this) {

                if (mileageItemDTO.maxBuyableCntServer == -1 && mileageItemDTO.maxBuyableCnt != -1) {
                    // 서버 수량 제한 X, 개인 수량 제한 O
                    if (mileageItemDTO.maxBuyableCnt <= getUserBuyCnt(uuid, mileageItemDTO)) {
                        // 구매 불가
                        player.sendMessage("[마일리지상점]: 구매횟수초과")
                        e.isCancelled = true
                        return false
                    }
                } else if (mileageItemDTO.maxBuyableCntServer != -1 && mileageItemDTO.maxBuyableCnt == -1) {
                    // 서버 수량 제한 O, 개인 수량 제한 X
                    if (mileageItemDTO.maxBuyableCntServer <= getServerBuyCnt(mileageItemDTO)) {
                        player.sendMessage("[마일리지상점]: 구매횟수초과")
                        e.isCancelled = true
                        return false
                    }
                } else if (mileageItemDTO.maxBuyableCntServer != -1 && mileageItemDTO.maxBuyableCnt != -1) {
                    // 서버 수량 제한 O, 개인 수량 제한 O
                    if (mileageItemDTO.maxBuyableCntServer <= getServerBuyCnt(mileageItemDTO) ||
                        mileageItemDTO.maxBuyableCnt <= getUserBuyCnt(uuid, mileageItemDTO)
                    ) {
                        player.sendMessage("[마일리지상점]: 구매횟수초과")
                        e.isCancelled = true
                        return false
                    }
                }

                logId = itemLogging(user.id, mileageItemDTO.mileageItemId, mileageItemDTO.mileageShopId, e)
            }
            //마일리지 로그에 기록

            //유저의 마일리지 차감
            val sql4 = "update user set mileage = mileage - ${mileageItemDTO.price} where uuid = '${user.uuid}';"
            val result4 = DatabaseManager.update(sql4)
            if (!result4) {
                println("[마일리지상점]: 마일리지 차감에 실패했습니다.")
                //실패시 로그 삭제
                DatabaseManager.delete("delete from mileage_log where id = ${logId};")
                e.isCancelled = true
                return false
            }

            //아이템 지급
            player.inventory.addItem(ItemUtil.deserialize(mileageItemDTO.itemInfo))
            UserUtil.playBuyCompleteSound(player)
            player.sendMessage("[마일리지상점]: §f◇ 아이템 구매에 성공하였습니다.")

            //마일리지 아이템 정보 다시 가져오기
            val item: ItemStack = getItemInfoById(mileageItemDTO, user.uuid)

            //마일리지샵의 구매한 아이템 정보 새로고침
            e.inventory.setItem(mileageItemDTO.slotNum, item)
        } else {
            player.sendMessage("[마일리지상점]: §f◇ §c잔액이 부족합니다.")
        }
        return true
    }

    private fun getItemInfoById(
        mileageItemDTO: MileageItemDTO,
        uuid: String
    ): ItemStack {
        val sql5 =
            "select * from mileage_item join item on mileage_item.item_id = item.id where mileage_item.id = ${mileageItemDTO.mileageItemId};"
        val result5 = DatabaseManager.select(sql5)!!

        result5.next()

        val reloadmileageItemDTO = MileageItemDTO(
            mileageItemId = result5.getInt("mileage_item.id"),
            maxBuyableCnt = result5.getInt("max_buyable_cnt"),
            price = result5.getInt("price"),
            itemId = result5.getInt("item_id"),
            mileageShopId = result5.getInt("mileage_shop_id"),
            maxBuyableCntServer = result5.getInt("max_buyable_cnt_server"),
            slotNum = result5.getInt("slot_num"),
            state = result5.getInt("state"),
            itemInfo = result5.getString("\u202Aitem_info"),
            name = result5.getString("name")
        )
        val item: ItemStack = ItemUtil.deserialize(reloadmileageItemDTO.itemInfo)

        val lore = item.itemMeta.lore ?: mutableListOf()
        val infoLore = mutableListOf(
            "",
            "§7--------------------------------------------",
            "",
            "§f가격: §e${reloadmileageItemDTO.price} §f마일리지"
        )
        if (reloadmileageItemDTO.maxBuyableCnt != -1) {
            //로그 뒤져서 남은 구매가능 횟수
            val sql =
                "select count(*) cnt from mileage_log where user_id = (select id from user where uuid = '${uuid}') " +
                        "and mileage_shop_id = ${reloadmileageItemDTO.mileageShopId} and mileage_item_id = ${reloadmileageItemDTO.mileageItemId};"
            val result = DatabaseManager.select(sql)!!

            result.next()
            val curBuyCnt = result.getInt("cnt")
            infoLore.add("§f서버 한정: §e${reloadmileageItemDTO.maxBuyableCnt - curBuyCnt}/${reloadmileageItemDTO.maxBuyableCnt}")
        }
        if (reloadmileageItemDTO.maxBuyableCntServer != -1) {
            val sql = "select count(*) cnt from mileage_log where " +
                    "mileage_shop_id = ${reloadmileageItemDTO.mileageShopId} and mileage_item_id = ${reloadmileageItemDTO.mileageItemId};"
            val result = DatabaseManager.select(sql)!!

            result.next()
            val curServerBuyCnt = result.getInt("cnt")

            infoLore.add("§f개인 한정: §e${reloadmileageItemDTO.maxBuyableCntServer - curServerBuyCnt}/${reloadmileageItemDTO.maxBuyableCntServer}")
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

    private fun getServerBuyCnt(mileageItemDTO: MileageItemDTO): Int {
        val sql = "select count(*) cnt from mileage_log where " +
                "mileage_shop_id = ${mileageItemDTO.mileageShopId} and mileage_item_id = ${mileageItemDTO.mileageItemId};"
        val result = DatabaseManager.select(sql)!!

        result.next()
        return result.getInt("cnt")
    }

    private fun getUserBuyCnt(uuid: UUID, mileageItemDTO: MileageItemDTO): Int {
        val sql = "select count(*) cnt from mileage_log where user_id = (select id from user where uuid = '${uuid}') " +
                "and mileage_shop_id = ${mileageItemDTO.mileageShopId} and mileage_item_id = ${mileageItemDTO.mileageItemId};"
        val result = DatabaseManager.select(sql)!!

        result.next()
        return result.getInt("cnt")
    }

    private fun itemLogging(userId: Int, itemId: Int, shopId: Int, e: InventoryClickEvent): Long {
        val statement = "insert into mileage_log (user_id, mileage_item_id, purchase_at, mileage_shop_id) " +
                "values (${userId}, ${itemId}, NOW(), ${shopId});"
        val insertedId = DatabaseManager.insertAndGetGeneratedKey(statement)
        if (insertedId == -1L) {
            println("[마일리지상점]: 로깅 실패")
            e.isCancelled = true
            return -1
        }
        return insertedId!!
    }
}
