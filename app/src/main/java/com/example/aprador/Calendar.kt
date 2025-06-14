package com.example.aprador

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CalendarView
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * A simple [Fragment] subclass that displays a calendar and allows date selection.
 * Use the [CalendarFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class CalendarFragment : Fragment(R.layout.fragment_calendar) {

    // Parameters for fragment instantiation (if needed)
    private var param1: String? = null
    private var param2: String? = null

    // UI elements declared with lateinit to be initialized in onViewCreated
    private lateinit var calendarView: CalendarView
    private lateinit var tvSelectedDate: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Retrieve arguments passed to the fragment, if any
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment using the fragment_calendar.xml
        return inflater.inflate(R.layout.fragment_calendar, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize UI elements by finding them from the inflated view
        calendarView = view.findViewById(R.id.calendarView)
        tvSelectedDate = view.findViewById(R.id.tvSelectedDate)

        // Set an OnDateChangeListener to update the TextView with the selected date
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            // Create a Calendar instance and set it to the selected date
            val calendar = Calendar.getInstance()
            calendar.set(year, month, dayOfMonth)

            // Format the date for display (e.g., "Monday, January 01, 2024")
            val dateFormat = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault())
            tvSelectedDate.text = "Selected Date: ${dateFormat.format(calendar.time)}"
        }

        // Set the initial selected date in the TextView to today's date when the fragment loads
        val today = Calendar.getInstance()
        val initialFormattedDate = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault()).format(today.time)
        tvSelectedDate.text = "Selected Date: $initialFormattedDate"
        // Also set the CalendarView to display today's date initially
        calendarView.date = today.timeInMillis
    }

    companion object {
        // Constant arguments used for fragment instantiation.
        // Declared here to avoid conflicts and make them accessible via the companion object.
        private const val ARG_PARAM1 = "param1"
        private const val ARG_PARAM2 = "param2"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment CalendarFragment.
         */
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            CalendarFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}
