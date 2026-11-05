package com.uth.cinebox

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.example.cinebox.BaseActivity
import com.example.cinebox.R
import com.example.cinebox.SessionManager
import org.json.JSONObject
import kotlin.concurrent.thread

class LoginActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val session = SessionManager(this)

        // Đã tick "Ghi nhớ đăng nhập" từ lần trước -> vào thẳng màn hình chính
        if (session.shouldAutoLogin()) {
            goMain()
            return
        }

        setContentView(R.layout.activity_login)

        val edtUsername = findViewById<EditText>(R.id.edtUsername)
        val edtPassword = findViewById<EditText>(R.id.edtPassword)
        val chkRemember = findViewById<CheckBox>(R.id.chkRemember)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val txtGoRegister = findViewById<TextView>(R.id.txtGoRegister)

        // ĐÃ XÓA BIẾN VÀ SỰ KIỆN CỦA NÚT DÙNG THỬ Ở ĐÂY ĐỂ TRÁNH LỖI

        // Đánh thức máy chủ Render ngay khi mở màn hình (lần đầu có thể mất khoảng 1 phút)
        thread {
            try { ApiClient.get("/health") } catch (e: Exception) { }
        }

        btnLogin.setOnClickListener {
            val username = edtUsername.text.toString().trim()
            val password = edtPassword.text.toString()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Nhập đủ tên đăng nhập và mật khẩu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnLogin.isEnabled = false            // tránh bấm nhiều lần khi đang chờ server
            btnLogin.text = "Đang kết nối..."

            thread {
                try {
                    val body = JSONObject()
                        .put("username", username)
                        .put("password", password)
                    val res = ApiClient.post("/login", body)

                    runOnUiThread {
                        btnLogin.isEnabled = true
                        btnLogin.text = "Đăng nhập"
                        if (res.isOk) {
                            val json = JSONObject(res.body)
                            session.save(
                                json.getString("token"),
                                json.getString("username"),
                                chkRemember.isChecked
                            )
                            goMain()
                        } else {
                            Toast.makeText(this, res.errorMessage("Đăng nhập thất bại"), Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        btnLogin.isEnabled = true
                        btnLogin.text = "Đăng nhập"
                        Toast.makeText(this, "Không kết nối được server: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        txtGoRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun goMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}