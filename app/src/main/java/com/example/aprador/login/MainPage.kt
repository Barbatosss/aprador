package com.example.aprador.login

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.Toast
import android.widget.TextView
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.core.content.ContextCompat
import com.example.aprador.R
import com.example.aprador.items.MyItems
import com.example.aprador.outfits.MyOutfits
import com.example.aprador.recycler.Item
import com.example.aprador.recycler.ItemAdapter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

class MainPage : Fragment(R.layout.fragment_main_page) {

    private lateinit var itemsRecyclerView: RecyclerView
    private lateinit var itemAdapter: ItemAdapter
    private lateinit var tabsLayout: LinearLayout
    private lateinit var tabAllItem: TextView
    private lateinit var emptyStateLayout: LinearLayout

    private var allItems = listOf<Item>()
    private var filteredItems = listOf<Item>()
    private val dynamicTabs = mutableListOf<TextView>()
    private var selectedCategory = "All"

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
        tabsLayout = view.findViewById(R.id.tabs_layout)
        tabAllItem = view.findViewById(R.id.tab_all_items)
        emptyStateLayout = view.findViewById(R.id.empty_state_layout)
    }

    private fun setupRecyclerView() {
        itemAdapter = ItemAdapter(filteredItems) { item ->
            onItemClicked(item)
        }

        itemsRecyclerView.apply {
            adapter = itemAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            // Prevent nested scrolling conflicts
            isNestedScrollingEnabled = false
        }
    }

    private fun setupClickListeners(view: View) {
        val itemView: View = view.findViewById(R.id.ItemView)
        val outfitView: View = view.findViewById(R.id.OutfitView)

        // Tab click listener for "All" tab
        tabAllItem.setOnClickListener { selectTab("All") }

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
        updateFilteredItems()
        updateTabCounts()
    }

    private fun updateFilteredItems() {
        filteredItems = if (selectedCategory == "All") {
            allItems
        } else {
            allItems.filter { it.subcategory.equals(selectedCategory, ignoreCase = true) }
        }

        // Update the adapter with new filtered items
        itemAdapter = ItemAdapter(filteredItems) { item ->
            onItemClicked(item)
        }
        itemsRecyclerView.adapter = itemAdapter

    }

    private fun updateTabCounts() {
        // Update "All" tab count
        tabAllItem.text = "All (${allItems.size})"

        // Update dynamic tabs
        val categories = allItems.groupBy { it.subcategory }

        // Clear existing dynamic tabs
        dynamicTabs.forEach { tabsLayout.removeView(it) }
        dynamicTabs.clear()

        // Create new dynamic tabs for each subcategory
        categories.forEach { (category, items) ->
            val tabView = createDynamicTab(category, items.size)
            tabsLayout.addView(tabView)
            dynamicTabs.add(tabView)
        }

        // Update tab appearances after creating all tabs
        updateTabAppearances()
    }

    private fun createDynamicTab(category: String, count: Int): TextView {
        val tabView = layoutInflater.inflate(R.layout.dynamic_tab, tabsLayout, false) as TextView
        tabView.text = "$category ($count)"
        tabView.setOnClickListener { selectTab(category) }
        return tabView
    }

    private fun selectTab(category: String) {
        selectedCategory = category

        // Update tab appearances
        updateTabAppearances()

        // Update displayed items
        updateFilteredItems()
    }

    private fun updateTabAppearances() {
        // Reset all tabs to unselected state
        tabAllItem.setBackgroundResource(R.drawable.tab_unselected_background)
        tabAllItem.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))

        dynamicTabs.forEach { tab ->
            tab.setBackgroundResource(R.drawable.tab_unselected_background)
            tab.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
        }

        // Set selected tab appearance
        if (selectedCategory == "All") {
            tabAllItem.setBackgroundResource(R.drawable.tab_selected_background)
            tabAllItem.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
        } else {
            dynamicTabs.find {
                it.text.toString().startsWith(selectedCategory)
            }?.apply {
                setBackgroundResource(R.drawable.tab_selected_background)
                setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
            }
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

    private fun onItemClicked(item: Item) {
        // Handle item click - show toast and navigate to MyItems
        Toast.makeText(requireContext(), "Clicked: ${item.name}", Toast.LENGTH_SHORT).show()

        // Navigate to MyItems fragment when an item is clicked
        val itemFragment = MyItems()
        parentFragmentManager.beginTransaction()
            .replace(R.id.flFragment, itemFragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when returning to this fragment
        if (::itemAdapter.isInitialized) {
            loadItemsData()
        }
    }
}