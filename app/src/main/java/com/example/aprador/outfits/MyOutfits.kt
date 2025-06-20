package com.example.aprador.outfits

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.TextView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.aprador.R
import com.example.aprador.landing.MainPage
import com.example.aprador.navigation.NavBar
import com.example.aprador.item_recycler.Item
import com.example.aprador.outfit_recycler.Outfit
import com.example.aprador.outfit_recycler.OutfitSection
import com.example.aprador.outfit_recycler.OutfitSectionAdapter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

class MyOutfits : Fragment(R.layout.fragment_my_outfits) {

    private lateinit var tabsLayout: LinearLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var outfitSectionAdapter: OutfitSectionAdapter

    private var allOutfits = listOf<Outfit>()
    private var outfitSections = listOf<OutfitSection>()
    private var selectedCategory = "All"
    private val dynamicTabs = mutableListOf<TextView>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews(view)
        loadOutfitsData()
        setupRecyclerView(view)
        setupClickListeners(view)
        setupDynamicTabs()
    }

    private fun setupViews(view: View) {
        try {
            tabsLayout = view.findViewById(R.id.tabs_layout)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadOutfitsData() {
        allOutfits = loadOutfits(requireContext())
        updateOutfitSections()
    }

    private fun updateOutfitSections() {
        if (selectedCategory == "All") {
            // Group all outfits by category, maintaining the latest first order within each category
            val groupedOutfits = allOutfits.groupBy { it.category }
            outfitSections = groupedOutfits.map { (category, outfits) ->
                // Sort outfits within each category by createdAt descending (latest first)
                OutfitSection(category, outfits.sortedByDescending { it.createdAt })
            }.sortedBy { it.category }
        } else {
            // Show only selected category, sorted by createdAt descending (latest first)
            val filteredOutfits = allOutfits.filter { it.category == selectedCategory }
                .sortedByDescending { it.createdAt }
            outfitSections = if (filteredOutfits.isNotEmpty()) {
                listOf(OutfitSection(selectedCategory, filteredOutfits))
            } else {
                emptyList()
            }
        }
    }

    private fun loadOutfits(context: Context): List<Outfit> {
        return try {
            val file = File(context.filesDir, "outfits.json")
            if (file.exists() && file.readText().isNotBlank()) {
                val json = file.readText()
                val type = object : TypeToken<List<Outfit>>() {}.type
                val outfits: List<Outfit> = Gson().fromJson(json, type) ?: emptyList()
                // Sort by createdAt timestamp in descending order (latest first)
                outfits.sortedByDescending { it.createdAt }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun setupRecyclerView(view: View) {
        recyclerView = view.findViewById(R.id.recyclerView_outfits)
        outfitSectionAdapter = OutfitSectionAdapter(outfitSections, requireContext()) { outfit ->
            // Handle outfit click
            onOutfitClicked(outfit)
        }

        // Set LinearLayoutManager with 1 column (vertical)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = outfitSectionAdapter
    }

    private fun setupClickListeners(view: View) {
        // Move to MainPage (Back button)
        val outfitView: View? = view.findViewById(R.id.BackOutfit)
        outfitView?.setOnClickListener {
            // Show bottom navigation when going back to MainPage
            (activity as? NavBar)?.showBottomNavigation()

            val outfitFragment = MainPage()
            parentFragmentManager.beginTransaction()
                .replace(R.id.flFragment, outfitFragment)
                .addToBackStack(null)
                .commit()
        }

        // Navigate to CreateOutfit fragment
        val createOutfitView: View? = view.findViewById(R.id.CreateOutfitButton)
        createOutfitView?.setOnClickListener {
            val createOutfitFragment = CreateOutfit()
            parentFragmentManager.beginTransaction()
                .replace(R.id.flFragment, createOutfitFragment)
                .addToBackStack(null)
                .commit()
        }
    }

    private fun setupDynamicTabs() {
        // Clear existing tabs
        tabsLayout.removeAllViews()
        dynamicTabs.clear()

        // Get unique categories and their counts
        val categoryCounts = allOutfits.groupingBy { it.category }.eachCount()
        val totalCount = allOutfits.size

        // Create "All" tab
        val allTab = createTabView("All", totalCount, selectedCategory == "All")
        tabsLayout.addView(allTab)
        dynamicTabs.add(allTab)

        // Create tabs for each category that has outfits
        categoryCounts.forEach { (category, count) ->
            val categoryTab = createTabView(category, count, selectedCategory == category)
            tabsLayout.addView(categoryTab)
            dynamicTabs.add(categoryTab)
        }
    }

    private fun createTabView(category: String, count: Int, isSelected: Boolean): TextView {
        val tabView = TextView(requireContext())

        // Set layout parameters
        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins(0, 0, dpToPx(8), 0) // 8dp margin end
        tabView.layoutParams = layoutParams

        // Set text and appearance
        tabView.text = if (category == "All") "All ($count)" else "$category ($count)"
        tabView.setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8))
        tabView.textSize = 14f

        // Set click listener
        tabView.setOnClickListener {
            selectTab(category)
        }

        // Set initial appearance
        updateTabAppearance(tabView, isSelected)

        return tabView
    }

    private fun updateTabAppearance(tabView: TextView, isSelected: Boolean) {
        try {
            if (isSelected) {
                tabView.setBackgroundResource(R.drawable.tab_selected_background)
                tabView.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
            } else {
                tabView.setBackgroundResource(R.drawable.tab_unselected_background)
                tabView.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun selectTab(category: String) {
        selectedCategory = category

        // Update all tab appearances
        val categoryCounts = allOutfits.groupingBy { it.category }.eachCount()
        val totalCount = allOutfits.size

        dynamicTabs.forEachIndexed { index, tabView ->
            val tabCategory = if (index == 0) "All" else categoryCounts.keys.elementAtOrNull(index - 1) ?: ""
            val isSelected = tabCategory == selectedCategory
            updateTabAppearance(tabView, isSelected)
        }

        // Update outfit sections based on selected category
        updateOutfitSections()

        // Update adapter with new sections
        outfitSectionAdapter.updateSections(outfitSections)
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }

    override fun onResume() {
        super.onResume()
        // Refresh outfits data when returning to this fragment
        loadOutfitsData()
        outfitSectionAdapter.updateSections(outfitSections)
        setupDynamicTabs()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Show bottom navigation when leaving this fragment (safety net)
        (activity as? NavBar)?.showBottomNavigation()
    }

    // In MyOutfits.kt (if you have such navigation)
    private fun onOutfitClicked(outfit: Outfit) {
        val outfitDetailsFragment = OutfitDetails.newInstance(outfit.id, "MyOutfits")
        parentFragmentManager.beginTransaction()
            .replace(R.id.flFragment, outfitDetailsFragment)
            .addToBackStack(null)
            .commit()
    }

    private fun showOutfitDetails(outfit: Outfit) {
        // Load items that belong to this outfit
        val allItems = loadItems(requireContext())
        val outfitItems = allItems.filter { item -> outfit.items.contains(item.id) }

        val itemNames = outfitItems.joinToString(", ") { it.name }
        val message = "Outfit: ${outfit.title}\n" +
                "Category: ${outfit.category}\n" +
                "Gender: ${outfit.gender}\n" +
                "Items: $itemNames"

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Outfit Details")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
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
}