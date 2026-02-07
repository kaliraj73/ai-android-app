package com.yourapp.api

import com.yourapp.data.model.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Retrofit API service for Modal backend
 * 
 * Replace BASE_URL with your actual Modal deployment URL:
 * https://your-username--ai-app-backend-fastapi-app.modal.run
 */
interface ModalApiService {

    @GET("/health")
    suspend fun healthCheck(): Response<HealthResponse>

    @POST("/generate")
    suspend fun generateText(@Body request: GenerateRequest): Response<GenerateResponse>

    @POST("/summarize")
    suspend fun summarizeText(@Body request: SummarizeRequest): Response<SummarizeResponse>

    @POST("/sentiment")
    suspend fun analyzeSentiment(@Body request: SentimentRequest): Response<SentimentResponse>
}
