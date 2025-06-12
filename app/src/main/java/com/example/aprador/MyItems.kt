package com.example.aprador

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
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
import android.widget.TextView
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MyItems : Fragment(R.layout.fragment_my_items) {

    private lateinit var tabAllItem: TextView
    private lateinit var tabTshirt: TextView
    private lateinit var tabSweater: TextView

    // Camera related properties
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
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

        // Initialize permission launcher
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                openCamera()
            } else {
                Toast.makeText(requireContext(), "Camera permission is required to take photos", Toast.LENGTH_SHORT).show()
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

        // Modified CreateItem click listener to open camera
        val addItemView: View = view.findViewById(R.id.CreateItem)
        addItemView.setOnClickListener {
            checkCameraPermissionAndOpen()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Show bottom navigation when leaving this fragment (safety net)
        (activity as? NavBar)?.showBottomNavigation()
    }

    private fun checkCameraPermissionAndOpen() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                // Permission already granted
                openCamera()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                // Show rationale and request permission
                Toast.makeText(requireContext(), "Camera permission is needed to take photos of your items", Toast.LENGTH_LONG).show()
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
            else -> {
                // Request permission directly
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
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

        // Pass the photo path to AddItem fragment
        val bundle = Bundle()
        bundle.putString("photo_path", currentPhotoPath)
        bundle.putString("photo_uri", capturedImageUri.toString())
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