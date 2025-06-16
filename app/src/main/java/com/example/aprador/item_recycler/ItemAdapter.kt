package com.example.aprador.item_recycler
// ItemAdapter.kt
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.aprador.R
import java.io.File
import androidx.core.net.toUri

class ItemAdapter(

    private val items: List<Item>,
    private val onItemClick: (Item) -> Unit = {},


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

        val context = holder.itemView.context

        val imageSource = when {
            item.imagePath.startsWith("content://") -> item.imagePath.toUri()
            item.imagePath.startsWith("file://") -> item.imagePath.toUri()
            File(item.imagePath).exists() -> File(item.imagePath)
            else -> null
        }

        if (imageSource != null) {
            Glide.with(context)
                .load(imageSource)
                .centerCrop()
                .placeholder(R.drawable.shirt)
                .error(R.drawable.shirt)
                .into(holder.itemImage)
        } else {
            holder.itemImage.setImageResource(R.drawable.shirt)
        }

        holder.itemView.setOnClickListener {
            onItemClick(item)
        }


    }

    override fun getItemCount(): Int = items.size
}

