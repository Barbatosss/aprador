package com.example.aprador.outfits

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
import com.example.aprador.recycler.Item
import android.net.Uri
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.example.aprador.recycler.ItemAdapter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

class CreateOutfit : Fragment(R.layout.fragment_create_outfit) {

    private lateinit var itemsRecyclerView: RecyclerView
    private lateinit var itemAdapter: ItemAdapter
    private lateinit var itemsEmptyState: LinearLayout
    private lateinit var backButton: View
    private lateinit var saveButton: View
    private lateinit var categorySpinner: Spinner
    private lateinit var subcategorySpinner: Spinner
    private lateinit var genderSpinner: Spinner
    private lateinit var outfitCategorySpinner: Spinner

    // Gender toggle tabs
    private lateinit var tabMen: TextView
    private lateinit var tabWomen: TextView

    private var allItems = listOf<Item>()
    private var filteredItems = listOf<Item>()
    private var selectedItemCategory = "All Categories"
    private var selectedItemSubcategory = "All"
    private var selectedGender = "Men" // Default gender
    private var selectedOutfitCategory = "Casual" // Default category
    private val selectedItems = mutableListOf<Item>() // Track selected items for outfit

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

        // Setup gender filter
        setupGenderFilter()

        // Setup dropdowns
        setupCategorySpinner()
        setupSubcategorySpinner()
        setupGenderSpinner()
        setupOutfitCategorySpinner()

        // Setup RecyclerView
        setupRecyclerView()

        // Load and display items
        loadItemsData()

        // Setup click listeners
        setupClickListeners()
    }

    private fun initializeViews(view: View) {
        itemsRecyclerView = view.findViewById(R.id.items_recycler_view)
        itemsEmptyState = view.findViewById(R.id.items_empty_state)
        backButton = view.findViewById(R.id.BackCreateOutfit)
        saveButton = view.findViewById(R.id.SaveOutfit)
        categorySpinner = view.findViewById(R.id.category_spinner)
        subcategorySpinner = view.findViewById(R.id.subcategory_spinner)
        genderSpinner = view.findViewById(R.id.gender_spinner)
        outfitCategorySpinner = view.findViewById(R.id.outfit_category_spinner)

        // Gender tabs
        tabMen = view.findViewById(R.id.toggle_men)
        tabWomen = view.findViewById(R.id.toggle_women)
    }

    private fun setupGenderFilter() {
        // Initialize with default gender selection
        selectGenderTab("Men")

        // Set up gender tab click listeners
        tabMen.setOnClickListener { selectGenderTab("Men") }
        tabWomen.setOnClickListener { selectGenderTab("Women") }
    }

    private fun selectGenderTab(gender: String) {
        selectedGender = gender

        // Reset all gender tabs
        resetGenderTabs()

        // Set selected gender tab
        val selectedTab = when (gender) {
            "Men" -> tabMen
            "Women" -> tabWomen
            else -> tabMen
        }

        selectedTab.setBackgroundResource(R.drawable.tab_selected_background)
        selectedTab.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))

        // Update subcategory options when gender changes
        updateSubcategoryOptions()

        // Update filtered items when gender changes
        updateFilteredItems()
    }

    private fun resetGenderTabs() {
        val genderTabs = arrayOf(tabMen, tabWomen)

        genderTabs.forEach { tab ->
            tab.setBackgroundResource(R.drawable.tab_unselected_background)
            tab.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
        }
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
                if (selectedCategoryFromSpinner != selectedItemCategory) {
                    selectedItemCategory = selectedCategoryFromSpinner
                    // Reset subcategory selection when category changes
                    selectedItemSubcategory = "All"
                    updateSubcategorySpinner()
                    updateSubcategoryOptions()
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
                val subcategories = getFilteredSubcategories()
                val selectedSubcategoryFromSpinner = subcategories[position]
                if (selectedSubcategoryFromSpinner != selectedItemSubcategory) {
                    selectedItemSubcategory = selectedSubcategoryFromSpinner
                    updateFilteredItems()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }
    }

    private fun setupGenderSpinner() {
        val genders = listOf("Men", "Women")

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            genders
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        genderSpinner.adapter = adapter

        // Set default selection
        genderSpinner.setSelection(genders.indexOf(selectedGender))

        genderSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedGenderFromSpinner = genders[position]
                if (selectedGenderFromSpinner != selectedGender) {
                    selectedGender = selectedGenderFromSpinner
                    selectGenderTab(selectedGender)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }
    }

    private fun setupOutfitCategorySpinner() {
        val outfitCategories = listOf("Casual", "Ethnic", "Formal", "Sports", "Smart Casual", "Travel", "Party", "Home")

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            outfitCategories
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        outfitCategorySpinner.adapter = adapter

        // Set default selection
        outfitCategorySpinner.setSelection(outfitCategories.indexOf(selectedOutfitCategory))

        outfitCategorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedCategoryFromSpinner = outfitCategories[position]
                if (selectedCategoryFromSpinner != selectedOutfitCategory) {
                    selectedOutfitCategory = selectedCategoryFromSpinner
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }
    }

    private fun updateSubcategoryOptions() {
        when (selectedItemCategory) {
            "Top" -> {
                // Update subcategory based on gender
                if (selectedGender == "Men") {
                    // Auto-select appropriate subcategory for men
                    if (!listOf("All", "T-Shirts", "Polo", "Dress Shirt", "Tank Top").contains(selectedItemSubcategory)) {
                        selectedItemSubcategory = "All"
                    }
                } else { // Women
                    // Auto-select appropriate subcategory for women
                    if (!listOf("All", "T-Shirts", "Blouse", "Camisole", "Crop Top").contains(selectedItemSubcategory)) {
                        selectedItemSubcategory = "All"
                    }
                }
            }
            "Bottom" -> {
                // Update subcategory based on gender
                if (selectedGender == "Men") {
                    // Auto-select appropriate subcategory for men
                    if (!listOf("All", "Jeans", "Chinos", "Shorts", "Joggers").contains(selectedItemSubcategory)) {
                        selectedItemSubcategory = "All"
                    }
                } else { // Women
                    // Auto-select appropriate subcategory for women
                    if (!listOf("All", "Jeans", "Leggings", "Skirt", "Dress").contains(selectedItemSubcategory)) {
                        selectedItemSubcategory = "All"
                    }
                }
            }
            "Outerwear" -> {
                // Update subcategory based on gender
                if (selectedGender == "Men") {
                    // Auto-select appropriate subcategory for men
                    if (!listOf("All", "Jacket", "Hoodie", "Blazer", "Coat").contains(selectedItemSubcategory)) {
                        selectedItemSubcategory = "All"
                    }
                } else { // Women
                    // Auto-select appropriate subcategory for women
                    if (!listOf("All", "Cardigan", "Blazer", "Coat", "Kimono").contains(selectedItemSubcategory)) {
                        selectedItemSubcategory = "All"
                    }
                }
            }
            "Footwear" -> {
                // Update subcategory based on gender
                if (selectedGender == "Men") {
                    // Auto-select appropriate subcategory for men
                    if (!listOf("All", "Sneakers", "Dress Shoes", "Boots", "Sandals").contains(selectedItemSubcategory)) {
                        selectedItemSubcategory = "All"
                    }
                } else { // Women
                    // Auto-select appropriate subcategory for women
                    if (!listOf("All", "Sneakers", "Heels", "Flats", "Boots").contains(selectedItemSubcategory)) {
                        selectedItemSubcategory = "All"
                    }
                }
            }
        }
        updateSubcategorySpinner()
    }

    private fun getFilteredSubcategories(): List<String> {
        return when (selectedItemCategory) {
            "Top" -> {
                if (selectedGender == "Men") {
                    listOf("All", "T-Shirts", "Polo", "Dress Shirt", "Tank Top")
                } else {
                    listOf("All", "T-Shirts", "Blouse", "Camisole", "Crop Top")
                }
            }
            "Bottom" -> {
                if (selectedGender == "Men") {
                    listOf("All", "Jeans", "Chinos", "Shorts", "Joggers")
                } else {
                    listOf("All", "Jeans", "Leggings", "Skirt", "Dress")
                }
            }
            "Outerwear" -> {
                if (selectedGender == "Men") {
                    listOf("All", "Jacket", "Hoodie", "Blazer", "Coat")
                } else {
                    listOf("All", "Cardigan", "Blazer", "Coat", "Kimono")
                }
            }
            "Footwear" -> {
                if (selectedGender == "Men") {
                    listOf("All", "Sneakers", "Dress Shoes", "Boots", "Sandals")
                } else {
                    listOf("All", "Sneakers", "Heels", "Flats", "Boots")
                }
            }
            else -> listOf("All")
        }
    }

    private fun updateSubcategorySpinner() {
        val subcategories = getFilteredSubcategories()

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            subcategories
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        subcategorySpinner.adapter = adapter

        // Set selection to current subcategory or "All" if not found
        val currentIndex = subcategories.indexOf(selectedItemSubcategory)
        if (currentIndex != -1) {
            subcategorySpinner.setSelection(currentIndex)
        } else {
            subcategorySpinner.setSelection(0) // Select "All"
            selectedItemSubcategory = "All"
        }
    }

    private fun setupRecyclerView() {
        itemAdapter = ItemAdapter(filteredItems) { item ->
            onItemClicked(item)
        }

        itemsRecyclerView.apply {
            adapter = itemAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            isNestedScrollingEnabled = false
        }
    }

    private fun setupClickListeners() {
        // Back button - navigate to MyOutfits
        backButton.setOnClickListener {
            navigateToMyOutfits()
        }

        // Save button - handle outfit saving
        saveButton.setOnClickListener {
            saveOutfit()
        }
    }

    private fun loadItemsData() {
        allItems = loadItems(requireContext())
        updateFilteredItems()
    }

    private fun updateFilteredItems() {
        // First filter by gender based on subcategory
        val genderFilteredItems = allItems.filter { item ->
            isItemForSelectedGender(item)
        }

        // Then filter by category
        val categoryFilteredItems = if (selectedItemCategory == "All Categories") {
            genderFilteredItems
        } else {
            genderFilteredItems.filter { it.category.equals(selectedItemCategory, ignoreCase = true) }
        }

        // Finally filter by subcategory within the category-filtered items
        filteredItems = if (selectedItemSubcategory == "All") {
            categoryFilteredItems
        } else {
            categoryFilteredItems.filter { it.subcategory.equals(selectedItemSubcategory, ignoreCase = true) }
        }

        // Update the adapter with new filtered items
        itemAdapter = ItemAdapter(filteredItems) { item ->
            onItemClicked(item)
        }
        itemsRecyclerView.adapter = itemAdapter

        updateEmptyState()
    }

    /**
     * Determines if an item belongs to the selected gender based on its subcategory
     */
    private fun isItemForSelectedGender(item: Item): Boolean {
        val menSubcategories = setOf(
            // Tops
            "Polo", "Dress Shirt", "Tank Top",
            // Bottoms
            "Chinos", "Joggers",
            // Outerwear
            "Jacket", "Hoodie",
            // Footwear
            "Dress Shoes"
        )

        val womenSubcategories = setOf(
            // Tops
            "Blouse", "Camisole", "Crop Top",
            // Bottoms
            "Leggings", "Skirt", "Dress",
            // Outerwear
            "Cardigan", "Kimono",
            // Footwear
            "Heels", "Flats"
        )

        val neutralSubcategories = setOf(
            // Shared items
            "T-Shirts", "Jeans", "Shorts", "Blazer", "Coat", "Sneakers", "Boots", "Sandals"
        )

        return when (selectedGender) {
            "Men" -> {
                // Show men-specific items and neutral items, but not women-specific items
                !womenSubcategories.contains(item.subcategory)
            }
            "Women" -> {
                // Show women-specific items and neutral items, but not men-specific items
                !menSubcategories.contains(item.subcategory)
            }
            else -> true // Fallback to show all items
        }
    }

    private fun updateEmptyState() {
        if (filteredItems.isEmpty()) {
            itemsRecyclerView.visibility = View.GONE
            itemsEmptyState.visibility = View.VISIBLE
        } else {
            itemsRecyclerView.visibility = View.VISIBLE
            itemsEmptyState.visibility = View.GONE
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
        // Check if item is being removed from selection
        if (selectedItems.contains(item)) {
            selectedItems.remove(item)
            Toast.makeText(requireContext(), "Removed ${item.name} from outfit", Toast.LENGTH_SHORT).show()
        } else {
            // Check if there's already an item from the same category
            val existingItemInCategory = selectedItems.find {
                it.category.equals(item.category, ignoreCase = true)
            }

            if (existingItemInCategory != null) {
                // Replace the existing item with the new one
                selectedItems.remove(existingItemInCategory)
                selectedItems.add(item)
                Toast.makeText(
                    requireContext(),
                    "Replaced ${existingItemInCategory.name} with ${item.name} in ${item.category} category",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                // Check if we've reached the maximum of 4 items (one per main category)
                val mainCategories = setOf("Top", "Bottom", "Outerwear", "Footwear")
                val selectedMainCategories = selectedItems.map { it.category }.toSet()

                if (selectedMainCategories.size >= 4) {
                    Toast.makeText(
                        requireContext(),
                        "Maximum 4 items allowed (one per category: Top, Bottom, Outerwear, Footwear)",
                        Toast.LENGTH_LONG
                    ).show()
                    return
                }

                // Add the new item
                selectedItems.add(item)
                Toast.makeText(requireContext(), "Added ${item.name} to outfit", Toast.LENGTH_SHORT).show()
            }
        }

        // Update outfit preview
        updateOutfitPreview()
    }

    private fun updateOutfitPreview() {
        // Update the outfit preview container
        val outfitPreviewPlaceholder = view?.findViewById<TextView>(R.id.outfit_preview_placeholder)
        val outfitItemsContainer = view?.findViewById<LinearLayout>(R.id.outfit_items_container)

        if (selectedItems.isEmpty()) {
            outfitPreviewPlaceholder?.visibility = View.VISIBLE
            outfitItemsContainer?.visibility = View.GONE

            // Update placeholder text to show category guidance
            outfitPreviewPlaceholder?.text = "Select items from different categories:\nTop, Bottom, Outerwear, Footwear"
        } else {
            outfitPreviewPlaceholder?.visibility = View.GONE
            outfitItemsContainer?.visibility = View.VISIBLE

            // Clear existing preview items
            outfitItemsContainer?.removeAllViews()

            // Sort items by category order for consistent display
            val categoryOrder = listOf("Top", "Bottom", "Outerwear", "Footwear")
            val sortedItems = selectedItems.sortedBy { item ->
                categoryOrder.indexOf(item.category).takeIf { it >= 0 } ?: Int.MAX_VALUE
            }

            // Add selected items to preview
            sortedItems.forEach { item ->
                val itemView = layoutInflater.inflate(R.layout.item_card, outfitItemsContainer, false)

                // Set up the preview item
                val itemImage = itemView.findViewById<ImageView>(R.id.item_image)
                val itemName = itemView.findViewById<TextView>(R.id.item_name)

                // Show category name instead of item name for clarity
                itemName.text = item.category
                itemName.visibility = View.VISIBLE
                itemName.textSize = 12f

                // Load the item image
                val context = requireContext()
                val imageSource = when {
                    item.imagePath.startsWith("content://") -> Uri.parse(item.imagePath)
                    item.imagePath.startsWith("file://") -> Uri.parse(item.imagePath)
                    File(item.imagePath).exists() -> File(item.imagePath)
                    else -> null
                }

                if (imageSource != null) {
                    Glide.with(context)
                        .load(imageSource)
                        .centerCrop()
                        .placeholder(R.drawable.shirt)
                        .error(R.drawable.shirt)
                        .into(itemImage)
                } else {
                    itemImage.setImageResource(R.drawable.shirt)
                }

                // Add click listener to remove item from outfit when clicked in preview
                itemView.setOnClickListener {
                    selectedItems.remove(item)
                    Toast.makeText(requireContext(), "Removed ${item.name} from outfit", Toast.LENGTH_SHORT).show()
                    updateOutfitPreview()
                }

                outfitItemsContainer?.addView(itemView)
            }
        }
    }

    private fun saveOutfit() {
        if (selectedItems.isEmpty()) {
            Toast.makeText(requireContext(), "Please select items for your outfit", Toast.LENGTH_SHORT).show()
            return
        }

        // Here you would implement the logic to save the outfit
        // For now, just show a success message
        Toast.makeText(
            requireContext(),
            "Outfit saved with ${selectedItems.size} items in $selectedOutfitCategory category for $selectedGender",
            Toast.LENGTH_LONG
        ).show()

        // Navigate back to MyOutfits after saving
        navigateToMyOutfits()
    }

    private fun navigateToMyOutfits() {
        try {
            val myOutfitsFragment = MyOutfits()
            parentFragmentManager.beginTransaction()
                .replace(R.id.flFragment, myOutfitsFragment)
                .addToBackStack(null)
                .commit()
        } catch (e: Exception) {
            // Fallback navigation
            parentFragmentManager.popBackStack()
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when returning to this fragment
        if (::itemAdapter.isInitialized) {
            loadItemsData()
        }
        // Ensure correct gender tab appearance
        selectGenderTab(selectedGender)
    }
}