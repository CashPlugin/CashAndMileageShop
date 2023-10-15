package com.uomaep.cashandmileageshop.utils

import org.bukkit.Bukkit
import org.bukkit.entity.Player

object Message {
    lateinit var failureLogMessageHeader: String
    lateinit var successfulLogMessageHeader: String
    fun successfulLogMessage(message: String) {
        Bukkit.getConsoleSender().sendMessage("$successfulLogMessageHeader $[CashAndMileageShop] [§a  ok  §r] §a$message")
    }

    fun failureLogMessage(message: String) {
        Bukkit.getConsoleSender().sendMessage("$failureLogMessageHeader [CashAndMileageShop] [§c no §r] §c$message")
    }
}
