package com.example.quizaiapp.data.presentation.ai

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.quizaiapp.R
import com.example.quizaiapp.data.local.KetNoiCoSoDuLieu
import com.example.quizaiapp.data.repository.GeminiRepository
import com.example.quizaiapp.data.service.WordParserService
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AiQuizActivity : AppCompatActivity() {

    private var uploadedDocContent = ""

    private val viewModel: AiQuizViewModel by viewModels {
        val database = KetNoiCoSoDuLieu.layInstance(applicationContext)
        val apiKey = com.example.quizaiapp.BuildConfig.GEMINI_API_KEY
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
        val btnUploadDoc: Button,
        val tvDocName: TextView,
        val edtTopic: EditText,
        val edtCount: EditText,
        val btnGenerate: Button,
        val progressBar: ProgressBar
    )

    private var ui: UIComponents? = null

    private val docPickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { fileUri ->
            lifecycleScope.launch {
                Toast.makeText(this@AiQuizActivity, "Đang đọc nội dung tài liệu...", Toast.LENGTH_SHORT).show()
                val database = KetNoiCoSoDuLieu.layInstance(applicationContext)
                val parser = WordParserService(applicationContext, database.nganHangDao())
                val (fileName, _) = parser.getFileInfo(fileUri)
                val text = parser.extractRawTextFromUri(fileUri)
                if (text.isNotBlank()) {
                    uploadedDocContent = text
                    val displayName = if (fileName.isNotBlank()) fileName else "Tài liệu"
                    ui?.tvDocName?.text = "Đã đính kèm: $displayName (${text.length} ký tự)"
                    Toast.makeText(this@AiQuizActivity, "Đã đính kèm tài liệu thành công ($displayName)!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@AiQuizActivity, "Không thể trích xuất văn bản từ tệp đã chọn.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ai_quiz)

        ui = UIComponents(
            btnBack = findViewById(R.id.btnBack),
            btnUploadDoc = findViewById(R.id.btnUploadDoc),
            tvDocName = findViewById(R.id.tvDocName),
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

        ui?.btnUploadDoc?.setOnClickListener {
            docPickerLauncher.launch("*/*")
        }

        ui?.btnGenerate?.setOnClickListener {
            val topic = ui?.edtTopic?.text?.toString()?.trim() ?: ""
            val countStr = ui?.edtCount?.text?.toString()?.trim() ?: "10"
            val count = countStr.toIntOrNull() ?: 10

            viewModel.taoDeThiBangAi(topic, count, uploadedDocContent)
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