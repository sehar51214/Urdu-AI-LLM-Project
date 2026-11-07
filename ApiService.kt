package com.example.urduai

import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("chat")
    suspend fun chat(@Body request: ChatRequest): ChatResponse
}

// Models
data class ChatRequest(
    val message: String,
    val history: List<HistoryMessage> = emptyList()
)

data class HistoryMessage(
    val role: String,
    val content: String
)
data class ChatResponse(val response: String)