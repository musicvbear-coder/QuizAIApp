package com.example.quizaiapp.data.presentation.ai

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.quizaiapp.R
import com.example.quizaiapp.data.local.KetNoiCoSoDuLieu
import com.example.quizaiapp.data.repository.GeminiRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AiQuizActivity : AppCompatActivity() {

    private val viewModel: AiQuizViewModel by viewModels {
        val database = KetNoiCoSoDuLieu.layInstance(applicationContext)
        val apiKey = "AQ.Ab8RN6JxktdcaS39qNyV06agH5s_5AkPRDKgLyz61Qq9oI3-WQ"
        val repository = GeminiRepository(database.truyVanDeThiDao(), apiKey)
        object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return AiQuizViewModel(repository) as T
            }
        }
    }

    private class UIComponents(
        val btnBack: ImageView,
        val edtTopic: EditText,
        val edtCount: EditText,
        val btnGenerate: Button,
        val progressBar: ProgressBar
    )

    private var ui: UIComponents? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ai_quiz)

        ui = UIComponents(
            btnBack = findViewById(R.id.btnBack),
            edtTopic = findViewById(R.id.edtTopic),
            edtCount = findViewById(R.id.edtCount),
            btnGenerate = findViewById(R.id.btnGenerate),
            progressBar = findViewById(R.id.progressBar)
        )

        val passedTopic = intent.getStringExtra("EXTRA_TOPIC") ?: ""
        val passedCount = intent.getIntExtra("EXTRA_COUNT", 10)

        if (passedTopic.isNotBlank()) {
            ui?.edtTopic?.setText(passedTopic)
        }
        ui?.edtCount?.setText(passedCount.toString())

        ui?.btnBack?.setOnClickListener {
            finish()
        }

        ui?.btnGenerate?.setOnClickListener {
            val topic = ui?.edtTopic?.text?.toString()?.trim() ?: ""
            val countStr = ui?.edtCount?.text?.toString()?.trim() ?: "10"
            val count = countStr.toIntOrNull() ?: 10

            viewModel.taoDeThiBangAi(topic, count)
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    when (state) {
                        is AiQuizUiState.Idle -> {
                            ui?.progressBar?.visibility = View.GONE
                            ui?.btnGenerate?.isEnabled = true
                        }
                        is AiQuizUiState.Loading -> {
                            ui?.progressBar?.visibility = View.VISIBLE
                            ui?.btnGenerate?.isEnabled = false
                        }
                        is AiQuizUiState.Success -> {
                            ui?.progressBar?.visibility = View.GONE
                            ui?.btnGenerate?.isEnabled = true

                            val intent = Intent(this@AiQuizActivity, AiQuizPreviewActivity::class.java).apply {
                                putExtra("EXTRA_ID_DE_THI", state.idDeThi)
                            }
                            startActivity(intent)
                        }
                        is AiQuizUiState.Error -> {
                            ui?.progressBar?.visibility = View.GONE
                            ui?.btnGenerate?.isEnabled = true
                            Toast.makeText(
                                this@AiQuizActivity,
                                "Lỗi: ${state.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        }
    }
}