package com.example.plog.ui.exchange

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.plog.R

class MatchingFragment : Fragment(R.layout.fragment_matching) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.postDelayed({

            findNavController().navigate(
                R.id.matchConfirmFragment
            )

        }, 2000)
    }
}