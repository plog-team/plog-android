package com.example.plog.ui.exchange

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.plog.R
import com.google.android.material.button.MaterialButton
import com.example.plog.MainActivity

class MatchConfirmFragment : Fragment(R.layout.fragment_match_confirm) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvMessage = view.findViewById<TextView>(R.id.tvMatchMessage)

        val tvUserNickname =
            view.findViewById<TextView>(R.id.tvUserNickname)

        val btnAccept = view.findViewById<MaterialButton>(R.id.btnAccept)
        val btnReject = view.findViewById<MaterialButton>(R.id.btnReject)
        val btnCancel = view.findViewById<MaterialButton>(R.id.btnCancel)

        btnAccept.setOnClickListener {

            tvMessage.text = "상대방의 응답을 기다리는 중..."

            btnAccept.visibility = View.GONE
            btnReject.visibility = View.GONE
            btnCancel.visibility = View.VISIBLE

            view.postDelayed({

                (activity as MainActivity).showNotification(
                    "매칭 성공!",
                    "상대방이 신청을 수락했습니다."
                )

                // 알림 보여준 뒤 이동
                view.postDelayed({

                    val bundle = Bundle()

                    bundle.putString(
                        "partnerName",
                        tvUserNickname.text.toString()
                    )

                    findNavController().navigate(
                        R.id.matchedFragment,
                        bundle
                    )

                }, 1500)

            }, 2000)
        }

        btnReject.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnCancel.setOnClickListener {

            tvMessage.text = "이 사용자에게 교환일기를 신청할까요?"

            btnAccept.visibility = View.VISIBLE
            btnReject.visibility = View.VISIBLE
            btnCancel.visibility = View.GONE
        }
    }
}