package com.example.cinebox

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject
import kotlin.concurrent.thread

class MyReviewsActivity : BaseActivity() {

    private var listView: ListView? = null
    private var txtEmpty: TextView? = null
    private val reviews = mutableListOf<JSONObject>()
    private var adapter: ReviewAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_reviews)

        findViewById<TextView>(R.id.btnBack)?.setOnClickListener { finish() }

        listView = findViewById(R.id.listReviews)
        txtEmpty = findViewById(R.id.txtEmpty)

        adapter = ReviewAdapter()
        listView?.adapter = adapter

        listView?.setOnItemClickListener { _, _, position, _ ->
            if (position >= 0 && position < reviews.size) {
                val r = reviews[position]
                val movieId = r.optInt("movieId", 0)
                if (movieId > 0) openMovie(movieId)
            }
        }

        loadReviews()
    }

    private fun loadReviews() {
        val session = SessionManager(this)
        runOnUiThread {
            txtEmpty?.text = "Đang tải đánh giá..."
            txtEmpty?.visibility = View.VISIBLE
        }

        thread {
            try {
                val res = ApiClient.get("/me/reviews", session.token)
                val list = mutableListOf<JSONObject>()

                if (res.isOk && !res.body.isNullOrEmpty()) {
                    try {
                        val arr = JSONArray(res.body)
                        for (i in 0 until arr.length()) {
                            val obj = arr.optJSONObject(i)
                            if (obj != null) {
                                list.add(obj)
                            }
                        }
                    } catch (e: Exception) {
                        // Bỏ qua lỗi parse JSON thô
                    }
                }

                runOnUiThread {
                    reviews.clear()
                    reviews.addAll(list)
                    adapter?.notifyDataSetChanged()

                    if (reviews.isEmpty()) {
                        txtEmpty?.text = "Bạn chưa có đánh giá nào"
                        txtEmpty?.visibility = View.VISIBLE
                    } else {
                        txtEmpty?.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    txtEmpty?.text = "Không kết nối được server"
                    txtEmpty?.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun openMovie(movieId: Int) {
        try {
            val i = Intent(this, MovieDetailActivity::class.java)
            i.putExtra("movieId", movieId)
            startActivity(i)
        } catch (e: Exception) {
            // Tránh văng app khi mở chi tiết phim
        }
    }

    inner class ReviewAdapter : BaseAdapter() {
        override fun getCount(): Int = reviews.size
        override fun getItem(position: Int): Any = reviews.getOrElse(position) { JSONObject() }
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: LayoutInflater.from(this@MyReviewsActivity)
                .inflate(R.layout.item_my_review, parent, false)

            try {
                val r = reviews.getOrElse(position) { JSONObject() }

                val title = view.findViewById<TextView>(R.id.txtMovieTitle)
                val score = view.findViewById<TextView>(R.id.txtRating)
                val comment = view.findViewById<TextView>(R.id.txtComment)

                val movieTitle = r.optString("movieTitle", "Phim chưa rõ tên")
                val year = r.optInt("year", 0)
                val rating = r.optInt("rating", 0)
                val commentText = r.optString("comment", "")

                title?.text = if (year > 0) "$movieTitle ($year)" else movieTitle
                score?.text = "★ $rating/10"
                comment?.text = commentText
            } catch (e: Exception) {
                // Xử lý an toàn nếu view item bị lỗi
            }

            return view
        }
    }
}