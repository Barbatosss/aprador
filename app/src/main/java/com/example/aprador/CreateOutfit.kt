package com.example.aprador

import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.content.ContextCompat

/**
 * A simple [Fragment] subclass.
 * Use the [CreateOutfit.newInstance] factory method to
 * create an instance of this fragment.
 */
class CreateOutfit : Fragment(R.layout.fragment_create_outfit) {

    // Parameters for fragment instantiation (if needed)
    private var param1: String? = null
    private var param2: String? = null

    // Keep track of the currently selected category button
    private var selectedCategoryButton: Button? = null

    // Keep track of the currently selected items filter button
    private var selectedItemsButton: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            // Your existing parameter handling
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_create_outfit, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- Select Category Buttons Logic ---
        val btnCategoryClassic = view.findViewById<Button>(R.id.btnCategoryClassic)
        val btnCategorySport = view.findViewById<Button>(R.id.btnCategorySport)
        val btnCategoryCasual = view.findViewById<Button>(R.id.btnCategoryCasual)
        val btnCategoryFestive = view.findViewById<Button>(R.id.btnCategoryFestive)
        val btnCategoryHome = view.findViewById<Button>(R.id.btnCategoryHome)
        val btnCategoryOutside = view.findViewById<Button>(R.id.btnCategoryOutside)

        val categoryButtons = listOf(
            btnCategoryClassic,
            btnCategorySport,
            btnCategoryCasual,
            btnCategoryFestive,
            btnCategoryHome,
            btnCategoryOutside
        )

        // Set the initial selected category button (e.g., "Casual")
        // This ensures it looks selected when the fragment first loads
        selectedCategoryButton = btnCategoryCasual
        selectedCategoryButton?.apply {
            background = ContextCompat.getDrawable(requireContext(), R.drawable.selected_button_background)
            setTextColor(Color.WHITE) // Selected text color
        }

        // Define a common click listener for all category buttons
        val categoryClickListener = View.OnClickListener { clickedButton ->
            // If there was a previously selected button, reset its appearance
            selectedCategoryButton?.let { previousButton ->
                previousButton.background = ContextCompat.getDrawable(requireContext(), R.drawable.unselected_button_background)
                previousButton.setTextColor(Color.parseColor("#1A1A1A")) // Unselected text color
            }

            // Set the appearance of the newly clicked button to selected
            if (clickedButton is Button) {
                clickedButton.background = ContextCompat.getDrawable(requireContext(), R.drawable.selected_button_background)
                clickedButton.setTextColor(Color.WHITE) // Selected text color
                selectedCategoryButton = clickedButton // Update the selected button reference
            }
        }

        // Apply the category click listener to each category button
        categoryButtons.forEach { button ->
            button.setOnClickListener(categoryClickListener)
        }

        // --- Select Items Buttons Logic ---
        val btnAllItems = view.findViewById<Button>(R.id.btnAllItems)
        val btnClassicItems = view.findViewById<Button>(R.id.btnClassicItems)
        val btnSportItems = view.findViewById<Button>(R.id.btnSportItems)
        val btnCasualItems = view.findViewById<Button>(R.id.btnCasualItems)

        val itemButtons = listOf(
            btnAllItems,
            btnClassicItems,
            btnSportItems,
            btnCasualItems
        )

        // Set the initial selected items button (e.g., "All")
        selectedItemsButton = btnAllItems
        selectedItemsButton?.apply {
            background = ContextCompat.getDrawable(requireContext(), R.drawable.selected_button_background)
            setTextColor(Color.WHITE) // Selected text color
        }

        // Define a common click listener for all items filter buttons
        val itemFilterClickListener = View.OnClickListener { clickedButton ->
            // If there was a previously selected button, reset its appearance
            selectedItemsButton?.let { previousButton ->
                previousButton.background = ContextCompat.getDrawable(requireContext(), R.drawable.unselected_button_background)
                previousButton.setTextColor(Color.parseColor("#1A1A1A")) // Unselected text color
            }

            // Set the appearance of the newly clicked button to selected
            if (clickedButton is Button) {
                clickedButton.background = ContextCompat.getDrawable(requireContext(), R.drawable.selected_button_background)
                clickedButton.setTextColor(Color.WHITE) // Selected text color
                selectedItemsButton = clickedButton // Update the selected button reference
            }
        }

        // Apply the item filter click listener to each item button
        itemButtons.forEach { button ->
            button.setOnClickListener(itemFilterClickListener)
        }

        // --- Handle other buttons or views as needed ---
        view.findViewById<View>(R.id.BackCreateOutfit)?.setOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }
        view.findViewById<View>(R.id.SaveOutfit)?.setOnClickListener {
            val selectedCategory = selectedCategoryButton?.text.toString()
            val selectedItemFilter = selectedItemsButton?.text.toString()
            // Perform save operation with selectedCategory and selectedItemFilter
        }
    }

    companion object {
        private const val ARG_PARAM1 = "param1"
        private const val ARG_PARAM2 = "param2"

        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            CreateOutfit().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}
