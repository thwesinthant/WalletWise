package com.example.walletwise.ui.onboarding

import androidx.annotation.DrawableRes

/**
 * Simple model for one onboarding screen.
 *
 * @param imageRes drawable resource id for the illustration
 * @param title headline shown in the bottom card
 * @param description supporting copy shown under the title
 */
data class OnboardingItem(
    @DrawableRes val imageRes: Int,
    val title: String,
    val description: String
)
