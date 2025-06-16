package com.example.aprador.landing

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
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.widget.AdapterView
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.aprador.R
import com.example.aprador.items.AddItem
import com.example.aprador.items.ItemDetails
import com.example.aprador.items.MyItems
import com.example.aprador.login.UserPreferences
import com.example.aprador.outfits.MyOutfits
import com.example.aprador.outfit_recycler.Outfit
import com.example.aprador.outfit_recycler.OutfitAdapter
import com.example.aprador.outfits.OutfitDetails
import com.example.aprador.item_recycler.Item
import com.example.aprador.item_recycler.ItemAdapter
import com.example.aprador.outfits.CreateOutfit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MainPage : Fragment(R.layout.fragment_main_page) {

    private lateinit var itemsRecyclerView: RecyclerView
    private lateinit var itemAdapter: ItemAdapter
    private lateinit var outfitsRecyclerView: RecyclerView
    private lateinit var outfitAdapter: OutfitAdapter
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var categorySpinner: Spinner
    private lateinit var subcategorySpinner: Spinner
    private lateinit var userPreferences: UserPreferences

    private var allItems = listOf<Item>()
    private var filteredItems = listOf<Item>()
    private var allOutfits = listOf<Outfit>()
    private var selectedCategory = "All Categories"
    private var selectedSubcategory = "All"

    // Camera related properties
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var galleryLauncher: ActivityResultLauncher<Intent>
    private lateinit var cameraPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var storagePermissionLauncher: ActivityResultLauncher<String>
    private lateinit var mediaPermissionLauncher: ActivityResultLauncher<Array<String>>
    private var currentPhotoPath: String = ""
    private var capturedImageUri: Uri? = null

    // Subcategory mappings based on AddItem.kt structure
    private val subcategoryMap = mapOf(
        "Top" to listOf("All", "T-Shirt", "Polo", "Dress Shirt", "Tank Top", "Blouse", "Camisole", "Crop Top"),
        "Bottom" to listOf("All", "Jeans", "Chinos", "Shorts", "Joggers", "Leggings", "Skirt", "Dress"),
        "Outerwear" to listOf("All", "Jacket", "Hoodie", "Blazer", "Coat", "Cardigan", "Kimono"),
        "Footwear" to listOf("All", "Sneakers", "Dress Shoes", "Boots", "Sandals", "Heels", "Flats"),
        "All Categories" to listOf("All")
    )

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        initializeViews(view)

        // Setup dropdowns
        setupCategorySpinner()
        setupSubcategorySpinner()

        // Setup RecyclerViews
        setupRecyclerView()
        setupOutfitsRecyclerView()

        // Load and display data
        loadItemsData()
        loadOutfitsData()

        // Setup existing click listeners
        setupClickListeners(view)
    }

    private fun initializeViews(view: View) {
        itemsRecyclerView = view.findViewById(R.id.items_recycler_view)
        outfitsRecyclerView = view.findViewById(R.id.outfits_recycler_view)
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

    private fun setupOutfitsRecyclerView() {
        outfitAdapter = OutfitAdapter(allOutfits, requireContext()) { outfit ->
            onOutfitClicked(outfit)
        }

        outfitsRecyclerView.apply {
            adapter = outfitAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            // Prevent nested scrolling conflicts
            isNestedScrollingEnabled = false
            // Always keep the RecyclerView visible
            visibility = View.VISIBLE
        }
    }

    private fun setupClickListeners(view: View) {
        val itemView: View = view.findViewById(R.id.ItemView)
        val outfitView: View = view.findViewById(R.id.OutfitView)
        val itemIconsView: View = view.findViewById(R.id.ItemIcons)

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

        // Add item with camera/gallery functionality
        itemIconsView.setOnClickListener {
            showPhotoSelectionDialog()
        }

        // Navigate to CreateOutfit fragment
        val createOutfitView: View? = view.findViewById(R.id.OutfitIcons)
        createOutfitView?.setOnClickListener {
            val createOutfitFragment = CreateOutfit()
            parentFragmentManager.beginTransaction()
                .replace(R.id.flFragment, createOutfitFragment)
                .addToBackStack(null)
                .commit()
        }
    }

    private fun loadItemsData() {
        allItems = loadItems(requireContext())
        updateFilteredItems()
    }

    private fun loadOutfitsData() {
        // Load outfits using the exact same method as MyOutfits.kt
        allOutfits = loadOutfitsFromFile(requireContext())

        // Update adapter with the loaded outfits (no validation or filtering)
        if (::outfitAdapter.isInitialized) {
            outfitAdapter = OutfitAdapter(allOutfits, requireContext()) { outfit ->
                onOutfitClicked(outfit)
            }
            outfitsRecyclerView.adapter = outfitAdapter
        }
    }

    private fun loadOutfitsFromFile(context: Context): List<Outfit> {
        return try {
            val file = File(context.filesDir, "outfits.json")
            if (file.exists() && file.readText().isNotBlank()) {
                val json = file.readText()
                val type = object : TypeToken<List<Outfit>>() {}.type
                val outfits: List<Outfit>? = Gson().fromJson(json, type)
                outfits ?: emptyList()
            } else {
                // No outfits file exists yet
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Return empty list if there's any error reading the file
            emptyList()
        }
    }

    // Enhanced gender preference mapping with complete subcategory lists (copied from MyItems.kt)
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

    // Enhanced sorting function that PRIORITIZES user's gender items completely (copied from MyItems.kt)
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

    private fun updateFilteredItems() {
        // First filter by category
        val categoryFilteredItems = if (selectedCategory == "All Categories") {
            allItems
        } else {
            allItems.filter { it.category.equals(selectedCategory, ignoreCase = true) }
        }

        // Then filter by subcategory within the category-filtered items
        val subcategoryFilteredItems = if (selectedSubcategory == "All") {
            categoryFilteredItems
        } else {
            categoryFilteredItems.filter { it.subcategory.equals(selectedSubcategory, ignoreCase = true) }
        }

        // Apply gender-based sorting to the filtered items
        filteredItems = sortItemsByGenderPreference(subcategoryFilteredItems)

        // Update the adapter with new filtered and sorted items
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
            putString("source_fragment", "MainPage")
        }
        itemDetailsFragment.arguments = bundle

        parentFragmentManager.beginTransaction()
            .replace(R.id.flFragment, itemDetailsFragment)
            .addToBackStack(null)
            .commit()
    }

    private fun onOutfitClicked(outfit: Outfit) {
        // Navigate to OutfitDetails fragment with the outfit ID
        val outfitDetailsFragment = OutfitDetails.newInstance(outfit.id)
        parentFragmentManager.beginTransaction()
            .replace(R.id.flFragment, outfitDetailsFragment)
            .addToBackStack(null)
            .commit()
    }


    // Camera and Gallery functionality methods
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

    private fun checkStoragePermissionAndOpen() {
        when {
            // For Android 13+ (API 33+)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
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
            // For Android 10-12 (API 29-32)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                // No permission needed for gallery access on Android 10+
                openGallery()
            }
            // For older Android versions (API < 29)
            else -> {
                when {
                    ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED -> {
                        openGallery()
                    }
                    shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE) -> {
                        showPermissionRationaleDialog(
                            "Storage Permission Required",
                            "This app needs storage access to let you select images from your gallery. Please grant storage permission to continue."
                        ) { storagePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE) }
                    }
                    else -> {
                        showPermissionExplanationDialog(
                            "Gallery Access",
                            "To select photos from your gallery, we need access to your storage. This will help you add existing photos to your wardrobe."
                        ) { storagePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE) }
                    }
                }
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

    override fun onResume() {
        super.onResume()
        // Refresh data when returning to this fragment
        if (::itemAdapter.isInitialized) {
            loadItemsData()
        }
        if (::outfitAdapter.isInitialized) {
            loadOutfitsData()
        }
    }
}