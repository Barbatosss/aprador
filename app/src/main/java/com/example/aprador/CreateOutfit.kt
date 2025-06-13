package com.example.aprador

import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.content.ContextCompat // Needed for getColor

/**
 * A simple [Fragment] subclass.
 * Use the [CreateOutfit.newInstance] factory method to
 * create an instance of this fragment.
 */
class CreateOutfit : Fragment(R.layout.fragment_create_outfit) {

    // Keep track of the currently selected category button
    private var selectedCategoryButton: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            // Your existing parameter handling
            // You might retrieve your arguments here, e.g.:
            // val param1 = it.getString(ARG_PARAM1)
            // val param2 = it.getString(ARG_PARAM2)
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

        // Find all category buttons by their IDs
        val btnCategoryClassic = view.findViewById<Button>(R.id.btnCategoryClassic)
        val btnCategorySport = view.findViewById<Button>(R.id.btnCategorySport)
        val btnCategoryCasual = view.findViewById<Button>(R.id.btnCategoryCasual)
        val btnCategoryFestive = view.findViewById<Button>(R.id.btnCategoryFestive)
        val btnCategoryHome = view.findViewById<Button>(R.id.btnCategoryHome)
        val btnCategoryOutside = view.findViewById<Button>(R.id.btnCategoryOutside)

        // Group all category buttons into a list for easier iteration
        val categoryButtons = listOf(
            btnCategoryClassic,
            btnCategorySport,
            btnCategoryCasual,
            btnCategoryFestive,
            btnCategoryHome,
            btnCategoryOutside
        )

        // Set the initial selected button (e.g., "Casual" as it was in your previous XML)
        // Ensure that this button initially has the selected background and text color in XML
        // or set it here if it's dynamic
        selectedCategoryButton = btnCategoryCasual // Set initial selected button

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
                clickedButton.setTextColor(Color.WHITE) // Selected text color (white as requested)
                selectedCategoryButton = clickedButton // Update the selected button reference
            }
        }

        // Apply the click listener to each category button
        categoryButtons.forEach { button ->
            button.setOnClickListener(categoryClickListener)
        }

        // Initialize the appearance of the initially selected button (e.g., "Casual")
        // This ensures it looks selected when the fragment first loads
        selectedCategoryButton?.let { initialButton ->
            initialButton.background = ContextCompat.getDrawable(requireContext(), R.drawable.selected_button_background)
            initialButton.setTextColor(Color.WHITE) // Initial selected text color
        }

        // --- Handle other buttons or views as needed ---
        // Example for Back button (assuming it's in your header_layout and you want to handle its click)
        view.findViewById<View>(R.id.BackCreateOutfit)?.setOnClickListener {
            // Handle back button click, e.g., navigate back
            activity?.onBackPressedDispatcher?.onBackPressed()
        }
        // Example for Save button
        view.findViewById<View>(R.id.SaveOutfit)?.setOnClickListener {
            // Handle save outfit click
            // You can get the selected category using selectedCategoryButton?.text.toString()
            val selectedCategory = selectedCategoryButton?.text.toString()
            // Perform save operation
        }
    }

    companion object {
        // Define constants for argument keys
        private const val ARG_PARAM1 = "param1"
        private const val ARG_PARAM2 = "param2"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment CreateOutfit.
         */
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
