package com.folioreader.ui.fragment

import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.folioreader.R

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val BG_COLOR_VALUE = "bg_color_value"
private const val TEXT_COLOR_VALUE = "text_color_value"

/**
 * A simple [Fragment] subclass.
 * Use the [DummyFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class DummyFragment : Fragment() {
    private var bgColor: Int = 0
    private var textColor: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        arguments?.let {
            bgColor = it.getInt(BG_COLOR_VALUE)
            textColor = it.getInt(TEXT_COLOR_VALUE)
        }

        val fragmentView = inflater.inflate(R.layout.fragment_dummy, container, false)
        fragmentView.setBackgroundColor(resources.getColor(bgColor))
        val textView = fragmentView.findViewById<TextView>(R.id.dummyTextView)
        textView.setTextColor(resources.getColor(textColor))
        return fragmentView
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment DummyFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(bgColor: Int, textColor: Int) =
            DummyFragment().apply {
                arguments = Bundle().apply {
                    putInt(BG_COLOR_VALUE, bgColor)
                    putInt(TEXT_COLOR_VALUE, textColor)
                }
            }
    }
}