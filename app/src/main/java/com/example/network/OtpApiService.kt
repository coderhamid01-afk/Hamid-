package com.example.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

@JsonClass(generateAdapter = true)
data class SendOtpRequest(
    @Json(name = "email") val email: String,
    @Json(name = "purpose") val purpose: String = "signup",
    @Json(name = "otp") val otp: String
)

@JsonClass(generateAdapter = true)
data class SendOtpResponse(
    @Json(name = "message") val message: String? = null,
    @Json(name = "success") val success: Boolean? = null
)

interface OtpApiService {
    @Headers("Content-Type: application/json")
    @POST("api/send-otp")
    suspend fun sendOtp(@Body request: SendOtpRequest): Response<SendOtpResponse>

    companion object {
        private const val BASE_URL = "https://plenxo-back.netlify.app/"

        fun create(): OtpApiService {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(NetworkModule.okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(NetworkModule.moshi))
                .build()
                .create(OtpApiService::class.java)
        }
    }
}
