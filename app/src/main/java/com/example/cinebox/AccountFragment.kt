package com.example.cinebox

import android.content.Intent
import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Tab "Tài khoản": thông tin người dùng, xuất/đọc file, đăng xuất
class AccountFragment : Fragment() {
    private lateinit var txtFavCount: TextView
    private lateinit var favDb: FavoriteDbHelper

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_account, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val session = SessionManager(requireContext())
        favDb = FavoriteDbHelper(requireContext())
        view.findViewById<TextView>(R.id.txtUsername).text = session.username
        txtFavCount = view.findViewById(R.id.txtFavCount)

        // --- ĐOẠN CODE BẮT SỰ KIỆN NÚT ---
        view.findViewById<Button>(R.id.btnMyReviews).setOnClickListener {
            val intent = Intent(requireContext(), MyReviewsActivity::class.java)
            startActivity(intent)
        }

        view.findViewById<Button>(R.id.btnChangePassword).setOnClickListener {
            val intent = Intent(requireContext(), ChangePasswordActivity::class.java)
            startActivity(intent)
        }
        // -----------------------------------

        view.findViewById<Button>(R.id.btnExport).setOnClickListener { exportFavorites() }
        view.findViewById<Button>(R.id.btnReadExport).setOnClickListener { showFiles() }
        view.findViewById<Button>(R.id.btnLogout).setOnClickListener {
            (requireActivity() as MainActivity).logout()
        }
    }

    override fun onResume() {
        super.onResume()
        txtFavCount.text = "Số phim yêu thích: ${favDb.getAll().size}"
    }

    // ===== Ghi file (File Storage, chương 6) =====
    private fun exportFavorites() {
        val list = favDb.getAll()
        if (list.isEmpty()) {
            Toast.makeText(requireContext(), "Chưa có phim yêu thích để xuất", Toast.LENGTH_SHORT).show()
            return
        }
        val time = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "Favorites_$time.txt"
        val text = list.joinToString("\n") { "${it.title} (${it.year}) - ★ ${it.rating}" }
        try {
            requireContext().openFileOutput(fileName, Context.MODE_PRIVATE).use {
                it.write(text.toByteArray())
            }
            Toast.makeText(requireContext(), "Đã xuất: $fileName", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Lỗi ghi file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // ===== Đọc file: hộp thoại danh sách file, chọn xong hiện nội dung =====
    private fun showFiles() {
        val files = requireContext().fileList()
            .filter { it.startsWith("Favorites_") }
            .sortedDescending()
            .toTypedArray()
        if (files.isEmpty()) {
            Toast.makeText(requireContext(), "Chưa có file nào được xuất", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Chọn file")
            .setItems(files) { _, which ->
                try {
                    val text = requireContext().openFileInput(files[which])
                        .bufferedReader().use { it.readText() }
                    AlertDialog.Builder(requireContext())
                        .setTitle(files[which])
                        .setMessage(text)
                        .setPositiveButton("Đóng", null)
                        .show()
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Không đọc được file", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }
}