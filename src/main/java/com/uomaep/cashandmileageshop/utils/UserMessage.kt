package com.uomaep.cashandmileageshop.utils

import org.bukkit.Bukkit
import org.bukkit.entity.Player

object UserMessage {
    lateinit var failMessageHeader: String
    lateinit var successMessageHeader: String
    fun sendFailMessage(player: Player, message: String) {
        player.sendMessage("$failMessageHeader $message")
    }

    fun sendSuccessMessage(player: Player, message: String) {
        player.sendMessage("$successMessageHeader $message")
    }
}
