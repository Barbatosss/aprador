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
            if (result.resultCode == RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                handleSignInResult(task)
            } else {
                Log.w(TAG, "Google Sign-in was cancelled or failed")
                Toast.makeText(this, "Sign in cancelled", Toast.LENGTH_SHORT).show()
            }
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
            .requestIdToken(getString(R.string.client_id))
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun signInWithGoogle() {
        // Sign out first to ensure account picker is shown
        googleSignInClient.signOut().addOnCompleteListener {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            Log.d(TAG, "signInResult:success")

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
                    Toast.makeText(this, "Sign in cancelled", Toast.LENGTH_SHORT).show()
                }
                7 -> {
                    Toast.makeText(this, "Network error. Please check your connection.", Toast.LENGTH_SHORT).show()
                }
                10 -> {
                    Toast.makeText(this, "Developer error. Please check your configuration.", Toast.LENGTH_LONG).show()
                }
                else -> {
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
            } catch (e: Exception) {
                Log.e(TAG, "Error copying JSON file", e)
            }
        }
    }
}