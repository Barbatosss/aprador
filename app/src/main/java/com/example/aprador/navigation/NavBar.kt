package com.example.aprador.navigation

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.aprador.outfits.MainPage
import com.example.aprador.R
import com.google.android.material.bottomnavigation.BottomNavigationView

class NavBar : AppCompatActivity() {

    private lateinit var firstFragment: Fragment
    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_navbar)

        firstFragment = MainPage()

        setFragment(firstFragment)

        // Initialize the class property, not a local variable
        bottomNavigationView = findViewById(R.id.bottomNavigationView)

        bottomNavigationView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.navHome -> setFragment(firstFragment)
            }
            true
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.bottomNavigationView)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(view.paddingLeft, view.paddingTop, view.paddingRight, systemBars.bottom)
            insets
        }

    }

    private fun setFragment(fragment: Fragment) =
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.flFragment, fragment)
            commit()
        }

    // Public functions that fragments can call
    fun hideBottomNavigation() {
        bottomNavigationView.visibility = View.GONE
    }

    fun showBottomNavigation() {
        bottomNavigationView.visibility = View.VISIBLE
    }
}