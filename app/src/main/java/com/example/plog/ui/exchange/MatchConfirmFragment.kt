package com.example.plog.ui.exchange

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.plog.R

class MatchConfirmFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(
            R.layout.fragment_match_confirm,
            container,
            false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 버튼 연결
        val btnAccept = view.findViewById<View>(R.id.btnAccept)
        val btnReject = view.findViewById<View>(R.id.btnReject)

        // "예" 버튼 → 매칭 완료 화면
        btnAccept.setOnClickListener {
            findNavController().navigate(R.id.matchedFragment)
        }

        // "다시 찾기" 버튼 → 다시 매칭중 화면
        btnReject.setOnClickListener {
            findNavController().navigate(R.id.matchingFragment)
        }
    }
}