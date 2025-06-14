package com.example.aprador.outfits

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.aprador.R
import com.example.aprador.login.MainPage
import com.example.aprador.navigation.NavBar

class MyOutfits : Fragment(R.layout.fragment_my_outfits) {

    private lateinit var tabAll: TextView
    private lateinit var tabClassic: TextView
    private lateinit var tabSport: TextView
    private lateinit var tabHome: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var outfitAdapter: OutfitAdapter

    private val allOutfits = listOf(
        Outfit(1, "Casual", R.drawable.shirt, listOf("#8B4513", "#D2691E", "#A0522D", "#000000"), "Classic"),
        Outfit(2, "Casual", R.drawable.shirt, listOf("#90EE90", "#FFFF00", "#FF0000", "#000000"), "Classic"),
        Outfit(3, "Casual", R.drawable.shirt, listOf("#8B4513", "#D2691E", "#A0522D", "#F5DEB3"), "Classic"),
        Outfit(4, "Casual", R.drawable.shirt, listOf("#808080", "#D3D3D3", "#000000"), "Classic"),
        Outfit(5, "Sport", R.drawable.shirt, listOf("#FF0000", "#000000", "#FFFFFF"), "Sport"),
        Outfit(6, "Sport", R.drawable.shirt, listOf("#0000FF", "#FFFFFF", "#000000"), "Sport"),
        // Add more outfits as needed
    )

    private var filteredOutfits = allOutfits

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Hide bottom navigation when this fragment loads
        view.post {
            (activity as? NavBar)?.hideBottomNavigation()
        }

        setupViews(view)
        setupRecyclerView(view)
        setupClickListeners(view)
    }

    private fun setupViews(view: View) {
        try {
            tabAll = view.findViewById(R.id.tab_all)
            tabClassic = view.findViewById(R.id.tab_classic)
            tabSport = view.findViewById(R.id.tab_sport)
            tabHome = view.findViewById(R.id.tab_home)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupRecyclerView(view: View) {
        recyclerView = view.findViewById(R.id.recyclerView_outfits)
        outfitAdapter = OutfitAdapter(filteredOutfits) { outfit ->
            // Handle outfit click
            onOutfitClicked(outfit)
        }

        // Set GridLayoutManager with 2 columns
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        recyclerView.adapter = outfitAdapter
    }

    private fun setupClickListeners(view: View) {
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

        // Navigate to CreateOutfit fragment
        val createOutfitView: View? = view.findViewById(R.id.CreateOutfitButton)
        createOutfitView?.setOnClickListener {
            val createOutfitFragment = CreateOutfit()
            parentFragmentManager.beginTransaction()
                .replace(R.id.flFragment, createOutfitFragment)
                .addToBackStack(null)
                .commit()
        }

        // Set click listeners for tabs
        try {
            tabAll.setOnClickListener { selectTab(tabAll) }
            tabClassic.setOnClickListener { selectTab(tabClassic) }
            tabSport.setOnClickListener { selectTab(tabSport) }
            tabHome.setOnClickListener { selectTab(tabHome) }
        } catch (e: Exception) {
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

        // Filter outfits based on selected tab
        when (selectedTab) {
            tabAll -> {
                filteredOutfits = allOutfits
            }
            tabClassic -> {
                filteredOutfits = allOutfits.filter { it.category == "Classic" }
            }
            tabSport -> {
                filteredOutfits = allOutfits.filter { it.category == "Sport" }
            }
            tabHome -> {
                filteredOutfits = allOutfits.filter { it.category == "Home" }
            }
        }

        // Update adapter with filtered outfits
        outfitAdapter.updateOutfits(filteredOutfits)
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

    private fun onOutfitClicked(outfit: Outfit) {
        // Handle outfit item click - navigate to outfit detail or perform action
        // For example, you could navigate to an outfit detail fragment
        // or show a dialog with outfit options
    }
}