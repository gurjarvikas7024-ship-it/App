package com.example.service

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class TTSManager(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var isInitialized = false
    private var pendingSpeech: String? = null
    private var pendingPreset: String = "Jethalal"

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("hi", "IN"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.language = Locale.US
            }
            isInitialized = true
            pendingSpeech?.let {
                speak(it, pendingPreset)
                pendingSpeech = null
            }
        } else {
            Log.e("TTSManager", "TTS Initialization failed!")
        }
    }

    fun speak(text: String, voicePreset: String = "Jethalal") {
        val formattedSpeech = getCharacterFormattedText(text, voicePreset)

        if (!isInitialized) {
            pendingSpeech = text
            pendingPreset = voicePreset
            return
        }

        // Apply character pitch and speech rate profile
        applyVoicePreset(voicePreset)

        tts?.speak(formattedSpeech, TextToSpeech.QUEUE_FLUSH, null, "YAAD_AI_SPEECH_ID")
    }

    private fun getCharacterFormattedText(rawText: String, preset: String): String {
        val cleanText = rawText.trim()
        if (cleanText.contains("Chai Piyo") || cleanText.contains("Khali pet") || cleanText.contains("Maa, Mataji") || cleanText.contains("darwaza tod do")) {
            return cleanText
        }

        return when (preset) {
            "Jethalal" -> "Aey Chal Chal Avey! Chai piyo, biscuit khao! Aapka reminder hai: $cleanText"
            "Motu" -> "Khali pet mere dimaag ki batti nahi chalti! Samosa khane se pehle suno, aapka reminder hai: $cleanText"
            "Patlu" -> "Suno Motu, mere dimaag me ek super idea aaya hai! Aapka reminder hai: $cleanText"
            "Daya Bhabhi" -> "Hey Maa, Mataji! Tapu ke papa, jaldi suniye! Aapka reminder aaya hai: $cleanText"
            "Inspector Daya" -> "Daya, darwaza tod do! CID ka order hai, aapka reminder suno: $cleanText"
            else -> cleanText
        }
    }

    private fun applyVoicePreset(preset: String) {
        when (preset) {
            "Jethalal" -> {
                tts?.setPitch(0.85f)
                tts?.setSpeechRate(1.15f)
                selectVoiceByGender(isFemale = false)
            }
            "Motu" -> {
                tts?.setPitch(0.70f)
                tts?.setSpeechRate(0.95f)
                selectVoiceByGender(isFemale = false)
            }
            "Patlu" -> {
                tts?.setPitch(1.35f)
                tts?.setSpeechRate(1.20f)
                selectVoiceByGender(isFemale = false)
            }
            "Daya Bhabhi" -> {
                tts?.setPitch(1.55f)
                tts?.setSpeechRate(1.25f)
                selectVoiceByGender(isFemale = true)
            }
            "Inspector Daya" -> {
                tts?.setPitch(0.60f)
                tts?.setSpeechRate(0.90f)
                selectVoiceByGender(isFemale = false)
            }
            "Executive Male" -> {
                tts?.setPitch(0.80f)
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
                tts?.setSpeechRate(1.15f)
                selectVoiceByGender(isFemale = false)
            }
            else -> { // "Studio Female"
                tts?.setPitch(1.15f)
                tts?.setSpeechRate(1.05f)
                selectVoiceByGender(isFemale = true)
            }
        }
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
