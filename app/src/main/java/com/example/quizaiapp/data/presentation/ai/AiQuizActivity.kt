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
import androidx.appcompat.app.AlertDialog
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
    private var uploadedPdfBytes: ByteArray? = null

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
        val btnGenerateOffline: Button,
        val progressBar: ProgressBar
    )

    private var ui: UIComponents? = null

    private val docPickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { fileUri ->
            lifecycleScope.launch {
                Toast.makeText(this@AiQuizActivity, "Đang đọc nội dung tài liệu...", Toast.LENGTH_SHORT).show()
                val database = KetNoiCoSoDuLieu.layInstance(applicationContext)
                val parser = WordParserService(applicationContext, database.nganHangDao())
                val (fileName, mimeType) = parser.getFileInfo(fileUri)

                uploadedDocContent = ""
                uploadedPdfBytes = null

                val isPdf = fileName.lowercase().endsWith(".pdf") || mimeType.lowercase().contains("pdf")

                if (isPdf) {
                    val bytes = parser.readBytesFromUri(fileUri)
                    if (bytes == null) {
                        Toast.makeText(this@AiQuizActivity, "Không thể đọc tệp PDF hoặc dung lượng vượt quá 15MB.", Toast.LENGTH_LONG).show()
                        ui?.tvDocName?.text = "Chưa chọn tài liệu đính kèm"
                    } else {
                        uploadedPdfBytes = bytes
                        val displayName = if (fileName.isNotBlank()) fileName else "Tài liệu.pdf"
                        val sizeKb = bytes.size / 1024
                        ui?.tvDocName?.text = "Đã đính kèm PDF: $displayName ($sizeKb KB)"
                        Toast.makeText(this@AiQuizActivity, "Đã đính kèm tài liệu PDF thành công!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    try {
                        val text = parser.extractRawTextFromUri(fileUri, maxChars = 150000)
                        if (text.isNotBlank()) {
                            uploadedDocContent = text
                            val displayName = if (fileName.isNotBlank()) fileName else "Tài liệu"
                            ui?.tvDocName?.text = "Đã đính kèm: $displayName (${text.length} ký tự)"
                            if (text.length >= 150000) {
                                Toast.makeText(this@AiQuizActivity, "Tài liệu dài, đã được cắt bớt ở 150.000 ký tự.", Toast.LENGTH_LONG).show()
                            }
                            Toast.makeText(this@AiQuizActivity, "Đã đính kèm tài liệu thành công ($displayName)!", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        ui?.tvDocName?.text = "Chưa chọn tài liệu đính kèm"
                        Toast.makeText(this@AiQuizActivity, "Lỗi đọc tệp: ${e.message}", Toast.LENGTH_LONG).show()
                    }
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
            btnGenerateOffline = findViewById(R.id.btnGenerateOffline),
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
            val countStr = ui?.edtCount?.text?.toString()?.trim() ?: ""
            val count = countStr.toIntOrNull()

            if (count == null || count < 1 || count > 50) {
                Toast.makeText(this@AiQuizActivity, "Số lượng câu hỏi phải từ 1 đến 50.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.taoDeThiBangAi(topic, count, uploadedDocContent, uploadedPdfBytes)
        }

        ui?.btnGenerateOffline?.setOnClickListener {
            val topic = ui?.edtTopic?.text?.toString()?.trim() ?: ""
            val countStr = ui?.edtCount?.text?.toString()?.trim() ?: ""
            val count = countStr.toIntOrNull()

            if (count == null || count < 1 || count > 50) {
                Toast.makeText(this@AiQuizActivity, "Số lượng câu hỏi phải từ 1 đến 50.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.taoDeThiMauOffline(topic, count, uploadedDocContent)
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
                            ui?.btnGenerateOffline?.isEnabled = true
                        }
                        is AiQuizUiState.Loading -> {
                            ui?.progressBar?.visibility = View.VISIBLE
                            ui?.btnGenerate?.isEnabled = false
                            ui?.btnGenerateOffline?.isEnabled = false
                        }
                        is AiQuizUiState.Success -> {
                            ui?.progressBar?.visibility = View.GONE
                            ui?.btnGenerate?.isEnabled = true
                            ui?.btnGenerateOffline?.isEnabled = true

                            val idDeThi = state.idDeThi
                            viewModel.resetState()

                            val intent = Intent(this@AiQuizActivity, AiQuizPreviewActivity::class.java).apply {
                                putExtra("EXTRA_ID_DE_THI", idDeThi)
                            }
                            startActivity(intent)
                        }
                        is AiQuizUiState.Error -> {
                            ui?.progressBar?.visibility = View.GONE
                            ui?.btnGenerate?.isEnabled = true
                            ui?.btnGenerateOffline?.isEnabled = true

                            val message = state.message
                            viewModel.resetState()

                            showErrorDialog(message)
                        }
                    }
                }
            }
        }
    }

    private fun showErrorDialog(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Lỗi tạo đề thi AI")
            .setMessage(message)
            .setPositiveButton("Thử lại") { dialog, _ ->
                dialog.dismiss()
                val topic = ui?.edtTopic?.text?.toString()?.trim() ?: ""
                val countStr = ui?.edtCount?.text?.toString()?.trim() ?: ""
                val count = countStr.toIntOrNull() ?: 10
                if (count in 1..50) {
                    viewModel.taoDeThiBangAi(topic, count, uploadedDocContent, uploadedPdfBytes)
                }
            }
            .setNeutralButton("Dùng đề mẫu offline") { dialog, _ ->
                dialog.dismiss()
                val topic = ui?.edtTopic?.text?.toString()?.trim() ?: ""
                val countStr = ui?.edtCount?.text?.toString()?.trim() ?: ""
                val count = countStr.toIntOrNull() ?: 10
                val validCount = if (count in 1..50) count else 10
                viewModel.taoDeThiMauOffline(topic, validCount, uploadedDocContent)
            }
            .setNegativeButton("Đóng") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}
