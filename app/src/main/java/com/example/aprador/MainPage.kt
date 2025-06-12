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

        val itemView: View = view.findViewById(R.id.ItemView)

        val outfitView: View = view.findViewById(R.id.OutfitView)

        // Move to MyOutfits
        outfitView.setOnClickListener {

            val outfitFragment = MyOuftits()
            parentFragmentManager.beginTransaction()
                .replace(R.id.flFragment, outfitFragment)
                .addToBackStack(null)
                .commit()
        }

        // Move to MyItems
        itemView.setOnClickListener {

            val itemFragment = MyItems()
            parentFragmentManager.beginTransaction()
                .replace(R.id.flFragment, itemFragment)
                .addToBackStack(null)
                .commit()
        }
    }

}