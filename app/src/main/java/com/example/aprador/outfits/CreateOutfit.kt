package com.example.aprador.outfits

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
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
import com.example.aprador.item_recycler.Item
import android.os.Build
import android.provider.MediaStore
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.example.aprador.item_recycler.ItemAdapter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import android.graphics.Canvas
import android.graphics.Color
import java.io.FileOutputStream
import java.io.IOException
import com.example.aprador.login.UserPreferences
import androidx.core.graphics.createBitmap
import androidx.core.view.isVisible
import com.example.aprador.outfit_recycler.Outfit
import androidx.core.net.toUri
import androidx.core.graphics.scale
import androidx.core.graphics.get


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

    private lateinit var categoryPredection : TextView

    // Gender toggle tabs
    private lateinit var tabMen: TextView
    private lateinit var tabWomen: TextView

    private lateinit var userPreferences: UserPreferences

    private lateinit var tflite: Interpreter

    private var allItems = listOf<Item>()
    private var filteredItems = listOf<Item>()
    private var selectedItemCategory = "All Categories"
    private var selectedItemSubcategory = "All"
    private var selectedGender = "Men" // This will be updated from user preferences
    private var selectedOutfitCategory = "Casual" // Default category
    private val selectedItems = mutableListOf<Item>() // Track selected items for outfit



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userPreferences = UserPreferences(requireContext())

        loadUserGenderPreference()

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

        tflite = Interpreter(loadModelFile())
    }

    private fun loadUserGenderPreference() {
        // Get the user's gender preference from UserPreferences
        val userGender = userPreferences.getUserGender()

        // Map the gender to match our toggle options
        selectedGender = when (userGender.lowercase()) {
            "male" -> "Men"
            "female" -> "Women"
            else -> "Men" // Default fallback
        }
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

        categoryPredection = view.findViewById(R.id.categoryprediction)
    }

    private fun setupGenderFilter() {
        // Initialize with default gender selection
        selectGenderTab(selectedGender)

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

        // Set selection to user's preferred gender
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

    private fun saveNewOutfitToJson(context: Context, newOutfit: Outfit) {
        val file = File(context.filesDir, "outfits.json")

        // Read existing outfits
        val outfits: MutableList<Outfit> = if (file.exists() && file.readText().isNotBlank()) {
            val json = file.readText()
            val type = object : TypeToken<List<Outfit>>() {}.type
            try {
                Gson().fromJson(json, type)
            } catch (e: Exception) {
                e.printStackTrace()
                mutableListOf()
            }
        } else {
            mutableListOf()
        }

        // Add new outfit
        outfits.add(newOutfit)

        // Write updated list back to file
        val updatedJson = Gson().toJson(outfits)
        file.writeText(updatedJson)
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

            // When item is removed, go back to that category
            navigateToCategoryAfterRemoval(item.category)
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

                // After replacement, move to next category in sequence
                navigateToNextCategory()
            } else {
                // Check if we've reached the maximum of 4 items (one per main category)
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

                // After adding item, move to next category in sequence
                navigateToNextCategory()

                if (selectedItems.size in 3..4) {
                    val bitmaps = selectedItems.mapNotNull { imagePathToBitmap(requireContext(), it.imagePath) }
                    if (bitmaps.size == selectedItems.size) {
                        val prediction = predictCategoryFromBitmaps(bitmaps, requireContext())
                        categoryPredection.text = prediction
                    } else {
                        categoryPredection.text = "Unable to convert image(s)"
                    }
                } else {
                    categoryPredection.text = ""
                }
            }
        }

        // Update outfit preview
        updateOutfitPreview()
    }

    private fun navigateToNextCategory() {
        // Define the order of categories
        val categoryOrder = listOf("Top", "Bottom", "Outerwear", "Footwear")
        val selectedCategories = selectedItems.map { it.category.lowercase() }.toSet()

        // Find the next category that hasn't been selected yet
        val nextCategory = categoryOrder.find { category ->
            !selectedCategories.contains(category.lowercase())
        }

        // If there's a next category, navigate to it
        nextCategory?.let { category ->
            navigateToCategory(category)
        }
    }

    private fun navigateToCategoryAfterRemoval(removedCategory: String) {
        // When an item is removed, go back to that category
        navigateToCategory(removedCategory)
    }

    private fun navigateToCategory(category: String) {
        // Update the category spinner selection
        val categories = listOf("All Categories", "Top", "Bottom", "Outerwear", "Footwear")
        val categoryIndex = categories.indexOf(category)

        if (categoryIndex != -1) {
            // Temporarily disable the listener to prevent infinite loops
            categorySpinner.onItemSelectedListener = null

            // Set the spinner selection
            categorySpinner.setSelection(categoryIndex)

            // Update the selected category
            selectedItemCategory = category

            // Reset subcategory to "All" when category changes
            selectedItemSubcategory = "All"

            // Update subcategory options and spinner
            updateSubcategoryOptions()
            updateSubcategorySpinner()

            // Update filtered items
            updateFilteredItems()

            // Re-enable the listener
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

            Toast.makeText(
                requireContext(),
                "Switched to $category category",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun updateCategorySpinnerToCategory(itemCategory: String) {
        val categories = listOf("All Categories", "Top", "Bottom", "Outerwear", "Footwear")
        val categoryIndex = categories.indexOf(itemCategory)

        if (categoryIndex != -1) {
            categorySpinner.setSelection(categoryIndex)
            // This will trigger the onItemSelected listener and update the filtered items
        }
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

            // Create a simple 2x2 grid using nested LinearLayouts
            val mainLayout = LinearLayout(requireContext())
            mainLayout.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            mainLayout.orientation = LinearLayout.VERTICAL
            mainLayout.background = ContextCompat.getDrawable(requireContext(), R.drawable.tab_unselected_background)

            // Create top row (OUTERWEAR | TOP)
            val topRow = LinearLayout(requireContext())
            topRow.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f // Takes 50% height
            )
            topRow.orientation = LinearLayout.HORIZONTAL

            // Create bottom row (FOOTWEAR | BOTTOM)
            val bottomRow = LinearLayout(requireContext())
            bottomRow.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f // Takes 50% height
            )
            bottomRow.orientation = LinearLayout.HORIZONTAL

            // Create quadrant containers
            val outerwearContainer = createQuadrantContainer()
            val topContainer = createQuadrantContainer()
            val footwearContainer = createQuadrantContainer()
            val bottomContainer = createQuadrantContainer()

            // Add containers to rows
            topRow.addView(outerwearContainer)
            topRow.addView(topContainer)
            bottomRow.addView(footwearContainer)
            bottomRow.addView(bottomContainer)

            // Add rows to main layout
            mainLayout.addView(topRow)
            mainLayout.addView(bottomRow)

            // Add items to their respective quadrants
            selectedItems.forEach { item ->
                val quadrantContainer = when (item.category.uppercase()) {
                    "TOP" -> topContainer
                    "BOTTOM" -> bottomContainer
                    "OUTERWEAR" -> outerwearContainer
                    "FOOTWEAR" -> footwearContainer
                    else -> topContainer // fallback
                }

                val itemView = createItemView(item)
                quadrantContainer.addView(itemView)
            }

            // Add the main layout to the container
            outfitItemsContainer?.addView(mainLayout)
        }
    }

    private fun createQuadrantContainer(): LinearLayout {
        val container = LinearLayout(requireContext())
        container.layoutParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.MATCH_PARENT,
            1f // Takes 50% width
        )
        container.orientation = LinearLayout.VERTICAL
        container.gravity = android.view.Gravity.CENTER
        // Add subtle border
        container.setPadding(2, 2, 2, 2)
        container.background = ContextCompat.getDrawable(requireContext(), android.R.color.white)
        return container
    }

    private fun createItemView(item: Item): View {
        val itemView = layoutInflater.inflate(R.layout.item_card, null, false)

        // Set up the preview item
        val itemImage = itemView.findViewById<ImageView>(R.id.item_image)
        val itemName = itemView.findViewById<TextView>(R.id.item_name)

        // Hide the item name to show only the image
        itemName.visibility = View.GONE

        // Make the item view fill the quadrant
        itemView.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )

        // Load the item image
        val context = requireContext()
        val imageSource = when {
            item.imagePath.startsWith("content://") -> item.imagePath.toUri()
            item.imagePath.startsWith("file://") -> item.imagePath.toUri()
            File(item.imagePath).exists() -> File(item.imagePath)
            else -> null
        }

        if (imageSource != null) {
            Glide.with(context)
                .load(imageSource)
                .fitCenter() // Changed from centerCrop() to fitCenter()
                .placeholder(R.drawable.shirt)
                .error(R.drawable.shirt)
                .into(itemImage)
        } else {
            itemImage.setImageResource(R.drawable.shirt)
        }

        // Make the image view fill the entire quadrant
        itemImage.layoutParams = itemImage.layoutParams.apply {
            width = LinearLayout.LayoutParams.MATCH_PARENT
            height = LinearLayout.LayoutParams.MATCH_PARENT
        }
        // Changed from CENTER_CROP to FIT_CENTER to show full image without cropping
        itemImage.scaleType = ImageView.ScaleType.FIT_CENTER

        // Add click listener to remove item from outfit when clicked in preview
        itemView.setOnClickListener {
            selectedItems.remove(item)
            Toast.makeText(requireContext(), "Removed ${item.name} from outfit", Toast.LENGTH_SHORT).show()
            updateCategorySpinnerToCategory(item.category)
            updateOutfitPreview()

            // Update category prediction when items are removed
            if (selectedItems.size in 3..4) {
                val bitmaps = selectedItems.mapNotNull { imagePathToBitmap(requireContext(), it.imagePath) }
                if (bitmaps.size == selectedItems.size) {
                    val prediction = predictCategoryFromBitmaps(bitmaps, requireContext())
                    categoryPredection.text = prediction
                } else {
                    categoryPredection.text = "Unable to convert image(s)"
                }
            } else {
                categoryPredection.text = ""
            }
        }

        // Add subtle elevation
        itemView.elevation = 2f

        return itemView
    }

    private fun captureOutfitPreview(): Bitmap? {
        return try {
            val outfitItemsContainer = view?.findViewById<LinearLayout>(R.id.outfit_items_container)

            if (outfitItemsContainer != null && outfitItemsContainer.isVisible) {
                // Create a bitmap with the same dimensions as the container
                val bitmap = createBitmap(outfitItemsContainer.width, outfitItemsContainer.height)

                // Create a canvas to draw the view onto the bitmap
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE) // Set background color

                // Draw the container view onto the canvas
                outfitItemsContainer.draw(canvas)

                bitmap
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Add this function to save the preview bitmap to internal storage
    private fun savePreviewImage(bitmap: Bitmap, outfitId: String): String? {
        return try {
            val filename = "outfit_preview_$outfitId.png"
            val file = File(requireContext().filesDir, filename)

            FileOutputStream(file).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, outputStream)
            }

            file.absolutePath
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    private fun saveOutfit() {
        if (selectedItems.isEmpty()) {
            Toast.makeText(requireContext(), "Please select items for your outfit", Toast.LENGTH_SHORT).show()
            return
        }

        // Show a dialog to get outfit name
        showOutfitNameDialog { outfitName ->
            val outfitId = System.currentTimeMillis().toString()

            // Capture the outfit preview
            val previewBitmap = captureOutfitPreview()
            val previewImagePath = previewBitmap?.let { bitmap ->
                savePreviewImage(bitmap, outfitId)
            }

            val newOutfit = Outfit(
                id = outfitId,
                title = outfitName,
                category = selectedOutfitCategory,
                gender = selectedGender,
                items = selectedItems.map { it.id }, // Store item IDs
                createdAt = System.currentTimeMillis(),
                previewImagePath = previewImagePath // Now saves the actual preview image path
            )

            saveNewOutfitToJson(requireContext(), newOutfit)

            val message = if (previewImagePath != null) {
                "Outfit '$outfitName' saved with ${selectedItems.size} items and preview image"
            } else {
                "Outfit '$outfitName' saved with ${selectedItems.size} items (preview not captured)"
            }

            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()

            // Navigate back to MyOutfits after saving
            navigateToMyOutfits()
        }
    }

    private fun showOutfitNameDialog(onNameConfirmed: (String) -> Unit) {
        val builder = android.app.AlertDialog.Builder(requireContext())
        val input = android.widget.EditText(requireContext())

        input.hint = "Enter outfit name"
        input.setText("$selectedOutfitCategory Outfit") // Default name

        builder.setTitle("Name Your Outfit")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val outfitName = input.text.toString().trim()
                if (outfitName.isNotEmpty()) {
                    onNameConfirmed(outfitName)
                } else {
                    Toast.makeText(requireContext(), "Please enter a name for your outfit", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
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

    private fun loadModelFile(): MappedByteBuffer {
        val fileDescriptor = requireContext().assets.openFd("outfit_classifier.tflite")
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun imagePathToBitmap(context: Context, path: String): Bitmap? {
        return try {


            val uri = path.toUri()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            } else {
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun predictCategoryFromBitmaps(bitmaps: List<Bitmap>, context: Context): String {
        val inputSize = 64
        val maxImages = 4
        val input = Array(1) { Array(maxImages) { Array(inputSize) { Array(inputSize) { FloatArray(3) } } } }

        for (i in 0 until maxImages) {
            val bmp = if (i < bitmaps.size) {
                val scaled = bitmaps[i].scale(inputSize, inputSize)
                scaled.copy(Bitmap.Config.ARGB_8888, false)
            } else {
                createBitmap(inputSize, inputSize)
            }

            for (y in 0 until inputSize) {
                for (x in 0 until inputSize) {
                    val pixel = bmp[x, y]
                    input[0][i][y][x][0] = (pixel shr 16 and 0xFF) / 255.0f
                    input[0][i][y][x][1] = (pixel shr 8 and 0xFF) / 255.0f
                    input[0][i][y][x][2] = (pixel and 0xFF) / 255.0f
                }
            }
        }

        val output = Array(1) { FloatArray(7) } // Or dynamically detect size from model if needed
        tflite.run(input, output)

        // Load categories from labels.txt
        val categories = loadLabelsFromAssets(context)

        val predictedIndex = output[0].indices.maxByOrNull { output[0][it] } ?: -1

        return if (predictedIndex in categories.indices) {
            val predictedCategory = categories[predictedIndex]

            // Update the spinner selection based on prediction
            updateOutfitCategorySpinner(predictedCategory)

            "Predicted: $predictedCategory"
        } else {
            "Prediction failed"
        }
    }

    private fun updateOutfitCategorySpinner(predictedCategory: String) {
        // Define the available outfit categories (same as in setupOutfitCategorySpinner)
        val outfitCategories = listOf("Casual", "Ethnic", "Formal", "Sports", "Smart Casual", "Travel", "Party", "Home")

        // Map ML prediction to spinner categories if needed
        val mappedCategory = mapPredictionToSpinnerCategory(predictedCategory)

        // Find the index of the predicted category
        val categoryIndex = outfitCategories.indexOf(mappedCategory)

        if (categoryIndex != -1) {
            // Update the spinner selection
            outfitCategorySpinner.setSelection(categoryIndex)
            selectedOutfitCategory = mappedCategory

            Toast.makeText(
                requireContext(),
                "Auto-selected category: $mappedCategory",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun mapPredictionToSpinnerCategory(prediction: String): String {
        // Map your ML model's categories to the spinner categories
        // Adjust these mappings based on what categories your model returns
        return when (prediction.lowercase()) {
            "casual" -> "Casual"
            "formal" -> "Formal"
            "sport", "sports", "athletic" -> "Sports"
            "smart casual", "smart_casual" -> "Smart Casual"
            "party", "evening" -> "Party"
            "ethnic", "traditional" -> "Ethnic"
            "travel", "vacation" -> "Travel"
            "home", "loungewear" -> "Home"
            else -> {
                // If no direct match, try to find the closest match or default to current selection
                val outfitCategories = listOf("Casual", "Ethnic", "Formal", "Sports", "Smart Casual", "Travel", "Party", "Home")
                outfitCategories.find { it.contains(prediction, ignoreCase = true) } ?: selectedOutfitCategory
            }
        }
    }

    private fun loadLabelsFromAssets(context: Context): List<String> {
        val labels = mutableListOf<String>()
        try {
            context.assets.open("labels.txt").bufferedReader().useLines { lines ->
                lines.forEach { labels.add(it.trim()) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return labels
    }
}