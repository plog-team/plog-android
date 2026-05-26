package com.example.plog.ui.exchange

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.plog.R
import com.example.plog.MainActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import kotlin.random.Random

/**
 * 교환일기 신청 전 상대방의 프로필을 확인하고 매칭을 확정하는 프래그먼트
 */
class MatchConfirmFragment : Fragment(R.layout.fragment_match_confirm) {

    // ==========================================
    // 시연용 더미 데이터 세트
    // TODO: 추후 유저 성향 분석 서버 API 연동 필요
    // ==========================================
    private val partnerPool = listOf(
        listOf("user1", "#영화", "#드라마", "#집순이"),
        listOf("user2", "#카페", "#사진", "#여행"),
        listOf("user3", "#개발", "#독서", "#고양이"),
        listOf("user4", "#맛집", "#베이킹", "#디저트"),
        listOf("user5", "#음악", "#기타", "#감성")
    )

    private var currentPartnerIndex = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // UI 컴포넌트 바인딩
        val tvMessage = view.findViewById<TextView>(R.id.tvMatchMessage)
        val tvUserNickname = view.findViewById<TextView>(R.id.tvUserNickname)

        val chipTag1 = view.findViewById<Chip>(R.id.chipTag1)
        val chipTag2 = view.findViewById<Chip>(R.id.chipTag2)
        val chipTag3 = view.findViewById<Chip>(R.id.chipTag3)

        val layoutButtons = view.findViewById<LinearLayout>(R.id.layoutButtons)
        val btnAccept = view.findViewById<MaterialButton>(R.id.btnAccept)
        val btnReject = view.findViewById<MaterialButton>(R.id.btnReject)
        val btnCancel = view.findViewById<MaterialButton>(R.id.btnCancel)

        // 초기 가상 사용자 데이터 로드
        setNewPartner(tvUserNickname, chipTag1, chipTag2, chipTag3)

        // ==========================================
        // 버튼 이벤트 리스너 설정
        // ==========================================

        // 교환 신청 수락 ("예" 버튼 클릭)
        btnAccept.setOnClickListener {
            tvMessage.text = "상대방의 응답을 기다리는 중..."

            layoutButtons.visibility = View.GONE
            btnCancel.visibility = View.VISIBLE

            // 비동기 매칭 수락 시뮬레이션 (2초 대기)
            view.postDelayed({
                // 시스템 상단 노티피케이션 알림 발생
                (activity as? MainActivity)?.showNotification(
                    "매칭 성공!",
                    "${tvUserNickname.text}님이 신청을 수락했습니다."
                )

                // 알림 노출 후 최종 일기장 화면으로 이동 (1.5초 대기)
                view.postDelayed({
                    val bundle = Bundle().apply {
                        putString("partnerName", tvUserNickname.text.toString())
                    }
                    findNavController().navigate(
                        R.id.matchedFragment,
                        bundle
                    )
                }, 1500)

            }, 2000)
        }

        // 재탐색 ("다시 찾기" 버튼 클릭)
        btnReject.setOnClickListener {
            // 중복 방지 처리가 적용된 렌덤 인덱스 추출
            var nextIndex = currentPartnerIndex
            if (partnerPool.size > 1) {
                while (nextIndex == currentPartnerIndex) {
                    nextIndex = Random.nextInt(partnerPool.size)
                }
            } else {
                nextIndex = 0
            }
            currentPartnerIndex = nextIndex

            // 새로운 유저 프로필 갱신
            setNewPartner(tvUserNickname, chipTag1, chipTag2, chipTag3)
        }

        // 신청 취소 버튼 클릭
        btnCancel.setOnClickListener {
            tvMessage.text = "이 사용자에게 교환일기를 신청할까요?"

            layoutButtons.visibility = View.VISIBLE
            btnCancel.visibility = View.GONE
        }
    }

    // ==========================================
    // UI 데이터 바인딩 헬퍼 함수
    // ==========================================

    /**
     * 지정된 인덱스의 가상 유저 데이터(닉네임 및 태그)를 뷰에 매핑합니다.
     */
    private fun setNewPartner(tvNickname: TextView, chip1: Chip, chip2: Chip, chip3: Chip) {
        val partnerData = partnerPool[currentPartnerIndex]

        tvNickname.text = partnerData[0]
        chip1.text = partnerData[1]
        chip2.text = partnerData[2]
        chip3.text = partnerData[3]
    }
}