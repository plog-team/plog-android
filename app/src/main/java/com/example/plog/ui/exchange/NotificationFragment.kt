package com.example.plog.ui.exchange

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.example.plog.R
import com.google.android.material.button.MaterialButton

class NotificationFragment : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        return inflater.inflate(
            R.layout.fragment_notification,
            container,
            false
        )
    }

    override fun onStart() {
        super.onStart()

        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        dialog?.window?.setBackgroundDrawableResource(
            android.R.color.transparent
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvTitle =
            view.findViewById<TextView>(R.id.tvNotificationTitle)

        val tvMessage =
            view.findViewById<TextView>(R.id.tvNotificationMessage)

        val btnClose =
            view.findViewById<MaterialButton>(R.id.btnCloseNotification)

        val title = arguments?.getString("title")
        val message = arguments?.getString("message")

        tvTitle.text = title
        tvMessage.text = message

        btnClose.setOnClickListener {
            dismiss()
        }
    }
}