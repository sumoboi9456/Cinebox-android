package com.uth.cinebox

import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.cinebox.AirplaneReceiver
import com.example.cinebox.BaseActivity
import com.example.cinebox.BatteryLowReceiver
import com.example.cinebox.ExploreFragment
import com.example.cinebox.FavoriteFragment
import com.example.cinebox.R
import com.example.cinebox.SessionManager
import org.json.JSONObject
import kotlin.concurrent.thread

class MainActivity : BaseActivity() {

    private lateinit var tabExplore: TextView
    private lateinit var tabFavorite: TextView
    private lateinit var tabAccount: TextView
    private lateinit var txtOffline: TextView
    private lateinit var btnMenu: TextView
    private lateinit var session: SessionManager

    // Hai receiver (chương 7), đăng ký trong onStart và hủy trong onStop
    private val airplaneReceiver = AirplaneReceiver { isOn -> showAirplane(isOn) }
    private val batteryReceiver = BatteryLowReceiver()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        session = SessionManager(this)

        tabExplore = findViewById(R.id.tabExplore)
        tabFavorite = findViewById(R.id.tabFavorite)
        tabAccount = findViewById(R.id.tabAccount)
        txtOffline = findViewById(R.id.txtOffline)
        btnMenu = findViewById(R.id.btnMenu)

        tabExplore.setOnClickListener { openTab(tabExplore, ExploreFragment()) }
        tabFavorite.setOnClickListener { openTab(tabFavorite, FavoriteFragment()) }
        tabAccount.setOnClickListener { openTab(tabAccount, AccountFragment()) }
        btnMenu.setOnClickListener { showMenu() }

        if (savedInstanceState == null) {
            openTab(tabExplore, ExploreFragment())
        } else {
            // Sau khi xoay màn hình, Android tự khôi phục Fragment; ta chỉ cần tô lại tab đang chọn
            val current = supportFragmentManager.findFragmentById(R.id.fragment_container)
            val tab = when (current) {
                is FavoriteFragment -> tabFavorite
                is AccountFragment -> tabAccount
                else -> tabExplore
            }
            selectTab(tab)
        }
    }

    // Tô sáng tab được chọn
    private fun selectTab(selected: View) {
        listOf(tabExplore, tabFavorite, tabAccount).forEach { it.isSelected = (it == selected) }
    }

    // Chọn tab và thay Fragment trong khung
    private fun openTab(tab: View, fragment: Fragment) {
        selectTab(tab)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    // Menu ba chấm: sắp xếp và đăng xuất (PopupMenu, chương 3)
    private fun showMenu() {
        val popup = PopupMenu(this, btnMenu)
        popup.menu.add(0, 1, 0, "Sắp xếp theo điểm")
        popup.menu.add(0, 2, 1, "Sắp xếp theo năm")
        popup.menu.add(0, 3, 2, "Đăng xuất")
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> sortExplore("rating")
                2 -> sortExplore("year")
                3 -> logout()
            }
            true
        }
        popup.show()
    }

    private fun sortExplore(kind: String) {
        val current = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (current is ExploreFragment) {
            current.sortBy(kind)
        } else {
            Toast.makeText(this, "Chỉ sắp xếp được ở tab Khám phá", Toast.LENGTH_SHORT).show()
        }
    }

    // Gọi từ menu và từ AccountFragment
    fun logout() {
        // Báo máy chủ hủy token (làm ở luồng nền, lỗi mạng thì bỏ qua)
        val token = session.token
        if (token.isNotEmpty()) {
            thread {
                try { ApiClient.post("/logout", JSONObject(), token) } catch (e: Exception) { }
            }
        }
        session.clear()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun showAirplane(isOn: Boolean) {
        txtOffline.text = "Đang bật chế độ máy bay – không tải được phim mới"
        txtOffline.visibility = if (isOn) View.VISIBLE else View.GONE
    }

    override fun onStart() {
        super.onStart()
        ContextCompat.registerReceiver(
            this, airplaneReceiver,
            IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        ContextCompat.registerReceiver(
            this, batteryReceiver,
            IntentFilter(Intent.ACTION_BATTERY_LOW),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        // Kiểm tra trạng thái hiện tại ngay khi mở màn hình
        val airplaneOn = Settings.Global.getInt(contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) != 0
        showAirplane(airplaneOn)
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(airplaneReceiver)
        unregisterReceiver(batteryReceiver)
    }
}
