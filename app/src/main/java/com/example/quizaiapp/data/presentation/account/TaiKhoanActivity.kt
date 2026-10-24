package com.example.quizaiapp.data.presentation.account

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.quizaiapp.R
import com.example.quizaiapp.data.local.KetNoiCoSoDuLieu
import com.example.quizaiapp.data.local.SessionManager
import com.example.quizaiapp.data.presentation.auth.LoginActivity
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.Locale

class TaiKhoanActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tai_khoan)

        val sessionManager = SessionManager(applicationContext)

        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val tvAvatarBadge = findViewById<TextView>(R.id.tvAvatarBadge)
        val tvUserName = findViewById<TextView>(R.id.tvUserName)
        val tvUserEmail = findViewById<TextView>(R.id.tvUserEmail)
        val tvTotalExams = findViewById<TextView>(R.id.tvTotalExams)
        val tvAvgScore = findViewById<TextView>(R.id.tvAvgScore)
        val tvTotalBankQuestions = findViewById<TextView>(R.id.tvTotalBankQuestions)
        val btnLogout = findViewById<Button>(R.id.btnLogout)

        val userName = sessionManager.layTenNguoiDung()
        val userEmail = sessionManager.layEmailNguoiDung()

        tvUserName.text = userName
        tvUserEmail.text = if (userEmail.isNotEmpty()) userEmail else "teacher@example.com"

        if (userName.isNotBlank()) {
            val words = userName.trim().split(" ")
            val initials = if (words.size >= 2) {
                "${words.first().take(1)}${words.last().take(1)}".uppercase()
            } else {
                userName.take(2).uppercase()
            }
            tvAvatarBadge.text = initials
        }

        btnBack.setOnClickListener {
            finish()
        }

        val db = KetNoiCoSoDuLieu.layInstance(applicationContext)
        lifecycleScope.launch {
            val historyList = db.truyVanDeThiDao().layToanBoLichSuDeThi().firstOrNull() ?: emptyList()
            val totalExams = historyList.size
            val avgScore = if (totalExams > 0) {
                historyList.map { it.diemDatDuoc }.average()
            } else {
                0.0
            }

            val bankQuestions = db.nganHangDao().layToanBoCauHoi()

            tvTotalExams.text = totalExams.toString()
            tvAvgScore.text = String.format(Locale.getDefault(), "%.1f", avgScore)
            tvTotalBankQuestions?.text = bankQuestions.size.toString()
        }

        btnLogout.setOnClickListener {
            sessionManager.dangXuat()
            Toast.makeText(this, "Đã đăng xuất thành công!", Toast.LENGTH_SHORT).show()

            val intent = Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }
    }
}