package com.alex.materialdiary.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.alex.materialdiary.R
import com.alex.materialdiary.databinding.FragmentFatalErrorBinding


/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FatalErrorFragment : Fragment() {
    private var _binding: FragmentFatalErrorBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentFatalErrorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toHome.setOnClickListener {
            findNavController().navigate(R.id.to_diary)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
