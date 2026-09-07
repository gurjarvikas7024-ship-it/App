package com.example.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import java.util.Locale

class TTSManager(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var isInitialized = false
    private var pendingSpeech: String? = null
    private var pendingPreset: String? = null

    var onSpeechStarted: (() -> Unit)? = null
    var onSpeechFinished: (() -> Unit)? = null

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

            // Setup UtteranceProgressListener for volume ducking and callback
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    Log.d("TTSManager", "TTS speech started")
                    onSpeechStarted?.invoke()
                }

                override fun onDone(utteranceId: String?) {
                    Log.d("TTSManager", "TTS speech completed")
                    onSpeechFinished?.invoke()
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    Log.w("TTSManager", "TTS speech error")
                    onSpeechFinished?.invoke()
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    Log.w("TTSManager", "TTS speech error code: $errorCode")
                    onSpeechFinished?.invoke()
                }
            })

            // Set Indian locale setup (prefer Hindi / Indian English with sweet female voice)
            val indianLocale = Locale("en", "IN")
            val result = tts?.setLanguage(indianLocale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                val hiResult = tts?.setLanguage(Locale("hi", "IN"))
                if (hiResult == TextToSpeech.LANG_MISSING_DATA || hiResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.language = Locale.getDefault()
                }
            }

            // Sweet, warm, crystal clear Indian female tone (0.90x gentle pacing, 1.03x warm pitch)
            tts?.setPitch(1.03f)
            tts?.setSpeechRate(0.90f)
            selectSweetIndianFemaleVoice(Locale.getDefault().language)
            isInitialized = true

            pendingSpeech?.let { speech ->
                speak(speech)
                pendingSpeech = null
                pendingPreset = null
            }
        } else {
            Log.e("TTSManager", "TTS Initialization failed!")
        }
    }

    /**
     * Speaks text using a clear Indian voice (Indian English or Hindi depending on text content)
     */
    fun speak(text: String, voicePreset: String = "Indian Female") {
        val cleanText = text.trim()
        if (cleanText.isEmpty()) return

        if (!isInitialized) {
            pendingSpeech = cleanText
            pendingPreset = voicePreset
            return
        }

        try {
            val isDevanagari = containsDevanagari(cleanText)
            val isHindiPreset = voicePreset.contains("Hindi", ignoreCase = true)

            // Select Target Indian Locale
            val targetLocale = if (isDevanagari || isHindiPreset) {
                Locale("hi", "IN")
            } else {
                Locale("en", "IN")
            }

            val langResult = tts?.setLanguage(targetLocale)
            if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Fallback to whichever Indian language is available
                val altLocale = if (targetLocale.language == "hi") Locale("en", "IN") else Locale("hi", "IN")
                val altResult = tts?.setLanguage(altLocale)
                if (altResult == TextToSpeech.LANG_MISSING_DATA || altResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.setLanguage(Locale.ENGLISH)
                }
            }

            val isFemale = true // Single unified Sweet Indian Female Voice (Bodyguard style)

            // Fine-tune pitch and speech rate for sweet, soft, crystal-clear Indian tone (Bodyguard Kareena Kapoor style)
            tts?.setPitch(1.03f) // Sweet, gentle, feminine warmth
            tts?.setSpeechRate(0.90f) // Soft, relaxed, crystal-clear pronunciation
            selectSweetIndianFemaleVoice(targetLocale.language)

            val params = Bundle().apply {
                putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_ALARM)
            }

            val utteranceId = "YAAD_AI_SPEECH_${System.currentTimeMillis()}"
            val res = tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
            Log.d("TTSManager", "Speaking with Indian voice: '$cleanText' [result=$res]")
        } catch (e: Exception) {
            Log.e("TTSManager", "Error during speak: ${e.message}")
            try {
                tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, "YAAD_AI_SPEECH_FALLBACK")
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    private fun containsDevanagari(text: String): Boolean {
        return text.any { it in '\u0900'..'\u097F' }
    }

    /**
     * Finds and selects the sweetest, clearest Indian female voice on the device (Bodyguard Kareena Kapoor style)
     */
    private fun selectSweetIndianFemaleVoice(targetLanguage: String) {
        try {
            val voices = tts?.voices ?: return
            if (voices.isEmpty()) return

            val indianVoices = voices.filter { voice -> isVoiceIndian(voice) }

            // Priority 1: Specifically sweet female neural voice identifiers ('hie', 'ahp', 'cxx', 'hid', 'enc')
            val candidate = indianVoices.firstOrNull { voice ->
                val name = voice.name.lowercase()
                (name.contains("hie") || name.contains("ahp") || name.contains("cxx") || name.contains("hid") || name.contains("enc")) &&
                !voice.isNetworkConnectionRequired
            } ?: indianVoices.firstOrNull { voice ->
                val name = voice.name.lowercase()
                name.contains("hie") || name.contains("ahp") || name.contains("cxx") || name.contains("hid") || name.contains("enc")
            } ?: indianVoices.firstOrNull { voice ->
                voice.locale.language.equals(targetLanguage, ignoreCase = true) &&
                matchesGender(voice, isFemale = true) &&
                !voice.isNetworkConnectionRequired
            } ?: indianVoices.firstOrNull { voice ->
                matchesGender(voice, isFemale = true)
            } ?: indianVoices.firstOrNull()
              ?: voices.firstOrNull { matchesGender(it, isFemale = true) }

            if (candidate != null) {
                tts?.voice = candidate
                Log.d("TTSManager", "Selected Sweet Indian Female Voice: ${candidate.name} (${candidate.locale})")
            }
        } catch (e: Exception) {
            Log.w("TTSManager", "Error selecting sweet female voice: ${e.message}")
        }
    }

    private fun isVoiceIndian(voice: Voice): Boolean {
        val loc = voice.locale ?: return false
        val country = loc.country.uppercase()
        val lang = loc.language.lowercase()
        val name = voice.name.lowercase()

        return country == "IN" || country == "IND" ||
               lang == "hi" || lang == "hin" ||
               name.contains("in-") || name.contains("-in") ||
               name.contains("india") || name.contains("hindi")
    }

    private fun matchesGender(voice: Voice, isFemale: Boolean): Boolean {
        val nameLower = voice.name.lowercase()
        return if (isFemale) {
            nameLower.contains("female") || nameLower.contains("f0") ||
            nameLower.contains("ahp") || nameLower.contains("cxx") ||
            nameLower.contains("enc") || nameLower.contains("hie") ||
            nameLower.contains("hid") || nameLower.contains("a-")
        } else {
            nameLower.contains("male") || nameLower.contains("m0") ||
            nameLower.contains("end") || nameLower.contains("ene") ||
            nameLower.contains("hic") || nameLower.contains("hia") ||
            nameLower.contains("b-")
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

