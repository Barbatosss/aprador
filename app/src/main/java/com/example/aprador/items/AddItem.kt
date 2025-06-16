package com.example.aprador.items

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.example.aprador.landing.MainPage
import com.example.aprador.R
import com.example.aprador.navigation.NavBar
import com.example.aprador.item_recycler.Item
import com.example.aprador.item_recycler.ImageUtil
import com.example.aprador.login.UserPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

class AddItem : Fragment(R.layout.fragment_add_item) {
    private lateinit var nameView: TextView

    // Category tabs
    private lateinit var tabBottom: TextView
    private lateinit var tabTop: TextView
    private lateinit var tabOuterwear: TextView
    private lateinit var tabFootwear: TextView

    // Gender toggle tabs
    private lateinit var tabMen: TextView
    private lateinit var tabWomen: TextView

    // Subcategory tabs
    private lateinit var subTab1: TextView
    private lateinit var subTab2: TextView
    private lateinit var subTab3: TextView
    private lateinit var subTab4: TextView

    // Photo display
    private lateinit var photoImageView: ImageView

    // User preferences
    private lateinit var userPreferences: UserPreferences

    // Current selections
    private var selectedCategory = "Top"
    private var selectedSubcategory = "T-Shirt"
    private var selectedGender = "Men" // Will be updated from user preferences

    // Photo data
    private var photoPath: String? = null
    private var photoUri: String? = null

    // Loaded bitmap reference for cleanup
    private var loadedBitmap: Bitmap? = null

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize user preferences
        userPreferences = UserPreferences(requireContext())

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

        // Load user's default gender and initialize with it
        loadUserDefaultGender()

        // Initialize with default selections (now using user's gender)
        selectGenderTab(selectedGender)
        selectCategoryTab("Top")
    }

    private fun loadUserDefaultGender() {
        val userGender = userPreferences.getUserGender()

        // Map the gender from Profile format to AddItem format
        selectedGender = when (userGender) {
            "Male" -> "Men"
            "Female" -> "Women"
            else -> "Men" // Default fallback
        }
    }

    private fun initializeViews(view: View) {
        // Initialize gender tabs
        tabMen = view.findViewById(R.id.toggle_men)
        tabWomen = view.findViewById(R.id.toggle_women)

        nameView = view.findViewById(R.id.item_name_edittext)

        // Initialize category tabs
        tabBottom = view.findViewById(R.id.tab_bottom)
        tabTop = view.findViewById(R.id.tab_top)
        tabOuterwear = view.findViewById(R.id.tab_outerwear)
        tabFootwear = view.findViewById(R.id.tab_footwear)

        // Initialize subcategory tabs
        subTab1 = view.findViewById(R.id.subcategory_tab_1)
        subTab2 = view.findViewById(R.id.subcategory_tab_2)
        subTab3 = view.findViewById(R.id.subcategory_tab_3)
        subTab4 = view.findViewById(R.id.subcategory_tab_4)

        // Initialize photo display ImageView
        photoImageView = view.findViewById(R.id.BlackShirt)
    }

    private fun setupClickListeners(view: View) {
        // Set up gender tab click listeners
        tabMen.setOnClickListener { selectGenderTab("Men") }
        tabWomen.setOnClickListener { selectGenderTab("Women") }

        // Set up category tab click listeners
        tabBottom.setOnClickListener { selectCategoryTab("Bottom") }
        tabTop.setOnClickListener { selectCategoryTab("Top") }
        tabOuterwear.setOnClickListener { selectCategoryTab("Outerwear") }
        tabFootwear.setOnClickListener { selectCategoryTab("Footwear") }

        // Set up subcategory tab click listeners
        subTab1.setOnClickListener {
            val subcategory = subTab1.text.toString()
            selectSubcategoryTab(subcategory)
        }
        subTab2.setOnClickListener {
            val subcategory = subTab2.text.toString()
            selectSubcategoryTab(subcategory)
        }
        subTab3.setOnClickListener {
            val subcategory = subTab3.text.toString()
            selectSubcategoryTab(subcategory)
        }
        subTab4.setOnClickListener {
            val subcategory = subTab4.text.toString()
            selectSubcategoryTab(subcategory)
        }

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
        photoImageView.setOnClickListener {
            // Navigate back to MyItems with retake flag
            val myItemsFragment = MyItems()
            val bundle = Bundle()
            bundle.putBoolean("retake_photo", true)
            myItemsFragment.arguments = bundle

            parentFragmentManager.beginTransaction()
                .replace(R.id.flFragment, myItemsFragment)
                .addToBackStack(null)
                .commit()
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun loadCapturedPhoto() {
        try {
            // Clean up previous bitmap
            ImageUtil.recycleBitmap(loadedBitmap)
            loadedBitmap = null

            val bitmap = when {
                !photoUri.isNullOrEmpty() && photoUri != "null" -> {
                    val uri = photoUri!!.toUri()
                    if (uri.scheme == "content") {
                        // Use the aspect-preserving method for URI loading
                        ImageUtil.loadImageFromUriPreserveAspect(
                            requireContext(),
                            uri,
                            getImageViewWidth(),
                            getImageViewHeight()
                        )
                    } else {
                        // For non-content URIs, convert to path and use path loading
                        ImageUtil.loadImageFromPath(
                            photoPath ?: "",
                            getImageViewWidth(),
                            getImageViewHeight()
                        )
                    }
                }
                !photoPath.isNullOrEmpty() -> {
                    // For file paths, continue using the existing path loading method
                    ImageUtil.loadImageFromPath(
                        photoPath!!,
                        getImageViewWidth(),
                        getImageViewHeight()
                    )
                }
                else -> null
            }

            if (bitmap != null) {
                loadedBitmap = bitmap
                // Change ScaleType to FIT_CENTER to preserve aspect ratio
                ImageUtil.setImageToView(photoImageView, bitmap, ImageView.ScaleType.FIT_CENTER)
            } else {
                setPlaceholderImage()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            setPlaceholderImage()
        }
    }

    private fun getImageViewWidth(): Int {
        return if (photoImageView.width > 0) {
            photoImageView.width
        } else {
            (300 * resources.displayMetrics.density).toInt()
        }
    }

    private fun getImageViewHeight(): Int {
        return if (photoImageView.height > 0) {
            photoImageView.height
        } else {
            (200 * resources.displayMetrics.density).toInt()
        }
    }

    private fun setPlaceholderImage() {
        photoImageView.setImageResource(R.drawable.shirt)
        photoImageView.scaleType = ImageView.ScaleType.CENTER_INSIDE
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Clean up bitmap to prevent memory leaks
        ImageUtil.recycleBitmap(loadedBitmap)
        loadedBitmap = null

        // Show bottom navigation when leaving this fragment
        (activity as? NavBar)?.showBottomNavigation()
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

        // Update subcategory options based on selected gender and category
        updateSubcategoryOptions()
    }

    private fun resetGenderTabs() {
        val genderTabs = arrayOf(tabMen, tabWomen)

        genderTabs.forEach { tab ->
            tab.setBackgroundResource(R.drawable.tab_unselected_background)
            tab.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
        }
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
            "Footwear" -> tabFootwear
            else -> tabTop
        }

        selectedTab.setBackgroundResource(R.drawable.tab_selected_background)
        selectedTab.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))

        // Update subcategory options based on selected category and gender
        updateSubcategoryOptions()
    }

    private fun selectSubcategoryTab(subcategory: String) {
        selectedSubcategory = subcategory

        // Reset all subcategory tabs
        resetSubcategoryTabs()

        // Find which tab contains this subcategory text and select it
        val selectedTab = when (subcategory) {
            subTab1.text.toString() -> subTab1
            subTab2.text.toString() -> subTab2
            subTab3.text.toString() -> subTab3
            subTab4.text.toString() -> subTab4
            else -> subTab1
        }

        selectedTab.setBackgroundResource(R.drawable.tab_selected_background)
        selectedTab.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
    }

    private fun resetCategoryTabs() {
        val categoryTabs = arrayOf(tabBottom, tabTop, tabOuterwear, tabFootwear)

        categoryTabs.forEach { tab ->
            tab.setBackgroundResource(R.drawable.tab_unselected_background)
            tab.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
        }
    }

    private fun resetSubcategoryTabs() {
        val subcategoryTabs = arrayOf(subTab1, subTab2, subTab3, subTab4)

        subcategoryTabs.forEach { tab ->
            tab.setBackgroundResource(R.drawable.tab_unselected_background)
            tab.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
        }
    }

    private fun updateSubcategoryOptions() {
        when (selectedCategory) {
            "Top" -> {
                // Show all subcategory tabs for tops
                subTab1.visibility = View.VISIBLE
                subTab2.visibility = View.VISIBLE
                subTab3.visibility = View.VISIBLE
                subTab4.visibility = View.VISIBLE

                // Update subcategory texts based on gender
                if (selectedGender == "Men") {
                    subTab1.text = "T-Shirts"
                    subTab2.text = "Polo"
                    subTab3.text = "Dress Shirt"
                    subTab4.text = "Tank Top"

                    // Auto-select appropriate subcategory for men
                    if (!listOf("T-Shirt", "Polo", "Dress Shirt", "Tank Top").contains(selectedSubcategory)) {
                        selectedSubcategory = "T-Shirt"
                        selectSubcategoryTab("T-Shirt")
                    }
                } else { // Women
                    subTab1.text = "T-Shirts"
                    subTab2.text = "Blouse"
                    subTab3.text = "Camisole"
                    subTab4.text = "Crop Top"

                    // Auto-select appropriate subcategory for women
                    if (!listOf("T-Shirt", "Blouse", "Camisole", "Crop Top").contains(selectedSubcategory)) {
                        selectedSubcategory = "T-Shirt"
                        selectSubcategoryTab("T-Shirt")
                    }
                }
            }
            "Bottom" -> {
                // Show all subcategory tabs for bottoms
                subTab1.visibility = View.VISIBLE
                subTab2.visibility = View.VISIBLE
                subTab3.visibility = View.VISIBLE
                subTab4.visibility = View.VISIBLE

                // Update subcategory texts based on gender
                if (selectedGender == "Men") {
                    subTab1.text = "Jeans"
                    subTab2.text = "Chinos"
                    subTab3.text = "Shorts"
                    subTab4.text = "Joggers"

                    // Auto-select appropriate subcategory for men
                    if (!listOf("Jeans", "Chinos", "Shorts", "Joggers").contains(selectedSubcategory)) {
                        selectedSubcategory = "Jeans"
                        selectSubcategoryTab("Jeans")
                    }
                } else { // Women
                    subTab1.text = "Jeans"
                    subTab2.text = "Leggings"
                    subTab3.text = "Skirt"
                    subTab4.text = "Dress"

                    // Auto-select appropriate subcategory for women
                    if (!listOf("Jeans", "Leggings", "Skirt", "Dress").contains(selectedSubcategory)) {
                        selectedSubcategory = "Jeans"
                        selectSubcategoryTab("Jeans")
                    }
                }
            }
            "Outerwear" -> {
                // Show all subcategory tabs for outerwear
                subTab1.visibility = View.VISIBLE
                subTab2.visibility = View.VISIBLE
                subTab3.visibility = View.VISIBLE
                subTab4.visibility = View.VISIBLE

                // Update subcategory texts based on gender
                if (selectedGender == "Men") {
                    subTab1.text = "Jacket"
                    subTab2.text = "Hoodie"
                    subTab3.text = "Blazer"
                    subTab4.text = "Coat"

                    // Auto-select appropriate subcategory for men
                    if (!listOf("Jacket", "Hoodie", "Blazer", "Coat").contains(selectedSubcategory)) {
                        selectedSubcategory = "Jacket"
                        selectSubcategoryTab("Jacket")
                    }
                } else { // Women
                    subTab1.text = "Cardigan"
                    subTab2.text = "Blazer"
                    subTab3.text = "Coat"
                    subTab4.text = "Kimono"

                    // Auto-select appropriate subcategory for women
                    if (!listOf("Cardigan", "Blazer", "Coat", "Kimono").contains(selectedSubcategory)) {
                        selectedSubcategory = "Cardigan"
                        selectSubcategoryTab("Cardigan")
                    }
                }
            }
            "Footwear" -> {
                // Show all subcategory tabs for footwear
                subTab1.visibility = View.VISIBLE
                subTab2.visibility = View.VISIBLE
                subTab3.visibility = View.VISIBLE
                subTab4.visibility = View.VISIBLE

                // Update subcategory texts based on gender
                if (selectedGender == "Men") {
                    subTab1.text = "Sneakers"
                    subTab2.text = "Dress Shoes"
                    subTab3.text = "Boots"
                    subTab4.text = "Sandals"

                    // Auto-select appropriate subcategory for men
                    if (!listOf("Sneakers", "Dress Shoes", "Boots", "Sandals").contains(selectedSubcategory)) {
                        selectedSubcategory = "Sneakers"
                        selectSubcategoryTab("Sneakers")
                    }
                } else { // Women
                    subTab1.text = "Sneakers"
                    subTab2.text = "Heels"
                    subTab3.text = "Flats"
                    subTab4.text = "Boots"

                    // Auto-select appropriate subcategory for women
                    if (!listOf("Sneakers", "Heels", "Flats", "Boots").contains(selectedSubcategory)) {
                        selectedSubcategory = "Sneakers"
                        selectSubcategoryTab("Sneakers")
                    }
                }
            }
        }
    }

    private fun saveNewItemToJson(context: Context, newItem: Item) {
        val file = File(context.filesDir, "db.json")

        // Read existing items
        val items: MutableList<Item> = if (file.exists() && file.readText().isNotBlank()) {
            val json = file.readText()
            val type = object : TypeToken<List<Item>>() {}.type
            Gson().fromJson(json, type)
        } else {
            mutableListOf()
        }

        // Add new item
        items.add(newItem)

        // Write updated list back to file
        val updatedJson = Gson().toJson(items)
        file.writeText(updatedJson)
    }

    private fun addItem() {
        if (photoPath != null || photoUri != null) {
            val imagePathToUse = photoPath ?: photoUri.toString()

            val newItem = Item(
                id = System.currentTimeMillis().toString(),
                name = nameView.text.toString(),
                imagePath = imagePathToUse,
                category = selectedCategory,
                subcategory = selectedSubcategory
            )

            saveNewItemToJson(requireContext(), newItem)

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