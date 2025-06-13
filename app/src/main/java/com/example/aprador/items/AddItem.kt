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

    // Subcategory tabs
    private lateinit var tabTShirt: TextView
    private lateinit var tabSweater: TextView
    private lateinit var tabShirt: TextView
    private lateinit var tabJacket: TextView

    // Photo display
    private lateinit var photoImageView: ImageView

    // Current selections
    private var selectedCategory = "Top"
    private var selectedSubcategory = "T-Shirt"

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
    }

    private fun initializeViews(view: View) {

        nameView = view.findViewById(R.id.item_name_edittext)
        // Initialize category tabs
        tabBottom = view.findViewById(R.id.tab_bottom)
        tabTop = view.findViewById(R.id.tab_top)
        tabOuterwear = view.findViewById(R.id.tab_outerwear)

        // Initialize subcategory tabs
        tabTShirt = view.findViewById(R.id.tab_tshirt)
        tabSweater = view.findViewById(R.id.tab_sweater)
        tabShirt = view.findViewById(R.id.tab_shirt)
        tabJacket = view.findViewById(R.id.tab_jacket)

        // Initialize photo display ImageView
        photoImageView = view.findViewById(R.id.BlackShirt)
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
        photoImageView.setOnClickListener {
            // Navigate back to MyItems to retake photo
            val myItemsFragment = MyItems()
            parentFragmentManager.beginTransaction()
                .replace(R.id.flFragment, myItemsFragment)
                .addToBackStack(null)
                .commit()
        }
    }

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
        // Handle the logic for adding the item with selected category and subcategory
        // You can access selectedCategory, selectedSubcategory, and photoPath variables here

        if (photoPath != null || photoUri != null) {
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