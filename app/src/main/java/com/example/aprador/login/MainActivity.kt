package com.example.aprador.login

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.aprador.navigation.NavBar
import com.example.aprador.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var userPreferences: UserPreferences
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>

    private companion object {
        private const val TAG = "GoogleSignIn"
        private const val RC_SIGN_IN = 9001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize UserPreferences
        userPreferences = UserPreferences(this)

        // Initialize Activity Result Launcher for Google Sign-in
        googleSignInLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            Log.d(TAG, "Activity result received with code: ${result.resultCode}")
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            handleSignInResult(task)
        }

        // Check if user is already logged in
        if (userPreferences.isUserLoggedIn()) {
            navigateToNavBar()
            return
        }

        // Initialize Google Sign-in
        setupGoogleSignIn()
        copyJsonToInternalStorageIfNotExists(this)

        val loginButton = findViewById<Button>(R.id.googleSignInButton)

        // Google Sign-in button click
        loginButton.setOnClickListener {
            signInWithGoogle()
        }
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestId()
            .requestProfile()
            .requestIdToken(getString(R.string.client_id)) // Make sure this is correct
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun signInWithGoogle() {
        Log.d(TAG, "Starting Google Sign-In process")

        // Don't sign out first - this can cause issues
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            Log.d(TAG, "signInResult:success - Account: ${account?.displayName}")

            // Save user data locally
            account?.let {
                userPreferences.saveUserData(
                    name = it.displayName ?: "User",
                    email = it.email ?: "",
                    photoUrl = it.photoUrl?.toString(),
                    userId = it.id ?: ""
                )
                userPreferences.setUserLoggedIn(true)

                Toast.makeText(this, "Welcome ${it.displayName}!", Toast.LENGTH_SHORT).show()
                navigateToNavBar()
            }

        } catch (e: ApiException) {
            Log.w(TAG, "signInResult:failed code=" + e.statusCode)
            when (e.statusCode) {
                12501 -> {
                    Log.d(TAG, "Sign in was cancelled by user")
                    Toast.makeText(this, "Sign in cancelled", Toast.LENGTH_SHORT).show()
                }
                12500 -> {
                    Log.d(TAG, "Sign in failed - invalid configuration")
                    Toast.makeText(this, "Sign in failed. Please check app configuration.", Toast.LENGTH_LONG).show()
                }
                7 -> {
                    Log.d(TAG, "Network error during sign in")
                    Toast.makeText(this, "Network error. Please check your connection.", Toast.LENGTH_SHORT).show()
                }
                10 -> {
                    Log.d(TAG, "Developer error - invalid client configuration")
                    Toast.makeText(this, "Developer error. Please check your configuration.", Toast.LENGTH_LONG).show()
                }
                else -> {
                    Log.d(TAG, "Unknown error during sign in: ${e.statusCode}")
                    Toast.makeText(this, "Sign in failed. Please try again.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun navigateToNavBar() {
        val intent = Intent(this, NavBar::class.java)
        startActivity(intent)
        finish()
    }

    private fun copyJsonToInternalStorageIfNotExists(context: Context) {
        val file = File(context.filesDir, "db.json")
        if (!file.exists()) {
            try {
                context.assets.open("db.json").use { inputStream ->
                    FileOutputStream(file).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                Log.d(TAG, "JSON file copied successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error copying JSON file", e)
            }
        }
    }
}