package com.example.walletwise.network

data class ForgotPasswordRequest(
    val email: String
)

data class VerifyOtpRequest(
    val user_id: Int,
    val otp: String
)

data class ResetPasswordRequest(
    val reset_token: String,
    val new_password: String
)

data class RegisterRequest(
    val full_name: String,
    val email: String,
    val password: String
)


data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T?
)

data class ForgotPasswordData(
    val user_id: Int
)

data class VerifyOtpData(
    val reset_token: String
)

data class ResetPasswordData(
    val email: String
)

data class RegisterData(
    val user_id: Int
)