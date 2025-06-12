package com.example.aprador

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView


class MainPage : Fragment(R.layout.fragment_main_page) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Move to MyOutfits
        val outfitView: View = view.findViewById(R.id.OutfitView)

        outfitView.setOnClickListener {

            val outfitFragment = MyOuftits()
            parentFragmentManager.beginTransaction()
                .replace(R.id.flFragment, outfitFragment)
                .addToBackStack(null)
                .commit()
        }
    }

}