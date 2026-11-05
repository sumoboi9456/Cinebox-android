package com.example.cinebox

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class ApiResult(val code: Int, val body: String) {
    val isOk: Boolean get() = code in 200..299
    fun errorMessage(default: String): String {
        return try {
            JSONObject(body).getString("error")
        } catch (e: Exception) {
            default
        }
    }
}

object ApiClient {
    private fun request(method: String, path: String, json: JSONObject?, token: String?): ApiResult {
        val conn = URL(ApiConfig.BASE_URL + path).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = method
            conn.connectTimeout = 15000
            conn.readTimeout = 60000
            if (token != null) conn.setRequestProperty("Authorization", "Bearer $token")
            if (json != null) {
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conn.doOutput = true
                conn.outputStream.use { it.write(json.toString().toByteArray()) }
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() } ?: ""
            return ApiResult(code, body)
        } finally {
            conn.disconnect()
        }
    }

    fun get(path: String): ApiResult = request("GET", path, null, null)
    fun get(path: String, token: String?): ApiResult = request("GET", path, null, token)
    fun post(path: String, json: JSONObject, token: String? = null) = request("POST", path, json, token)
    fun put(path: String, json: JSONObject, token: String? = null) = request("PUT", path, json, token)
    fun delete(path: String, token: String? = null) = request("DELETE", path, null, token)
}