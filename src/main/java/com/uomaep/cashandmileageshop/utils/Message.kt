package com.uomaep.cashandmileageshop.utils

import org.bukkit.Bukkit

class Message {
    companion object {
        fun successfulLogMessage(message: String) {
            Bukkit.getConsoleSender().sendMessage("[CashAndMileageShop] [§a  ok  §r] §a$message")
        }

        fun failureLogMessage(message: String) {
            Bukkit.getConsoleSender().sendMessage("[CashAndMileageShop] [§c no §r] §c$message")
        }
    }
}