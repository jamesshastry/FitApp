package com.fitapp.stepcounter

import android.util.Log
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
    val temperature: Double = 1.8,  // Increased even more for maximum creativity
    val top_p: Double = 0.9,       // Add top_p for more randomness
    val frequency_penalty: Double = 0.5,  // Add frequency penalty to avoid repetition
    val presence_penalty: Double = 0.3    // Add presence penalty for more variety
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
    
    // Test function to verify randomness
    fun testRandomness(): String {
        val testResults = mutableListOf<String>()
        repeat(5) {
            val prompt = createCoachPrompt(5000)
            val personality = generateRandomSystemPrompt()
            testResults.add("Test ${it + 1}: Prompt=${prompt.take(30)}..., Personality=${personality.take(30)}...")
        }
        return testResults.joinToString("\n")
    }
    
    suspend fun getCoachFeedback(steps: Int): String {
        return withContext(Dispatchers.IO) {
            try {
                val prompt = createCoachPrompt(steps)
                val systemPrompt = generateRandomSystemPrompt()
                
                Log.d("OpenAI", "Steps: $steps, Prompt: $prompt")
                Log.d("OpenAI", "System Prompt: $systemPrompt")
                
                val request = OpenAIRequest(
                    messages = listOf(
                        Message(
                            role = "system",
                            content = systemPrompt
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
                    val result = openAIResponse.choices.firstOrNull()?.message?.content ?: "Great job on your steps today! Keep moving!"
                    Log.d("OpenAI", "API Response: $result")
                    result
                } else {
                    Log.e("OpenAI", "API Error: ${response.code} - ${response.message}")
                    Log.e("OpenAI", "Response body: $responseBody")
                    "Keep up the great work! Every step counts towards a healthier you! 💪"
                }
            } catch (e: Exception) {
                Log.e("OpenAI", "Exception occurred: ${e.message}", e)
                Log.e("OpenAI", "API Key present: ${BuildConfig.OPENAI_API_KEY.isNotEmpty()}")
                Log.e("OpenAI", "API Key starts with: ${BuildConfig.OPENAI_API_KEY.take(10)}...")
                "You're doing amazing! Keep stepping towards your goals! 🚶‍♂️✨"
            }
        }
    }
    
    private fun createCoachPrompt(steps: Int): String {
        return "I did $steps steps today. What feedback do you have?"
    }
    
    private fun generateRandomSystemPrompt(): String {
        val personalities = listOf(
            "You are an enthusiastic fitness coach. Give motivational feedback about step counts. Be encouraging and positive. Keep responses under 100 words.",
            "You are a supportive fitness mentor. Provide encouraging feedback about daily step progress. Be warm and motivating. Keep responses under 100 words.",
            "You are a fun fitness buddy. Give playful and encouraging feedback about step counts. Be lighthearted and supportive. Keep responses under 100 words.",
            "You are a determined fitness coach. Provide strong motivational feedback about step progress. Be passionate and inspiring. Keep responses under 100 words.",
            "You are a caring fitness friend. Give supportive feedback about step counts. Be empathetic and encouraging. Keep responses under 100 words.",
            "You are a creative fitness coach. Provide imaginative feedback about step progress. Use creative metaphors and inspiring language. Keep responses under 100 words.",
            "You are a knowledgeable fitness expert. Give informative feedback about step counts. Be encouraging with helpful insights. Keep responses under 100 words.",
            "You are an adventurous fitness guide. Provide exciting feedback about step progress. Use journey and exploration metaphors. Keep responses under 100 words."
        )
        
        val selectedPersonality = personalities.random()
        Log.d("OpenAI", "Selected personality: $selectedPersonality")
        return selectedPersonality
    }
}
