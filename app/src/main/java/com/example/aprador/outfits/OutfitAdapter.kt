package com.example.aprador.outfits

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.aprador.R

class OutfitAdapter(
    private var outfits: List<Outfit>,
    private val onOutfitClick: (Outfit) -> Unit
) : RecyclerView.Adapter<OutfitAdapter.OutfitViewHolder>() {

    class OutfitViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val outfitImage: ImageView = itemView.findViewById(R.id.iv_outfit)
        val outfitTitle: TextView = itemView.findViewById(R.id.tv_outfit_title) }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OutfitViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_outfit, parent, false)
        return OutfitViewHolder(view)
    }

    override fun onBindViewHolder(holder: OutfitViewHolder, position: Int) {
        val outfit = outfits[position]

        // Set outfit image
        holder.outfitImage.setImageResource(outfit.imageResId)

        // Set outfit title
        holder.outfitTitle.text = outfit.title


        // Set click listener
        holder.itemView.setOnClickListener {
            onOutfitClick(outfit)
        }
    }

    override fun getItemCount(): Int = outfits.size

    fun updateOutfits(newOutfits: List<Outfit>) {
        outfits = newOutfits
        notifyDataSetChanged()
    }
}