package com.example.cinebox

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import org.json.JSONObject
import kotlin.concurrent.thread

class ChangePasswordActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_password)

        val edtOld = findViewById<EditText>(R.id.edtOldPassword)
        val edtNew = findViewById<EditText>(R.id.edtNewPassword)
        val edtConfirm = findViewById<EditText>(R.id.edtConfirmPassword)
        val btnSubmit = findViewById<Button>(R.id.btnSubmitChange)

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        btnSubmit.setOnClickListener {
            val oldPw = edtOld.text.toString().trim()
            val newPw = edtNew.text.toString().trim()
            val confirmPw = edtConfirm.text.toString().trim()

            if (oldPw.isEmpty() || newPw.isEmpty() || confirmPw.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPw.length < 6) {
                Toast.makeText(this, "Mật khẩu mới tối thiểu 6 ký tự", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPw != confirmPw) {
                Toast.makeText(this, "Mật khẩu xác nhận không khớp", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val session = SessionManager(this)
            thread {
                try {
                    // Tạo đúng kiểu JSONObject theo yêu cầu của ApiClient.put
                    val jsonBody = JSONObject().apply {
                        put("oldPassword", oldPw)
                        put("newPassword", newPw)
                    }

                    val res = ApiClient.put("/me/password", jsonBody, session.token)

                    runOnUiThread {
                        if (res.isOk) {
                            Toast.makeText(this, "Đổi mật khẩu thành công", Toast.LENGTH_SHORT).show()
                            finish()
                        } else {
                            Toast.makeText(this, "Lỗi: ${res.body}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(this, "Lỗi kết nối server", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}