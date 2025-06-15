package com.example.aprador.outfits

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.aprador.R
import com.example.aprador.recycler.Item
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

class OutfitAdapter(
    private var outfits: List<Outfit>,
    private val context: Context,
    private val onOutfitClick: (Outfit) -> Unit
) : RecyclerView.Adapter<OutfitAdapter.OutfitViewHolder>() {

    class OutfitViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val outfitImage: ImageView = itemView.findViewById(R.id.iv_outfit)
        val outfitTitle: TextView = itemView.findViewById(R.id.tv_outfit_title)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OutfitViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_outfit, parent, false)
        return OutfitViewHolder(view)
    }

    override fun onBindViewHolder(holder: OutfitViewHolder, position: Int) {
        val outfit = outfits[position]

        holder.outfitTitle.text = outfit.title

        // Load outfit preview image
        loadOutfitPreview(holder, outfit)

        holder.itemView.setOnClickListener {
            onOutfitClick(outfit)
        }
    }

    private fun loadOutfitPreview(holder: OutfitViewHolder, outfit: Outfit) {
        // If outfit has a preview image, use it
        if (!outfit.previewImagePath.isNullOrEmpty()) {
            val imageSource = when {
                outfit.previewImagePath.startsWith("content://") -> Uri.parse(outfit.previewImagePath)
                outfit.previewImagePath.startsWith("file://") -> Uri.parse(outfit.previewImagePath)
                File(outfit.previewImagePath).exists() -> File(outfit.previewImagePath)
                else -> null
            }

            if (imageSource != null) {
                Glide.with(context)
                    .load(imageSource)
                    .centerCrop()
                    .placeholder(R.drawable.shirt)
                    .error(R.drawable.shirt)
                    .into(holder.outfitImage)
                return
            }
        }

        // Otherwise, load the first item's image as preview
        val allItems = loadItems(context)
        val firstOutfitItem = allItems.find { item -> outfit.items.contains(item.id) }

        if (firstOutfitItem != null) {
            val imageSource = when {
                firstOutfitItem.imagePath.startsWith("content://") -> Uri.parse(firstOutfitItem.imagePath)
                firstOutfitItem.imagePath.startsWith("file://") -> Uri.parse(firstOutfitItem.imagePath)
                File(firstOutfitItem.imagePath).exists() -> File(firstOutfitItem.imagePath)
                else -> null
            }

            if (imageSource != null) {
                Glide.with(context)
                    .load(imageSource)
                    .centerCrop()
                    .placeholder(R.drawable.shirt)
                    .error(R.drawable.shirt)
                    .into(holder.outfitImage)
            } else {
                holder.outfitImage.setImageResource(R.drawable.shirt)
            }
        } else {
            // Fallback to default image
            holder.outfitImage.setImageResource(R.drawable.shirt)
        }
    }

    private fun loadItems(context: Context): List<Item> {
        return try {
            val file = File(context.filesDir, "db.json")
            if (file.exists() && file.readText().isNotBlank()) {
                val json = file.readText()
                val type = object : TypeToken<List<Item>>() {}.type
                Gson().fromJson(json, type)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    override fun getItemCount(): Int = outfits.size

    fun updateOutfits(newOutfits: List<Outfit>) {
        outfits = newOutfits
        notifyDataSetChanged()
    }
}