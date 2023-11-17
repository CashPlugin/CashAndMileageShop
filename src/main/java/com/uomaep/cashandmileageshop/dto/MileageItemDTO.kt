package com.uomaep.cashandmileageshop.dto

data class MileageItemDTO(
    val mileageItemId: Int,
    val maxBuyableCnt: Int,
    val price: Int,
    val itemId: Int,
    val mileageShopId: Int,
    val maxBuyableCntServer: Int,
    val slotNum: Int,
    val state: Int,
    val itemInfo: String,
    val name: String
)
