package com.example.aprador.login

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
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
import com.example.aprador.R
import com.example.aprador.utils.ImageUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions

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
        // Load user data from preferences
        val userName = userPreferences.getUserName()
        val userPhotoUrl = userPreferences.getUserPhotoUrl()

        tvUsername.text = userName

        // Load profile picture if available
        userPhotoUrl?.let { photoUrl ->
            Glide.with(this)
                .load(photoUrl)
                .placeholder(R.drawable.ic_default_profile) // Add a placeholder image
                .error(R.drawable.ic_default_profile)
                .circleCrop()
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
        // Clear local user data
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
                updateGenderToggleUI()
            }
        }

        tvFemale.setOnClickListener {
            if (selectedGender != "Female") {
                selectedGender = "Female"
                updateGenderToggleUI()
            }
        }
    }

    private fun updateGenderToggleUI() {
        if (selectedGender == "Male") {
            tvMale.setBackgroundResource(R.drawable.tab_selected_background)
            tvMale.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))

            tvFemale.setBackgroundResource(R.drawable.tab_unselected_background)
            tvFemale.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
        } else {
            tvFemale.setBackgroundResource(R.drawable.tab_selected_background)
            tvFemale.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))

            tvMale.setBackgroundResource(R.drawable.tab_unselected_background)
            tvMale.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
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
        // TODO: Implement full picture view
        Toast.makeText(requireContext(), "View full picture functionality not implemented yet", Toast.LENGTH_SHORT).show()
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

            bitmap?.let {
                // Set the processed image to the ImageView
                ImageUtil.setImageToView(
                    ivProfilePicture,
                    it,
                    ImageView.ScaleType.CENTER_CROP
                )

                // TODO: Save the image URI or bitmap to preferences/database
                Toast.makeText(requireContext(), "Profile picture updated", Toast.LENGTH_SHORT).show()
            } ?: run {
                Toast.makeText(requireContext(), "Failed to load image", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Error processing image", Toast.LENGTH_SHORT).show()
        }
    }
}