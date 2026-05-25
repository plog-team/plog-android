package com.example.plog.ui.exchange

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.plog.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import android.widget.PopupWindow

class MatchedFragment : Fragment() {

    private var isMine = true
    private var currentDay = 1

    private lateinit var tvUserName: TextView

    private lateinit var cvProfile: CardView
    private lateinit var tvDate: TextView
    private lateinit var tvWeather: TextView
    private lateinit var tvLocation: TextView
    private lateinit var tvTitleDiary: TextView
    private lateinit var tvBody: TextView
    private lateinit var btnEdit: MaterialButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(R.layout.fragment_matched, container, false)

        val partnerName =
            arguments?.getString("partnerName") ?: "사용자"

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

        cvProfile.setOnClickListener {
            if (!isMine) {
                showReportPopup(it)
            }
        }

        updateDiary(partnerName)

        typeTab.addOnTabSelectedListener(object :
            TabLayout.OnTabSelectedListener {

            override fun onTabSelected(tab: TabLayout.Tab) {
                isMine = tab.position == 0
                updateDiary(partnerName)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        dayTab.addOnTabSelectedListener(object :
            TabLayout.OnTabSelectedListener {

            override fun onTabSelected(tab: TabLayout.Tab) {
                currentDay = tab.position + 1
                updateDiary(partnerName)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        return view
    }

    // =========================
    // UPDATE DIARY
    // =========================
    private fun updateDiary(partnerName: String) {

        val params =
            cvProfile.layoutParams as ConstraintLayout.LayoutParams

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
    }

    // =========================
    // POPUP
    // =========================
    private fun showReportPopup(anchor: View) {

        val popupView = layoutInflater.inflate(
            R.layout.popup_report,
            anchor.parent as ViewGroup,
            false
        )

        val popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )

        val btnReportBlock =
            popupView.findViewById<TextView>(R.id.btnReportBlock)

        btnReportBlock.setOnClickListener {
            popupWindow.dismiss()
            showExitConfirmDialog()
        }

        popupWindow.showAsDropDown(anchor, 0, 10)
    }

    // =========================
    // 1단계
    // =========================
    private fun showExitConfirmDialog() {

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("신고 및 차단")
            .setMessage("신고 및 차단 시 교환일기가 즉시 종료됩니다.\n계속하시겠습니까?")
            .setNegativeButton("취소", null)
            .setPositiveButton("계속") { _, _ ->
                showReasonDialog()
            }
            .show()
    }

    // =========================
    // 2단계
    // =========================
    private fun showReasonDialog() {

        val reasons = arrayOf(
            "부적절한 내용",
            "욕설/비방",
            "스팸"
        )

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("신고 사유")
            .setItems(reasons) { _, which ->
                val reason = reasons[which]
                showFinalConfirmDialog(reason)
            }
            .show()
    }

    // =========================
    // 3단계
    // =========================
    private fun showFinalConfirmDialog(reason: String) {

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("신고 및 차단")
            .setMessage("사유: $reason\n\n계속 진행하시겠습니까?")
            .setNegativeButton("취소", null)
            .setPositiveButton("계속") { _, _ ->
                showExitCompleteDialog(reason)
            }
            .show()
    }

    // =========================
    // 종료 + 화면 이동
    // =========================
    private fun showExitCompleteDialog(reason: String) {

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("종료")
            .setMessage("교환일기가 종료되었습니다.\n사유: $reason")
            .setPositiveButton("확인") { _, _ ->
                findNavController().navigate(R.id.notMatchedFragment)
            }
            .show()
    }
}