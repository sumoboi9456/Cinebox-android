package com.uth.cinebox

import com.example.cinebox.ApiConfig
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

// Kết quả một lần gọi server: mã trạng thái (200, 201, 401...) và nội dung trả về
class ApiResult(val code: Int, val body: String) {
    val isOk: Boolean get() = code in 200..299

    // Lấy câu báo lỗi do server trả về ({"error": "..."}), không có thì dùng câu mặc định
    fun errorMessage(default: String): String =
        try { JSONObject(body).getString("error") } catch (e: Exception) { default }
}

object ApiClient {
    // LƯU Ý: các hàm này chạy mạng nên PHẢI gọi bên trong thread { }, không gọi trên UI thread
    fun get(path: String, token: String? = null): ApiResult =
        request("GET", path, null, token)

    fun post(path: String, json: JSONObject, token: String? = null): ApiResult =
        request("POST", path, json, token)

    // Sửa dữ liệu (ví dụ sửa đánh giá, đổi mật khẩu)
    fun put(path: String, json: JSONObject, token: String? = null): ApiResult =
        request("PUT", path, json, token)

    // Xóa dữ liệu (ví dụ xóa đánh giá)
    fun delete(path: String, token: String? = null): ApiResult =
        request("DELETE", path, null, token)

    private fun request(method: String, path: String, json: JSONObject?, token: String?): ApiResult {
        val conn = URL(ApiConfig.BASE_URL + path).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = method
            conn.connectTimeout = 15000
            conn.readTimeout = 60000   // máy chủ Render có thể mất khoảng 1 phút để thức dậy
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
}
