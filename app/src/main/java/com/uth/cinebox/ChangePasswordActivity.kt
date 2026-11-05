package com.uth.cinebox

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.example.cinebox.BaseActivity
import com.example.cinebox.R
import com.example.cinebox.SessionManager
import org.json.JSONObject
import kotlin.concurrent.thread

// Đổi mật khẩu: cập nhật dữ liệu người dùng trên máy chủ (PUT /api/me/password)
class ChangePasswordActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_password)

        val session = SessionManager(this)
        val edtOld = findViewById<EditText>(R.id.edtOld)
        val edtNew = findViewById<EditText>(R.id.edtNew)
        val edtConfirm = findViewById<EditText>(R.id.edtConfirm)
        val btnChange = findViewById<Button>(R.id.btnChange)

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        btnChange.setOnClickListener {
            val oldPw = edtOld.text.toString()
            val newPw = edtNew.text.toString()
            val confirm = edtConfirm.text.toString()

            if (oldPw.isEmpty()) {
                edtOld.error = "Nhập mật khẩu hiện tại"
                edtOld.requestFocus()
                return@setOnClickListener
            }
            if (newPw.length < 6) {
                edtNew.error = "Mật khẩu mới tối thiểu 6 ký tự"
                edtNew.requestFocus()
                return@setOnClickListener
            }
            if (newPw == oldPw) {
                edtNew.error = "Mật khẩu mới phải khác mật khẩu hiện tại"
                edtNew.requestFocus()
                return@setOnClickListener
            }
            if (newPw != confirm) {
                edtConfirm.error = "Mật khẩu nhập lại không khớp"
                edtConfirm.requestFocus()
                return@setOnClickListener
            }

            btnChange.isEnabled = false

            thread {
                try {
                    val body = JSONObject().put("oldPassword", oldPw).put("newPassword", newPw)
                    val res = ApiClient.put("/me/password", body, session.token)

                    runOnUiThread {
                        btnChange.isEnabled = true
                        when {
                            res.isOk -> {
                                Toast.makeText(this, "Đổi mật khẩu thành công", Toast.LENGTH_LONG).show()
                                finish()
                            }
                            res.code == 401 -> {
                                Toast.makeText(this, "Phiên đăng nhập hết hạn, hãy đăng nhập lại", Toast.LENGTH_LONG).show()
                                session.clear()
                                val i = Intent(this, LoginActivity::class.java)
                                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                startActivity(i)
                            }
                            else -> Toast.makeText(this, res.errorMessage("Đổi mật khẩu thất bại"), Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        btnChange.isEnabled = true
                        Toast.makeText(this, "Không kết nối được server", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}
