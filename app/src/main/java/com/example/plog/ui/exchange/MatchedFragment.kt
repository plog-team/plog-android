package com.example.plog.ui.exchange

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import com.example.plog.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout

class MatchedFragment : Fragment() {

    private var isMine = true
    private var currentDay = 1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(R.layout.fragment_matched, container, false)

        val typeTab = view.findViewById<TabLayout>(R.id.typeTab)
        val dayTab = view.findViewById<TabLayout>(R.id.dayTab)

        val cvProfile = view.findViewById<CardView>(R.id.cvProfile)

        val tvDate = view.findViewById<TextView>(R.id.tvDate)
        val tvWeather = view.findViewById<TextView>(R.id.tvWeather)
        val tvLocation = view.findViewById<TextView>(R.id.tvLocation)
        val tvTitleDiary = view.findViewById<TextView>(R.id.tvTitleDiary)
        val tvBody = view.findViewById<TextView>(R.id.tvBody)

        val btnEdit =
            view.findViewById<MaterialButton>(R.id.btn_start_match)

        // =========================
        // 일기 업데이트 함수
        // =========================

        fun updateDiary() {

            val params =
                cvProfile.layoutParams as ConstraintLayout.LayoutParams

            if (isMine) {

                // =========================
                // 내 일기
                // =========================

                params.startToStart =
                    ConstraintLayout.LayoutParams.PARENT_ID

                params.endToEnd =
                    ConstraintLayout.LayoutParams.UNSET

                btnEdit.visibility = View.VISIBLE

                when (currentDay) {

                    1 -> {
                        tvDate.text = "2026.05.18"
                        tvWeather.text = "맑음"
                        tvLocation.text = "서울 성수동"
                        tvTitleDiary.text = "Day1 내 일기"
                        tvBody.text = "친구들과 카페를 갔다."
                    }

                    2 -> {
                        tvDate.text = "2026.05.19"
                        tvWeather.text = "흐림"
                        tvLocation.text = "강남"
                        tvTitleDiary.text = "Day2 내 일기"
                        tvBody.text = "오늘은 공부를 했다."
                    }

                    3 -> {
                        tvDate.text = "2026.05.20"
                        tvWeather.text = "비"
                        tvLocation.text = "홍대"
                        tvTitleDiary.text = "Day3 내 일기"
                        tvBody.text = "비 오는 거리를 걸었다."
                    }

                    else -> {
                        tvDate.text = "2026.05.${17 + currentDay}"
                        tvWeather.text = "맑음"
                        tvLocation.text = "서울"
                        tvTitleDiary.text = "Day$currentDay 내 일기"
                        tvBody.text = "내 일기 내용"
                    }
                }

            } else {

                // =========================
                // 상대 일기
                // =========================

                params.startToStart =
                    ConstraintLayout.LayoutParams.UNSET

                params.endToEnd =
                    ConstraintLayout.LayoutParams.PARENT_ID

                btnEdit.visibility = View.GONE

                when (currentDay) {

                    1 -> {
                        tvDate.text = "2026.05.18"
                        tvWeather.text = "맑음"
                        tvLocation.text = "인천 송도"
                        tvTitleDiary.text = "Day1 상대 일기"
                        tvBody.text = "오늘은 산책을 했다."
                    }

                    2 -> {
                        tvDate.text = "2026.05.19"
                        tvWeather.text = "비"
                        tvLocation.text = "부산"
                        tvTitleDiary.text = "Day2 상대 일기"
                        tvBody.text = "집에서 영화를 봤다."
                    }

                    3 -> {
                        tvDate.text = "2026.05.20"
                        tvWeather.text = "흐림"
                        tvLocation.text = "대전"
                        tvTitleDiary.text = "Day3 상대 일기"
                        tvBody.text = "맛집을 다녀왔다."
                    }

                    else -> {
                        tvDate.text = "2026.05.${17 + currentDay}"
                        tvWeather.text = "흐림"
                        tvLocation.text = "인천"
                        tvTitleDiary.text = "Day$currentDay 상대 일기"
                        tvBody.text = "상대 일기 내용"
                    }
                }
            }

            cvProfile.layoutParams = params
        }

        // =========================
        // 초기 화면
        // =========================

        updateDiary()

        // =========================
        // 내 일기 / 상대 일기 탭
        // =========================

        typeTab.addOnTabSelectedListener(object :
            TabLayout.OnTabSelectedListener {

            override fun onTabSelected(tab: TabLayout.Tab) {

                isMine = tab.position == 0

                updateDiary()
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}

            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        // =========================
        // Day 탭
        // =========================

        dayTab.addOnTabSelectedListener(object :
            TabLayout.OnTabSelectedListener {

            override fun onTabSelected(tab: TabLayout.Tab) {

                currentDay = tab.position + 1

                updateDiary()
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}

            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        return view
    }
}