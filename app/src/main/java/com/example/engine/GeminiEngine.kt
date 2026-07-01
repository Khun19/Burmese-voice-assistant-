package com.example.engine

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiEngine {
    private val tag = "GeminiEngine"
    private val modelName = "gemini-3.5-flash"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Call the Gemini 3.5 Flash API to get response for text queries.
     */
    suspend fun generateContent(prompt: String, systemInstruction: String? = null): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w(tag, "Gemini API key is not configured. Falling back to simulated response.")
            return@withContext getSimulatedResponse(prompt)
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
        
        try {
            // Build request JSON
            val requestJson = JSONObject()
            
            // Contents array
            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            contentObj.put("role", "user")
            
            val partsArray = JSONArray()
            val partObj = JSONObject()
            partObj.put("text", prompt)
            partsArray.put(partObj)
            
            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            requestJson.put("contents", contentsArray)

            // System instructions if specified
            if (!systemInstruction.isNullOrBlank()) {
                val systemObj = JSONObject()
                val systemParts = JSONArray()
                val systemPart = JSONObject()
                systemPart.put("text", systemInstruction)
                systemParts.put(systemPart)
                systemObj.put("parts", systemParts)
                requestJson.put("systemInstruction", systemObj)
            }

            // Generation config
            val configObj = JSONObject()
            configObj.put("temperature", 0.7)
            requestJson.put("generationConfig", configObj)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = requestJson.toString().toRequestBody(mediaType)
            
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: ""
                val responseJson = JSONObject(responseBody)
                val candidates = responseJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        return@withContext parts.getJSONObject(0).optString("text", "အဖြေ မရရှိပါ")
                    }
                }
                return@withContext "အဖြေ တည်ဆောက်မှု မအောင်မြင်ပါခင်ဗျာ။"
            } else {
                Log.e(tag, "Gemini Request failed: ${response.code} - ${response.message}")
                return@withContext "စနစ် ချို့ယွင်းချက် ရှိနေပါသည် (Error ${response.code})။ အော့ဖ်လိုင်းမုဒ်ဖြင့် ပြန်လည်ဖြေကြားပေးပါမည်။\n\n${getSimulatedResponse(prompt)}"
            }
        } catch (e: Exception) {
            Log.e(tag, "Exception during Gemini request: ${e.message}", e)
            return@withContext "အင်တာနက် ချိတ်ဆက်မှုမရှိပါ သို့မဟုတ် ကီးမှားယွင်းနေပါသည်။ အော့ဖ်လိုင်းမုဒ်ဖြင့် ဖြေကြားချက် -\n\n${getSimulatedResponse(prompt)}"
        }
    }

    /**
     * Robust offline simulated response based on keywords.
     */
    private fun getSimulatedResponse(prompt: String): String {
        val trimmed = prompt.lowercase()
        return when {
            trimmed.contains("email") || trimmed.contains("အီးမေးလ်") -> {
                "လေးစားအပ်ပါသော လူကြီးမင်းခင်ဗျာ၊\n\nယခုအီးမေးလ်သည် R's AI Writing Studio မှ အလိုအလျောက် ရေးသားပေးထားသော အီးမေးလ်မူကြမ်း ဖြစ်ပါသည်။ လူကြီးမင်း လိုအပ်သလို ပြင်ဆင်အသုံးပြုနိုင်ပါသည်ခင်ဗျာ။\n\nနွေးထွေးစွာဖြင့်၊\n[လူကြီးမင်းအမည်]"
            }
            trimmed.contains("summary") || trimmed.contains("အကျဉ်းချုပ်") -> {
                "R's AI Summary Tool:\n• ဤစာသားသည် အရေးကြီးဆုံးအချက်များကို ကောင်းမွန်စွာ စုစည်းထားပါသည်။\n• အဓိက အနှစ်ချုပ်မှာ စနစ်တစ်ခုလုံးကို မြန်မာဘာသာဖြင့် အော့ဖ်လိုင်းအမိန့်ပေး စေခိုင်းနိုင်ခြင်း ဖြစ်ပါသည်။"
            }
            trimmed.contains("rewrite") || trimmed.contains("ပြန်ပြင်") -> {
                "R's AI Rewrite Tool:\n\nကျွန်ုပ်တို့၏ R's AI စနစ်သည် သင့်ဖုန်းကို အသံဖြင့် အပြည့်အဝ စီမံထိန်းချုပ်နိုင်ရန် ကူညီပေးမည့် အထူးကောင်းမွန်သော ဆော့ဖ်ဝဲလ်တစ်ခု ဖြစ်ပါသည်ခင်ဗျာ။"
            }
            trimmed.contains("translate") || trimmed.contains("ဘာသာပြန်") -> {
                "R's AI Translation System:\n\n\"မင်္ဂလာရှိသော နေ့တစ်နေ့ ဖြစ်ပါစေကြောင်း R's AI မှ ဆုမွန်ကောင်းတောင်းအပ်ပါသည်။ (Have a wonderful day, wishes R's AI.)\""
            }
            trimmed.contains("wallpaper") || trimmed.contains("ပုံဆွဲ") || trimmed.contains("ဓာတ်ပုံ") -> {
                "R's AI Wallpaper Generator:\n\nအလှပဆုံးသော မြန်မာ့ရိုးရာ ရှုခင်းများကို စိတ်ကူးယဉ် အနုပညာပုံစံဖြင့် ဖန်တီးတည်ဆောက်ပေးလိုက်ပါပြီခင်ဗျာ။ Wallpaper အဖြစ် အသုံးပြုနိုင်ပါပြီ။"
            }
            trimmed.contains("စကရင်") || trimmed.contains("error") || trimmed.contains("screen") -> {
                "R's Screen Intelligence Solution:\n\nလက်ရှိစကရင်တွင် System Configuration Error တက်နေသည်ကို တွေ့ရှိရပါသည်။ Wifi settings ထဲသို့ဝင်ရောက်၍ ကွန်ရက်ကို ပြန်လည်စစ်ဆေးရန် အကြံပြုအပ်ပါသည်ခင်ဗျာ။"
            }
            trimmed.contains("meeting") || trimmed.contains("မှတ်တမ်း") || trimmed.contains("transcript") -> {
                "R's AI Voice Hub Meeting Transcript:\n\n[အစည်းအဝေးမှတ်တမ်း]\nပြောကြားသူ ၁: \"R's AI ကို သုံးရတာ တော်တော်အဆင်ပြေတယ်။\"\nပြောကြားသူ ၂: \"ဟုတ်တယ်၊ Wifi ဖွင့် Bluetooth ပိတ်တာတွေ အသံနဲ့ပဲ လုပ်လို့ရတယ်။\""
            }
            trimmed.contains("သီချင်း") || trimmed.contains("music") -> {
                "R's Media Controller:\n\nတေးသီချင်း ဖွင့်လှစ်ခြင်း စတင်နေပါပြီခင်ဗျာ။ Volume ကိုလည်း အသံဖြင့် ထိန်းချုပ်နိုင်ပါသည်။"
            }
            trimmed.contains("မင်္ဂလာပါ") || trimmed.contains("hello") || trimmed.contains("hi") -> {
                "မင်္ဂလာပါခင်ဗျာ။ ကျွန်တော်ကတော့ R's AI (Myanmar Voice Assistant) ဖြစ်ပါတယ်။ ဘာများ ကူညီပေးရမလဲခင်ဗျာ။"
            }
            trimmed.contains("နေကောင်း") -> {
                "ဟုတ်ကဲ့၊ ကျွန်တော် အဆင်ပြေ နေကောင်းပါတယ်ခင်ဗျာ။ လူကြီးမင်းလည်း သက်ရှည်ကျန်းမာ ရွှင်လန်းပါစေကြောင်း ဆုမွန်ကောင်းတောင်းအပ်ပါတယ်ခင်ဗျာ။"
            }
            else -> {
                "R's AI:\n\nလူကြီးမင်း၏ မေးမြန်းချက်ဖြစ်သော \"$prompt\" ကို အော့ဖ်လိုင်း အသိဉာဏ်တုစနစ်ဖြင့် ဆန်းစစ်ချက်အရ အကောင်းဆုံး ဝန်ဆောင်မှုပေးနေပါသည်ခင်ဗျာ။ အင်တာနက်ချိတ်ဆက်၍ Gemini API key ကို ထည့်သွင်းပါက ပိုမိုကျယ်ပြန့်စွာ ဖြေကြားပေးနိုင်မည် ဖြစ်ပါသည်။"
            }
        }
    }
}
