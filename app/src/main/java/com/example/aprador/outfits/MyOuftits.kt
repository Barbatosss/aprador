package com.example.aprador.outfits

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.aprador.R
import com.example.aprador.navigation.NavBar

class MyOuftits : Fragment(R.layout.fragment_my_ouftits) {

    private lateinit var tabAll: TextView
    private lateinit var tabClassic: TextView
    private lateinit var tabSport: TextView
    private lateinit var tabHome: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Hide bottom navigation when this fragment loads
        (activity as? NavBar)?.hideBottomNavigation()

        // Move to MainPage (Back button)
        val outfitView: View? = view.findViewById(R.id.BackOutfit)
        outfitView?.setOnClickListener {
            // Show bottom navigation when going back to MainPage
            (activity as? NavBar)?.showBottomNavigation()

            val outfitFragment = MainPage()
            parentFragmentManager.beginTransaction()
                .replace(R.id.flFragment, outfitFragment)
                .addToBackStack(null)
                .commit()
        }

        // Initialize tabs with null checks
        try {
            tabAll = view.findViewById(R.id.tab_all)
            tabClassic = view.findViewById(R.id.tab_classic)
            tabSport = view.findViewById(R.id.tab_sport)
            tabHome = view.findViewById(R.id.tab_home)

            // Set click listeners for tabs
            tabAll.setOnClickListener { selectTab(tabAll) }
            tabClassic.setOnClickListener { selectTab(tabClassic) }
            tabSport.setOnClickListener { selectTab(tabSport) }
            tabHome.setOnClickListener { selectTab(tabHome) }
        } catch (e: Exception) {
            // Handle case where tab views don't exist in layout
            e.printStackTrace()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Show bottom navigation when leaving this fragment (safety net)
        (activity as? NavBar)?.showBottomNavigation()
    }

    private fun selectTab(selectedTab: TextView) {
        // Reset all tabs to unselected state
        resetAllTabs()

        // Set selected tab appearance
        try {
            selectedTab.setBackgroundResource(R.drawable.tab_selected_background)
            selectedTab.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Handle tab selection logic here
        when (selectedTab) {
            tabAll -> {
                // Handle All tab selection
            }
            tabClassic -> {
                // Handle Classic tab selection
            }
            tabSport -> {
                // Handle Sport tab selection
            }
            tabHome -> {
                // Handle Home tab selection
            }
        }
    }

    private fun resetAllTabs() {
        try {
            val tabs = arrayOf(tabAll, tabClassic, tabSport, tabHome)

            tabs.forEach { tab ->
                tab.setBackgroundResource(R.drawable.tab_unselected_background)
                tab.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}