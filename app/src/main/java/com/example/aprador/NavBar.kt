package com.example.aprador

import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.aprador.MainPage
import com.example.aprador.R
import com.google.android.material.bottomnavigation.BottomNavigationView

class NavBar : AppCompatActivity() {

    private lateinit var firstFragment: Fragment
//    private var secondFragment: Fragment = ProfileFragment()



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_navbar)

        firstFragment = MainPage()
//        secondFragment = ProfileFragment()

        setFragment(firstFragment)

        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottomNavigationView)

        bottomNavigationView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.navHome -> setFragment(firstFragment)
//                R.id.navProfile -> setFragment(secondFragment)
            }
            true
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.navbar)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setFragment(fragment: Fragment) =
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.flFragment, fragment)
            commit()
        }
}
