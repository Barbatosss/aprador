package com.example.aprador

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat


class MyItems : Fragment(R.layout.fragment_my_items) {

    private lateinit var tabAllItem: TextView
    private lateinit var tabTshirt: TextView
    private lateinit var tabSweater: TextView


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


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
                // Handle Classic tab selection
            }
            tabSweater -> {
                // Handle Sport tab selection
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