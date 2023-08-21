package com.uomaep.cashandmileageshop.DTO

data class CashItemDTO(
    val cashItemId: Int,
    val maxBuyableCnt: Int,
    val price: Int,
    val itemId: Int,
    val cashShopId: Int,
    val maxBuyableCntServer: Int,
    val slotNum: Int,
    val state: Int,
    val itemInfo: String,
    val name: String
)
