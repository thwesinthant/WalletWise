package com.example.walletwise.notification

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.graphics.drawable.toDrawable
import com.example.walletwise.R
import com.example.walletwise.entity.Notification
import kotlin.math.abs

class NotificationPopupManager(
    private val activity: Activity
) {

    private var popupWindow: PopupWindow? = null

    fun show(notification: Notification) {

        // Prevent multiple popup cards from stacking.
        dismiss()

        // Use the Activity content view as the root so that
        // layout parameters can be resolved correctly.
        val parent =
            activity.findViewById<ViewGroup>(
                android.R.id.content
            )

        val view =
            LayoutInflater.from(activity).inflate(
                R.layout.item_notification_popup,
                parent,
                false
            )

        val imgIcon =
            view.findViewById<ImageView>(
                R.id.imgPopupIcon
            )

        val tvTitle =
            view.findViewById<TextView>(
                R.id.tvPopupTitle
            )

        val tvMessage =
            view.findViewById<TextView>(
                R.id.tvPopupMessage
            )

        // ---------------------------------------------------------
        // NOTIFICATION CONTENT
        // ---------------------------------------------------------
        tvTitle.text =
            notification.title

        tvMessage.text =
            notification.message

        imgIcon.setImageResource(
            getNotificationIcon(
                notification.type
            )
        )

// ---------------------------------------------------------
// CLICK POPUP → NOTIFICATION PAGE
// ---------------------------------------------------------

        view.setOnClickListener {

            val intent =
                Intent(
                    activity,
                    NotificationActivity::class.java
                )

            intent.putExtra(
                "USER_ID",
                notification.userId
            )

            activity.startActivity(
                intent
            )

            dismiss()
        }

        // ---------------------------------------------------------
        // POPUP WINDOW
        // ---------------------------------------------------------

        val window =
            PopupWindow(
                view,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                false
            )

        // Transparent popup background.
        // The CardView inside the layout provides the visible card.
        window.setBackgroundDrawable(
            Color.TRANSPARENT.toDrawable()
        )

        window.isOutsideTouchable = false
        window.isFocusable = false
        window.elevation = 10f

        popupWindow = window

        // ---------------------------------------------------------
        // INITIAL ANIMATION STATE
        // ---------------------------------------------------------

        view.translationY = -300f
        view.alpha = 0f

        // ---------------------------------------------------------
        // SHOW POPUP
        // ---------------------------------------------------------

        window.showAtLocation(
            activity.window.decorView,
            Gravity.TOP or Gravity.CENTER_HORIZONTAL,
            0,
            getTopMargin()
        )

        // ---------------------------------------------------------
        // SLIDE + FADE IN
        // ---------------------------------------------------------

        view.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(280)
            .setInterpolator(
                AccelerateDecelerateInterpolator()
            )
            .start()

        // ---------------------------------------------------------
        // SWIPE TO DISMISS
        // ---------------------------------------------------------

        setupSwipeToDismiss(view)

        // ---------------------------------------------------------
        // AUTO DISMISS AFTER 4 SECONDS
        // ---------------------------------------------------------

        view.postDelayed({

            if (popupWindow == window) {
                dismiss()
            }

        }, 4000)
    }

    // =============================================================
    // SWIPE TO DISMISS
    // =============================================================

    private fun setupSwipeToDismiss(
        view: View
    ) {

        var downX = 0f
        var isSwiping = false

        view.setOnTouchListener { v, event ->

            when (event.actionMasked) {

                MotionEvent.ACTION_DOWN -> {

                    downX = event.rawX
                    isSwiping = false

                    true
                }

                MotionEvent.ACTION_MOVE -> {

                    val differenceX =
                        event.rawX - downX

                    if (
                        abs(differenceX) >
                        v.width * 0.05f
                    ) {
                        isSwiping = true
                    }

                    v.translationX =
                        differenceX

                    true
                }

                MotionEvent.ACTION_UP -> {

                    val differenceX =
                        event.rawX - downX

                    if (
                        abs(differenceX) >
                        v.width * 0.30f
                    ) {

                        // ---------------------------------------------
                        // SWIPE → DISMISS
                        // ---------------------------------------------

                        val direction =
                            if (differenceX > 0) {
                                1
                            } else {
                                -1
                            }

                        v.animate()
                            .translationX(
                                direction *
                                        v.width.toFloat()
                            )
                            .alpha(0f)
                            .setDuration(200)
                            .withEndAction {
                                dismiss()
                            }
                            .start()

                    } else {

                        // ---------------------------------------------
                        // NOT A SWIPE
                        // ---------------------------------------------

                        v.animate()
                            .translationX(0f)
                            .setDuration(150)
                            .start()

                        // ---------------------------------------------
                        // TAP
                        // ---------------------------------------------

                        if (!isSwiping) {
                            v.performClick()
                        }
                    }

                    true
                }

                MotionEvent.ACTION_CANCEL -> {

                    v.animate()
                        .translationX(0f)
                        .setDuration(150)
                        .start()

                    true
                }

                else -> false
            }
        }
    }

    // =============================================================
    // TOP MARGIN
    // =============================================================

    private fun getTopMargin(): Int {

        return (
                activity.resources.displayMetrics.density *
                        52
                ).toInt()
    }

    // =============================================================
    // NOTIFICATION ICON
    // =============================================================

    private fun getNotificationIcon(
        type: String
    ): Int {

        return when (type.uppercase()) {

            "INCOME" ->
                R.drawable.ic_notification_income

            "EXPENSE" ->
                R.drawable.ic_notification_expense

            "TRANSACTION" ->
                R.drawable.ic_notification_transaction

            "TRANSFER" ->
                R.drawable.ic_notification_transaction

            "BUDGET_ALERT",
            "BUDGET_EXCEEDED",
            "CATEGORY_BUDGET_EXCEEDED" ->
                R.drawable.ic_notification_budget

            "BILL",
            "BILL_DUE" ->
                R.drawable.ic_notification_bill

            "GOAL",
            "GOAL_REMINDER",
            "GOAL_COMPLETED" ->
                R.drawable.ic_notification_goal

            "SUMMARY" ->
                R.drawable.ic_notification_summary

            else ->
                R.drawable.ic_notification_default
        }
    }

    // =============================================================
    // DISMISS
    // =============================================================

    fun dismiss() {

        popupWindow?.dismiss()

        popupWindow = null
    }
}