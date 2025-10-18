package com.fitapp.stepcounter

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

data class ElevenLabsRequest(
    val text: String,
    val model_id: String = "eleven_multilingual_v2",
    val voice_settings: VoiceSettings = VoiceSettings()
)

data class VoiceSettings(
    val stability: Double = 0.75,
    val similarity_boost: Double = 0.75,
    val style: Double = 0.0,
    val use_speaker_boost: Boolean = true
)

class ElevenLabsService(private val context: Context) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val gson = Gson()
    private var mediaPlayer: MediaPlayer? = null
    
    suspend fun textToSpeech(text: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Create request
                val request = ElevenLabsRequest(text = text)
                val json = gson.toJson(request)
                val body = json.toRequestBody("application/json".toMediaType())
                
                val httpRequest = Request.Builder()
                    .url("https://api.elevenlabs.io/v1/text-to-speech/JBFqnCBsd6RMkjVDRZzb")
                    .addHeader("Accept", "audio/mpeg")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("xi-api-key", BuildConfig.ELEVENLABS_API_KEY)
                    .post(body)
                    .build()
                
                val response = client.newCall(httpRequest).execute()
                
                if (response.isSuccessful) {
                    val audioBytes = response.body?.bytes()
                    if (audioBytes != null) {
                        // Save audio to temporary file
                        val tempFile = File(context.cacheDir, "coach_feedback_${System.currentTimeMillis()}.mp3")
                        FileOutputStream(tempFile).use { it.write(audioBytes) }
                        
                        // Play audio
                        withContext(Dispatchers.Main) {
                            playAudioFile(tempFile)
                        }
                        true
                    } else {
                        false
                    }
                } else {
                    Log.e("ElevenLabs", "API Error: ${response.code} - ${response.message}")
                    false
                }
            } catch (e: Exception) {
                Log.e("ElevenLabs", "Error converting text to speech", e)
                false
            }
        }
    }
    
    private fun playAudioFile(file: File) {
        try {
            // Stop any existing playback
            mediaPlayer?.release()
            
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                
                setDataSource(file.absolutePath)
                prepare()
                start()
                
                setOnCompletionListener {
                    release()
                    // Clean up temp file
                    if (file.exists()) {
                        file.delete()
                    }
                }
                
                setOnErrorListener { _, what, extra ->
                    Log.e("ElevenLabs", "MediaPlayer error: $what, $extra")
                    release()
                    if (file.exists()) {
                        file.delete()
                    }
                    true
                }
            }
        } catch (e: Exception) {
            Log.e("ElevenLabs", "Error playing audio", e)
        }
    }
    
    fun stopPlayback() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
    
    fun cleanup() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
