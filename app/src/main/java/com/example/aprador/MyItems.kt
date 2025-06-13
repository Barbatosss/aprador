package com.example.aprador

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.TextView
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.net.toUri

class MyItems : Fragment(R.layout.fragment_my_items) {

    private lateinit var tabAllItem: TextView
    private lateinit var mainRecyclerView: RecyclerView
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var categoryAdapter: CategorySectionAdapter

    // Dynamic tabs container
    private lateinit var tabsLayout: LinearLayout
    private val dynamicTabs = mutableListOf<TextView>()
    private var selectedCategory = "All"

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

        // Hide bottom navigation when this fragment loads
        view.post {
            (activity as? NavBar)?.hideBottomNavigation()
        }

        // Initialize views
        initializeViews(view)

        // Setup RecyclerView
        setupRecyclerView()

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

    private fun setupClickListeners(view: View) {
        // Tab click listener
        tabAllItem.setOnClickListener { selectTab("All") }

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
        // TODO: Replace this with actual data loading from your database/storage
        allItems = getSampleData() // Replace with your actual data source
        updateCategorySections()
        updateTabCounts()
        updateEmptyState()
    }

    private fun updateCategorySections() {
        val filteredItems = if (selectedCategory == "All") {
            allItems
        } else {
            allItems.filter { it.subcategory.equals(selectedCategory, ignoreCase = true) }
        }

        categorySections = if (selectedCategory == "All") {
            // Group by subcategory for "All" tab
            filteredItems.groupBy { it.subcategory }
                .map { (subcategory, items) ->
                    CategorySection(subcategory, items)
                }
                .sortedBy { it.subcategory }
        } else {
            // Show single category
            if (filteredItems.isNotEmpty()) {
                listOf(CategorySection(selectedCategory, filteredItems))
            } else {
                emptyList()
            }
        }

        categoryAdapter.updateData(categorySections)
    }

    private fun updateTabCounts() {
        // Update "All" tab count
        tabAllItem.text = "All (${allItems.size})"

        // Update dynamic tabs
        val categories = allItems.groupBy { it.subcategory }

        // Clear existing dynamic tabs
        dynamicTabs.forEach { tabsLayout.removeView(it) }
        dynamicTabs.clear()

        // Create new dynamic tabs
        categories.forEach { (category, items) ->
            val tabView = createDynamicTab(category, items.size)
            tabsLayout.addView(tabView)
            dynamicTabs.add(tabView)
        }
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
        // Handle item click - navigate to item details, etc.
        Toast.makeText(requireContext(), "Clicked: ${item.name}", Toast.LENGTH_SHORT).show()

        // TODO: Navigate to item details fragment
        // val itemDetailsFragment = ItemDetailsFragment()
        // val bundle = Bundle().apply {
        //     putString("item_id", item.id)
        // }
        // itemDetailsFragment.arguments = bundle
        // parentFragmentManager.beginTransaction()
        //     .replace(R.id.flFragment, itemDetailsFragment)
        //     .addToBackStack(null)
        //     .commit()
    }

    // TODO: Replace this with your actual data loading method
    private fun getSampleData(): List<Item> {
        return listOf(
            Item("1", "Blue T-Shirt", "/path/to/tshirt1.jpg", "Tops", "T-Shirts"),
            Item("2", "Red T-Shirt", "/path/to/tshirt2.jpg", "Tops", "T-Shirts"),
            Item("3", "Wool Sweater", "/path/to/sweater1.jpg", "Tops", "Sweaters"),
            Item("4", "Cotton Sweater", "/path/to/sweater2.jpg", "Tops", "Sweaters"),
            Item("5", "Denim Jeans", "/path/to/jeans1.jpg", "Bottoms", "Jeans"),
        )
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
                    "This app needs camera access to take photos of your clothing items. Please grant camera permission to continue.",
                    { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }
                )
            }
            else -> {
                showPermissionExplanationDialog(
                    "Camera Access",
                    "To take photos of your clothing items, we need access to your camera. This will help you catalog your wardrobe.",
                    { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }
                )
            }
        }
    }

    private fun checkStoragePermissionAndOpen() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED -> {
                openGallery()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.READ_MEDIA_IMAGES) -> {
                showPermissionRationaleDialog(
                    "Media Permission Required",
                    "This app needs access to your photos to let you select images from your gallery. Please grant media permission to continue.",
                    { mediaPermissionLauncher.launch(arrayOf(Manifest.permission.READ_MEDIA_IMAGES)) }
                )
            }
            else -> {
                showPermissionExplanationDialog(
                    "Gallery Access",
                    "To select photos from your gallery, we need access to your media files. This will help you add existing photos to your wardrobe.",
                    { mediaPermissionLauncher.launch(arrayOf(Manifest.permission.READ_MEDIA_IMAGES)) }
                )
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