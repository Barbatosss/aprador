package com.example.aprador.outfits

data class Outfit(
    val id: Int,
    val title: String,
    val imageResId: Int, // Resource ID for the outfit image
    val category: String // "Classic", "Sports", "Home", etc.
)