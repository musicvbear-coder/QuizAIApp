package com.example.quizaiapp.data.presentation.ai

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.quizaiapp.R
import com.example.quizaiapp.data.local.KetNoiCoSoDuLieu
import com.example.quizaiapp.data.local.entity.CauHoi
import com.example.quizaiapp.data.local.entity.PhuongAnTraLoi
import com.example.quizaiapp.data.presentation.main.MainActivity
import com.example.quizaiapp.data.presentation.quizz.QuizActivity
import com.example.quizaiapp.data.repository.FirebaseRepository
import com.example.quizaiapp.data.service.PdfGeneratorService
import kotlinx.coroutines.launch

class AiQuizPreviewActivity : AppCompatActivity() {

    private val idDeThi by lazy { intent.getStringExtra("EXTRA_ID_DE_THI") ?: "" }
    private val firebaseRepo by lazy { FirebaseRepository() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ai_quiz_preview)

        val tvExamInfo = findViewById<TextView>(R.id.tvExamInfo)
        val recyclerViewAiPreview = findViewById<RecyclerView>(R.id.recyclerViewAiPreview)
        val btnLamBaiNgay = findViewById<Button>(R.id.btnLamBaiNgay)
        val btnTaoPhongThi = findViewById<Button>(R.id.btnTaoPhongThi)
        val btnXuatFileDeThi = findViewById<Button>(R.id.btnXuatFileDeThi)
        val btnLuuNganHang = findViewById<Button>(R.id.btnLuuNganHang)
        val btnTaoLai = findViewById<Button>(R.id.btnTaoLai)
        val btnBackHome = findViewById<Button>(R.id.btnBackHome)

        recyclerViewAiPreview.layoutManager = LinearLayoutManager(this)

        val db = KetNoiCoSoDuLieu.layInstance(applicationContext)

        lifecycleScope.launch {
            val deThi = db.truyVanDeThiDao().layDeThiTheoId(idDeThi)
            val questions = db.truyVanDeThiDao().layDanhSachCauHoiTheoDeThi(idDeThi)

            if (deThi != null) {
                tvExamInfo.text = "Chủ đề: ${deThi.chuDeKiemsTra} · ${questions.size} câu · Hỗn hợp · Gợi ý: 15 phút"
                firebaseRepo.luuLichSuDeThiLenCloud(deThi, questions)
            }

            recyclerViewAiPreview.adapter = AiQuizPreviewAdapter(questions)

            // 1. Nút Làm bài thi ngay
            btnLamBaiNgay.setOnClickListener {
                val intent = Intent(this@AiQuizPreviewActivity, QuizActivity::class.java).apply {
                    putExtra("EXTRA_ID_DE_THI", idDeThi)
                }
                startActivity(intent)
                finish()
            }

            // 2. Nút Tạo phòng thi & Chia sẻ Mã
            btnTaoPhongThi.setOnClickListener {
                lifecycleScope.launch {
                    if (deThi != null && questions.isNotEmpty()) {
                        Toast.makeText(this@AiQuizPreviewActivity, "Đang khởi tạo phòng thi Đám mây...", Toast.LENGTH_SHORT).show()
                        val roomCode = firebaseRepo.taoPhongThi(deThi, questions)
                        Toast.makeText(
                            this@AiQuizPreviewActivity,
                            "🔑 Mã phòng thi của bạn là: #$roomCode. Hãy chia sẻ cho học sinh/bạn học!",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }

            // 3. Nút Xuất file đề thi (PDF)
            btnXuatFileDeThi.setOnClickListener {
                lifecycleScope.launch {
                    if (deThi != null && questions.isNotEmpty()) {
                        Toast.makeText(this@AiQuizPreviewActivity, "Đang xuất file PDF...", Toast.LENGTH_SHORT).show()
                        val pdfService = PdfGeneratorService(applicationContext)
                        val result = pdfService.xuatFilePdfDeThiVaDapAn(deThi, questions)
                        if (result.isSuccess) {
                            Toast.makeText(
                                this@AiQuizPreviewActivity,
                                "Đã xuất file PDF thành công vào thư mục Downloads!",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            Toast.makeText(
                                this@AiQuizPreviewActivity,
                                "Lỗi khi xuất PDF: ${result.exceptionOrNull()?.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }

            // 4. Nút Lưu vào Ngân hàng câu hỏi
            btnLuuNganHang.setOnClickListener {
                lifecycleScope.launch {
                    if (deThi != null && questions.isNotEmpty()) {
                        val topic = deThi.chuDeKiemsTra
                        val mappedQuestions = questions.map { chiTiet ->
                            val cauHoi = CauHoi(
                                idChuong = 0,
                                noiDungCauHoi = chiTiet.noiDungCauHoi,
                                mucDoKho = 1,
                                dapAnDung = chiTiet.dapAnChinhXac
                            )
                            val phuongAnList = listOf(
                                PhuongAnTraLoi(idCauHoi = 0, nhanPhuongAn = "A. ${chiTiet.phuongAnA}"),
                                PhuongAnTraLoi(idCauHoi = 0, nhanPhuongAn = "B. ${chiTiet.phuongAnB}"),
                                PhuongAnTraLoi(idCauHoi = 0, nhanPhuongAn = "C. ${chiTiet.phuongAnC}"),
                                PhuongAnTraLoi(idCauHoi = 0, nhanPhuongAn = "D. ${chiTiet.phuongAnD}")
                            )
                            firebaseRepo.luuCauHoiNganHangLenCloud(cauHoi)
                            Pair(cauHoi, phuongAnList)
                        }

                        db.nganHangDao().luuBoCauHoiVaoNganHang(
                            tenMonHoc = topic,
                            tenChuong = "Bài tập AI - $topic",
                            danhSachCauHoi = mappedQuestions
                        )

                        Toast.makeText(
                            this@AiQuizPreviewActivity,
                            "Đã lưu thành công bộ câu hỏi vào Ngân hàng & Cloud!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            // 5. Nút Tạo lại
            btnTaoLai.setOnClickListener {
                finish()
            }

            // 6. Nút Quay về trang chủ
            btnBackHome.setOnClickListener {
                val intent = Intent(this@AiQuizPreviewActivity, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
                finish()
            }
        }
    }
}