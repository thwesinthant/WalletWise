package com.example.walletwise.network

data class ChangePasswordRequest(
    val email: String,
    val current_password: String,
    val new_password: String
)

