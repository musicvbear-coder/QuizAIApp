package com.example.quizaiapp.data.presentation.quizz

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.quizaiapp.R
import com.example.quizaiapp.data.local.KetNoiCoSoDuLieu
import com.example.quizaiapp.data.presentation.result.ResultActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale

class QuizActivity : AppCompatActivity() {

    private val idDeThi by lazy { intent.getStringExtra("EXTRA_ID_DE_THI") ?: "" }

    private val viewModel: QuizViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val dao = KetNoiCoSoDuLieu.layInstance(applicationContext).truyVanDeThiDao()
                @Suppress("UNCHECKED_CAST")
                return QuizViewModel(dao, idDeThi) as T
            }
        }
    }

    private var btnCloseQuiz: ImageView? = null
    private var tvQuestionProgress: TextView? = null
    private var tvTimer: TextView? = null
    private var progressBarQuiz: ProgressBar? = null
    private var tvQuizQuestionContent: TextView? = null

    private var layoutOptionA: LinearLayout? = null
    private var tvCircleA: TextView? = null
    private var tvOptionA: TextView? = null

    private var layoutOptionB: LinearLayout? = null
    private var tvCircleB: TextView? = null
    private var tvOptionB: TextView? = null

    private var layoutOptionC: LinearLayout? = null
    private var tvCircleC: TextView? = null
    private var tvOptionC: TextView? = null

    private var layoutOptionD: LinearLayout? = null
    private var tvCircleD: TextView? = null
    private var tvOptionD: TextView? = null

    private var btnPrevious: Button? = null
    private var btnNextOrSubmit: Button? = null

    private var countDownTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz)

        initViews()
        setupTimer(15 * 60 * 1000L)
        observeViewModel()
        setupListeners()
    }

    private fun initViews() {
        btnCloseQuiz = findViewById(R.id.btnCloseQuiz)
        tvQuestionProgress = findViewById(R.id.tvQuestionProgress)
        tvTimer = findViewById(R.id.tvTimer)
        progressBarQuiz = findViewById(R.id.progressBarQuiz)
        tvQuizQuestionContent = findViewById(R.id.tvQuizQuestionContent)

        layoutOptionA = findViewById(R.id.layoutOptionA)
        tvCircleA = findViewById(R.id.tvCircleA)
        tvOptionA = findViewById(R.id.tvOptionA)

        layoutOptionB = findViewById(R.id.layoutOptionB)
        tvCircleB = findViewById(R.id.tvCircleB)
        tvOptionB = findViewById(R.id.tvOptionB)

        layoutOptionC = findViewById(R.id.layoutOptionC)
        tvCircleC = findViewById(R.id.tvCircleC)
        tvOptionC = findViewById(R.id.tvOptionC)

        layoutOptionD = findViewById(R.id.layoutOptionD)
        tvCircleD = findViewById(R.id.tvCircleD)
        tvOptionD = findViewById(R.id.tvOptionD)

        btnPrevious = findViewById(R.id.btnPrevious)
        btnNextOrSubmit = findViewById(R.id.btnNextOrSubmit)

        btnCloseQuiz?.setOnClickListener {
            finish()
        }
    }

    private fun setupTimer(millisInFuture: Long) {
        countDownTimer = object : CountDownTimer(millisInFuture, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val minutes = (millisUntilFinished / 1000) / 60
                val seconds = (millisUntilFinished / 1000) % 60
                tvTimer?.text = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
            }

            override fun onFinish() {
                Toast.makeText(this@QuizActivity, "Hết thời gian làm bài!", Toast.LENGTH_LONG).show()
                nopBaiThi()
            }
        }.start()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.cauHoiList.collectLatest { list ->
                if (list.isNotEmpty()) {
                    progressBarQuiz?.max = list.size
                    updateQuestionUI(viewModel.currentIndex.value)
                }
            }
        }

        lifecycleScope.launch {
            viewModel.currentIndex.collectLatest { index ->
                updateQuestionUI(index)
            }
        }
    }

    private fun updateQuestionUI(index: Int) {
        val list = viewModel.cauHoiList.value
        if (list.isEmpty() || index !in list.indices) return

        val cauHoi = list[index]
        tvQuestionProgress?.text = "Câu ${index + 1} / ${list.size}"
        progressBarQuiz?.progress = index + 1
        tvQuizQuestionContent?.text = cauHoi.noiDungCauHoi

        tvOptionA?.text = cauHoi.phuongAnA
        tvOptionB?.text = cauHoi.phuongAnB
        tvOptionC?.text = cauHoi.phuongAnC
        tvOptionD?.text = cauHoi.phuongAnD

        resetOptionStyles()
        when (viewModel.layDapAnUser(index)) {
            "A" -> applySelectedStyle(layoutOptionA, tvCircleA)
            "B" -> applySelectedStyle(layoutOptionB, tvCircleB)
            "C" -> applySelectedStyle(layoutOptionC, tvCircleC)
            "D" -> applySelectedStyle(layoutOptionD, tvCircleD)
        }

        btnPrevious?.isEnabled = index > 0
        btnNextOrSubmit?.text = if (index == list.size - 1) "Nộp bài ✓" else "Câu tiếp →"
    }

    private fun setupListeners() {
        val currentIndex = { viewModel.currentIndex.value }

        layoutOptionA?.setOnClickListener {
            viewModel.chonDapAn(currentIndex(), "A")
            highlightSelected("A")
        }
        layoutOptionB?.setOnClickListener {
            viewModel.chonDapAn(currentIndex(), "B")
            highlightSelected("B")
        }
        layoutOptionC?.setOnClickListener {
            viewModel.chonDapAn(currentIndex(), "C")
            highlightSelected("C")
        }
        layoutOptionD?.setOnClickListener {
            viewModel.chonDapAn(currentIndex(), "D")
            highlightSelected("D")
        }

        btnPrevious?.setOnClickListener {
            viewModel.setIndex(currentIndex() - 1)
        }

        btnNextOrSubmit?.setOnClickListener {
            val listSize = viewModel.cauHoiList.value.size
            if (currentIndex() < listSize - 1) {
                viewModel.setIndex(currentIndex() + 1)
            } else {
                nopBaiThi()
            }
        }
    }

    private fun highlightSelected(answer: String) {
        resetOptionStyles()
        when (answer) {
            "A" -> applySelectedStyle(layoutOptionA, tvCircleA)
            "B" -> applySelectedStyle(layoutOptionB, tvCircleB)
            "C" -> applySelectedStyle(layoutOptionC, tvCircleC)
            "D" -> applySelectedStyle(layoutOptionD, tvCircleD)
        }
    }

    private fun applySelectedStyle(layout: LinearLayout?, circle: TextView?) {
        layout?.setBackgroundResource(R.drawable.bg_option_selected)
        circle?.setBackgroundResource(R.drawable.bg_purple_circle)
        circle?.setTextColor(resources.getColor(android.R.color.white, theme))
    }

    private fun resetOptionStyles() {
        val options = listOf(
            Pair(layoutOptionA, tvCircleA),
            Pair(layoutOptionB, tvCircleB),
            Pair(layoutOptionC, tvCircleC),
            Pair(layoutOptionD, tvCircleD)
        )
        for ((layout, circle) in options) {
            layout?.setBackgroundResource(R.drawable.bg_option_normal)
            circle?.setBackgroundResource(R.drawable.bg_circle_gray)
            circle?.setTextColor(resources.getColor(android.R.color.darker_gray, theme))
        }
    }

    private fun nopBaiThi() {
        countDownTimer?.cancel()
        val (soCauDung, tongDiem) = viewModel.tinhKetQua()

        lifecycleScope.launch {
            val dao = KetNoiCoSoDuLieu.layInstance(applicationContext).truyVanDeThiDao()
            dao.capNhatDiemSo(idDeThi, tongDiem)

            val intent = Intent(this@QuizActivity, ResultActivity::class.java).apply {
                putExtra("EXTRA_ID_DE_THI", idDeThi)
                putExtra("EXTRA_SO_CAU_DUNG", soCauDung)
                putExtra("EXTRA_TONG_DIEM", tongDiem)
                putExtra("EXTRA_TONG_CAU", viewModel.cauHoiList.value.size)
            }
            startActivity(intent)
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
}