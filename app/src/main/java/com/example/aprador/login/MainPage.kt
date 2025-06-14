package com.example.aprador.login

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.Toast
import android.widget.TextView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.widget.AdapterView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.core.content.ContextCompat
import com.example.aprador.R
import com.example.aprador.items.ItemDetails
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
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var categorySpinner: Spinner
    private lateinit var subcategorySpinner: Spinner

    private var allItems = listOf<Item>()
    private var filteredItems = listOf<Item>()
    private var selectedCategory = "All Categories"
    private var selectedSubcategory = "All"

    // Subcategory mappings based on AddItem.kt structure
    private val subcategoryMap = mapOf(
        "Top" to listOf("All", "T-Shirts", "Polo", "Dress Shirt", "Tank Top", "Blouse", "Camisole", "Crop Top"),
        "Bottom" to listOf("All", "Jeans", "Chinos", "Shorts", "Joggers", "Leggings", "Skirt", "Dress"),
        "Outerwear" to listOf("All", "Jacket", "Hoodie", "Blazer", "Coat", "Cardigan", "Kimono"),
        "Footwear" to listOf("All", "Sneakers", "Dress Shoes", "Boots", "Sandals", "Heels", "Flats"),
        "All Categories" to listOf("All")
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        initializeViews(view)

        // Setup dropdowns
        setupCategorySpinner()
        setupSubcategorySpinner()

        // Setup RecyclerView
        setupRecyclerView()

        // Load and display items
        loadItemsData()

        // Setup existing click listeners
        setupClickListeners(view)
    }

    private fun initializeViews(view: View) {
        itemsRecyclerView = view.findViewById(R.id.items_recycler_view)
        emptyStateLayout = view.findViewById(R.id.empty_state_layout)
        categorySpinner = view.findViewById(R.id.category_spinner)
        subcategorySpinner = view.findViewById(R.id.subcategory_spinner)
    }

    private fun setupCategorySpinner() {
        val categories = listOf("All Categories", "Top", "Bottom", "Outerwear", "Footwear")

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            categories
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        categorySpinner.adapter = adapter

        categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedCategoryFromSpinner = categories[position]
                if (selectedCategoryFromSpinner != selectedCategory) {
                    selectedCategory = selectedCategoryFromSpinner
                    // Reset subcategory selection when category changes
                    selectedSubcategory = "All"
                    updateSubcategorySpinner()
                    updateFilteredItems()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }
    }

    private fun setupSubcategorySpinner() {
        // Initially setup with "All Categories" subcategories
        updateSubcategorySpinner()

        subcategorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val subcategories = subcategoryMap[selectedCategory] ?: listOf("All")
                val selectedSubcategoryFromSpinner = subcategories[position]
                if (selectedSubcategoryFromSpinner != selectedSubcategory) {
                    selectedSubcategory = selectedSubcategoryFromSpinner
                    updateFilteredItems()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }
    }

    private fun updateSubcategorySpinner() {
        val subcategories = subcategoryMap[selectedCategory] ?: listOf("All")

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            subcategories
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        subcategorySpinner.adapter = adapter

        // Reset to "All" when category changes
        val allIndex = subcategories.indexOf("All")
        if (allIndex != -1) {
            subcategorySpinner.setSelection(allIndex)
        }
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
    }

    private fun updateFilteredItems() {
        // First filter by category
        val categoryFilteredItems = if (selectedCategory == "All Categories") {
            allItems
        } else {
            allItems.filter { it.category.equals(selectedCategory, ignoreCase = true) }
        }

        // Then filter by subcategory within the category-filtered items
        filteredItems = if (selectedSubcategory == "All") {
            categoryFilteredItems
        } else {
            categoryFilteredItems.filter { it.subcategory.equals(selectedSubcategory, ignoreCase = true) }
        }

        // Update the adapter with new filtered items
        itemAdapter = ItemAdapter(filteredItems) { item ->
            onItemClicked(item)
        }
        itemsRecyclerView.adapter = itemAdapter

        // Show/hide empty state
        if (filteredItems.isEmpty()) {
            itemsRecyclerView.visibility = View.GONE
            emptyStateLayout.visibility = View.VISIBLE
        } else {
            itemsRecyclerView.visibility = View.VISIBLE
            emptyStateLayout.visibility = View.GONE
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
        // Navigate to item details fragment
        val itemDetailsFragment = ItemDetails()
        val bundle = Bundle().apply {
            putString("item_id", item.id)
        }
        itemDetailsFragment.arguments = bundle

        parentFragmentManager.beginTransaction()
            .replace(R.id.flFragment, itemDetailsFragment)
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