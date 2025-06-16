package com.example.aprador.outfit_recycler

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.aprador.R

class OutfitSectionAdapter(
    private var sections: List<OutfitSection>,
    private val context: Context,
    private val onOutfitClick: (Outfit) -> Unit
) : RecyclerView.Adapter<OutfitSectionAdapter.SectionViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SectionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_outfit_section, parent, false)
        return SectionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SectionViewHolder, position: Int) {
        val section = sections[position]
        holder.bind(section)
    }

    override fun getItemCount(): Int = sections.size

    fun updateSections(newSections: List<OutfitSection>) {
        sections = newSections
        notifyDataSetChanged()
    }

    inner class SectionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val categoryTitle: TextView = itemView.findViewById(R.id.tv_category_title)
        private val horizontalRecyclerView: RecyclerView = itemView.findViewById(R.id.rv_horizontal_outfits)

        fun bind(section: OutfitSection) {
            categoryTitle.text = section.category

            // Setup horizontal RecyclerView for outfits
            val outfitAdapter = OutfitAdapter(section.outfits, context, onOutfitClick)
            horizontalRecyclerView.layoutManager = LinearLayoutManager(
                context,
                LinearLayoutManager.HORIZONTAL,
                false
            )
            horizontalRecyclerView.adapter = outfitAdapter
        }
    }
}