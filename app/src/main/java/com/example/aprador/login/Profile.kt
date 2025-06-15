package com.example.aprador.login

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.aprador.R
import com.example.aprador.utils.ImageUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import java.io.File
import java.io.FileOutputStream

class Profile : Fragment(R.layout.fragment_profile) {

    private lateinit var ivProfilePicture: ImageView
    private lateinit var tvUsername: TextView
    private lateinit var tvMale: TextView
    private lateinit var tvFemale: TextView
    private lateinit var btnLogout: Button
    private lateinit var userPreferences: UserPreferences

    private var selectedGender = "Male" // Default gender

    // Activity result launchers
    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher: ActivityResultLauncher<String>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userPreferences = UserPreferences(requireContext())
        initializeViews(view)
        setupActivityResultLaunchers()
        setupClickListeners()
        setupGenderToggle()
        loadUserData()
    }

    private fun initializeViews(view: View) {
        ivProfilePicture = view.findViewById(R.id.iv_profile_picture)
        tvUsername = view.findViewById(R.id.tv_username)
        tvMale = view.findViewById(R.id.tv_male)
        tvFemale = view.findViewById(R.id.tv_female)
        btnLogout = view.findViewById(R.id.btn_logout)
    }

    private fun loadUserData() {
        // First, try to restore profile data from JSON for current user
        val userId = userPreferences.getUserId()
        if (userId.isNotEmpty()) {
            userPreferences.restoreUserProfileFromJson(userId)
        }

        // Load user data from preferences (now restored from JSON if available)
        val userName = userPreferences.getUserName()
        val userPhotoUrl = userPreferences.getUserPhotoUrl()
        selectedGender = userPreferences.getUserGender()

        tvUsername.text = userName

        // Load profile picture if available
        loadProfilePicture(userPhotoUrl)

        // Update gender toggle UI
        updateGenderToggleUI()
    }

    private fun loadProfilePicture(photoUrl: String?) {
        val requestOptions = RequestOptions()
            .placeholder(R.drawable.ic_default_profile)
            .error(R.drawable.ic_default_profile)
            .circleCrop() // This makes the image round by default

        if (!photoUrl.isNullOrEmpty()) {
            // Check if it's a local file path
            if (photoUrl.startsWith("/") && File(photoUrl).exists()) {
                // Load from local file
                Glide.with(this)
                    .load(File(photoUrl))
                    .apply(requestOptions)
                    .into(ivProfilePicture)
            } else {
                // Load from URL
                Glide.with(this)
                    .load(photoUrl)
                    .apply(requestOptions)
                    .into(ivProfilePicture)
            }
        } else {
            // Load default image
            Glide.with(this)
                .load(R.drawable.ic_default_profile)
                .apply(requestOptions)
                .into(ivProfilePicture)
        }
    }

    private fun setupActivityResultLaunchers() {
        // Image picker launcher
        imagePickerLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    handleImageSelection(uri)
                }
            }
        }

        // Permission launcher
        permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                openImagePicker()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Permission denied. Cannot access gallery.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun setupClickListeners() {
        // Profile picture click listener
        ivProfilePicture.setOnClickListener {
            showProfilePictureDialog()
        }

        // Logout button click listener
        btnLogout.setOnClickListener {
            showLogoutDialog()
        }
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performLogout() {
        // Clear local user session data (but preserve profile data in JSON)
        userPreferences.clearUserData()

        // Sign out from Google
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
        val googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

        googleSignInClient.signOut().addOnCompleteListener {
            Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()

            // Navigate back to MainActivity
            val intent = Intent(requireContext(), MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    private fun setupGenderToggle() {
        updateGenderToggleUI()

        tvMale.setOnClickListener {
            if (selectedGender != "Male") {
                selectedGender = "Male"
                userPreferences.saveUserGender(selectedGender)
                updateGenderToggleUI()
                Toast.makeText(requireContext(), "Gender updated to Male", Toast.LENGTH_SHORT).show()
            }
        }

        tvFemale.setOnClickListener {
            if (selectedGender != "Female") {
                selectedGender = "Female"
                userPreferences.saveUserGender(selectedGender)
                updateGenderToggleUI()
                Toast.makeText(requireContext(), "Gender updated to Female", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateGenderToggleUI() {
        if (selectedGender == "Male") {
            tvMale.setBackgroundResource(R.drawable.tab_selected_background)
            tvMale.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))

            tvFemale.setBackgroundResource(R.drawable.tab_unselected_background)
            tvFemale.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    android.R.color.darker_gray
                )
            )
        } else {
            tvFemale.setBackgroundResource(R.drawable.tab_selected_background)
            tvFemale.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))

            tvMale.setBackgroundResource(R.drawable.tab_unselected_background)
            tvMale.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    android.R.color.darker_gray
                )
            )
        }
    }

    private fun showProfilePictureDialog() {
        val options = arrayOf("View Full Picture", "Change Picture")

        AlertDialog.Builder(requireContext())
            .setTitle("Profile Picture")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> viewFullPicture()
                    1 -> changeProfilePicture()
                }
            }
            .show()
    }

    private fun viewFullPicture() {
        // Create a dialog to show full picture
        val photoUrl = userPreferences.getUserPhotoUrl()
        if (!photoUrl.isNullOrEmpty()) {
            val dialog = AlertDialog.Builder(requireContext())
                .create()

            val imageView = ImageView(requireContext())
            imageView.scaleType = ImageView.ScaleType.FIT_CENTER

            if (photoUrl.startsWith("/") && File(photoUrl).exists()) {
                Glide.with(this)
                    .load(File(photoUrl))
                    .into(imageView)
            } else {
                Glide.with(this)
                    .load(photoUrl)
                    .into(imageView)
            }

            dialog.setView(imageView)
            dialog.show()
        } else {
            Toast.makeText(requireContext(), "No profile picture available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun changeProfilePicture() {
        // Check for permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ uses READ_MEDIA_IMAGES
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.READ_MEDIA_IMAGES
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                openImagePicker()
            } else {
                permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
            }
        } else {
            // Below Android 13 uses READ_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                openImagePicker()
            } else {
                permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        imagePickerLauncher.launch(intent)
    }

    private fun handleImageSelection(uri: Uri) {
        try {
            // Use ImageUtil to load and process the image
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                ImageUtil.loadImageFromUri(
                    requireContext(),
                    uri,
                    400, // target width
                    400  // target height (square for profile picture)
                )
            } else {
                // Fallback for older Android versions
                ImageUtil.loadImageFromPath(
                    uri.path ?: "",
                    400,
                    400
                )
            }

            bitmap?.let { processedBitmap ->
                // Save the processed image to internal storage
                val savedImagePath = saveImageToInternalStorage(processedBitmap)

                if (savedImagePath != null) {
                    // Save the local path to preferences
                    userPreferences.saveLocalPhotoPath(savedImagePath)

                    // Load the image with round cropping
                    loadProfilePicture(savedImagePath)

                    Toast.makeText(requireContext(), "Profile picture updated", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Failed to save image", Toast.LENGTH_SHORT).show()
                }
            } ?: run {
                Toast.makeText(requireContext(), "Failed to load image", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Error processing image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveImageToInternalStorage(bitmap: Bitmap): String? {
        return try {
            val userId = userPreferences.getUserId()
            val filename = "profile_${userId}_${System.currentTimeMillis()}.jpg"
            val file = File(requireContext().filesDir, filename)

            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }

            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}