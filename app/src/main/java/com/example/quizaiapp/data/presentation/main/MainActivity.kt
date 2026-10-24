package com.example.quizaiapp.data.presentation.main

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.quizaiapp.R
import com.example.quizaiapp.data.presentation.account.TaiKhoanFragment
import com.example.quizaiapp.data.presentation.ai.AiQuizActivity
import com.example.quizaiapp.data.presentation.nganhang.NganHangFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private var bottomNavigationView: BottomNavigationView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNavigationView = findViewById(R.id.bottomNavigationView)

        if (savedInstanceState == null) {
            replaceFragment(TrangChuFragment())
        }

        bottomNavigationView?.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    replaceFragment(TrangChuFragment())
                    true
                }
                R.id.nav_bank -> {
                    replaceFragment(NganHangFragment())
                    true
                }
                R.id.nav_ai -> {
                    startActivity(Intent(this, AiQuizActivity::class.java))
                    false
                }
                R.id.nav_account -> {
                    replaceFragment(TaiKhoanFragment())
                    true
                }
                else -> false
            }
        }
    }

    fun switchFragmentByMenuId(menuId: Int) {
        bottomNavigationView?.selectedItemId = menuId
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}