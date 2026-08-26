package com.example.walletwise.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {

    @POST("api/register.php")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<ApiResponse<RegisterData>>

    @POST("api/forgot_password.php")
    suspend fun forgotPassword(
        @Body request: ForgotPasswordRequest
    ): Response<ApiResponse<ForgotPasswordData>>

    @POST("api/verify_otp.php")
    suspend fun verifyOtp(
        @Body request: VerifyOtpRequest
    ): Response<ApiResponse<VerifyOtpData>>

    @POST("api/reset_password.php")
    suspend fun resetPassword(
        @Body request: ResetPasswordRequest
    ): Response<ApiResponse<ResetPasswordData>>

}