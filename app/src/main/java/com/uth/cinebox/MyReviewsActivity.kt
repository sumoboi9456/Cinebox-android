package com.uth.cinebox

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cinebox.BaseActivity
import com.example.cinebox.R
import com.example.cinebox.SessionManager
import org.json.JSONArray
import org.json.JSONObject
import kotlin.concurrent.thread

// Một đánh giá của tôi (kèm tên phim) lấy từ máy chủ
data class MyReview(
    val movieId: Int,
    val movieTitle: String,
    val year: Int,
    val rating: Int,
    val comment: String,
    val createdAt: String
)

private class MyReviewAdapter(
    private val list: List<MyReview>,
    private val onClick: (MyReview) -> Unit
) : RecyclerView.Adapter<MyReviewAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val txtMovieTitle: TextView = v.findViewById(R.id.txtMovieTitle)
        val txtScore: TextView = v.findViewById(R.id.txtScore)
        val txtComment: TextView = v.findViewById(R.id.txtComment)
        val txtDate: TextView = v.findViewById(R.id.txtDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_my_review, parent, false))

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val r = list[position]
        holder.txtMovieTitle.text = "${r.movieTitle} (${r.year})"
        holder.txtScore.text = "★ ${r.rating}"
        holder.txtComment.text = if (r.comment.isEmpty()) "(không có bình luận)" else r.comment
        holder.txtDate.text = "Ngày đánh giá: " + toVnDate(r.createdAt)
        holder.itemView.setOnClickListener { onClick(r) }
    }

    // "2026-11-02T08:54:24.689Z" -> "02/11/2026"
    private fun toVnDate(iso: String): String {
        val d = iso.take(10).split("-")
        return if (d.size == 3) "${d[2]}/${d[1]}/${d[0]}" else iso
    }
}

// Danh sách các đánh giá mà người dùng đã gửi; chạm vào một dòng để mở phim và sửa hoặc xóa
class MyReviewsActivity : BaseActivity() {

    private lateinit var session: SessionManager
    private lateinit var txtEmpty: TextView
    private lateinit var txtCount: TextView
    private lateinit var adapter: MyReviewAdapter
    private val reviews = mutableListOf<MyReview>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_reviews)

        session = SessionManager(this)
        txtEmpty = findViewById(R.id.txtEmpty)
        txtCount = findViewById(R.id.txtCount)
        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        adapter = MyReviewAdapter(reviews) { r -> openMovie(r.movieId) }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    // Quay lại từ màn chi tiết (có thể vừa sửa hoặc xóa) thì tải lại danh sách
    override fun onResume() {
        super.onResume()
        loadReviews()
    }

    private fun loadReviews() {
        thread {
            try {
                val res = ApiClient.get("/me/reviews", session.token)
                if (res.code == 401) {
                    runOnUiThread { backToLogin() }
                    return@thread
                }
                if (!res.isOk) throw Exception(res.errorMessage("Lỗi ${res.code}"))

                val arr = JSONArray(res.body)
                val list = (0 until arr.length()).map {
                    val o = arr.getJSONObject(it)
                    MyReview(
                        movieId = o.getInt("movieId"),
                        movieTitle = o.optString("movieTitle"),
                        year = o.optInt("year"),
                        rating = o.getInt("rating"),
                        comment = o.optString("comment"),
                        createdAt = o.optString("createdAt")
                    )
                }
                runOnUiThread {
                    reviews.clear()
                    reviews.addAll(list)
                    adapter.notifyDataSetChanged()
                    txtCount.text = "${reviews.size} đánh giá"
                    txtEmpty.visibility = if (reviews.isEmpty()) View.VISIBLE else View.GONE
                }
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this, "Không tải được đánh giá: ${e.message}", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    // Lấy thông tin phim từ máy chủ rồi mở màn chi tiết
    private fun openMovie(movieId: Int) {
        thread {
            try {
                val res = ApiClient.get("/movies/$movieId")
                if (!res.isOk) throw Exception(res.errorMessage("Không tìm thấy phim"))
                val movie = Movie.fromJson(JSONObject(res.body))
                runOnUiThread { MovieDetailActivity.start(this, movie) }
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this, "Không mở được phim: ${e.message}", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    private fun backToLogin() {
        Toast.makeText(this, "Phiên đăng nhập hết hạn, hãy đăng nhập lại", Toast.LENGTH_LONG).show()
        session.clear()
        val i = Intent(this, LoginActivity::class.java)
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(i)
    }
}
