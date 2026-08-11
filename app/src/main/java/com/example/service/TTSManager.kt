package com.example.service

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class TTSManager(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var isInitialized = false
    private var pendingSpeech: String? = null

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("hi", "IN"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.language = Locale.US
            }
            // Standard clear AI voice tone
            tts?.setPitch(1.0f)
            tts?.setSpeechRate(1.0f)
            isInitialized = true
            pendingSpeech?.let {
                speak(it)
                pendingSpeech = null
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
            return
        }

        // Apply pitch, rate, and gender based on selected HD Voice Profile
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

        tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, "YAAD_AI_SPEECH_ID")
    }

    private fun selectVoiceByGender(isFemale: Boolean) {
        try {
            val voices = tts?.voices ?: return
            val matchedVoice = voices.firstOrNull { voice ->
                if (isFemale) voice.name.lowercase().contains("female") || voice.name.lowercase().contains("f0") || voice.name.lowercase().contains("a-")
                else voice.name.lowercase().contains("male") || voice.name.lowercase().contains("m0") || voice.name.lowercase().contains("b-")
            } ?: voices.firstOrNull()

            if (matchedVoice != null) {
                tts?.voice = matchedVoice
            }
        } catch (e: Exception) {
            Log.w("TTSManager", "Error setting custom voice: ${e.message}")
        }
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}
