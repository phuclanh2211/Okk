package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GeminiPart(val text: String)

@JsonClass(generateAdapter = true)
data class GeminiContent(val parts: List<GeminiPart>, val role: String? = null)

@JsonClass(generateAdapter = true)
data class GeminiSystemInstruction(val parts: List<GeminiPart>)

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val systemInstruction: GeminiSystemInstruction? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidateContent(val parts: List<GeminiPart>)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(val content: GeminiCandidateContent)

@JsonClass(generateAdapter = true)
data class GeminiResponse(val candidates: List<GeminiCandidate>?)

object GeminiClient {
    private const val TAG = "GeminiClient"
    private const val MODEL = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val requestAdapter = moshi.adapter(GeminiRequest::class.java)
    private val responseAdapter = moshi.adapter(GeminiResponse::class.java)

    suspend fun getChatResponse(
        prompt: String,
        history: List<GeminiContent> = emptyList(),
        systemInstructionText: String? = null
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "Gemini API key is not configured!")
            return@withContext "Lỗi: Chưa cấu hình khóa API Gemini trong AI Studio Secrets. Vui lòng thêm khóa GEMINI_API_KEY."
        }

        val url = "$BASE_URL/$MODEL:generateContent?key=$apiKey"
        val mediaType = "application/json; charset=utf-8".toMediaType()

        // Build the contents list
        val contents = mutableListOf<GeminiContent>()
        contents.addAll(history)
        contents.add(GeminiContent(parts = listOf(GeminiPart(text = prompt)), role = "user"))

        val systemInstruction = systemInstructionText?.let {
            GeminiSystemInstruction(parts = listOf(GeminiPart(text = it)))
        }

        val geminiRequest = GeminiRequest(
            contents = contents,
            systemInstruction = systemInstruction
        )

        val jsonRequest = try {
            requestAdapter.toJson(geminiRequest)
        } catch (e: Exception) {
            Log.e(TAG, "Serialization error", e)
            return@withContext "Lỗi mã hóa yêu cầu: ${e.message}"
        }

        val body = jsonRequest.toRequestBody(mediaType)
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val responseBodyStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    Log.e(TAG, "Unsuccessful response: Code ${response.code}, Body: $responseBodyStr")
                    return@withContext "Lỗi kết nối AI (${response.code}). Vui lòng kiểm tra khóa API của bạn hoặc thử lại sau."
                }

                val geminiResponse = responseAdapter.fromJson(responseBodyStr)
                val textResponse = geminiResponse?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (textResponse != null) {
                    textResponse
                } else {
                    Log.e(TAG, "Empty response structure: $responseBodyStr")
                    "Xin lỗi, trợ lý AI hiện chưa phản hồi được nội dung này."
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network or parsing exception", e)
            "Không thể kết nối với máy chủ AI: ${e.message}. Vui lòng thử lại sau."
        }
    }
}
