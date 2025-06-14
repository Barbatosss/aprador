package com.example.aprador.login

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.aprador.R
import com.example.aprador.items.MyItems
import com.example.aprador.outfits.MyOutfits
import com.example.aprador.recycler.CategorySection
import com.example.aprador.recycler.CategorySectionAdapter
import com.example.aprador.recycler.Item
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

class MainPage : Fragment(R.layout.fragment_main_page) {

    private lateinit var itemsRecyclerView: RecyclerView
    private lateinit var categoryAdapter: CategorySectionAdapter
    private var allItems = listOf<Item>()
    private var categorySections = listOf<CategorySection>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        initializeViews(view)

        // Setup RecyclerView
        setupRecyclerView()

        // Load and display items
        loadItemsData()

        // Setup existing click listeners
        setupClickListeners(view)
    }

    private fun initializeViews(view: View) {
        itemsRecyclerView = view.findViewById(R.id.items_recycler_view)
    }

    private fun setupRecyclerView() {
        categoryAdapter = CategorySectionAdapter(categorySections) { item ->
            onItemClicked(item)
        }

        itemsRecyclerView.apply {
            adapter = categoryAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun setupClickListeners(view: View) {
        val itemView: View = view.findViewById(R.id.ItemView)
        val outfitView: View = view.findViewById(R.id.OutfitView)

        // Move to MyOutfits
        outfitView.setOnClickListener {
            val outfitFragment = MyOutfits()
            parentFragmentManager.beginTransaction()
                .replace(R.id.flFragment, outfitFragment)
                .addToBackStack(null)
                .commit()
        }

        // Move to MyItems
        itemView.setOnClickListener {
            val itemFragment = MyItems()
            parentFragmentManager.beginTransaction()
                .replace(R.id.flFragment, itemFragment)
                .addToBackStack(null)
                .commit()
        }
    }

    private fun loadItemsData() {
        allItems = loadItems(requireContext())
        updateCategorySections()
    }

    private fun updateCategorySections() {
        // Group items by subcategory, same as in MyItems
        categorySections = allItems.groupBy { it.subcategory }
            .map { (subcategory, items) ->
                CategorySection(subcategory, items)
            }
            .sortedBy { it.subcategory }

        categoryAdapter.updateData(categorySections)
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

    private fun onItemClicked(item: Item) {
        // Handle item click - you can navigate to item details or show a toast
        Toast.makeText(requireContext(), "Clicked: ${item.name}", Toast.LENGTH_SHORT).show()

        // Optional: Navigate to MyItems fragment when an item is clicked
        val itemFragment = MyItems()
        parentFragmentManager.beginTransaction()
            .replace(R.id.flFragment, itemFragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when returning to this fragment
        if (::categoryAdapter.isInitialized) {
            loadItemsData()
        }
    }
}