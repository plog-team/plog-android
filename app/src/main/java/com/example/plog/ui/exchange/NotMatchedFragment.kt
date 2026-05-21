package com.example.plog.ui.exchange

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.plog.R
import com.google.android.material.button.MaterialButton

class NotMatchedFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(
            R.layout.fragment_not_matched,
            container,
            false
        )

        // 매칭하기 버튼 연결
        val btnStartMatch =
            view.findViewById<MaterialButton>(R.id.btn_start_match)

        // 버튼 클릭 시 매칭중 화면으로 이동
        btnStartMatch.setOnClickListener {

            findNavController().navigate(
                R.id.matchingFragment
            )

        }

        return view
    }
}