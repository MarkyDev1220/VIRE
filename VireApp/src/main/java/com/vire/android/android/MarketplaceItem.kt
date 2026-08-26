package com.vire.android.android


data class MarketplaceItem(
    val id: Long,
    val title: String,
    val description: String,
    val price: Double,
    val category: String,
    val imageUri: String? = null, // Uri.toString()
    val owner: String,
    var sold: Boolean = false
)