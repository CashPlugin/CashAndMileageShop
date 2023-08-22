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
        val holder = e.clickedInventory?.holder
        if(holder is CashShopGUI) {
            e.isCancelled = true
        }
    }
}