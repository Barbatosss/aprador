package com.example.aprador.outfits

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.aprador.R
import com.example.aprador.item_recycler.Item
import com.example.aprador.item_recycler.ItemAdapter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import androidx.core.net.toUri
import com.example.aprador.outfit_recycler.Outfit

class OutfitDetails : Fragment(R.layout.fragment_outfit_details) {

    private lateinit var outfitTitle: TextView
    private lateinit var outfitPhoto: ImageView
    private lateinit var outfitNameValue: TextView
    private lateinit var outfitNameEdit: EditText
    private lateinit var outfitCategoryValue: TextView
    private lateinit var outfitCategorySpinner: Spinner
    private lateinit var usedItemsCount: TextView
    private lateinit var itemsRecyclerView: RecyclerView
    private lateinit var backButton: View
    private lateinit var editButton: View
    private lateinit var deleteButton: Button

    private lateinit var itemAdapter: ItemAdapter
    private var currentOutfit: Outfit? = null
    private var outfitItems = listOf<Item>()
    private var isEditMode = false

    companion object {
        private const val ARG_OUTFIT_ID = "outfit_id"

        fun newInstance(outfitId: String): OutfitDetails {
            val fragment = OutfitDetails()
            val args = Bundle()
            args.putString(ARG_OUTFIT_ID, outfitId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        setupRecyclerView()
        setupClickListeners()
        loadOutfitData()
    }

    private fun initializeViews(view: View) {
        outfitTitle = view.findViewById(R.id.OutfitTitle)
        outfitPhoto = view.findViewById(R.id.OutfitPhoto)
        outfitNameValue = view.findViewById(R.id.outfit_name_value)
        outfitNameEdit = view.findViewById(R.id.outfit_name_edit)
        outfitCategoryValue = view.findViewById(R.id.outfit_category_value)
        outfitCategorySpinner = view.findViewById(R.id.outfit_category_spinner)
        usedItemsCount = view.findViewById(R.id.used_items_count)
        itemsRecyclerView = view.findViewById(R.id.items_recycler_view)
        backButton = view.findViewById(R.id.BackOutfitDetails)
        editButton = view.findViewById(R.id.EditOutfit)
        deleteButton = view.findViewById(R.id.delete_outfit_button)
    }

    private fun setupRecyclerView() {
        itemAdapter = ItemAdapter(outfitItems) { item ->
            // Handle item click in details view - maybe show item details
            showItemDetails(item)
        }

        itemsRecyclerView.apply {
            adapter = itemAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            isNestedScrollingEnabled = false
        }
    }

    private fun setupClickListeners() {
        backButton.setOnClickListener {
            navigateBack()
        }

        editButton.setOnClickListener {
            toggleEditMode()
        }

        deleteButton.setOnClickListener {
            showDeleteConfirmationDialog()
        }
    }

    private fun loadOutfitData() {
        val outfitId = arguments?.getString(ARG_OUTFIT_ID)
        if (outfitId != null) {
            currentOutfit = loadOutfitById(requireContext(), outfitId)
            currentOutfit?.let { outfit ->
                displayOutfitData(outfit)
                loadOutfitItems(outfit)
            }
        }
    }

    private fun displayOutfitData(outfit: Outfit) {
        // Set title and basic info
        outfitTitle.text = outfit.title
        outfitNameValue.text = outfit.title
        outfitNameEdit.setText(outfit.title)
        outfitCategoryValue.text = outfit.category

        // Set used items count
        usedItemsCount.text = outfit.items.size.toString()

        // Load outfit preview image
        loadOutfitImage(outfit)

        // Setup category spinner for edit mode
        setupCategorySpinner(outfit.category)
    }

    private fun loadOutfitImage(outfit: Outfit) {
        // Try to load the preview image first
        outfit.previewImagePath?.let { previewPath ->
            val previewFile = File(previewPath)
            if (previewFile.exists()) {
                Glide.with(this)
                    .load(previewFile)
                    .placeholder(R.drawable.shirt)
                    .error(R.drawable.shirt)
                    .into(outfitPhoto)
                return
            }
        }

        // Fallback: Create a composite image from the first few items
        if (outfitItems.isNotEmpty()) {
            loadCompositeImage()
        } else {
            outfitPhoto.setImageResource(R.drawable.shirt)
        }
    }

    private fun loadCompositeImage() {
        // For now, just load the first item's image as a placeholder
        // You could implement a more sophisticated composite image creation here
        if (outfitItems.isNotEmpty()) {
            val firstItem = outfitItems[0]
            loadItemImage(firstItem.imagePath, outfitPhoto)
        }
    }

    private fun loadItemImage(imagePath: String, imageView: ImageView) {
        val context = requireContext()
        val imageSource = when {
            imagePath.startsWith("content://") -> imagePath.toUri()
            imagePath.startsWith("file://") -> imagePath.toUri()
            File(imagePath).exists() -> File(imagePath)
            else -> null
        }

        if (imageSource != null) {
            Glide.with(context)
                .load(imageSource)
                .centerCrop()
                .placeholder(R.drawable.shirt)
                .error(R.drawable.shirt)
                .into(imageView)
        } else {
            imageView.setImageResource(R.drawable.shirt)
        }
    }

    private fun setupCategorySpinner(currentCategory: String) {
        val categories = listOf("Casual", "Ethnic", "Formal", "Sports", "Smart Casual", "Travel", "Party", "Home")

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            categories
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        outfitCategorySpinner.adapter = adapter
        outfitCategorySpinner.setSelection(categories.indexOf(currentCategory))
    }

    private fun loadOutfitItems(outfit: Outfit) {
        val allItems = loadItems(requireContext())
        outfitItems = allItems.filter { item -> outfit.items.contains(item.id) }

        // Update the adapter with the filtered items
        itemAdapter = ItemAdapter(outfitItems) { item ->
            showItemDetails(item)
        }
        itemsRecyclerView.adapter = itemAdapter

        // Update the count
        usedItemsCount.text = outfitItems.size.toString()
    }

    private fun toggleEditMode() {
        isEditMode = !isEditMode

        if (isEditMode) {
            // Switch to edit mode
            outfitNameValue.visibility = View.GONE
            outfitNameEdit.visibility = View.VISIBLE
            outfitCategoryValue.visibility = View.GONE
            outfitCategorySpinner.visibility = View.VISIBLE
            deleteButton.visibility = View.VISIBLE
            editButton.setBackgroundResource(R.drawable.ic_confirm_add)
        } else {
            // Switch to view mode and save changes
            saveOutfitChanges()
            outfitNameValue.visibility = View.VISIBLE
            outfitNameEdit.visibility = View.GONE
            outfitCategoryValue.visibility = View.VISIBLE
            outfitCategorySpinner.visibility = View.GONE
            deleteButton.visibility = View.GONE
            editButton.setBackgroundResource(R.drawable.ic_edit)
        }
    }

    private fun saveOutfitChanges() {
        currentOutfit?.let { outfit ->
            val newName = outfitNameEdit.text.toString().trim()
            val newCategory = outfitCategorySpinner.selectedItem.toString()

            if (newName.isNotEmpty()) {
                val updatedOutfit = outfit.copy(
                    title = newName,
                    category = newCategory
                )

                updateOutfitInJson(requireContext(), updatedOutfit)
                currentOutfit = updatedOutfit
                displayOutfitData(updatedOutfit)

                Toast.makeText(requireContext(), "Outfit updated successfully!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Outfit name cannot be empty", Toast.LENGTH_SHORT).show()
                outfitNameEdit.setText(outfit.title) // Reset to original name
            }
        }
    }

    private fun showDeleteConfirmationDialog() {
        currentOutfit?.let { outfit ->
            android.app.AlertDialog.Builder(requireContext())
                .setTitle("Delete Outfit")
                .setMessage("Are you sure you want to delete '${outfit.title}'? This action cannot be undone.")
                .setPositiveButton("Delete") { _, _ ->
                    deleteOutfit(outfit)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun deleteOutfit(outfit: Outfit) {
        deleteOutfitFromJson(requireContext(), outfit.id)

        // Delete preview image if it exists
        outfit.previewImagePath?.let { previewPath ->
            val previewFile = File(previewPath)
            if (previewFile.exists()) {
                previewFile.delete()
            }
        }

        Toast.makeText(requireContext(), "Outfit '${outfit.title}' deleted", Toast.LENGTH_SHORT).show()
        navigateBack()
    }

    private fun showItemDetails(item: Item) {
        val message = "Item: ${item.name}\n" +
                "Category: ${item.category}\n" +
                "Subcategory: ${item.subcategory}"

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Item Details")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun navigateBack() {
        try {
            val myOutfitsFragment = MyOutfits()
            parentFragmentManager.beginTransaction()
                .replace(R.id.flFragment, myOutfitsFragment)
                .addToBackStack(null)
                .commit()
        } catch (e: Exception) {
            parentFragmentManager.popBackStack()
        }
    }

    // Data loading and saving methods
    private fun loadOutfitById(context: Context, outfitId: String): Outfit? {
        return try {
            val file = File(context.filesDir, "outfits.json")
            if (file.exists() && file.readText().isNotBlank()) {
                val json = file.readText()
                val type = object : TypeToken<List<Outfit>>() {}.type
                val outfits: List<Outfit> = Gson().fromJson(json, type) ?: emptyList()
                outfits.find { it.id == outfitId }
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun loadItems(context: Context): List<Item> {
        return try {
            val file = File(context.filesDir, "db.json")
            if (file.exists() && file.readText().isNotBlank()) {
                val json = file.readText()
                val type = object : TypeToken<List<Item>>() {}.type
                Gson().fromJson(json, type) ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun updateOutfitInJson(context: Context, updatedOutfit: Outfit) {
        try {
            val file = File(context.filesDir, "outfits.json")
            val outfits: MutableList<Outfit> = if (file.exists() && file.readText().isNotBlank()) {
                val json = file.readText()
                val type = object : TypeToken<List<Outfit>>() {}.type
                Gson().fromJson(json, type) ?: mutableListOf()
            } else {
                mutableListOf()
            }

            // Find and update the outfit
            val index = outfits.indexOfFirst { it.id == updatedOutfit.id }
            if (index != -1) {
                outfits[index] = updatedOutfit
                val updatedJson = Gson().toJson(outfits)
                file.writeText(updatedJson)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun deleteOutfitFromJson(context: Context, outfitId: String) {
        try {
            val file = File(context.filesDir, "outfits.json")
            val outfits: MutableList<Outfit> = if (file.exists() && file.readText().isNotBlank()) {
                val json = file.readText()
                val type = object : TypeToken<List<Outfit>>() {}.type
                Gson().fromJson(json, type) ?: mutableListOf()
            } else {
                mutableListOf()
            }

            // Remove the outfit
            outfits.removeAll { it.id == outfitId }
            val updatedJson = Gson().toJson(outfits)
            file.writeText(updatedJson)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}