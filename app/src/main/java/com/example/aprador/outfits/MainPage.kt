package com.example.aprador.outfits

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import com.example.aprador.R
import com.example.aprador.items.MyItems


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