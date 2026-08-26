package com.example.walletwise.ui.onboarding

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.walletwise.R

/**
 * Renders a single onboarding page (illustration, title, description,
 * dot indicator and the Login / Create Account buttons).
 *
 * The Skip button and logo are NOT part of this fragment — they live in
 * [OnboardingActivity]'s layout so they stay fixed in place (top-right)
 * while the ViewPager2 content underneath swipes between pages.
 */
class OnboardingFragment : Fragment(
    R.layout.fragment_onboarding_page) {

    private var position: Int = 0
    private var totalPages: Int = 1
    private var imageRes: Int = 0
    private lateinit var title: String
    private lateinit var description: String

    /** Optional callback so the hosting Activity can react to button taps. */
    interface OnboardingActionListener {
        fun onLoginClicked()
        fun onCreateAccountClicked()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            position = it.getInt(ARG_POSITION)
            totalPages = it.getInt(ARG_TOTAL)
            imageRes = it.getInt(ARG_IMAGE)
            title = it.getString(ARG_TITLE).orEmpty()
            description = it.getString(ARG_DESC).orEmpty()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<ImageView>(R.id.imgIllustration).setImageResource(imageRes)
        view.findViewById<TextView>(R.id.tvTitle).text = title
        view.findViewById<TextView>(R.id.tvDescription).text = description

        updateDots(view)

        view.findViewById<Button>(R.id.btnLogin).setOnClickListener {
            (activity as? OnboardingActionListener)?.onLoginClicked()
        }
        view.findViewById<Button>(R.id.btnCreateAccount).setOnClickListener {
            (activity as? OnboardingActionListener)?.onCreateAccountClicked()
        }
    }

    /** Highlights the dot that matches [position]; supports any [totalPages] up to 3 dots in the layout. */
    private fun updateDots(view: View) {
        val dotIds = intArrayOf(R.id.dot1, R.id.dot2, R.id.dot3)
        dotIds.forEachIndexed { index, id ->
            val dot = view.findViewById<View>(id) ?: return@forEachIndexed
            val isActive = index == position
            dot.setBackgroundResource(if (isActive) R.drawable.dot_active else R.drawable.dot_inactive)

            val activeWidth = dpToPx(view, 18)
            val inactiveSize = dpToPx(view, 8)
            val params = dot.layoutParams
            params.width = if (isActive) activeWidth else inactiveSize
            params.height = inactiveSize
            dot.layoutParams = params
        }
    }

    private fun dpToPx(view: View, dp: Int): Int {
        val density = view.resources.displayMetrics.density
        return (dp * density).toInt()
    }

    companion object {
        private const val ARG_IMAGE = "arg_image"
        private const val ARG_TITLE = "arg_title"
        private const val ARG_DESC = "arg_desc"
        private const val ARG_POSITION = "arg_position"
        private const val ARG_TOTAL = "arg_total"

        fun newInstance(item: OnboardingItem, position: Int, totalPages: Int): OnboardingFragment {
            return OnboardingFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_IMAGE, item.imageRes)
                    putString(ARG_TITLE, item.title)
                    putString(ARG_DESC, item.description)
                    putInt(ARG_POSITION, position)
                    putInt(ARG_TOTAL, totalPages)
                }
            }
        }
    }
}
