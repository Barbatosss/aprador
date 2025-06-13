package com.example.aprador
// ItemAdapter.kt
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.io.File

class ItemAdapter(
    private val items: List<Item>,
    private val onItemClick: (Item) -> Unit = {}
) : RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {

    class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val itemImage: ImageView = itemView.findViewById(R.id.item_image)
        val itemName: TextView = itemView.findViewById(R.id.item_name)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_card, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = items[position]

        holder.itemName.text = item.name

        // Load image from device storage
        val imageFile = File(item.imagePath)
        if (imageFile.exists()) {
            Glide.with(holder.itemView.context)
                .load(imageFile)
                .centerCrop()
                .placeholder(R.drawable.shirt) // Default placeholder
                .into(holder.itemImage)
        } else {
            // Use default image if file doesn't exist
            holder.itemImage.setImageResource(R.drawable.shirt)
        }

        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount(): Int = items.size
}

