package com.example.aprador.outfits

data class Outfit(
    val id: String,
    val title: String,
    val category: String, // "Casual", "Ethnic", "Formal", "Sports", etc.
    val gender: String, // "Men", "Women"
    val items: List<String>, // List of item IDs that make up this outfit
    val createdAt: Long = System.currentTimeMillis(),
    val previewImagePath: String? = null // Optional: path to a preview image of the complete outfit
)