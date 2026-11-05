package com.uth.cinebox

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import com.example.cinebox.BaseActivity
import com.example.cinebox.ImageLoader
import com.example.cinebox.R
import com.example.cinebox.SessionManager
import org.json.JSONArray
import org.json.JSONObject
import kotlin.concurrent.thread

class MovieDetailActivity : BaseActivity() {

    private lateinit var movie: Movie
    private lateinit var session: SessionManager
    private lateinit var favDb: FavoriteDbHelper

    private lateinit var btnFavorite: Button
    private lateinit var txtCommunity: TextView
    private lateinit var layoutNote: LinearLayout
    private lateinit var edtNote: EditText
    private lateinit var layoutReviews: LinearLayout
    private lateinit var txtNoReview: TextView
    private lateinit var txtFormTitle: TextView
    private lateinit var spnScore: Spinner
    private lateinit var edtComment: EditText
    private lateinit var btnSendReview: Button
    private lateinit var btnDeleteReview: Button

    private var myReview: JSONObject? = null       // đánh giá của chính người dùng (nếu đã gửi)
    private val scores = listOf("10", "9", "8", "7", "6", "5", "4", "3", "2", "1")

    companion object {
        // Explicit Intent + putExtra (chương 4): gửi cả phim sang màn chi tiết dưới dạng chuỗi JSON
        fun start(context: Context, movie: Movie) {
            val intent = Intent(context, MovieDetailActivity::class.java)
            intent.putExtra("MOVIE_JSON", movie.toJson().toString())
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val json = intent.getStringExtra("MOVIE_JSON")
        if (json == null) {
            finish()
            return
        }
        movie = Movie.fromJson(JSONObject(json))
        session = SessionManager(this)
        favDb = FavoriteDbHelper(this)

        setContentView(R.layout.activity_movie_detail)

        // Nút mũi tên quay về màn hình trước
        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        // --- Hiển thị thông tin phim ---
        findViewById<TextView>(R.id.txtTitle).text = movie.title
        findViewById<TextView>(R.id.txtMeta).text = "${movie.year} - ${movie.genresText}"
        findViewById<TextView>(R.id.txtRating).text = "★ ${movie.rating}"
        findViewById<TextView>(R.id.txtOverview).text =
            if (movie.overview.isEmpty()) "Chưa có mô tả" else movie.overview
        ImageLoader.load(movie.posterUrl, findViewById<ImageView>(R.id.imgPoster))

        txtCommunity = findViewById(R.id.txtCommunity)
        btnFavorite = findViewById(R.id.btnFavorite)
        layoutNote = findViewById(R.id.layoutNote)
        edtNote = findViewById(R.id.edtNote)
        layoutReviews = findViewById(R.id.layoutReviews)
        txtNoReview = findViewById(R.id.txtNoReview)
        txtFormTitle = findViewById(R.id.txtFormTitle)
        spnScore = findViewById(R.id.spnScore)
        edtComment = findViewById(R.id.edtComment)
        btnSendReview = findViewById(R.id.btnSendReview)
        btnDeleteReview = findViewById(R.id.btnDeleteReview)

        updateCommunity()

        // --- Yêu thích (SQLite) + ghi chú cá nhân ---
        edtNote.setText(favDb.getNote(movie.id))
        updateFavoriteUi()
        btnFavorite.setOnClickListener {
            if (favDb.isFavorite(movie.id)) {
                favDb.remove(movie.id)
                edtNote.setText("")
            } else {
                favDb.add(movie)
            }
            updateFavoriteUi()
        }
        findViewById<Button>(R.id.btnSaveNote).setOnClickListener {
            favDb.updateNote(movie.id, edtNote.text.toString().trim())
            Toast.makeText(this, "Đã lưu ghi chú", Toast.LENGTH_SHORT).show()
        }

        // --- Implicit Intent (chương 4) ---
        // 1. Xem trailer: có mã YouTube thì mở đúng video, không có thì tìm "tên phim trailer"
        findViewById<Button>(R.id.btnTrailer).setOnClickListener {
            val url = if (movie.trailerKey.isNotEmpty())
                "https://www.youtube.com/watch?v=${movie.trailerKey}"
            else
                "https://www.youtube.com/results?search_query=" + Uri.encode("${movie.title} ${movie.year} trailer")
            safeStart(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
        // 2. Chia sẻ phim qua ứng dụng khác
        findViewById<Button>(R.id.btnShare).setOnClickListener {
            val send = Intent(Intent.ACTION_SEND)
            send.type = "text/plain"
            send.putExtra(
                Intent.EXTRA_TEXT,
                "Mình vừa xem phim \"${movie.title}\" (${movie.year}), điểm ${movie.rating}/10. Đi xem nhé!"
            )
            startActivity(Intent.createChooser(send, "Chia sẻ qua"))
        }
        // 3. Tìm rạp chiếu phim gần đây trên bản đồ
        findViewById<Button>(R.id.btnCinema).setOnClickListener {
            safeStart(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode("rạp chiếu phim"))))
        }

        // --- Đánh giá ---
        spnScore.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, scores)
        spnScore.setSelection(2)                           // mặc định 8 điểm
        btnSendReview.setOnClickListener { sendReview() }
        btnDeleteReview.setOnClickListener { confirmDeleteReview() }

        loadMovieStats()
        loadReviews()
    }

    private fun updateCommunity() {
        txtCommunity.text =
            if (movie.reviewCount == 0) "Cộng đồng CineBox: chưa có đánh giá"
            else "Cộng đồng CineBox: ★ ${movie.communityRating} (${movie.reviewCount} lượt đánh giá)"
    }

    // Ghi chú chỉ hiện khi phim đang nằm trong danh sách yêu thích
    private fun updateFavoriteUi() {
        val fav = favDb.isFavorite(movie.id)
        btnFavorite.text = if (fav) "♥ Đã thích" else "♡ Yêu thích"
        layoutNote.visibility = if (fav) View.VISIBLE else View.GONE
    }

    // Không có ứng dụng nào xử lý được Intent thì báo thay vì để app bị tắt
    private fun safeStart(intent: Intent) {
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "Không có ứng dụng phù hợp để mở", Toast.LENGTH_SHORT).show()
        }
    }

    // Phiên đăng nhập hết hạn (máy chủ trả 401): xóa phiên, về màn hình đăng nhập
    private fun backToLogin() {
        Toast.makeText(this, "Phiên đăng nhập hết hạn, hãy đăng nhập lại", Toast.LENGTH_LONG).show()
        session.clear()
        val i = Intent(this, LoginActivity::class.java)
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(i)
    }

    // ===== GET thông tin mới nhất của phim (điểm cộng đồng, số lượt đánh giá, trailer) =====
    private fun loadMovieStats() {
        thread {
            try {
                val res = ApiClient.get("/movies/${movie.id}")
                if (!res.isOk) return@thread
                val fresh = Movie.fromJson(JSONObject(res.body))
                runOnUiThread {
                    movie = fresh
                    updateCommunity()
                }
            } catch (e: Exception) {
                // Không tải được thì giữ dữ liệu cũ
            }
        }
    }

    // ===== GET danh sách đánh giá từ server =====
    private fun loadReviews() {
        thread {
            try {
                val res = ApiClient.get("/movies/${movie.id}/reviews")
                if (!res.isOk) return@thread
                val arr = JSONArray(res.body)

                // Tìm đánh giá của chính mình trong danh sách
                var mine: JSONObject? = null
                for (i in 0 until arr.length()) {
                    val r = arr.getJSONObject(i)
                    if (r.getString("username") == session.username) mine = r
                }
                runOnUiThread {
                    myReview = mine
                    showReviews(arr)
                    applyMyReviewUi()
                }
            } catch (e: Exception) {
                runOnUiThread { txtNoReview.text = "Không tải được đánh giá" }
            }
        }
    }

    private fun showReviews(arr: JSONArray) {
        layoutReviews.removeAllViews()
        txtNoReview.visibility = if (arr.length() == 0) View.VISIBLE else View.GONE
        txtNoReview.text = "Chưa có đánh giá nào"

        for (i in 0 until arr.length()) {
            val r = arr.getJSONObject(i)
            val name = r.getString("username")
            val row = LayoutInflater.from(this).inflate(R.layout.item_review, layoutReviews, false)
            row.findViewById<TextView>(R.id.txtUser).text =
                if (name == session.username) "$name (bạn)" else name
            row.findViewById<TextView>(R.id.txtScore).text = "★ ${r.getInt("rating")}"
            row.findViewById<TextView>(R.id.txtComment).text = r.optString("comment")
            layoutReviews.addView(row)
        }
    }

    // Đã đánh giá rồi thì biểu mẫu chuyển thành "Cập nhật" kèm nút "Xóa"
    private fun applyMyReviewUi() {
        val mine = myReview
        if (mine != null) {
            txtFormTitle.text = "Đánh giá của bạn (có thể sửa hoặc xóa)"
            btnSendReview.text = "Cập nhật đánh giá"
            btnDeleteReview.visibility = View.VISIBLE
            spnScore.setSelection(10 - mine.getInt("rating"))
            edtComment.setText(mine.optString("comment"))
        } else {
            txtFormTitle.text = "Gửi đánh giá của bạn"
            btnSendReview.text = "Gửi đánh giá"
            btnDeleteReview.visibility = View.GONE
        }
    }

    // ===== POST (thêm) hoặc PUT (sửa) đánh giá, cần token đăng nhập =====
    private fun sendReview() {
        val score = spnScore.selectedItem.toString().toInt()
        val comment = edtComment.text.toString().trim()

        if (comment.length > 500) {
            edtComment.error = "Bình luận tối đa 500 ký tự"
            edtComment.requestFocus()
            return
        }

        val isUpdate = myReview != null
        btnSendReview.isEnabled = false

        thread {
            try {
                val body = JSONObject().put("rating", score).put("comment", comment)
                val res = if (isUpdate)
                    ApiClient.put("/movies/${movie.id}/reviews/mine", body, session.token)
                else
                    ApiClient.post("/movies/${movie.id}/reviews", body, session.token)

                runOnUiThread {
                    btnSendReview.isEnabled = true
                    when {
                        res.isOk -> {
                            Toast.makeText(this, if (isUpdate) "Đã cập nhật đánh giá" else "Đã gửi đánh giá", Toast.LENGTH_SHORT).show()
                            loadReviews()
                            loadMovieStats()
                        }
                        res.code == 401 -> backToLogin()
                        else -> Toast.makeText(this, res.errorMessage("Gửi đánh giá thất bại"), Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    btnSendReview.isEnabled = true
                    Toast.makeText(this, "Không kết nối được server", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun confirmDeleteReview() {
        AlertDialog.Builder(this)
            .setTitle("Xóa đánh giá")
            .setMessage("Bạn có chắc muốn xóa đánh giá của mình cho phim này?")
            .setPositiveButton("Xóa") { _, _ -> deleteReview() }
            .setNegativeButton("Hủy", null)
            .show()
    }

    // ===== DELETE đánh giá của chính mình =====
    private fun deleteReview() {
        thread {
            try {
                val res = ApiClient.delete("/movies/${movie.id}/reviews/mine", session.token)
                runOnUiThread {
                    when {
                        res.isOk -> {
                            Toast.makeText(this, "Đã xóa đánh giá", Toast.LENGTH_SHORT).show()
                            edtComment.setText("")
                            spnScore.setSelection(2)
                            loadReviews()
                            loadMovieStats()
                        }
                        res.code == 401 -> backToLogin()
                        else -> Toast.makeText(this, res.errorMessage("Xóa đánh giá thất bại"), Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this, "Không kết nối được server", Toast.LENGTH_SHORT).show() }
            }
        }
    }
}