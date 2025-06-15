package com.example.aprador.outfits

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.TextView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.aprador.R
import com.example.aprador.login.MainPage
import com.example.aprador.navigation.NavBar

class MyOutfits : Fragment(R.layout.fragment_my_outfits) {

    private lateinit var tabsLayout: LinearLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var outfitAdapter: OutfitAdapter

    private val allOutfits = listOf(
        Outfit(1, "Casual", R.drawable.shirt, "Casual"),
        Outfit(2, "Casual", R.drawable.shirt, "Casual"),
        Outfit(3, "Casual", R.drawable.shirt, "Casual"),
        Outfit(4, "Casual", R.drawable.shirt, "Casual"),
        Outfit(5, "Sport", R.drawable.shirt, "Sports"),
        Outfit(6, "Sport", R.drawable.shirt, "Sports"),
        // Add more outfits as needed
    )

    private var filteredOutfits = allOutfits
    private var selectedCategory = "All"
    private val dynamicTabs = mutableListOf<TextView>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Hide bottom navigation when this fragment loads
        view.post {
            (activity as? NavBar)?.hideBottomNavigation()
        }

        setupViews(view)
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

    private fun setupRecyclerView(view: View) {
        recyclerView = view.findViewById(R.id.recyclerView_outfits)
        outfitAdapter = OutfitAdapter(filteredOutfits) { outfit ->
            // Handle outfit click
            onOutfitClicked(outfit)
        }

        // Set GridLayoutManager with 2 columns
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        recyclerView.adapter = outfitAdapter
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

        // Filter outfits based on selected category
        filteredOutfits = if (category == "All") {
            allOutfits
        } else {
            allOutfits.filter { it.category == category }
        }

        // Update adapter with filtered outfits
        outfitAdapter.updateOutfits(filteredOutfits)
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Show bottom navigation when leaving this fragment (safety net)
        (activity as? NavBar)?.showBottomNavigation()
    }

    private fun onOutfitClicked(outfit: Outfit) {
        // Handle outfit item click - navigate to outfit detail or perform action
        // For example, you could navigate to an outfit detail fragment
        // or show a dialog with outfit options
    }
}