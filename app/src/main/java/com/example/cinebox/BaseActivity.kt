package com.example.cinebox

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

// Lớp cha cho mọi Activity của app: tự ghi log vòng đời (chương 5).
// Lọc trong Logcat bằng:  tag=:Lifecycle
open class BaseActivity : AppCompatActivity() {

    private fun log(name: String) {
        Log.d("Lifecycle", "${javaClass.simpleName} - $name")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        log("onCreate")
    }

    override fun onStart() {
        super.onStart()
        log("onStart")
    }

    override fun onResume() {
        super.onResume()
        log("onResume")
    }

    override fun onPause() {
        super.onPause()
        log("onPause")
    }

    override fun onStop() {
        super.onStop()
        log("onStop")
    }

    override fun onRestart() {
        super.onRestart()
        log("onRestart")
    }

    override fun onDestroy() {
        super.onDestroy()
        log("onDestroy")
    }
}