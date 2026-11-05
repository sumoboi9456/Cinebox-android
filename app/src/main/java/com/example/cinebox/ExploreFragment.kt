package com.example.cinebox

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import kotlin.concurrent.thread

class ExploreFragment : Fragment() {
    private lateinit var edtSearch: EditText
    private lateinit var spnGenre: Spinner
    private lateinit var recyclerView: RecyclerView
    private lateinit var txtMessage: TextView
    private lateinit var adapter: MovieAdapter

    // Tạo 2 danh sách: 1 cái chứa TẤT CẢ phim từ server, 1 cái chỉ chứa phim đã lọc để hiển thị
    private val allMovies = mutableListOf<Movie>()
    private val displayMovies = mutableListOf<Movie>()

    private val genres = mutableListOf("Tất cả")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_explore, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        edtSearch = view.findViewById(R.id.edtSearch)
        spnGenre = view.findViewById(R.id.spnGenre)
        recyclerView = view.findViewById(R.id.recyclerView)
        txtMessage = view.findViewById(R.id.txtMessage)

        val favDb = FavoriteDbHelper(requireContext())
        // Adapter bây giờ sẽ lấy dữ liệu từ displayMovies
        adapter = MovieAdapter(displayMovies, favDb, { movie -> MovieDetailActivity.start(requireContext(), movie) })
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
        spnGenre.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, genres)

        // BẮT SỰ KIỆN: Gõ chữ tới đâu, lọc phim tới đó cực mượt trên máy
        edtSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterMoviesLocally() // Gọi hàm lọc nội bộ
            }
        })

        spnGenre.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, position: Int, id: Long) {
                (v as? TextView)?.setTextColor(android.graphics.Color.WHITE)
                filterMoviesLocally() // Chọn thể loại cũng lọc nội bộ luôn
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Gọi server ĐÚNG 1 LẦN để kéo hết phim về
        fetchAllMoviesFromServer()
    }

    private fun fetchAllMoviesFromServer() {
        txtMessage.text = "Đang tải dữ liệu phim..."
        txtMessage.visibility = View.VISIBLE
        thread {
            try {
                // Kéo toàn bộ phim (không truyền search hay genre lên server nữa)
                val res = ApiClient.get("/movies")
                if (!res.isOk) throw Exception(res.errorMessage("Server trả về lỗi ${res.code}"))
                val arr = JSONArray(res.body)
                val list = (0 until arr.length()).map { Movie.fromJson(arr.getJSONObject(it)) }

                activity?.runOnUiThread {
                    allMovies.clear()
                    allMovies.addAll(list) // Lưu vào kho tổng
                    fillGenres(list)
                    filterMoviesLocally() // Bắt đầu lọc lần đầu tiên
                }
            } catch (e: Exception) {
                activity?.runOnUiThread {
                    txtMessage.text = "Không kết nối được server.\n${e.message}"
                    txtMessage.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun filterMoviesLocally() {
        val keyword = edtSearch.text.toString().trim().lowercase()
        val selectedGenre = if (spnGenre.selectedItemPosition > 0) genres[spnGenre.selectedItemPosition] else ""

        // TIẾN HÀNH LỌC KHẮT KHE
        val filteredList = allMovies.filter { movie ->
            val matchGenre = if (selectedGenre.isEmpty()) true else movie.genres.contains(selectedGenre)

            val titleLower = movie.title.lowercase()

            // Quy tắc: Phim phải BẮT ĐẦU bằng từ khóa,
            // HOẶC một từ bất kỳ trong tên phim BẮT ĐẦU bằng từ khóa (Ví dụ: "Tro Tàn" sẽ khớp với "ta")
            val matchName = keyword.isEmpty() || titleLower.startsWith(keyword) || titleLower.split(" ").any { it.startsWith(keyword) }

            matchGenre && matchName
        }

        displayMovies.clear()
        displayMovies.addAll(filteredList)
        adapter.notifyDataSetChanged()

        if (displayMovies.isEmpty()) {
            txtMessage.text = "Không tìm thấy phim nào"
            txtMessage.visibility = View.VISIBLE
        } else {
            txtMessage.visibility = View.GONE
        }
    }

    private fun fillGenres(list: List<Movie>) {
        val all = list.flatMap { it.genres }.distinct().sorted()
        genres.clear()
        genres.add("Tất cả")
        genres.addAll(all)
        (spnGenre.adapter as ArrayAdapter<*>).notifyDataSetChanged()
    }

    fun sortBy(kind: String) {
        if (kind == "rating") displayMovies.sortByDescending { it.rating } else displayMovies.sortByDescending { it.year }
        adapter.notifyDataSetChanged()
    }

    override fun onResume() {
        super.onResume()
        Log.d("Lifecycle", "ExploreFragment - onResume")
        if (::adapter.isInitialized) adapter.notifyDataSetChanged()
    }
}