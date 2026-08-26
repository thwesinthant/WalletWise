package com.example.walletwise.ui.onboarding

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

/**
 * Supplies one [OnboardingFragment] per [OnboardingItem] to the ViewPager2
 * in [OnboardingActivity].
 */
class OnboardingAdapter(
    fragmentActivity: FragmentActivity,
    private val items: List<OnboardingItem>
) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = items.size

    override fun createFragment(position: Int): Fragment {
        return OnboardingFragment.newInstance(items[position], position, items.size)
    }
}
