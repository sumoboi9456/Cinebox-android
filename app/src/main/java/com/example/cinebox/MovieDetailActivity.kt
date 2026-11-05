package com.example.cinebox

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
import org.json.JSONArray
import org.json.JSONObject
import kotlin.concurrent.thread

class MovieDetailActivity : BaseActivity() {
    private lateinit var movie: Movie
    private lateinit var session: SessionManager
    private lateinit var favDb: FavoriteDbHelper
    private lateinit var btnFavorite: Button
    private lateinit var layoutReviews: LinearLayout
    private lateinit var txtNoReview: TextView
    private lateinit var spnScore: Spinner
    private lateinit var edtComment: EditText

    companion object {
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
        findViewById<android.view.View>(R.id.btnBack).setOnClickListener {
            finish()
        }

        findViewById<TextView>(R.id.txtTitle).text = movie.title
        findViewById<TextView>(R.id.txtMeta).text = "${movie.year} • ${movie.genresText}"
        findViewById<TextView>(R.id.txtRating).text = "★ ${movie.rating}"
        findViewById<TextView>(R.id.txtOverview).text =
            if (movie.overview.isEmpty()) "Chưa có mô tả" else movie.overview
        ImageLoader.load(movie.posterUrl, findViewById<ImageView>(R.id.imgPoster))

        btnFavorite = findViewById(R.id.btnFavorite)
        layoutReviews = findViewById(R.id.layoutReviews)
        txtNoReview = findViewById(R.id.txtNoReview)
        spnScore = findViewById(R.id.spnScore)
        edtComment = findViewById(R.id.edtComment)

        updateFavoriteButton()
        btnFavorite.setOnClickListener {
            if (favDb.isFavorite(movie.id)) favDb.remove(movie.id) else favDb.add(movie)
            updateFavoriteButton()
        }

        findViewById<Button>(R.id.btnTrailer).setOnClickListener {
            val q = Uri.encode("${movie.title} ${movie.year} trailer")
            safeStart(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=$q")))
        }

        findViewById<Button>(R.id.btnShare).setOnClickListener {
            val send = Intent(Intent.ACTION_SEND)
            send.type = "text/plain"
            send.putExtra(
                Intent.EXTRA_TEXT,
                "Mình vừa xem phim \"${movie.title}\" (${movie.year}), điểm ${movie.rating}/10. Đi xem nhé!"
            )
            startActivity(Intent.createChooser(send, "Chia sẻ qua"))
        }

        findViewById<Button>(R.id.btnCinema).setOnClickListener {
            safeStart(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode("rạp chiếu phim"))))
        }

        spnScore.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item,
            listOf("10", "9", "8", "7", "6", "5", "4", "3", "2", "1")
        )
        spnScore.setSelection(2)
        findViewById<Button>(R.id.btnSendReview).setOnClickListener { sendReview() }
        loadReviews()
    }

    private fun updateFavoriteButton() {
        btnFavorite.text = if (favDb.isFavorite(movie.id)) "♥ Đã thích" else "♡ Yêu thích"
    }

    private fun safeStart(intent: Intent) {
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "Không có ứng dụng phù hợp để mở", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadReviews() {
        thread {
            try {
                val res = ApiClient.get("/movies/${movie.id}/reviews")
                if (!res.isOk) return@thread
                val arr = JSONArray(res.body)
                runOnUiThread { showReviews(arr) }
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
            val row = LayoutInflater.from(this).inflate(R.layout.item_review, layoutReviews, false)
            row.findViewById<TextView>(R.id.txtUser).text = r.getString("username")
            row.findViewById<TextView>(R.id.txtScore).text = "★ ${r.getInt("rating")}"
            row.findViewById<TextView>(R.id.txtComment).text = r.optString("comment")
            layoutReviews.addView(row)
        }
    }

    private fun sendReview() {
        val score = spnScore.selectedItem.toString().toInt()
        val comment = edtComment.text.toString().trim()
        thread {
            try {
                val body = JSONObject().put("rating", score).put("comment", comment)
                val res = ApiClient.post("/movies/${movie.id}/reviews", body, session.token)
                runOnUiThread {
                    when {
                        res.isOk -> {
                            edtComment.setText("")
                            Toast.makeText(this, "Đã gửi đánh giá", Toast.LENGTH_SHORT).show()
                            loadReviews()
                        }
                        res.code == 401 -> {
                            Toast.makeText(this, "Phiên đăng nhập hết hạn, hãy đăng nhập lại", Toast.LENGTH_LONG).show()
                            session.clear()
                            val i = Intent(this, LoginActivity::class.java)
                            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            startActivity(i)
                        }
                        else -> Toast.makeText(this, res.errorMessage("Gửi đánh giá thất bại"), Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this, "Không kết nối được server", Toast.LENGTH_SHORT).show() }
            }
        }
    }
}