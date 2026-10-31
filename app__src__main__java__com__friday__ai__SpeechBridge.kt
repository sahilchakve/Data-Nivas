package com.friday.ai

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.webkit.JavascriptInterface
import java.util.Locale

/** Lets the family's voices speak through the phone's own text-to-speech engine. */
class SpeechBridge(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context, this)
    private var ready = false
    private val main = Handler(Looper.getMainLooper())

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
    }

    @JavascriptInterface
    fun speak(text: String, lang: String, pitch: Double, rate: Double) {
        main.post {
            val t = tts ?: return@post
            if (!ready) return@post
            val r = t.setLanguage(Locale.forLanguageTag(lang))
            if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
                t.setLanguage(Locale("en", "IN"))
            }
            t.setPitch(pitch.toFloat())
            t.setSpeechRate(rate.toFloat())
            t.speak(text, TextToSpeech.QUEUE_ADD, null, "dn")
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
