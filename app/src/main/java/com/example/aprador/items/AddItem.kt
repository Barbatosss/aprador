package com.example.aprador.items

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import java.io.File
import androidx.core.graphics.scale
import androidx.core.net.toUri
import com.example.aprador.outfits.MainPage
import com.example.aprador.R
import com.example.aprador.navigation.NavBar
import com.example.aprador.recycler.Item
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class AddItem : Fragment(R.layout.fragment_add_item) {
    private lateinit var nameView: TextView
    // Category tabs
    private lateinit var tabBottom: TextView
    private lateinit var tabTop: TextView
    private lateinit var tabOuterwear: TextView
    private lateinit var tabFootwear: TextView

    // Gender toggle tabs (you'll need to add these to your layout)
    private lateinit var tabMen: TextView
    private lateinit var tabWomen: TextView

    // Subcategory tabs
    private lateinit var subTab1: TextView
    private lateinit var subTab2: TextView
    private lateinit var subTab3: TextView
    private lateinit var subTab4: TextView

    // Photo display
    private lateinit var photoImageView: ImageView

    // Current selections
    private var selectedCategory = "Top"
    private var selectedSubcategory = "T-Shirt"
    private var selectedGender = "Men" // Default to Men

    // Photo data
    private var photoPath: String? = null
    private var photoUri: String? = null

    // Dynamic sizing constants - can be adjusted based on needs
    companion object {
        private const val DEFAULT_IMAGE_VIEW_WIDTH_DP = 300
        private const val DEFAULT_IMAGE_VIEW_HEIGHT_DP = 180
        private const val MAX_BITMAP_DIMENSION = 1024 // Reduced for better memory management
        private const val THUMBNAIL_SIZE = 400 // For thumbnail processing
    }

    @RequiresApi(Build.VERSION_CODES.N)
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

        // Initialize with default selections
        selectGenderTab("Men")
        selectCategoryTab("Top")
    }

    private fun initializeViews(view: View) {
        // Initialize gender tabs (add these IDs to your layout XML)
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
            // Navigate back to MyItems to retake photo
            val myItemsFragment = MyItems()
            parentFragmentManager.beginTransaction()
                .replace(R.id.flFragment, myItemsFragment)
                .addToBackStack(null)
                .commit()
        }
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

    // ... (keep all the existing image loading methods unchanged) ...

    @RequiresApi(Build.VERSION_CODES.N)
    private fun loadCapturedPhoto() {
        when {
            !photoUri.isNullOrEmpty() && photoUri != "null" -> {
                val uri = photoUri!!.toUri()
                if (uri.scheme == "content") {
                    loadImage(uri = uri)
                } else {
                    loadImage(path = photoPath)
                }
            }
            !photoPath.isNullOrEmpty() -> loadImage(path = photoPath)
            else -> showToast("No photo available")
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun loadImage(path: String? = null, uri: Uri? = null) {
        photoImageView.post {
            val width = photoImageView.width.takeIf { it > 0 } ?: dpToPx(DEFAULT_IMAGE_VIEW_WIDTH_DP)
            val height = photoImageView.height.takeIf { it > 0 } ?: dpToPx(DEFAULT_IMAGE_VIEW_HEIGHT_DP)

            val bitmap = when {
                path != null && File(path).exists() -> decodeImageWithOrientation(path, width, height)
                uri != null -> decodeBitmapFromUri(uri)?.let { correctOrientation(it, uri) }
                else -> null
            }

            if (bitmap != null) {
                photoImageView.apply {
                    setImageBitmap(scaleAndCropBitmap(bitmap, width, height))
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    background = null
                }
                showToast("Photo loaded successfully")
            } else {
                setPlaceholderImage()
                showToast("Failed to load photo")
            }
        }
    }

    private fun decodeBitmapFromUri(uri: Uri): Bitmap? = try {
        requireContext().contentResolver.openInputStream(uri).use {
            BitmapFactory.decodeStream(it)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun correctOrientation(bitmap: Bitmap, uri: Uri): Bitmap {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val exif = inputStream?.let { ExifInterface(it) }
            val orientation = exif?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) ?: 0
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(bitmap, 90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(bitmap, 180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(bitmap, 270f)
                else -> bitmap
            }
        } catch (e: Exception) {
            bitmap
        }
    }

    private fun decodeImageWithOrientation(path: String, width: Int, height: Int): Bitmap? = try {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(path, options)

        options.inSampleSize = calculateInSampleSize(options, width, height)
        options.inJustDecodeBounds = false

        val bitmap = BitmapFactory.decodeFile(path, options)
        val exif = ExifInterface(path)
        val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(bitmap, 90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(bitmap, 180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(bitmap, 270f)
            else -> bitmap
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun scaleAndCropBitmap(bitmap: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        val scale = maxOf(targetWidth.toFloat() / bitmap.width, targetHeight.toFloat() / bitmap.height)
        val scaled = Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
        val cropX = maxOf(0, (scaled.width - targetWidth) / 2)
        val cropY = maxOf(0, (scaled.height - targetHeight) / 2)
        return Bitmap.createBitmap(scaled, cropX, cropY, targetWidth, targetHeight)
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        var inSampleSize = 1
        val (width, height) = options.outWidth to options.outHeight
        if (height > reqHeight || width > reqWidth) {
            var halfHeight = height / 2
            var halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
    // Alternative methods for different image processing needs
    private fun createThumbnail(imagePath: String): Bitmap? {
        return decodeImageWithOrientation(imagePath, THUMBNAIL_SIZE, THUMBNAIL_SIZE)
    }

    private fun createFullSizeImage(imagePath: String): Bitmap? {
        return decodeImageWithOrientation(imagePath, MAX_BITMAP_DIMENSION, MAX_BITMAP_DIMENSION)
    }

    private fun scaleBitmapIfNeeded(bitmap: Bitmap, maxWidth: Int = MAX_BITMAP_DIMENSION, maxHeight: Int = MAX_BITMAP_DIMENSION): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxWidth && height <= maxHeight) {
            return bitmap
        }

        val scaleWidth = maxWidth.toFloat() / width
        val scaleHeight = maxHeight.toFloat() / height
        val scale = minOf(scaleWidth, scaleHeight)

        val scaledWidth = (width * scale).toInt()
        val scaledHeight = (height * scale).toInt()

        return bitmap.scale(scaledWidth, scaledHeight)
    }

    private fun setPlaceholderImage() {
        // Set a placeholder or default image
        photoImageView.setImageResource(R.drawable.shirt)
        photoImageView.scaleType = ImageView.ScaleType.CENTER_INSIDE
        // Restore background for placeholder
        photoImageView.setBackgroundResource(R.drawable.shirt)
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
        val selectedTab = when {
            subTab1.text.toString() == subcategory -> subTab1
            subTab2.text.toString() == subcategory -> subTab2
            subTab3.text.toString() == subcategory -> subTab3
            subTab4.text.toString() == subcategory -> subTab4
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
        // Handle the logic for adding the item with selected category, subcategory, gender, and photo
        // You can access selectedCategory, selectedSubcategory, selectedGender, and photoPath variables here

        if (photoPath != null || photoUri != null) {
            // Example: Log the selections or save to database
            println("Adding item - Gender: $selectedGender, Category: $selectedCategory, Subcategory: $selectedSubcategory, Photo: $photoPath")
            if (photoPath != null || photoUri != null) {
                val imagePathToUse = photoPath ?: photoUri.toString()

                val newItem = Item(
                    id = System.currentTimeMillis().toString(), // or use UUID.randomUUID().toString()
                    name = nameView.text.toString(), // You can replace this with actual user input
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
}