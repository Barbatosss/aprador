package com.example.aprador.items

import android.app.AlertDialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.Button
import android.widget.EditText
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.annotation.RequiresApi
import androidx.core.graphics.scale
import androidx.core.net.toUri
import com.example.aprador.R
import com.example.aprador.navigation.NavBar
import com.example.aprador.recycler.Item
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

// Add these imports for camera/gallery functionality
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class ItemDetails : Fragment(R.layout.fragment_item_details) {

    // UI Components
    private lateinit var itemPhoto: ImageView
    private lateinit var itemNameValue: TextView
    private lateinit var itemCategoryValue: TextView
    private lateinit var itemSubcategoryValue: TextView
    private lateinit var deleteButton: Button
    private lateinit var editPhotoHint: TextView

    // Edit mode UI components
    private lateinit var itemNameEdit: EditText
    private lateinit var itemCategorySpinner: Spinner
    private lateinit var itemSubcategorySpinner: Spinner
    private lateinit var editButton: View

    // Item data
    private var currentItem: Item? = null
    private var itemId: String? = null
    private var isEditMode = false

    // Photo data for editing
    private var newPhotoPath: String? = null
    private var newPhotoUri: String? = null
    private var currentPhotoFile: File? = null

    // Image processing constants
    companion object {
        private const val MAX_BITMAP_DIMENSION = 1024
        private const val DEFAULT_IMAGE_VIEW_WIDTH_DP = 313
        private const val DEFAULT_IMAGE_VIEW_HEIGHT_DP = 229
        private const val CAMERA_PERMISSION_REQUEST_CODE = 100
    }

    // Category and subcategory data (from AddItem.kt)
    private val categoryOptions = arrayOf("Top", "Bottom", "Outerwear", "Footwear")

    private val topSubcategories = arrayOf("T-Shirts", "Polo", "Dress Shirt", "Tank Top", "Blouse", "Camisole", "Crop Top")
    private val bottomSubcategories = arrayOf("Jeans", "Chinos", "Shorts", "Joggers", "Leggings", "Skirt", "Dress")
    private val outerwearSubcategories = arrayOf("Jacket", "Hoodie", "Blazer", "Coat", "Cardigan", "Kimono")
    private val footwearSubcategories = arrayOf("Sneakers", "Dress Shoes", "Boots", "Sandals", "Heels", "Flats")

    // Activity result launchers for camera and gallery
    @RequiresApi(Build.VERSION_CODES.N)
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            currentPhotoFile?.let { file ->
                if (file.exists()) {
                    newPhotoPath = file.absolutePath
                    newPhotoUri = null
                    loadNewPhoto()
                } else {
                    showToast("Failed to capture photo")
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                newPhotoUri = uri.toString()
                newPhotoPath = null
                loadNewPhoto()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            showPhotoSelectionDialog()
        } else {
            showToast("Camera permission is required to take photos")
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Hide bottom navigation when this fragment loads
        view.post {
            (activity as? NavBar)?.hideBottomNavigation()
        }

        // Get item ID from arguments
        arguments?.let { bundle ->
            itemId = bundle.getString("item_id")
        }

        // Initialize views
        initializeViews(view)

        // Load item data
        loadItemData()

        // Setup click listeners
        setupClickListeners(view)

        // Initialize in view mode
        setViewMode()
    }

    private fun initializeViews(view: View) {
        // View mode components
        itemPhoto = view.findViewById(R.id.ItemPhoto)
        itemNameValue = view.findViewById(R.id.item_name_value)
        itemCategoryValue = view.findViewById(R.id.item_category_value)
        itemSubcategoryValue = view.findViewById(R.id.item_subcategory_value)
        deleteButton = view.findViewById(R.id.DeleteItemButton)
        editPhotoHint = view.findViewById(R.id.edit_photo_hint)

        // Edit mode components
        itemNameEdit = view.findViewById(R.id.item_name_edit)
        itemCategorySpinner = view.findViewById(R.id.item_category_spinner)
        itemSubcategorySpinner = view.findViewById(R.id.item_subcategory_spinner)
        editButton = view.findViewById(R.id.EditItem)

        // Setup category spinner
        setupCategorySpinner()
    }

    private fun setupCategorySpinner() {
        val categoryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categoryOptions)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        itemCategorySpinner.adapter = categoryAdapter

        // Set listener for category changes to update subcategory options
        itemCategorySpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateSubcategorySpinner(categoryOptions[position])
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
    }

    private fun updateSubcategorySpinner(category: String) {
        val subcategories = when (category) {
            "Top" -> topSubcategories
            "Bottom" -> bottomSubcategories
            "Outerwear" -> outerwearSubcategories
            "Footwear" -> footwearSubcategories
            else -> topSubcategories
        }

        val subcategoryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, subcategories)
        subcategoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        itemSubcategorySpinner.adapter = subcategoryAdapter
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun setupClickListeners(view: View) {
        // Back button
        val backButton: View = view.findViewById(R.id.BackItemDetails)
        backButton.setOnClickListener {
            if (isEditMode) {
                // Ask user if they want to discard changes
                showDiscardChangesDialog()
            } else {
                navigateBackToMyItems()
            }
        }

        // Edit/Save button
        editButton.setOnClickListener {
            if (isEditMode) {
                saveChanges()
            } else {
                setEditMode()
            }
        }

        // Delete button
        deleteButton.setOnClickListener {
            showDeleteConfirmationDialog()
        }

        // Photo click - different behavior based on mode
        itemPhoto.setOnClickListener {
            if (isEditMode) {
                // Show photo selection dialog (camera/gallery)
                checkCameraPermissionAndShowDialog()
            } else {
                // Show full screen image view (existing functionality)
                Toast.makeText(requireContext(), "Full screen view coming soon", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun checkCameraPermissionAndShowDialog() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                showPhotoSelectionDialog()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                AlertDialog.Builder(requireContext())
                    .setTitle("Camera Permission Needed")
                    .setMessage("This app needs camera permission to take photos of your items.")
                    .setPositiveButton("Grant Permission") { _, _ ->
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            else -> {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun showPhotoSelectionDialog() {
        val options = arrayOf("Take Photo", "Choose from Gallery", "Cancel")
        AlertDialog.Builder(requireContext())
            .setTitle("Update Photo")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> openCamera()
                    1 -> openGallery()
                    2 -> dialog.dismiss()
                }
            }
            .show()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun openCamera() {
        try {
            val photoFile = createImageFile()
            currentPhotoFile = photoFile

            val photoURI = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.provider",
                photoFile
            )

            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)

            // Grant permission to camera app
            takePictureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

            cameraLauncher.launch(takePictureIntent)
        } catch (ex: IOException) {
            showToast("Error creating photo file")
            ex.printStackTrace()
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        galleryLauncher.launch(intent)
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "ITEM_${timeStamp}_"
        val storageDir = File(requireContext().getExternalFilesDir(null), "Pictures")

        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }

        return File.createTempFile(imageFileName, ".jpg", storageDir)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun loadNewPhoto() {
        when {
            !newPhotoUri.isNullOrEmpty() && newPhotoUri != "null" -> {
                val uri = newPhotoUri!!.toUri()
                if (uri.scheme == "content") {
                    loadImage(uri = uri)
                } else {
                    loadImage(path = newPhotoPath)
                }
            }
            !newPhotoPath.isNullOrEmpty() -> loadImage(path = newPhotoPath)
            else -> showToast("Failed to load new photo")
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun loadImage(path: String? = null, uri: Uri? = null) {
        itemPhoto.post {
            val width = itemPhoto.width.takeIf { it > 0 } ?: dpToPx(DEFAULT_IMAGE_VIEW_WIDTH_DP)
            val height = itemPhoto.height.takeIf { it > 0 } ?: dpToPx(DEFAULT_IMAGE_VIEW_HEIGHT_DP)

            val bitmap = when {
                path != null && File(path).exists() -> decodeImageWithOrientation(path, width, height)
                uri != null -> decodeBitmapFromUri(uri)?.let { correctOrientation(it, uri) }
                else -> null
            }

            if (bitmap != null) {
                itemPhoto.apply {
                    setImageBitmap(scaleAndCropBitmap(bitmap, width, height))
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    background = null
                }
                showToast("Photo updated")
            } else {
                showToast("Failed to load photo")
            }
        }
    }

    private fun setViewMode() {
        isEditMode = false

        // Show view components
        itemNameValue.visibility = View.VISIBLE
        itemCategoryValue.visibility = View.VISIBLE
        itemSubcategoryValue.visibility = View.VISIBLE
        editPhotoHint.visibility = View.GONE

        // Hide edit components
        itemNameEdit.visibility = View.GONE
        itemCategorySpinner.visibility = View.GONE
        itemSubcategorySpinner.visibility = View.GONE

        // Update edit button appearance
        editButton.setBackgroundResource(R.drawable.ic_edit)

        // Reset new photo data
        newPhotoPath = null
        newPhotoUri = null
    }

    private fun setEditMode() {
        isEditMode = true
        currentItem?.let { item ->

            // Hide view components
            itemNameValue.visibility = View.GONE
            itemCategoryValue.visibility = View.GONE
            itemSubcategoryValue.visibility = View.GONE
            editPhotoHint.visibility = View.VISIBLE

            // Show edit components
            itemNameEdit.visibility = View.VISIBLE
            itemCategorySpinner.visibility = View.VISIBLE
            itemSubcategorySpinner.visibility = View.VISIBLE

            // Populate edit fields with current values
            itemNameEdit.setText(item.name)

            // Set category spinner selection
            val categoryIndex = categoryOptions.indexOf(item.category)
            if (categoryIndex >= 0) {
                itemCategorySpinner.setSelection(categoryIndex)
            }

            // Update subcategory spinner and set selection
            updateSubcategorySpinner(item.category)
            itemSubcategorySpinner.post {
                val subcategoryAdapter = itemSubcategorySpinner.adapter
                for (i in 0 until subcategoryAdapter.count) {
                    if (subcategoryAdapter.getItem(i).toString() == item.subcategory) {
                        itemSubcategorySpinner.setSelection(i)
                        break
                    }
                }
            }

            // Update edit button appearance
            editButton.setBackgroundResource(R.drawable.ic_confirm_add)
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun saveChanges() {
        currentItem?.let { item ->
            // Get updated values
            val updatedName = itemNameEdit.text.toString().trim()
            val updatedCategory = itemCategorySpinner.selectedItem.toString()
            val updatedSubcategory = itemSubcategorySpinner.selectedItem.toString()

            // Validate name
            if (updatedName.isEmpty()) {
                Toast.makeText(requireContext(), "Item name cannot be empty", Toast.LENGTH_SHORT).show()
                return
            }

            // Use new photo if available, otherwise keep original
            val updatedImagePath = when {
                !newPhotoPath.isNullOrEmpty() -> newPhotoPath!!
                !newPhotoUri.isNullOrEmpty() -> newPhotoUri!!
                else -> item.imagePath
            }

            // Create updated item
            val updatedItem = item.copy(
                name = updatedName,
                category = updatedCategory,
                subcategory = updatedSubcategory,
                imagePath = updatedImagePath
            )

            // Save to JSON
            if (updateItemInJson(requireContext(), updatedItem)) {
                currentItem = updatedItem
                displayItemData(updatedItem)
                setViewMode()
                Toast.makeText(requireContext(), "Item updated successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Failed to update item", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun showDiscardChangesDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Discard Changes")
            .setMessage("Are you sure you want to discard your changes?")
            .setPositiveButton("Discard") { dialog, _ ->
                // Reload original image if photo was changed
                currentItem?.let { displayItemData(it) }
                setViewMode()
                dialog.dismiss()
            }
            .setNegativeButton("Keep Editing") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun loadItemData() {
        if (itemId.isNullOrEmpty()) {
            showToast("Item not found")
            navigateBackToMyItems()
            return
        }

        // Load item from JSON file
        currentItem = loadItemById(requireContext(), itemId!!)

        if (currentItem != null) {
            displayItemData(currentItem!!)
        } else {
            showToast("Item not found")
            navigateBackToMyItems()
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun displayItemData(item: Item) {
        // Set text values
        itemNameValue.text = item.name.ifBlank { "Unnamed Item" }
        itemCategoryValue.text = item.category
        itemSubcategoryValue.text = item.subcategory

        // Load and display image
        loadItemImage(item.imagePath)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun loadItemImage(imagePath: String?) {
        if (imagePath.isNullOrEmpty()) {
            setPlaceholderImage()
            return
        }

        itemPhoto.post {
            val width = itemPhoto.width.takeIf { it > 0 } ?: dpToPx(DEFAULT_IMAGE_VIEW_WIDTH_DP)
            val height = itemPhoto.height.takeIf { it > 0 } ?: dpToPx(DEFAULT_IMAGE_VIEW_HEIGHT_DP)

            val bitmap = when {
                imagePath.startsWith("content://") -> {
                    val uri = imagePath.toUri()
                    decodeBitmapFromUri(uri)?.let { correctOrientation(it, uri) }
                }
                File(imagePath).exists() -> {
                    decodeImageWithOrientation(imagePath, width, height)
                }
                else -> null
            }

            if (bitmap != null) {
                itemPhoto.apply {
                    setImageBitmap(scaleAndCropBitmap(bitmap, width, height))
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    background = null
                }
            } else {
                setPlaceholderImage()
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
        val scaled = bitmap.scale((bitmap.width * scale).toInt(), (bitmap.height * scale).toInt())
        val cropX = maxOf(0, (scaled.width - targetWidth) / 2)
        val cropY = maxOf(0, (scaled.height - targetHeight) / 2)
        return Bitmap.createBitmap(scaled, cropX, cropY, targetWidth, targetHeight)
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        var inSampleSize = 1
        val (width, height) = options.outWidth to options.outHeight
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    private fun setPlaceholderImage() {
        itemPhoto.setImageResource(R.drawable.shirt)
        itemPhoto.scaleType = ImageView.ScaleType.CENTER_INSIDE
        itemPhoto.setBackgroundResource(R.drawable.shirt)
    }

    private fun showDeleteConfirmationDialog() {
        currentItem?.let { item ->
            AlertDialog.Builder(requireContext())
                .setTitle("Delete Item")
                .setMessage("Are you sure you want to delete \"${item.name.ifBlank { "this item" }}\"? This action cannot be undone.")
                .setPositiveButton("Delete") { dialog, _ ->
                    deleteItem(item.id)
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
    }

    private fun deleteItem(itemId: String) {
        try {
            val file = File(requireContext().filesDir, "db.json")

            if (file.exists() && file.readText().isNotBlank()) {
                val json = file.readText()
                val type = object : TypeToken<MutableList<Item>>() {}.type
                val items: MutableList<Item> = Gson().fromJson(json, type)

                // Remove item with matching ID
                val removed = items.removeAll { it.id == itemId }

                if (removed) {
                    // Write updated list back to file
                    val updatedJson = Gson().toJson(items)
                    file.writeText(updatedJson)

                    showToast("Item deleted successfully")
                    navigateBackToMyItems()
                } else {
                    showToast("Failed to delete item")
                }
            } else {
                showToast("No items found")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            showToast("Error deleting item")
        }
    }

    private fun updateItemInJson(context: Context, updatedItem: Item): Boolean {
        return try {
            val file = File(context.filesDir, "db.json")

            if (file.exists() && file.readText().isNotBlank()) {
                val json = file.readText()
                val type = object : TypeToken<MutableList<Item>>() {}.type
                val items: MutableList<Item> = Gson().fromJson(json, type)

                // Find and update the item
                val index = items.indexOfFirst { it.id == updatedItem.id }
                if (index >= 0) {
                    items[index] = updatedItem

                    // Write updated list back to file
                    val updatedJson = Gson().toJson(items)
                    file.writeText(updatedJson)
                    true
                } else {
                    false
                }
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun loadItemById(context: Context, itemId: String): Item? {
        return try {
            val file = File(context.filesDir, "db.json")
            if (file.exists() && file.readText().isNotBlank()) {
                val json = file.readText()
                val type = object : TypeToken<List<Item>>() {}.type
                val items: List<Item> = Gson().fromJson(json, type)
                items.find { it.id == itemId }
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun navigateBackToMyItems() {
        val myItemsFragment = MyItems()
        parentFragmentManager.beginTransaction()
            .replace(R.id.flFragment, myItemsFragment)
            .addToBackStack(null)
            .commit()
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Show bottom navigation when leaving this fragment
        (activity as? NavBar)?.showBottomNavigation()
    }
}