package com.example.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class TTSManager(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var isInitialized = false
    private var pendingSpeech: String? = null
    private var pendingPreset: String? = null

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            try {
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                tts?.setAudioAttributes(audioAttributes)
            } catch (e: Exception) {
                Log.w("TTSManager", "Error setting audio attributes: ${e.message}")
            }

            // Default language setup
            val result = tts?.setLanguage(Locale("en", "IN"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                val resultUs = tts?.setLanguage(Locale.US)
                if (resultUs == TextToSpeech.LANG_MISSING_DATA || resultUs == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.language = Locale.getDefault()
                }
            }

            tts?.setPitch(1.0f)
            tts?.setSpeechRate(1.0f)
            isInitialized = true

            pendingSpeech?.let { speech ->
                speak(speech, pendingPreset ?: "Studio Female")
                pendingSpeech = null
                pendingPreset = null
            }
        } else {
            Log.e("TTSManager", "TTS Initialization failed!")
        }
    }

    fun speak(text: String, voicePreset: String = "Studio Female") {
        val cleanText = text.trim()
        if (cleanText.isEmpty()) return

        if (!isInitialized) {
            pendingSpeech = cleanText
            pendingPreset = voicePreset
            return
        }

        try {
            // Check if text has Hindi characters
            val hasHindi = cleanText.any { it in '\u0900'..'\u097F' }
            val targetLocale = if (hasHindi) Locale("hi", "IN") else Locale("en", "IN")
            val langResult = tts?.setLanguage(targetLocale)
            if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.US)
            }

            // Apply voice preset pitches and speeds
            when (voicePreset) {
                "Executive Male" -> {
                    tts?.setPitch(0.85f)
                    tts?.setSpeechRate(0.95f)
                    selectVoiceByGender(isFemale = false)
                }
                "Soft Narrator" -> {
                    tts?.setPitch(0.95f)
                    tts?.setSpeechRate(0.90f)
                    selectVoiceByGender(isFemale = true)
                }
                "Bold Leader" -> {
                    tts?.setPitch(1.05f)
                    tts?.setSpeechRate(1.10f)
                    selectVoiceByGender(isFemale = false)
                }
                else -> { // "Studio Female" or default
                    tts?.setPitch(1.15f)
                    tts?.setSpeechRate(1.0f)
                    selectVoiceByGender(isFemale = true)
                }
            }

            val params = Bundle().apply {
                putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_ALARM)
            }

            tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, params, "YAAD_AI_SPEECH_ID")
        } catch (e: Exception) {
            Log.e("TTSManager", "Error during speak: ${e.message}")
            try {
                tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, "YAAD_AI_SPEECH_ID")
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    private fun selectVoiceByGender(isFemale: Boolean) {
        try {
            val voices = tts?.voices ?: return
            val matchedVoice = voices.firstOrNull { voice ->
                val nameLower = voice.name.lowercase()
                if (isFemale) nameLower.contains("female") || nameLower.contains("f0") || nameLower.contains("a-")
                else nameLower.contains("male") || nameLower.contains("m0") || nameLower.contains("b-")
            } ?: voices.firstOrNull()

            if (matchedVoice != null) {
                tts?.voice = matchedVoice
            }
        } catch (e: Exception) {
            Log.w("TTSManager", "Error setting custom voice: ${e.message}")
        }
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

