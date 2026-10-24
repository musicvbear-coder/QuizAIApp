package com.example.quizaiapp.data.presentation.result

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.quizaiapp.R
import com.example.quizaiapp.data.presentation.main.MainActivity

class ResultActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        val tvScore = findViewById<TextView>(R.id.tvScore)
        val tvSummary = findViewById<TextView>(R.id.tvSummary)
        val btnBackHome = findViewById<Button>(R.id.btnBackHome)

        val diemSo = intent.getFloatExtra("EXTRA_TONG_DIEM", 0f)
        val soCauDung = intent.getIntExtra("EXTRA_SO_CAU_DUNG", 0)
        val tongSoCau = intent.getIntExtra("EXTRA_TONG_CAU", 0)

        tvScore.text = String.format(java.util.Locale.getDefault(), "%.1f / 10", diemSo)
        tvSummary.text = "Số câu trả lời đúng: $soCauDung / $tongSoCau câu"

        btnBackHome.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }
}