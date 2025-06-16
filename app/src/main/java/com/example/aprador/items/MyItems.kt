package com.example.aprador.items

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.TextView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.widget.AdapterView
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.aprador.landing.MainPage
import com.example.aprador.login.UserPreferences
import com.example.aprador.R
import com.example.aprador.navigation.NavBar
import com.example.aprador.item_recycler.CategorySection
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import com.example.aprador.item_recycler.CategorySectionAdapter
import com.example.aprador.item_recycler.Item
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken


class MyItems : Fragment(R.layout.fragment_my_items) {

    private lateinit var tabAllItem: TextView
    private lateinit var mainRecyclerView: RecyclerView
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var categoryAdapter: CategorySectionAdapter
    private lateinit var categoryFilterSpinner: Spinner
    private lateinit var userPreferences: UserPreferences

    // Dynamic tabs container
    private lateinit var tabsLayout: LinearLayout
    private val dynamicTabs = mutableListOf<TextView>()
    private var selectedSubcategory = "All"
    private var selectedCategoryFilter = "All Categories" // New category filter

    // Sample data - replace with your actual data source
    private var allItems = listOf<Item>() // This should come from your database/storage
    private var categorySections = listOf<CategorySection>()

    // Camera related properties
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var galleryLauncher: ActivityResultLauncher<Intent>
    private lateinit var cameraPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var storagePermissionLauncher: ActivityResultLauncher<String>
    private lateinit var mediaPermissionLauncher: ActivityResultLauncher<Array<String>>
    private var currentPhotoPath: String = ""
    private var capturedImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize UserPreferences
        userPreferences = UserPreferences(requireContext())

        // Initialize camera launcher
        cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                navigateToAddItemWithPhoto()
            } else {
                Toast.makeText(requireContext(), "Photo capture cancelled", Toast.LENGTH_SHORT).show()
            }
        }

        // Initialize gallery launcher
        galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val selectedImageUri = result.data?.data
                if (selectedImageUri != null) {
                    capturedImageUri = selectedImageUri
                    currentPhotoPath = selectedImageUri.toString()
                    navigateToAddItemWithPhoto()
                }
            } else {
                Toast.makeText(requireContext(), "Image selection cancelled", Toast.LENGTH_SHORT).show()
            }
        }

        // Initialize camera permission launcher
        cameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                openCamera()
            } else {
                Toast.makeText(requireContext(), "Camera permission is required to take photos", Toast.LENGTH_SHORT).show()
            }
        }

        // Initialize storage permission launcher (for older Android versions)
        storagePermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                openGallery()
            } else {
                Toast.makeText(requireContext(), "Storage permission is required to access photos", Toast.LENGTH_SHORT).show()
            }
        }

        // Initialize media permission launcher (for Android 13+)
        mediaPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val readImagesGranted = permissions[Manifest.permission.READ_MEDIA_IMAGES] ?: false

            if (readImagesGranted) {
                openGallery()
            } else {
                Toast.makeText(requireContext(), "Media permission is required to access photos", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Hide bottom navigation when this fragment loads
        view.post {
            (activity as? NavBar)?.hideBottomNavigation()
        }

        // Initialize views
        initializeViews(view)

        // Setup RecyclerView
        setupRecyclerView()

        // Setup category filter spinner
        setupCategoryFilterSpinner()

        // Load data and update UI
        loadItemsData()

        // Setup click listeners
        setupClickListeners(view)
    }

    private fun initializeViews(view: View) {
        tabAllItem = view.findViewById(R.id.tab_all_item)
        tabsLayout = view.findViewById(R.id.tabs_layout_item)
        mainRecyclerView = view.findViewById(R.id.main_recycler_view)
        emptyStateLayout = view.findViewById(R.id.empty_state_layout)
        categoryFilterSpinner = view.findViewById(R.id.category_filter_spinner)
    }

    private fun setupRecyclerView() {
        categoryAdapter = CategorySectionAdapter(categorySections) { item ->
            onItemClicked(item)
        }

        mainRecyclerView.apply {
            adapter = categoryAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun setupCategoryFilterSpinner() {
        // Create category filter options
        val categoryOptions = listOf("All Categories", "Top", "Bottom", "Outerwear", "Footwear")

        // Create adapter for spinner
        val spinnerAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            categoryOptions
        )
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        // Set adapter to spinner
        categoryFilterSpinner.adapter = spinnerAdapter

        // Set selection listener
        categoryFilterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedCategory = categoryOptions[position]
                if (selectedCategoryFilter != selectedCategory) {
                    selectedCategoryFilter = selectedCategory
                    updateCategorySections()
                    updateSubcategoryTabs()
                    updateEmptyState()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun setupClickListeners(view: View) {
        // Tab click listener
        tabAllItem.setOnClickListener { selectSubcategoryTab("All") }

        // Back button
        val backButton: View = view.findViewById(R.id.BackItem)
        backButton.setOnClickListener {
            val mainPageFragment = MainPage()
            parentFragmentManager.beginTransaction()
                .replace(R.id.flFragment, mainPageFragment)
                .addToBackStack(null)
                .commit()
        }

        // Add item button
        val addItemButton: View = view.findViewById(R.id.CreateItem)
        addItemButton.setOnClickListener {
            showPhotoSelectionDialog()
        }
    }

    private fun loadItemsData() {
        allItems = loadItems(requireContext())

        Log.d("LOAD_ITEMS", "Loaded ${allItems.size} items")
        allItems.forEach {
            Log.d("LOAD_ITEMS", it.toString())
        }

        updateCategorySections()
        updateSubcategoryTabs()
        updateEmptyState()
    }

    private fun getFilteredItemsByCategory(): List<Item> {
        return if (selectedCategoryFilter == "All Categories") {
            allItems
        } else {
            allItems.filter { it.category.equals(selectedCategoryFilter, ignoreCase = true) }
        }
    }

    // Enhanced gender preference mapping with complete subcategory lists
    private fun getGenderPreferredSubcategories(userGender: String): Map<String, List<String>> {
        return when (userGender.lowercase()) {
            "male", "men" -> mapOf(
                "top" to listOf("T-Shirt", "Polo", "Dress Shirt", "Tank Top"),
                "bottom" to listOf("Jeans", "Chinos", "Shorts", "Joggers"),
                "outerwear" to listOf("Jacket", "Hoodie", "Blazer", "Coat"),
                "footwear" to listOf("Sneakers", "Dress Shoes", "Boots", "Sandals")
            )
            "female", "women" -> mapOf(
                "top" to listOf("T-Shirt", "Blouse", "Camisole", "Crop Top"),
                "bottom" to listOf("Jeans", "Leggings", "Skirt", "Dress"),
                "outerwear" to listOf("Cardigan", "Blazer", "Coat", "Kimono"),
                "footwear" to listOf("Sneakers", "Heels", "Flats", "Boots")
            )
            else -> emptyMap()
        }
    }

    // Enhanced sorting function that PRIORITIZES user's gender items completely
    private fun sortItemsByGenderPreference(items: List<Item>): List<Item> {
        val userGender = userPreferences.getUserGender()
        val genderPreferences = getGenderPreferredSubcategories(userGender)

        // Separate items into user's gender and other gender
        val (userGenderItems, otherGenderItems) = items.partition { item ->
            val category = item.category.lowercase()
            val subcategory = item.subcategory
            val preferredSubcategories = genderPreferences[category] ?: emptyList()
            preferredSubcategories.contains(subcategory)
        }

        // Sort user's gender items alphabetically by subcategory
        val sortedUserGenderItems = userGenderItems.sortedBy { it.subcategory }

        // Sort other gender items alphabetically by subcategory
        val sortedOtherGenderItems = otherGenderItems.sortedBy { it.subcategory }

        // Return user's gender items first, then other gender items
        return sortedUserGenderItems + sortedOtherGenderItems
    }

    // Updated updateCategorySections method with enhanced gender prioritization
    private fun updateCategorySections() {
        // First filter by category
        val categoryFilteredItems = getFilteredItemsByCategory()

        // Then filter by subcategory
        val subcategoryFilteredItems = if (selectedSubcategory == "All") {
            categoryFilteredItems
        } else {
            categoryFilteredItems.filter { it.subcategory.equals(selectedSubcategory, ignoreCase = true) }
        }

        // Sort items with STRICT gender preference (user's gender first)
        val sortedItems = sortItemsByGenderPreference(subcategoryFilteredItems)

        categorySections = if (selectedSubcategory == "All") {
            // Group by subcategory, maintaining gender-priority order
            val groupedItems = sortedItems.groupBy { it.subcategory }

            // Create sections maintaining the order from sorted items
            val orderedSubcategories = sortedItems.map { it.subcategory }.distinct()

            orderedSubcategories.map { subcategory ->
                val subcategoryItems = groupedItems[subcategory] ?: emptyList()
                CategorySection(subcategory, subcategoryItems)
            }
        } else {
            // Show single subcategory with gender-based sorting
            if (sortedItems.isNotEmpty()) {
                listOf(CategorySection(selectedSubcategory, sortedItems))
            } else {
                emptyList()
            }
        }

        categoryAdapter.updateData(categorySections)
    }

    // Enhanced updateSubcategoryTabs with gender prioritization
    private fun updateSubcategoryTabs() {
        // Get items filtered by category first
        val categoryFilteredItems = getFilteredItemsByCategory()

        // Update "All" tab count
        tabAllItem.text = "All (${categoryFilteredItems.size})"

        // Group items by subcategory
        val subcategories = categoryFilteredItems.groupBy { it.subcategory }

        // Clear existing dynamic tabs
        dynamicTabs.forEach { tabsLayout.removeView(it) }
        dynamicTabs.clear()

        // Sort subcategories by gender preference - user's gender first
        val userGender = userPreferences.getUserGender()
        val genderPreferences = getGenderPreferredSubcategories(userGender)

        // Separate subcategories into user's gender and other gender
        val (userGenderSubcategories, otherGenderSubcategories) = subcategories.toList().partition { (subcategory, items) ->
            val category = items.firstOrNull()?.category?.lowercase() ?: ""
            val preferredSubcategories = genderPreferences[category] ?: emptyList()
            preferredSubcategories.contains(subcategory)
        }

        // Sort each group alphabetically
        val sortedUserGenderSubcategories = userGenderSubcategories.sortedBy { it.first }
        val sortedOtherGenderSubcategories = otherGenderSubcategories.sortedBy { it.first }

        // Combine: user's gender first, then other gender
        val finalSortedSubcategories = sortedUserGenderSubcategories + sortedOtherGenderSubcategories

        // Create tabs in the gender-prioritized order
        finalSortedSubcategories.forEach { (subcategory, items) ->
            val tabView = createDynamicTab(subcategory, items.size)
            tabsLayout.addView(tabView)
            dynamicTabs.add(tabView)
        }

        // Update tab appearances
        updateTabAppearances()
    }

    // Helper function to check if an item belongs to user's gender preferences
    private fun isUserGenderPreferred(item: Item): Boolean {
        val userGender = userPreferences.getUserGender()
        val genderPreferences = getGenderPreferredSubcategories(userGender)
        val category = item.category.lowercase()
        val preferredSubcategories = genderPreferences[category] ?: emptyList()
        return preferredSubcategories.contains(item.subcategory)
    }

    private fun createDynamicTab(subcategory: String, count: Int): TextView {
        val tabView = layoutInflater.inflate(R.layout.dynamic_tab, tabsLayout, false) as TextView
        tabView.text = "$subcategory ($count)"
        tabView.setOnClickListener { selectSubcategoryTab(subcategory) }
        return tabView
    }

    private fun selectSubcategoryTab(subcategory: String) {
        selectedSubcategory = subcategory

        // Update tab appearances
        updateTabAppearances()

        // Update displayed items
        updateCategorySections()

        // Update empty state
        updateEmptyState()
    }

    private fun updateTabAppearances() {
        // Reset all tabs
        tabAllItem.setBackgroundResource(R.drawable.tab_unselected_background)
        tabAllItem.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))

        dynamicTabs.forEach { tab ->
            tab.setBackgroundResource(R.drawable.tab_unselected_background)
            tab.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
        }

        // Set selected tab appearance
        if (selectedSubcategory == "All") {
            tabAllItem.setBackgroundResource(R.drawable.tab_selected_background)
            tabAllItem.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
        } else {
            dynamicTabs.find {
                it.text.toString().startsWith(selectedSubcategory)
            }?.apply {
                setBackgroundResource(R.drawable.tab_selected_background)
                setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
            }
        }
    }

    private fun updateEmptyState() {
        val hasItems = categorySections.isNotEmpty()

        if (hasItems) {
            emptyStateLayout.visibility = View.GONE
            mainRecyclerView.visibility = View.VISIBLE
        } else {
            emptyStateLayout.visibility = View.VISIBLE
            mainRecyclerView.visibility = View.GONE
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

    // Method to refresh data (call this when returning from AddItem)
    fun refreshItems() {
        loadItemsData()
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when returning to this fragment
        if (::categoryAdapter.isInitialized) {
            loadItemsData()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (activity as? NavBar)?.showBottomNavigation()
    }

    // All your existing camera/gallery methods remain the same...
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun showPhotoSelectionDialog() {
        val options = arrayOf("Take Photo", "Upload from Gallery")

        AlertDialog.Builder(requireContext())
            .setTitle("Add Photo")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> checkCameraPermissionAndOpen()
                    1 -> checkStoragePermissionAndOpen()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun checkCameraPermissionAndOpen() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                openCamera()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                showPermissionRationaleDialog(
                    "Camera Permission Required",
                    "This app needs camera access to take photos of your clothing items. Please grant camera permission to continue."
                ) { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }
            }
            else -> {
                showPermissionExplanationDialog(
                    "Camera Access",
                    "To take photos of your clothing items, we need access to your camera. This will help you catalog your wardrobe."
                ) { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun checkStoragePermissionAndOpen() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED -> {
                openGallery()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.READ_MEDIA_IMAGES) -> {
                showPermissionRationaleDialog(
                    "Media Permission Required",
                    "This app needs access to your photos to let you select images from your gallery. Please grant media permission to continue."
                ) { mediaPermissionLauncher.launch(arrayOf(Manifest.permission.READ_MEDIA_IMAGES)) }
            }
            else -> {
                showPermissionExplanationDialog(
                    "Gallery Access",
                    "To select photos from your gallery, we need access to your media files. This will help you add existing photos to your wardrobe."
                ) { mediaPermissionLauncher.launch(arrayOf(Manifest.permission.READ_MEDIA_IMAGES)) }
            }
        }
    }

    private fun showPermissionExplanationDialog(title: String, message: String, onPositive: () -> Unit) {
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Allow") { dialog, _ ->
                dialog.dismiss()
                onPositive()
            }
            .setNegativeButton("Not Now") { dialog, _ ->
                dialog.dismiss()
                Toast.makeText(requireContext(), "Permission needed to use this feature", Toast.LENGTH_SHORT).show()
            }
            .setCancelable(false)
            .show()
    }

    private fun showPermissionRationaleDialog(title: String, message: String, onPositive: () -> Unit) {
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Grant Permission") { dialog, _ ->
                dialog.dismiss()
                onPositive()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                Toast.makeText(requireContext(), "Permission is required for this feature", Toast.LENGTH_SHORT).show()
            }
            .setCancelable(false)
            .show()
    }

    private fun openCamera() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        if (takePictureIntent.resolveActivity(requireActivity().packageManager) != null) {
            val photoFile: File? = try {
                createImageFile()
            } catch (ex: IOException) {
                Toast.makeText(requireContext(), "Error creating image file", Toast.LENGTH_SHORT).show()
                null
            }

            photoFile?.also {
                val photoURI: Uri = FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.fileprovider",
                    it
                )
                capturedImageUri = photoURI
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                cameraLauncher.launch(takePictureIntent)
            }
        } else {
            Toast.makeText(requireContext(), "No camera app available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        galleryLauncher.launch(intent)
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "ITEM_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            currentPhotoPath = absolutePath
        }
    }

    private fun navigateToAddItemWithPhoto() {
        val addItemFragment = AddItem()

        val bundle = Bundle()

        if (capturedImageUri != null) {
            bundle.putString("photo_uri", capturedImageUri.toString())
            if (currentPhotoPath.isNotEmpty() && !currentPhotoPath.startsWith("content://")) {
                bundle.putString("photo_path", currentPhotoPath)
            }
        } else if (currentPhotoPath.isNotEmpty()) {
            bundle.putString("photo_path", currentPhotoPath)
        }

        addItemFragment.arguments = bundle

        parentFragmentManager.beginTransaction()
            .replace(R.id.flFragment, addItemFragment)
            .addToBackStack(null)
            .commit()
    }
}