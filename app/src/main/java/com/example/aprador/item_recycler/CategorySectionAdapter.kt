package com.example.aprador.item_recycler

// CategorySectionAdapter.kt
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.graphics.Rect
import com.example.aprador.R

class CategorySectionAdapter(
    private var categorySections: List<CategorySection>,
    private val onItemClick: (Item) -> Unit = {},


) : RecyclerView.Adapter<CategorySectionAdapter.CategorySectionViewHolder>() {

    // ViewHolder pool for better performance
    private val viewPool = RecyclerView.RecycledViewPool()

    class CategorySectionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val categoryTitle: TextView = itemView.findViewById(R.id.category_title)
        val itemCount: TextView = itemView.findViewById(R.id.item_count)
        val horizontalRecyclerView: RecyclerView = itemView.findViewById(R.id.horizontal_recycler_view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategorySectionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.category_selection, parent, false)
        return CategorySectionViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategorySectionViewHolder, position: Int) {
        val categorySection = categorySections[position]

        holder.categoryTitle.text = categorySection.subcategory
        holder.itemCount.text = categorySection.items.size.toString()

        // Set up horizontal RecyclerView for items in this category
        val itemAdapter = ItemAdapter(categorySection.items, onItemClick)
        holder.horizontalRecyclerView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = itemAdapter

            // Use shared ViewPool for better performance
            setRecycledViewPool(viewPool)

            // Add item decoration for spacing if not already added
            if (itemDecorationCount == 0) {
                addItemDecoration(HorizontalSpaceItemDecoration(12))
            }

            // Prevent nested scrolling conflicts
            isNestedScrollingEnabled = false
        }
    }

    override fun getItemCount(): Int = categorySections.size

    fun updateData(newCategorySections: List<CategorySection>) {
        // Use DiffUtil for better performance with large datasets
        if (categorySections != newCategorySections) {
            categorySections = newCategorySections
            notifyDataSetChanged()
        }
    }

}

class HorizontalSpaceItemDecoration(private val horizontalSpaceWidth: Int) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val position = parent.getChildAdapterPosition(view)
        val itemCount = parent.adapter?.itemCount ?: 0

        // Add right margin to all items except the last one
        if (position != RecyclerView.NO_POSITION && position < itemCount - 1) {
            outRect.right = horizontalSpaceWidth
        }

        // Add left margin to first item for consistent spacing
        if (position == 0) {
            outRect.left = horizontalSpaceWidth / 2
        }
    }
}