package com.example.service

import com.example.BuildConfig
import com.example.data.model.ReminderCategory
import com.example.data.model.RepeatType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

data class ParsedReminderResult(
    val title: String,
    val dateString: String, // YYYY-MM-DD
    val timeString: String, // HH:mm
    val category: String = "PERSONAL",
    val repeatType: String = "ONCE",
    val voiceGreeting: String = ""
)

class GeminiReminderService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun parseNaturalLanguageReminder(
        userPrompt: String,
        userName: String
    ): ParsedReminderResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext fallbackParse(userPrompt, userName)
        }

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm, EEEE", Locale.ENGLISH)
        val currentTimeStr = sdf.format(Calendar.getInstance().time)

        val systemPrompt = """
            You are Yaad AI, a smart Hindi & English AI reminder parser for an Android app.
            Today's current date and time is: $currentTimeStr.
            User's Name: $userName.
            
            Extract the reminder details from user input and respond ONLY in valid raw JSON.
            JSON structure must be:
            {
              "title": "Short title in original language (e.g. 'टेस्ट', 'दवा खानी है')",
              "dateString": "YYYY-MM-DD format based on current date",
              "timeString": "HH:mm format (24 hour format)",
              "category": "One of: STUDY, MEDICINE, OFFICE, MEETING, SHOPPING, BIRTHDAY, EXERCISE, WATER, PRAYER, PERSONAL",
              "repeatType": "One of: ONCE, DAILY, WEEKLY, MONTHLY, YEARLY",
              "voiceGreeting": "Warm respectful custom spoken message starting with user name e.g. '$userName जी, उठ जाइए। आज सुबह 6 बजे आपका टेस्ट है। Best of Luck.'"
            }
        """.trimIndent()

        val requestJsonObject = JSONObject().apply {
            put("contents", org.json.JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", org.json.JSONArray().apply {
                        put(JSONObject().apply { put("text", userPrompt) })
                    })
                })
            })
            put("systemInstruction", JSONObject().apply {
                put("parts", org.json.JSONArray().apply {
                    put(JSONObject().apply { put("text", systemPrompt) })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.2)
                put("responseMimeType", "application/json")
            })
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"
            val requestBody = requestJsonObject.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val bodyString = response.body?.string() ?: ""

            if (response.isSuccessful && bodyString.isNotEmpty()) {
                val parsedText = extractContentText(bodyString)
                if (parsedText.isNotEmpty()) {
                    val cleanJson = parsedText.replace("```json", "").replace("```", "").trim()
                    val json = JSONObject(cleanJson)
                    return@withContext ParsedReminderResult(
                        title = json.optString("title", userPrompt),
                        dateString = json.optString("dateString", ""),
                        timeString = json.optString("timeString", ""),
                        category = json.optString("category", "PERSONAL"),
                        repeatType = json.optString("repeatType", "ONCE"),
                        voiceGreeting = json.optString("voiceGreeting", "")
                    )
                }
            }
            return@withContext fallbackParse(userPrompt, userName)
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext fallbackParse(userPrompt, userName)
        }
    }

    private fun extractContentText(jsonResponse: String): String {
        return try {
            val root = JSONObject(jsonResponse)
            val candidates = root.getJSONArray("candidates")
            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.getJSONObject("content")
            val parts = content.getJSONArray("parts")
            parts.getJSONObject(0).getString("text")
        } catch (e: Exception) {
            ""
        }
    }

    private fun fallbackParse(userPrompt: String, userName: String): ParsedReminderResult {
        val cal = Calendar.getInstance()
        cal.add(Calendar.HOUR_OF_DAY, 1)

        val dateSdf = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        val timeSdf = SimpleDateFormat("HH:mm", Locale.ENGLISH)

        var category = ReminderCategory.PERSONAL.name
        val lower = userPrompt.lowercase()
        if (lower.contains("दवा") || lower.contains("medicine") || lower.contains("doctor")) {
            category = ReminderCategory.MEDICINE.name
        } else if (lower.contains("टेस्ट") || lower.contains("test") || lower.contains("exam") || lower.contains("पढ़ाई")) {
            category = ReminderCategory.STUDY.name
        } else if (lower.contains("खाना") || lower.contains("cook") || lower.contains("food")) {
            category = ReminderCategory.PERSONAL.name
        } else if (lower.contains("मीटिंग") || lower.contains("meeting")) {
            category = ReminderCategory.MEETING.name
        } else if (lower.contains("पानी") || lower.contains("water")) {
            category = ReminderCategory.WATER.name
        }

        return ParsedReminderResult(
            title = userPrompt.take(30),
            dateString = dateSdf.format(cal.time),
            timeString = timeSdf.format(cal.time),
            category = category,
            repeatType = RepeatType.ONCE.name,
            voiceGreeting = "$userName जी! आपका रिमाइंडर: $userPrompt. Best of luck!"
        )
    }
}
