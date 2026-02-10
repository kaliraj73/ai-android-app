package com.yourapp.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import com.yourapp.repository.AuthRepository

/**
 * Retrofit client singleton for API calls
 */
object RetrofitClient {

    // TODO: Replace with your actual Modal deployment URL
    // Format: https://your-username--ai-app-backend-fastapi-app.modal.run
    private const val BASE_URL = "https://kaliraj73--ai-app-backend-fastapi-app.modal.run/"


    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val original = chain.request()
            val token = AuthRepository.authToken.value
            val request = if (token.isNullOrBlank()) {
                original
            } else {
                original.newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .build()
            }
            chain.proceed(request)
        }
        .addInterceptor(loggingInterceptor)
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val modalApiService: ModalApiService by lazy {
        retrofit.create(ModalApiService::class.java)
    }

    /**
     * Update the base URL (useful for switching environments)
     */
    fun updateBaseUrl(newBaseUrl: String) {
        // Note: In production, you might want to recreate the Retrofit instance
        // This is a simplified version
    }
}
