package com.example.aprador

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import java.io.File

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

    // Photo display
    private lateinit var blackShirtView: View

    // Current selections
    private var selectedCategory = "Top"
    private var selectedSubcategory = "T-Shirt"

    // Photo data
    private var photoPath: String? = null
    private var photoUri: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Hide bottom navigation when this fragment loads
        view.post {
            (activity as? NavBar)?.hideBottomNavigation()
        }
        // Get photo data from arguments
        arguments?.let { bundle ->
            photoPath = bundle.getString("photo_path")
            photoUri = bundle.getString("photo_uri")
        }

        // Initialize views
        initializeViews(view)

        // Load and display the captured photo
        loadCapturedPhoto()

        // Set up click listeners
        setupClickListeners(view)
    }

    private fun initializeViews(view: View) {
        // Initialize category tabs
        tabBottom = view.findViewById(R.id.tab_bottom)
        tabTop = view.findViewById(R.id.tab_top)
        tabOuterwear = view.findViewById(R.id.tab_outerwear)

        // Initialize subcategory tabs
        tabTShirt = view.findViewById(R.id.tab_tshirt)
        tabSweater = view.findViewById(R.id.tab_sweater)
        tabShirt = view.findViewById(R.id.tab_shirt)
        tabJacket = view.findViewById(R.id.tab_jacket)

        // Initialize photo display view
        blackShirtView = view.findViewById(R.id.BlackShirt)
    }

    private fun setupClickListeners(view: View) {
        // Set up category tab click listeners
        tabBottom.setOnClickListener { selectCategoryTab("Bottom") }
        tabTop.setOnClickListener { selectCategoryTab("Top") }
        tabOuterwear.setOnClickListener { selectCategoryTab("Outerwear") }

        // Set up subcategory tab click listeners
        tabTShirt.setOnClickListener { selectSubcategoryTab("T-Shirt") }
        tabSweater.setOnClickListener { selectSubcategoryTab("Sweater") }
        tabShirt.setOnClickListener { selectSubcategoryTab("Shirt") }
        tabJacket.setOnClickListener { selectSubcategoryTab("Jacket") }

        // Back button
        val myItemsView: View = view.findViewById(R.id.BackAddItem)
        myItemsView.setOnClickListener {
            val mainPageFragment = MainPage()
            parentFragmentManager.beginTransaction()
                .replace(R.id.flFragment, mainPageFragment)
                .addToBackStack(null)
                .commit()
        }

        // Confirm add button
        val confirmAddView: View = view.findViewById(R.id.ConfirmAdd)
        confirmAddView.setOnClickListener {
            addItem()
        }

        // Allow clicking on photo to retake
        blackShirtView.setOnClickListener {
            // Navigate back to MyItems to retake photo
            val myItemsFragment = MyItems()
            parentFragmentManager.beginTransaction()
                .replace(R.id.flFragment, myItemsFragment)
                .addToBackStack(null)
                .commit()
        }
    }

    private fun loadCapturedPhoto() {
        photoPath?.let { path ->
            try {
                val file = File(path)
                if (file.exists()) {
                    // Load the bitmap from file
                    val bitmap = BitmapFactory.decodeFile(path)

                    if (bitmap != null) {
                        // Scale the bitmap to fit the view while maintaining aspect ratio
                        val scaledBitmap = scaleBitmapToFitView(bitmap, 366, 249)

                        // Create a custom drawable that centers the image on white background
                        val drawable = createCenteredImageDrawable(scaledBitmap)

                        // Set the drawable as background
                        blackShirtView.background = drawable

                        Toast.makeText(requireContext(), "Photo loaded successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "Failed to load photo", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Photo file not found", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error loading photo: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun scaleBitmapToFitView(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val originalWidth = bitmap.width
        val originalHeight = bitmap.height

        val scaleX = maxWidth.toFloat() / originalWidth
        val scaleY = maxHeight.toFloat() / originalHeight
        val scale = minOf(scaleX, scaleY)

        val scaledWidth = (originalWidth * scale).toInt()
        val scaledHeight = (originalHeight * scale).toInt()

        return Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
    }

    private fun createCenteredImageDrawable(bitmap: Bitmap): BitmapDrawable {
        val drawable = BitmapDrawable(resources, bitmap)
        drawable.setTileModeXY(android.graphics.Shader.TileMode.CLAMP, android.graphics.Shader.TileMode.CLAMP)
        return drawable
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
        // You can access selectedCategory, selectedSubcategory, and photoPath variables here

        if (photoPath != null) {
            // Example: Log the selections or save to database
            println("Adding item - Category: $selectedCategory, Subcategory: $selectedSubcategory, Photo: $photoPath")

            Toast.makeText(requireContext(), "Item added successfully!", Toast.LENGTH_SHORT).show()

            // Navigate back to MyItems
            val myItemsFragment = MyItems()
            parentFragmentManager.beginTransaction()
                .replace(R.id.flFragment, myItemsFragment)
                .addToBackStack(null)
                .commit()
        } else {
            Toast.makeText(requireContext(), "No photo available", Toast.LENGTH_SHORT).show()
        }
    }
}