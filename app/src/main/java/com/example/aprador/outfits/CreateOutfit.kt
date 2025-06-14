package com.example.aprador.outfits

import android.content.Context


import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.Toast
import android.widget.TextView
import android.widget.LinearLayout
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
import android.content.res.AssetFileDescriptor
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore

import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class CreateOutfit : Fragment(R.layout.fragment_create_outfit) {
    private lateinit var itemsRecyclerView: RecyclerView
    private lateinit var itemAdapter: ItemAdapter
    private lateinit var itemsTabsContainer: LinearLayout
    private lateinit var categoryTabsContainer: LinearLayout
    private lateinit var tabAllItems: TextView
    private lateinit var itemsEmptyState: LinearLayout
    private lateinit var backButton: View
    private lateinit var saveButton: View

    // Category tabs
    private lateinit var tabClassic: TextView
    private lateinit var tabSport: TextView
    private lateinit var tabCasual: TextView
    private lateinit var tabFestive: TextView
    private lateinit var tabHome: TextView
    private lateinit var tabOutside: TextView
    private lateinit var categoryPredection : TextView

    private var allItems = listOf<Item>()
    private var filteredItems = listOf<Item>()
    private val dynamicItemTabs = mutableListOf<TextView>()
    private var selectedItemCategory = "All"
    private var selectedOutfitCategory = "Casual" // Default category as shown in XML
    private val selectedItems = mutableListOf<Item>() // Track selected items for outfit

    private lateinit var tflite: Interpreter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        initializeViews(view)

        // Setup RecyclerView
        setupRecyclerView()

        // Load and display items
        loadItemsData()

        // Setup click listeners
        setupClickListeners()
    }

    private fun initializeViews(view: View) {
        itemsRecyclerView = view.findViewById(R.id.items_recycler_view)
        itemsTabsContainer = view.findViewById(R.id.items_tabs_container)
        categoryTabsContainer = view.findViewById(R.id.category_tabs_container)
        tabAllItems = view.findViewById(R.id.tab_all_items)
        itemsEmptyState = view.findViewById(R.id.items_empty_state)
        backButton = view.findViewById(R.id.BackCreateOutfit)
        saveButton = view.findViewById(R.id.SaveOutfit)

        // Category tabs
        tabClassic = view.findViewById(R.id.tab_classic)
        tabSport = view.findViewById(R.id.tab_sport)
        tabCasual = view.findViewById(R.id.tab_casual)
        tabFestive = view.findViewById(R.id.tab_festive)
        tabHome = view.findViewById(R.id.tab_home)
        tabOutside = view.findViewById(R.id.tab_outside)
        categoryPredection = view.findViewById(R.id.categoryprediction)

        tflite = Interpreter(loadModelFile())
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

        // Item filter tabs
        tabAllItems.setOnClickListener { selectItemTab("All") }

        // Category tabs
        tabClassic.setOnClickListener { selectCategoryTab("Classic") }
        tabSport.setOnClickListener { selectCategoryTab("Sport") }
        tabCasual.setOnClickListener { selectCategoryTab("Casual") }
        tabFestive.setOnClickListener { selectCategoryTab("Festive") }
        tabHome.setOnClickListener { selectCategoryTab("Home") }
        tabOutside.setOnClickListener { selectCategoryTab("Outside") }
    }

    private fun loadItemsData() {
        allItems = loadItems(requireContext())
        updateFilteredItems()
        updateItemTabCounts()
        updateEmptyState()
    }

    private fun updateFilteredItems() {
        filteredItems = if (selectedItemCategory == "All") {
            allItems
        } else {
            allItems.filter { it.subcategory.equals(selectedItemCategory, ignoreCase = true) }
        }

        // Update the adapter with new filtered items
        itemAdapter = ItemAdapter(filteredItems) { item ->
            onItemClicked(item)
        }
        itemsRecyclerView.adapter = itemAdapter

        updateEmptyState()
    }

    private fun updateItemTabCounts() {
        // Update "All" tab count
        tabAllItems.text = "All (${allItems.size})"

        // Update dynamic tabs
        val categories = allItems.groupBy { it.subcategory }

        // Clear existing dynamic tabs
        dynamicItemTabs.forEach { itemsTabsContainer.removeView(it) }
        dynamicItemTabs.clear()

        // Create new dynamic tabs for each subcategory
        categories.forEach { (category, items) ->
            val tabView = createDynamicItemTab(category, items.size)
            itemsTabsContainer.addView(tabView)
            dynamicItemTabs.add(tabView)
        }

        // Update tab appearances after creating all tabs
        updateItemTabAppearances()
    }

    private fun createDynamicItemTab(category: String, count: Int): TextView {
        val tabView = layoutInflater.inflate(R.layout.dynamic_tab, itemsTabsContainer, false) as TextView
        tabView.text = "$category ($count)"
        tabView.setOnClickListener { selectItemTab(category) }
        return tabView
    }

    private fun selectItemTab(category: String) {
        selectedItemCategory = category

        // Update tab appearances
        updateItemTabAppearances()

        // Update displayed items
        updateFilteredItems()
    }

    private fun updateItemTabAppearances() {
        // Reset all tabs to unselected state
        tabAllItems.setBackgroundResource(R.drawable.tab_unselected_background)
        tabAllItems.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))

        dynamicItemTabs.forEach { tab ->
            tab.setBackgroundResource(R.drawable.tab_unselected_background)
            tab.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
        }

        // Set selected tab appearance
        if (selectedItemCategory == "All") {
            tabAllItems.setBackgroundResource(R.drawable.tab_selected_background)
            tabAllItems.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
        } else {
            dynamicItemTabs.find {
                it.text.toString().startsWith(selectedItemCategory)
            }?.apply {
                setBackgroundResource(R.drawable.tab_selected_background)
                setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
            }
        }
    }

    private fun selectCategoryTab(category: String) {
        selectedOutfitCategory = category
        updateCategoryTabAppearances()
    }

    private fun updateCategoryTabAppearances() {
        // Reset all category tabs to unselected state
        val categoryTabs = listOf(tabClassic, tabSport, tabCasual, tabFestive, tabHome, tabOutside)
        categoryTabs.forEach { tab ->
            tab.setBackgroundResource(R.drawable.tab_unselected_background)
            tab.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
        }

        // Set selected category tab appearance
        val selectedTab = when (selectedOutfitCategory) {
            "Classic" -> tabClassic
            "Sport" -> tabSport
            "Casual" -> tabCasual
            "Festive" -> tabFestive
            "Home" -> tabHome
            "Outside" -> tabOutside
            else -> tabCasual
        }

        selectedTab.setBackgroundResource(R.drawable.tab_selected_background)
        selectedTab.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
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
        // Toggle item selection for outfit creation
        if (selectedItems.contains(item)) {
            selectedItems.remove(item)
            Toast.makeText(requireContext(), "Removed ${item.name} from outfit", Toast.LENGTH_SHORT).show()
        } else {
            selectedItems.add(item)
            Toast.makeText(requireContext(), "Added ${item.name + item.imagePath} to outfit", Toast.LENGTH_SHORT).show()
        }

        // Update outfit preview
        updateOutfitPreview()

        if (selectedItems.size in 3..4) {
            val bitmaps = selectedItems.mapNotNull { imagePathToBitmap(requireContext(), it.imagePath) }
            if (bitmaps.size == selectedItems.size) {
                val prediction = predictCategoryFromBitmaps(bitmaps, requireContext())
                categoryPredection.text = prediction
            } else {
                categoryPredection.text = "Unable to convert image(s)"
            }
        }

        else{
            categoryPredection.text = ""
        }
    }

    private fun updateOutfitPreview() {
        // Update the outfit preview container
        val outfitPreviewPlaceholder = view?.findViewById<TextView>(R.id.outfit_preview_placeholder)
        val outfitItemsContainer = view?.findViewById<LinearLayout>(R.id.outfit_items_container)

        if (selectedItems.isEmpty()) {
            outfitPreviewPlaceholder?.visibility = View.VISIBLE
            outfitItemsContainer?.visibility = View.GONE
        } else {
            outfitPreviewPlaceholder?.visibility = View.GONE
            outfitItemsContainer?.visibility = View.VISIBLE

            // Clear existing preview items
            outfitItemsContainer?.removeAllViews()

            // Add selected items to preview
            selectedItems.forEach { item ->
                val itemView = layoutInflater.inflate(R.layout.item_card, outfitItemsContainer, false)

                // Set up the preview item
                val itemImage = itemView.findViewById<ImageView>(R.id.item_image)
                val itemName = itemView.findViewById<TextView>(R.id.item_name)

                // Hide the item name in preview
                itemName.visibility = View.GONE

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
            "Outfit saved with ${selectedItems.size} items in $selectedOutfitCategory category",
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
        // Ensure correct category tab appearance
        updateCategoryTabAppearances()
    }


    //Model

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


            val uri = Uri.parse(path)
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
                val scaled = Bitmap.createScaledBitmap(bitmaps[i], inputSize, inputSize, true)
                scaled.copy(Bitmap.Config.ARGB_8888, false)
            } else {
                Bitmap.createBitmap(inputSize, inputSize, Bitmap.Config.ARGB_8888)
            }

            for (y in 0 until inputSize) {
                for (x in 0 until inputSize) {
                    val pixel = bmp.getPixel(x, y)
                    input[0][i][y][x][0] = (pixel shr 16 and 0xFF) / 255.0f
                    input[0][i][y][x][1] = (pixel shr 8 and 0xFF) / 255.0f
                    input[0][i][y][x][2] = (pixel and 0xFF) / 255.0f
                }
            }
        }

        val output = Array(1) { FloatArray(7) } // Or dynamically detect size from model if needed
        tflite.run(input, output)

        // ✅ Load categories from labels.txt
        val categories = loadLabelsFromAssets(context)

        val predictedIndex = output[0].indices.maxByOrNull { output[0][it] } ?: -1

        return if (predictedIndex in categories.indices) {
            "Predicted: ${categories[predictedIndex]}"
        } else {
            "Prediction failed"
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




