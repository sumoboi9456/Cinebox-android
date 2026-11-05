package com.example.cinebox

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import org.json.JSONObject
import kotlin.concurrent.thread

class RegisterActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val edtUsername = findViewById<EditText>(R.id.edtUsername)
        val edtPassword = findViewById<EditText>(R.id.edtPassword)
        val edtConfirm = findViewById<EditText>(R.id.edtConfirm)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val txtGoLogin = findViewById<TextView>(R.id.txtGoLogin)

        btnRegister.setOnClickListener {
            val username = edtUsername.text.toString().trim()
            val password = edtPassword.text.toString()
            val confirm = edtConfirm.text.toString()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Nhập đủ tên đăng nhập và mật khẩu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password != confirm) {
                Toast.makeText(this, "Mật khẩu nhập lại không khớp", Toast.LENGTH_SHORT).show()
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