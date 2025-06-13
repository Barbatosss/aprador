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
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.net.toUri

class MyItems : Fragment(R.layout.fragment_my_items) {

    private lateinit var tabAllItem: TextView
    private lateinit var tabTshirt: TextView
    private lateinit var tabSweater: TextView

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
                // Photo was captured successfully
                navigateToAddItemWithPhoto()
            } else {
                // Photo capture was cancelled or failed
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
        // Initialize tabs
        tabAllItem = view.findViewById(R.id.tab_all_item)
        tabTshirt = view.findViewById(R.id.tab_Tshirt)
        tabSweater = view.findViewById(R.id.tab_sweater)

        // Set click listeners for tabs
        tabAllItem.setOnClickListener { selectTab(tabAllItem) }
        tabTshirt.setOnClickListener { selectTab(tabTshirt) }
        tabSweater.setOnClickListener { selectTab(tabSweater) }

        // Move to MainPage
        val outfitView: View = view.findViewById(R.id.BackItem)
        outfitView.setOnClickListener {
            val outfitFragment = MainPage()
            parentFragmentManager.beginTransaction()
                .replace(R.id.flFragment, outfitFragment)
                .addToBackStack(null)
                .commit()
        }

        // Modified CreateItem click listener to show photo selection dialog
        val addItemView: View = view.findViewById(R.id.CreateItem)
        addItemView.setOnClickListener {
            showPhotoSelectionDialog()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Show bottom navigation when leaving this fragment (safety net)
        (activity as? NavBar)?.showBottomNavigation()
    }

    private fun showPhotoSelectionDialog() {
        val options = arrayOf("Take Photo", "Upload from Gallery")

        AlertDialog.Builder(requireContext())
            .setTitle("Add Photo")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> checkCameraPermissionAndOpen() // Take Photo
                    1 -> checkStoragePermissionAndOpen() // Upload from Gallery
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
                // Permission already granted
                openCamera()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                // Show rationale dialog for camera permission
                showPermissionRationaleDialog(
                    "Camera Permission Required",
                    "This app needs camera access to take photos of your clothing items. Please grant camera permission to continue.",
                    { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }
                )
            }
            else -> {
                // First time - show explanation dialog then request permission
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

        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(requireActivity().packageManager) != null) {
            // Create the File where the photo should go
            val photoFile: File? = try {
                createImageFile()
            } catch (ex: IOException) {
                // Error occurred while creating the File
                Toast.makeText(requireContext(), "Error creating image file", Toast.LENGTH_SHORT).show()
                null
            }

            // Continue only if the File was successfully created
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
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "ITEM_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
    }

    private fun navigateToAddItemWithPhoto() {
        val addItemFragment = AddItem()

        // Pass the photo data to AddItem fragment
        val bundle = Bundle()

        // For camera captures, currentPhotoPath contains the file path
        // For gallery selections, capturedImageUri contains the content URI
        if (capturedImageUri != null) {
            bundle.putString("photo_uri", capturedImageUri.toString())
            // Only add photo_path if it's a real file path (camera capture)
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

    private fun selectTab(selectedTab: TextView) {
        // Reset all tabs to unselected state
        resetAllTabs()

        // Set selected tab appearance
        selectedTab.setBackgroundResource(R.drawable.tab_selected_background)
        selectedTab.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))

        // Handle tab selection logic here
        when (selectedTab) {
            tabAllItem -> {
                // Handle All tab selection
            }
            tabTshirt -> {
                // Handle T-shirt tab selection
            }
            tabSweater -> {
                // Handle Sweater tab selection
            }
        }
    }

    private fun resetAllTabs() {
        val tabs = arrayOf(tabAllItem, tabTshirt, tabSweater)
        tabs.forEach { tab ->
            tab.setBackgroundResource(R.drawable.tab_unselected_background)
            tab.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
        }
    }
}