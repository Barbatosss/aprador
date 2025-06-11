package com.example.aprador

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager

class MyOuftits : Fragment(R.layout.fragment_my_ouftits) {

    private lateinit var tabAll: TextView
    private lateinit var tabClassic: TextView
    private lateinit var tabSport: TextView
    private lateinit var tabHome: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize tabs
        tabAll = view.findViewById(R.id.tab_all)
        tabClassic = view.findViewById(R.id.tab_classic)
        tabSport = view.findViewById(R.id.tab_sport)
        tabHome = view.findViewById(R.id.tab_home)

        // Set click listeners for tabs
        tabAll.setOnClickListener { selectTab(tabAll) }
        tabClassic.setOnClickListener { selectTab(tabClassic) }
        tabSport.setOnClickListener { selectTab(tabSport) }
        tabHome.setOnClickListener { selectTab(tabHome) }

        // Move to MainPage
        val outfitView: View = view.findViewById(R.id.BackOutfit)
        outfitView.setOnClickListener {
            val outfitFragment = MainPage()
            parentFragmentManager.beginTransaction()
                .replace(R.id.flFragment, outfitFragment)
                .addToBackStack(null)
                .commit()
        }
    }

    private fun selectTab(selectedTab: TextView) {
        // Reset all tabs to unselected state
        resetAllTabs()

        // Set selected tab appearance
        selectedTab.setBackgroundResource(R.drawable.tab_selected_background)
        selectedTab.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))

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
        val tabs = arrayOf(tabAll, tabClassic, tabSport, tabHome)

        tabs.forEach { tab ->
            tab.setBackgroundResource(R.drawable.tab_unselected_background)
            tab.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
        }
    }

}