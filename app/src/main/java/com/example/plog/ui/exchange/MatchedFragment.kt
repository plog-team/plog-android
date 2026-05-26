package com.example.plog.ui.exchange

import android.content.Context
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.plog.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout

/**
 * 매칭 완료 후 교환일기를 표시하고 세션을 관리하는 프래그먼트
 */
class MatchedFragment : Fragment() {

    // 상태 관리 변수
    private var isMine = true
    private var currentDay = 1

    // UI 컴포넌트
    private lateinit var tvUserName: TextView
    private lateinit var cvProfile: CardView
    private lateinit var tvDate: TextView
    private lateinit var tvWeather: TextView
    private lateinit var tvLocation: TextView
    private lateinit var tvTitleDiary: TextView
    private lateinit var tvBody: TextView
    private lateinit var btnEdit: MaterialButton
    private lateinit var cvDiaryCard: View

    // 7일 세션 관리 타임아웃 설정
    private var sessionTimer: CountDownTimer? = null
    private val SEVEN_DAYS_IN_MS: Long = 7 * 24 * 60 * 60 * 1000L

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_matched, container, false)
        val partnerName = arguments?.getString("partnerName") ?: "사용자"

        // 뷰 바인딩 및 초기화
        val typeTab = view.findViewById<TabLayout>(R.id.typeTab)
        val dayTab = view.findViewById<TabLayout>(R.id.dayTab)

        cvProfile = view.findViewById(R.id.cvProfile)
        tvDate = view.findViewById(R.id.tvDate)
        tvWeather = view.findViewById(R.id.tvWeather)
        tvLocation = view.findViewById(R.id.tvLocation)
        tvTitleDiary = view.findViewById(R.id.tvTitleDiary)
        tvBody = view.findViewById(R.id.tvBody)
        tvUserName = view.findViewById(R.id.tvUserName)
        btnEdit = view.findViewById(R.id.btn_start_match)

        // 일기 레이아웃 컨테이너 참조 (상위 부모 뷰)
        cvDiaryCard = tvTitleDiary.parent as View

        // 리스너 설정
        cvProfile.setOnClickListener {
            if (!isMine) { showReportPopup(it) }
        }

        // 초기 데이터 로드
        updateDiary(partnerName)

        // 작성자 탭(나/상대방) 전환 리스너
        typeTab.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                isMine = tab.position == 0
                animateSmoothTransition {
                    updateDiary(partnerName)
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        // 날짜 탭(1일차~7일차) 전환 리스너
        dayTab.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                currentDay = tab.position + 1
                animateSmoothTransition {
                    updateDiary(partnerName)
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        return view
    }

    override fun onResume() {
        super.onResume()
        startSessionValidation()
    }

    override fun onPause() {
        super.onPause()
        sessionTimer?.cancel()
    }

    // ==========================================
    // 세션 유효성 검증 로직
    // ==========================================

    /**
     * SharedPreferences에 저장된 시작 시간을 기준으로 7일 세션 만료 여부를 검증합니다.
     */
    private fun startSessionValidation() {
        val sharedPref = requireActivity().getSharedPreferences("ExchangeSessionPref", Context.MODE_PRIVATE)
        var startTime = sharedPref.getLong("start_time", 0L)

        if (startTime == 0L) {
            startTime = System.currentTimeMillis()
            sharedPref.edit().putLong("start_time", startTime).apply()
        }

        val currentTime = System.currentTimeMillis()
        val expireTime = startTime + SEVEN_DAYS_IN_MS
        val remainingTime = expireTime - currentTime

        if (remainingTime <= 0) {
            showSessionEndDialog()
        } else {
            sessionTimer?.cancel()
            sessionTimer = object : CountDownTimer(remainingTime, 1000) {
                override fun onTick(millisUntilFinished: Long) {}
                override fun onFinish() { showSessionEndDialog() }
            }.start()
        }
    }

    /**
     * 7일 만료 시 안내 다이얼로그를 표시합니다.
     */
    private fun showSessionEndDialog() {
        if (!isAdded || isRemoving) return
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("교환일기 기간 만료")
            .setMessage("약속된 7일간의 교환일기 기간이 종료되었습니다.\n\n이어서 일기를 계속 쓰시겠습니까?\n(나와 상대방이 모두 연장을 선택해야 기간이 7일 연장됩니다.)")
            .setCancelable(false)
            .setPositiveButton("기간 연장") { _, _ -> extendSession() }
            .setNegativeButton("교환 종료") { _, _ -> terminateSession() }
            .show()
    }

    /**
     * 세션 기간을 현재 시간으로부터 7일 연장합니다.
     */
    private fun extendSession() {
        val sharedPref = requireActivity().getSharedPreferences("ExchangeSessionPref", Context.MODE_PRIVATE)
        sharedPref.edit().putLong("start_time", System.currentTimeMillis()).apply()
        Toast.makeText(requireContext(), "교환일기 기간이 7일 연장되었습니다.", Toast.LENGTH_SHORT).show()
        startSessionValidation()
    }

    /**
     * 현재 교환 세션을 안전하게 종료하고 초기 화면으로 이동합니다.
     */
    private fun terminateSession() {
        val sharedPref = requireActivity().getSharedPreferences("ExchangeSessionPref", Context.MODE_PRIVATE)
        sharedPref.edit().remove("start_time").apply()
        Toast.makeText(requireContext(), "교환일기가 완전히 종료되었습니다.", Toast.LENGTH_SHORT).show()
        findNavController().navigate(R.id.notMatchedFragment)
    }

    // ==========================================
    // 애니메이션 및 UI 업데이트
    // ==========================================

    /**
     * 탭 전환 시 부드러운 하드웨어 가속 교차 전환 애니메이션을 수행합니다.
     */
    private fun animateSmoothTransition(onContentUpdate: () -> Unit) {
        // 1단계: 카드 축소 및 투명도 감소 효과 처리
        cvDiaryCard.animate()
            .alpha(0.3f)
            .scaleX(0.98f)
            .scaleY(0.98f)
            .translationY(10f)
            .setDuration(100)
            .withEndAction {
                // 2단계: 뷰 데이터 업데이트 수행
                onContentUpdate()

                // 3단계: 원상복귀 및 부드러운 가속도 이징 적용
                cvDiaryCard.animate()
                    .alpha(1.0f)
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .translationY(0f)
                    .setDuration(180)
                    .setInterpolator(android.view.animation.DecelerateInterpolator())
                    .start()
            }
            .start()
    }

    /**
     * 현재 상태(작성자, 날짜 등)에 따라 일기 데이터를 동적으로 맵핑합니다.
     */
    private fun updateDiary(partnerName: String) {
        val params = cvProfile.layoutParams as ConstraintLayout.LayoutParams
        val writer = if (isMine) "나" else partnerName

        // 작성자 상태에 따른 정렬 레이아웃 파라미터 갱신
        if (isMine) {
            params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            params.endToEnd = ConstraintLayout.LayoutParams.UNSET
            tvUserName.visibility = View.GONE
            btnEdit.visibility = View.VISIBLE
        } else {
            params.startToStart = ConstraintLayout.LayoutParams.UNSET
            params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            tvUserName.visibility = View.VISIBLE
            tvUserName.text = partnerName
            btnEdit.visibility = View.GONE
        }
        cvProfile.layoutParams = params

        // 텍스트 데이터 갱신
        tvDate.text = "2026년 05월 ${20 + currentDay}일"
        tvWeather.text = if (currentDay % 2 == 0) "☀️ 맑음" else "☁️ 흐림"
        tvLocation.text = if (isMine) "서울시 강남구" else "경기도 성남시"
        tvTitleDiary.text = "${currentDay}일차 - ${writer}의 일기 제목"
        tvBody.text = "여기는 ${currentDay}일차에 ${writer}가 작성한 일기 내용이 들어가는 공간입니다. 탭과 날짜를 바꾸면 모션과 함께 이 내용이 실시간으로 새로고침됩니다."
    }

    // ==========================================
    // 팝업 및 유저 신고/차단 시스템
    // ==========================================

    /**
     * 상대방 프로필 클릭 시 우측 상단 드롭다운 형태의 신고 팝업 창을 노출합니다.
     */
    private fun showReportPopup(anchor: View) {
        val popupView = layoutInflater.inflate(R.layout.popup_report, anchor.parent as ViewGroup, false)
        val popupWindow = PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true)
        val btnReportBlock = popupView.findViewById<TextView>(R.id.btnReportBlock)

        btnReportBlock.setOnClickListener {
            popupWindow.dismiss()
            showExitConfirmDialog()
        }
        popupWindow.showAsDropDown(anchor, 0, 10)
    }

    private fun showExitConfirmDialog() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("신고 및 차단")
            .setMessage("신고 및 차단 시 교환일기가 즉시 종료됩니다.\n계속하시겠습니까?")
            .setNegativeButton("취소", null)
            .setPositiveButton("계속") { _, _ -> showReasonDialog() }
            .show()
    }

    private fun showReasonDialog() {
        val reasons = arrayOf("부적절한 내용", "욕설/비방", "스팸")
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("신고 사유")
            .setItems(reasons) { _, which -> showFinalConfirmDialog(reasons[which]) }
            .show()
    }

    private fun showFinalConfirmDialog(reason: String) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("신고 및 차단")
            .setMessage("사유: $reason\n\n계속 진행하시겠습니까?")
            .setNegativeButton("취소", null)
            .setPositiveButton("계속") { _, _ -> showExitCompleteDialog(reason) }
            .show()
    }

    /**
     * 세션을 완전히 소멸시키고 탈퇴/차단 처리를 확정하는 다이얼로그
     */
    private fun showExitCompleteDialog(reason: String) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("종료")
            .setMessage("교환일기가 종료되었습니다.\n사유: $reason")
            .setPositiveButton("확인") { _, _ ->
                val sharedPref = requireActivity().getSharedPreferences("ExchangeSessionPref", Context.MODE_PRIVATE)
                sharedPref.edit().remove("start_time").apply()
                findNavController().navigate(R.id.notMatchedFragment)
            }
            .show()
    }
}