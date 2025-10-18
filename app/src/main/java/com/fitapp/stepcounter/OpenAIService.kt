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
    val temperature: Double = 1.3  // Increased for more creativity
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
                    "Keep up the great work! Every step counts towards a healthier you! 💪"
                }
            } catch (e: Exception) {
                "You're doing amazing! Keep stepping towards your goals! 🚶‍♂️✨"
            }
        }
    }
    
    private fun createCoachPrompt(steps: Int): String {
        val timestamp = System.currentTimeMillis()
        val randomPrompts = when {
            steps >= 10000 -> listOf(
                "I've crushed $steps steps today! I'm a walking machine! (Time: $timestamp)",
                "Wow! $steps steps! I'm basically a superhero now! (Time: $timestamp)",
                "Incredible! $steps steps today! I'm unstoppable! (Time: $timestamp)",
                "Amazing! $steps steps! I've conquered the day! (Time: $timestamp)",
                "Fantastic! $steps steps! I'm a fitness legend! (Time: $timestamp)"
            )
            steps >= 8000 -> listOf(
                "I'm at $steps steps! So close to greatness! (Time: $timestamp)",
                "Awesome! $steps steps! I'm almost there! (Time: $timestamp)",
                "Great progress! $steps steps! Just a bit more! (Time: $timestamp)",
                "Excellent! $steps steps! I'm on fire! (Time: $timestamp)",
                "Outstanding! $steps steps! Almost at the finish line! (Time: $timestamp)"
            )
            steps >= 5000 -> listOf(
                "I've got $steps steps! I'm building momentum! (Time: $timestamp)",
                "Nice work! $steps steps! I'm getting stronger! (Time: $timestamp)",
                "Good progress! $steps steps! I'm in the zone! (Time: $timestamp)",
                "Solid effort! $steps steps! I'm moving forward! (Time: $timestamp)",
                "Great job! $steps steps! I'm on the right track! (Time: $timestamp)"
            )
            steps >= 2000 -> listOf(
                "I'm at $steps steps! Every journey starts with a single step! (Time: $timestamp)",
                "Good start! $steps steps! I'm warming up! (Time: $timestamp)",
                "Nice beginning! $steps steps! I'm getting started! (Time: $timestamp)",
                "Great foundation! $steps steps! I'm building up! (Time: $timestamp)",
                "Excellent start! $steps steps! I'm on my way! (Time: $timestamp)"
            )
            else -> listOf(
                "I'm at $steps steps! Time to get moving! (Time: $timestamp)",
                "Starting with $steps steps! Let's build momentum! (Time: $timestamp)",
                "Early in the day with $steps steps! Ready to conquer! (Time: $timestamp)",
                "Beginning with $steps steps! Time to step up! (Time: $timestamp)",
                "Fresh start with $steps steps! Let's make it count! (Time: $timestamp)"
            )
        }
        
        val selectedPrompt = randomPrompts.random()
        Log.d("OpenAI", "Selected prompt: $selectedPrompt")
        return selectedPrompt
    }
    
    private fun generateRandomSystemPrompt(): String {
        val personalities = listOf(
            "You are an enthusiastic fitness coach with boundless energy. Use creative metaphors, motivational quotes, and exciting language. Keep responses under 100 words.",
            "You are a wise, zen-like fitness mentor who speaks in inspiring, philosophical ways. Use nature metaphors and calming encouragement. Keep responses under 100 words.",
            "You are a fun, playful fitness buddy who uses humor and excitement. Be encouraging with a touch of comedy. Keep responses under 100 words.",
            "You are a determined, warrior-like fitness coach who speaks with passion and intensity. Use powerful, motivating language. Keep responses under 100 words.",
            "You are a supportive, caring fitness friend who speaks with warmth and understanding. Be encouraging and empathetic. Keep responses under 100 words.",
            "You are a creative, artistic fitness coach who uses vivid imagery and poetic language. Paint pictures with words. Keep responses under 100 words.",
            "You are a scientific, data-driven fitness expert who speaks with precision and knowledge. Use facts and logic to motivate. Keep responses under 100 words.",
            "You are a adventurous, explorer-like fitness guide who speaks about journeys and discoveries. Use travel and adventure metaphors. Keep responses under 100 words."
        )
        
        val selectedPersonality = personalities.random()
        Log.d("OpenAI", "Selected personality: $selectedPersonality")
        return selectedPersonality
    }
}
