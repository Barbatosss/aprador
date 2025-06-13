package com.example.aprador.login

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.aprador.navigation.NavBar
import com.example.aprador.R
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }




        copyJsonToInternalStorageIfNotExists(this)
        val loginButton = findViewById<View>(R.id.Login)

        // Regular login button click
        loginButton.setOnClickListener {

            val intent = Intent(this, NavBar::class.java)
            startActivity(intent)

        }
    }

    fun copyJsonToInternalStorageIfNotExists(context: Context) {
        val file = File(context.filesDir, "db.json")
        if (!file.exists()) {
            context.assets.open("db.json").use { inputStream ->
                FileOutputStream(file).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        }
    }
}


