package com.example.cinebox

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import android.widget.ImageView
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

// Tải ảnh poster từ URL (không cần thư viện ngoài). Cách làm giống chương 8: tải trong thread.
object ImageLoader {
    // Bộ nhớ đệm giữ tối đa 40 ảnh đã tải để cuộn lại không phải tải lại
    private val cache = object : LruCache<String, Bitmap>(40) {}

    @android.annotation.SuppressLint("WrongThread") // Thêm dòng này để tắt cảnh báo nhầm
    fun load(url: String, view: ImageView) {
        view.setImageDrawable(null)
        view.tag = url                       // ghi nhớ ảnh nào đang được yêu cầu cho view này
        if (url.isEmpty()) return

        val cached = cache.get(url)
        if (cached != null) {
            view.setImageBitmap(cached)
            return
        }

        thread {
            try {
                val conn = URL(url).openConnection() as HttpURLConnection
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                val bmp = conn.inputStream.use { BitmapFactory.decodeStream(it) }
                if (bmp != null) {
                    cache.put(url, bmp)
                    // RecyclerView tái sử dụng view: chỉ gắn ảnh nếu view vẫn đang cần đúng URL này
                    view.post { if (view.tag == url) view.setImageBitmap(bmp) }
                }
            } catch (e: Exception) {
                // Lỗi tải ảnh thì bỏ qua, view giữ nền poster mặc định
            }
        }
    }
}