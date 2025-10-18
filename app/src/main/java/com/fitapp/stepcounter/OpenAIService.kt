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
                    "Keep up the great work! Every step counts towards a healthier you! 💪"
                }
            } catch (e: Exception) {
                "You're doing amazing! Keep stepping towards your goals! 🚶‍♂️✨"
            }
        }
    }
    
    private fun createCoachPrompt(steps: Int): String {
        val randomId = (1..1000).random()
        
        val randomPrompts = when {
            steps >= 10000 -> listOf(
                "I've absolutely DOMINATED $steps steps today! I'm a walking TITAN! (ID: $randomId)",
                "HOLY MOLY! $steps steps! I'm basically a FITNESS SUPERHERO! (ID: $randomId)",
                "INCREDIBLE! $steps steps today! I'm UNSTOPPABLE! (ID: $randomId)",
                "AMAZING! $steps steps! I've CONQUERED the day! (ID: $randomId)",
                "FANTASTIC! $steps steps! I'm a FITNESS LEGEND! (ID: $randomId)",
                "BOOM! $steps steps! I'm a STEPPING MACHINE! (ID: $randomId)",
                "WOWZA! $steps steps! I'm a FITNESS WARRIOR! (ID: $randomId)",
                "EPIC! $steps steps! I'm a MOVEMENT MASTER! (ID: $randomId)"
            )
            steps >= 8000 -> listOf(
                "I'm at $steps steps! SO CLOSE to greatness! (ID: $randomId)",
                "AWESOME! $steps steps! I'm ALMOST there! (ID: $randomId)",
                "GREAT progress! $steps steps! Just a BIT more! (ID: $randomId)",
                "EXCELLENT! $steps steps! I'm ON FIRE! (ID: $randomId)",
                "OUTSTANDING! $steps steps! Almost at the FINISH LINE! (ID: $randomId)",
                "FANTASTIC! $steps steps! I'm CRUSHING it! (ID: $randomId)",
                "AMAZING! $steps steps! I'm UNSTOPPABLE! (ID: $randomId)",
                "INCREDIBLE! $steps steps! I'm a CHAMPION! (ID: $randomId)"
            )
            steps >= 5000 -> listOf(
                "I've got $steps steps! I'm BUILDING momentum! (ID: $randomId)",
                "NICE work! $steps steps! I'm getting STRONGER! (ID: $randomId)",
                "GOOD progress! $steps steps! I'm in the ZONE! (ID: $randomId)",
                "SOLID effort! $steps steps! I'm moving FORWARD! (ID: $randomId)",
                "GREAT job! $steps steps! I'm on the RIGHT track! (ID: $randomId)",
                "AWESOME! $steps steps! I'm BUILDING power! (ID: $randomId)",
                "FANTASTIC! $steps steps! I'm GAINING strength! (ID: $randomId)",
                "EXCELLENT! $steps steps! I'm RISING up! (ID: $randomId)"
            )
            steps >= 2000 -> listOf(
                "I'm at $steps steps! Every journey starts with a SINGLE step! (ID: $randomId)",
                "GOOD start! $steps steps! I'm WARMING up! (ID: $randomId)",
                "NICE beginning! $steps steps! I'm getting STARTED! (ID: $randomId)",
                "GREAT foundation! $steps steps! I'm BUILDING up! (ID: $randomId)",
                "EXCELLENT start! $steps steps! I'm on my WAY! (ID: $randomId)",
                "AWESOME! $steps steps! I'm TAKING off! (ID: $randomId)",
                "FANTASTIC! $steps steps! I'm LAUNCHING! (ID: $randomId)",
                "INCREDIBLE! $steps steps! I'm IGNITING! (ID: $randomId)"
            )
            else -> listOf(
                "I'm at $steps steps! Time to get MOVING! (ID: $randomId)",
                "STARTING with $steps steps! Let's BUILD momentum! (ID: $randomId)",
                "EARLY in the day with $steps steps! Ready to CONQUER! (ID: $randomId)",
                "BEGINNING with $steps steps! Time to STEP up! (ID: $randomId)",
                "FRESH start with $steps steps! Let's make it COUNT! (ID: $randomId)",
                "NEW day with $steps steps! Let's CRUSH it! (ID: $randomId)",
                "FIRST steps with $steps steps! Let's DOMINATE! (ID: $randomId)",
                "LAUNCHING with $steps steps! Let's CONQUER! (ID: $randomId)"
            )
        }
        
        val selectedPrompt = randomPrompts.random()
        Log.d("OpenAI", "Selected prompt: $selectedPrompt")
        return selectedPrompt
    }
    
    private fun generateRandomSystemPrompt(): String {
        val personalities = listOf(
            "You are an EXTREMELY enthusiastic fitness coach with BOUNDLESS energy! Use CAPS LOCK, exclamation marks, and SUPER exciting language! Be OVER THE TOP! Keep responses under 100 words.",
            "You are a CALM, zen-like fitness mentor who speaks in WHISPERING, philosophical ways. Use NATURE metaphors and SERENE encouragement. Be VERY PEACEFUL. Keep responses under 100 words.",
            "You are a SILLY, playful fitness buddy who uses RIDICULOUS humor and WACKY excitement. Be ABSURDLY funny and goofy! Use SILLY jokes! Keep responses under 100 words.",
            "You are a FIERCE, warrior-like fitness coach who speaks with INTENSE passion and POWER! Use BATTLE metaphors and STRONG language! Be INTIMIDATING! Keep responses under 100 words.",
            "You are a WARM, caring fitness friend who speaks with GENTLE warmth and UNDERSTANDING. Be SUPER supportive and EMPATHETIC. Use HUGS and LOVE! Keep responses under 100 words.",
            "You are a CREATIVE, artistic fitness coach who uses VIVID imagery and POETIC language. Paint BEAUTIFUL pictures with words! Be VERY ARTISTIC! Keep responses under 100 words.",
            "You are a PRECISE, scientific fitness expert who speaks with DATA and FACTS. Use NUMBERS, STATISTICS, and LOGIC to motivate. Be VERY ANALYTICAL! Keep responses under 100 words.",
            "You are an ADVENTUROUS, explorer-like fitness guide who speaks about JOURNEYS and DISCOVERIES. Use TRAVEL metaphors and ADVENTURE language! Be VERY EXPLORATORY! Keep responses under 100 words.",
            "You are a MYSTERIOUS, mystical fitness guru who speaks in RIDDLES and SECRETS. Use MAGICAL metaphors and ENIGMATIC language! Be VERY MYSTICAL! Keep responses under 100 words.",
            "You are a REBELLIOUS, punk fitness coach who speaks with ATTITUDE and EDGE. Use REBEL language and PUNK metaphors! Be VERY DEFIANT! Keep responses under 100 words."
        )
        
        val selectedPersonality = personalities.random()
        Log.d("OpenAI", "Selected personality: $selectedPersonality")
        return selectedPersonality
    }
}
