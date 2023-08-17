package com.uomaep.cashandmileageshop.listeners

import com.uomaep.cashandmileageshop.utils.DatabaseManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

class SetUserAtFirstJoin: Listener {
    @EventHandler
    fun setUserToDatabase(e: PlayerJoinEvent) {
        val player = e.player
        if(player.hasPlayedBefore()) return

        DatabaseManager.insert("insert into user (uuid, cash, mileage) values ('${player.uniqueId}', 0, 0);")
    }
}