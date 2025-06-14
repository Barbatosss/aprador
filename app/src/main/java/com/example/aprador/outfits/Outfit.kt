package com.example.aprador.outfits

data class Outfit(
    val id: Int,
    val title: String,
    val imageResId: Int, // Resource ID for the outfit image
    val colors: List<String>, // List of color hex codes
    val category: String // "Classic", "Sport", "Home", etc.
)