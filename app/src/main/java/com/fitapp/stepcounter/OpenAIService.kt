package com.fitapp.stepcounter

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

data class OpenAIRequest(
    val model: String = "gpt-3.5-turbo",
    val messages: List<Message>,
    val max_tokens: Int = 150,
    val temperature: Double = 0.7
)

data class Message(
    val role: String,
    val content: String
)

data class OpenAIResponse(
    val choices: List<Choice>
)

data class Choice(
    val message: Message
)

class OpenAIService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val gson = Gson()
    
    suspend fun getCoachFeedback(steps: Int): String {
        return withContext(Dispatchers.IO) {
            try {
                val prompt = createCoachPrompt(steps)
                val request = OpenAIRequest(
                    messages = listOf(
                        Message(
                            role = "system",
                            content = "You are an enthusiastic fitness coach. Provide motivational feedback based on step count. Keep responses under 100 words, encouraging and positive."
                        ),
                        Message(
                            role = "user",
                            content = prompt
                        )
                    )
                )
                
                val json = gson.toJson(request)
                val body = json.toRequestBody("application/json".toMediaType())
                
                val httpRequest = Request.Builder()
                    .url("https://api.openai.com/v1/chat/completions")
                    .addHeader("Authorization", "Bearer ${BuildConfig.OPENAI_API_KEY}")
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build()
                
                val response = client.newCall(httpRequest).execute()
                val responseBody = response.body?.string()
                
                if (response.isSuccessful && responseBody != null) {
                    val openAIResponse = gson.fromJson(responseBody, OpenAIResponse::class.java)
                    openAIResponse.choices.firstOrNull()?.message?.content ?: "Great job on your steps today! Keep moving!"
                } else {
                    "Keep up the great work! Every step counts towards a healthier you! 💪"
                }
            } catch (e: Exception) {
                "You're doing amazing! Keep stepping towards your goals! 🚶‍♂️✨"
            }
        }
    }
    
    private fun createCoachPrompt(steps: Int): String {
        return when {
            steps >= 10000 -> "The user has taken $steps steps today. They've reached the recommended daily goal!"
            steps >= 8000 -> "The user has taken $steps steps today. They're very close to their daily goal!"
            steps >= 5000 -> "The user has taken $steps steps today. They're making good progress!"
            steps >= 2000 -> "The user has taken $steps steps today. They're getting started on their fitness journey!"
            else -> "The user has taken $steps steps today. They're just beginning their day!"
        }
    }
}
