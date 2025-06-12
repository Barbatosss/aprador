package com.example.aprador

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat

class AddItem : Fragment(R.layout.fragment_add_item) {

    // Category tabs
    private lateinit var tabBottom: TextView
    private lateinit var tabTop: TextView
    private lateinit var tabOuterwear: TextView

    // Subcategory tabs
    private lateinit var tabTShirt: TextView
    private lateinit var tabSweater: TextView
    private lateinit var tabShirt: TextView
    private lateinit var tabJacket: TextView

    // Current selections
    private var selectedCategory = "Top"
    private var selectedSubcategory = "T-Shirt"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Hide bottom navigation when this fragment loads
        (activity as? NavBar)?.hideBottomNavigation()

        // Initialize category tabs
        tabBottom = view.findViewById(R.id.tab_bottom)
        tabTop = view.findViewById(R.id.tab_top)
        tabOuterwear = view.findViewById(R.id.tab_outerwear)

        // Initialize subcategory tabs
        tabTShirt = view.findViewById(R.id.tab_tshirt)
        tabSweater = view.findViewById(R.id.tab_sweater)
        tabShirt = view.findViewById(R.id.tab_shirt)
        tabJacket = view.findViewById(R.id.tab_jacket)

        // Set up category tab click listeners
        tabBottom.setOnClickListener { selectCategoryTab("Bottom") }
        tabTop.setOnClickListener { selectCategoryTab("Top") }
        tabOuterwear.setOnClickListener { selectCategoryTab("Outerwear") }

        // Set up subcategory tab click listeners
        tabTShirt.setOnClickListener { selectSubcategoryTab("T-Shirt") }
        tabSweater.setOnClickListener { selectSubcategoryTab("Sweater") }
        tabShirt.setOnClickListener { selectSubcategoryTab("Shirt") }
        tabJacket.setOnClickListener { selectSubcategoryTab("Jacket") }

        // Move to AddItem
        val myItemsView: View = view.findViewById(R.id.BackAddItem)
        myItemsView.setOnClickListener {
            val outfitFragment = MainPage()
            parentFragmentManager.beginTransaction()
                .replace(R.id.flFragment, outfitFragment)
                .addToBackStack(null)
                .commit()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Show bottom navigation when leaving this fragment (safety net)
        (activity as? NavBar)?.showBottomNavigation()
    }

    private fun selectCategoryTab(category: String) {
        selectedCategory = category

        // Reset all category tabs
        resetCategoryTabs()

        // Set selected category tab
        val selectedTab = when (category) {
            "Bottom" -> tabBottom
            "Top" -> tabTop
            "Outerwear" -> tabOuterwear
            else -> tabTop
        }

        selectedTab.setBackgroundResource(R.drawable.tab_selected_background)
        selectedTab.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))

        // Update subcategory options based on selected category
        updateSubcategoryOptions()
    }

    private fun selectSubcategoryTab(subcategory: String) {
        selectedSubcategory = subcategory

        // Reset all subcategory tabs
        resetSubcategoryTabs()

        // Set selected subcategory tab
        val selectedTab = when (subcategory) {
            "T-Shirt" -> tabTShirt
            "Sweater" -> tabSweater
            "Shirt" -> tabShirt
            "Jacket" -> tabJacket
            else -> tabTShirt
        }

        selectedTab.setBackgroundResource(R.drawable.tab_selected_background)
        selectedTab.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
    }

    private fun resetCategoryTabs() {
        val categoryTabs = arrayOf(tabBottom, tabTop, tabOuterwear)

        categoryTabs.forEach { tab ->
            tab.setBackgroundResource(R.drawable.tab_unselected_background)
            tab.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
        }
    }

    private fun resetSubcategoryTabs() {
        val subcategoryTabs = arrayOf(tabTShirt, tabSweater, tabShirt, tabJacket)

        subcategoryTabs.forEach { tab ->
            tab.setBackgroundResource(R.drawable.tab_unselected_background)
            tab.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
        }
    }

    private fun updateSubcategoryOptions() {
        // Show/hide subcategory options based on selected category
        when (selectedCategory) {
            "Top" -> {
                // Show T-Shirt, Sweater, Shirt options
                tabTShirt.visibility = View.VISIBLE
                tabSweater.visibility = View.VISIBLE
                tabShirt.visibility = View.VISIBLE
                tabJacket.visibility = View.GONE // Hide jacket for tops

                // Auto-select T-Shirt if current selection is not available
                if (selectedSubcategory == "Jacket") {
                    selectSubcategoryTab("T-Shirt")
                }
            }
            "Bottom" -> {
                // You can customize subcategories for bottoms
                // For now, hiding all current subcategories
                tabTShirt.visibility = View.GONE
                tabSweater.visibility = View.GONE
                tabShirt.visibility = View.GONE
                tabJacket.visibility = View.GONE

                // You might want to add bottom-specific subcategories like:
                // Jeans, Shorts, Pants, Skirts, etc.
            }
            "Outerwear" -> {
                // Show Jacket, hide others
                tabTShirt.visibility = View.GONE
                tabSweater.visibility = View.VISIBLE // Sweaters can be outerwear
                tabShirt.visibility = View.GONE
                tabJacket.visibility = View.VISIBLE

                // Auto-select Jacket if current selection is not available
                if (selectedSubcategory == "T-Shirt" || selectedSubcategory == "Shirt") {
                    selectSubcategoryTab("Jacket")
                }
            }
        }
    }

    private fun addItem() {
        // Handle the logic for adding the item with selected category and subcategory
        // You can access selectedCategory and selectedSubcategory variables here

        // Example: Log the selections or save to database
        println("Adding item - Category: $selectedCategory, Subcategory: $selectedSubcategory")

        // Navigate back or show confirmation
        parentFragmentManager.popBackStack()
    }
}