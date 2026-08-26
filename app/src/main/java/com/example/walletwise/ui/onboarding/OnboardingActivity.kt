package com.example.walletwise.ui.onboarding

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.walletwise.R

class OnboardingActivity : AppCompatActivity(), OnboardingFragment.OnboardingActionListener {

    private lateinit var viewPager: ViewPager2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(
            R.layout.activity_onboarding)

        val pages = listOf(
            OnboardingItem(
                imageRes = R.drawable.illustration_fast_transfer,
                title = getString(R.string.title_page_1),
                description = getString(R.string.desc_page_1)
            ),
            OnboardingItem(
                imageRes = R.drawable.illustration_save,
                title = getString(R.string.title_page_2),
                description = getString(R.string.desc_page_2)
            ),
            OnboardingItem(
                imageRes = R.drawable.illustration_change_money,
                title = getString(R.string.title_page_3),
                description = getString(R.string.desc_page_3)
            )
        )

        viewPager = findViewById(R.id.viewPagerOnboarding)
        viewPager.adapter = OnboardingAdapter(this, pages)

        findViewById<android.widget.TextView>(R.id.btnSkip).setOnClickListener {
            skipOnboarding()
        }
    }

    /**
     * TODO: replace with real navigation, e.g.
     *   startActivity(Intent(this, LoginActivity::class.java))
     *   finish()
     */
    private fun skipOnboarding() {
        Toast.makeText(this, "Skip tapped — navigate to your next screen here", Toast.LENGTH_SHORT).show()
    }

    override fun onLoginClicked() {
        // TODO: replace with real navigation to your Login screen
        Toast.makeText(this, "Login tapped", Toast.LENGTH_SHORT).show()
    }

    override fun onCreateAccountClicked() {
        // TODO: replace with real navigation to your Sign Up screen
        Toast.makeText(this, "Create Account tapped", Toast.LENGTH_SHORT).show()
    }
}
