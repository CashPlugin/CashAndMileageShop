package com.uomaep.cashandmileageshop.listeners

import com.uomaep.cashandmileageshop.guis.CashShopGUI
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder

class ShopItemClickEvent: Listener {
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onShopItemClick(e: InventoryClickEvent) {
        if (e.isCancelled){
            return
        }

        val holder = e.clickedInventory?.holder

        //인벤토리 홀더가 CashShopGUI가 아니면 리턴
        //다른 리스너들이 작동해야 하기 때문에 e.isCancelled = true 하면 안됨
        if(holder !is CashShopGUI) {
            return
        }

        //생각해줘야 할 것: 꼼수 막기. 캐시샵에서 아래 유저의 인벤토리 홀더?를 클릭했을 때는 어떻게 되는지
        //이 리스너가 실행되는지 여부부터 시작해서 만약 실행된다면 꼼수 막아야 함.

        //캐시샵 로직 짜고 e.isCancelled = true 부여
    }
}
