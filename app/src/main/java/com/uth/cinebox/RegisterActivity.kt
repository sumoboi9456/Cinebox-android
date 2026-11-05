package com.uth.cinebox

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.example.cinebox.BaseActivity
import com.example.cinebox.R
import org.json.JSONObject
import kotlin.concurrent.thread

class RegisterActivity : BaseActivity() {

    private val usernameRegex = Regex("^[A-Za-z0-9_]{3,20}$")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val edtUsername = findViewById<EditText>(R.id.edtUsername)
        val edtPassword = findViewById<EditText>(R.id.edtPassword)
        val edtConfirm = findViewById<EditText>(R.id.edtConfirm)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val txtGoLogin = findViewById<TextView>(R.id.txtGoLogin)

        // Nút mũi tên quay về màn hình trước
        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        btnRegister.setOnClickListener {
            val username = edtUsername.text.toString().trim()
            val password = edtPassword.text.toString()
            val confirm = edtConfirm.text.toString()

            // Kiểm tra dữ liệu nhập (giống quy tắc của máy chủ)
            if (!usernameRegex.matches(username)) {
                edtUsername.error = "3 đến 20 ký tự: chữ, số hoặc dấu gạch dưới"
                edtUsername.requestFocus()
                return@setOnClickListener
            }
            if (password.length < 6) {
                edtPassword.error = "Mật khẩu tối thiểu 6 ký tự"
                edtPassword.requestFocus()
                return@setOnClickListener
            }
            if (password != confirm) {
                edtConfirm.error = "Mật khẩu nhập lại không khớp"
                edtConfirm.requestFocus()
                return@setOnClickListener
            }

            btnRegister.isEnabled = false

            thread {
                try {
                    val body = JSONObject()
                        .put("username", username)
                        .put("password", password)
                    val res = ApiClient.post("/register", body)

                    runOnUiThread {
                        btnRegister.isEnabled = true
                        if (res.isOk) {
                            Toast.makeText(this, "Đăng ký thành công, hãy đăng nhập", Toast.LENGTH_LONG).show()
                            finish()              // quay về màn hình đăng nhập
                        } else {
                            Toast.makeText(this, res.errorMessage("Đăng ký thất bại"), Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        btnRegister.isEnabled = true
                        Toast.makeText(this, "Không kết nối được server: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        txtGoLogin.setOnClickListener { finish() }
    }
}
