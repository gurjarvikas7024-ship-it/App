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
                tts?.language = Locale.ENGLISH
            }
            isInitialized = true
            pendingSpeech?.let {
                speak(it)
                pendingSpeech = null
            }
        } else {
            Log.e("TTSManager", "TTS Initialization failed!")
        }
    }

    fun speak(text: String, gender: String = "FEMALE") {
        if (!isInitialized) {
            pendingSpeech = text
            return
        }

        // Adjust pitch slightly for voice customization
        if (gender.uppercase() == "MALE") {
            tts?.setPitch(0.85f)
            tts?.setSpeechRate(0.95f)
        } else {
            tts?.setPitch(1.15f)
            tts?.setSpeechRate(1.0f)
        }

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "YAAD_AI_SPEECH_ID")
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}
