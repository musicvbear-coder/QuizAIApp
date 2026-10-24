package com.example.quizaiapp.data.service

import android.content.Context
import android.net.Uri
import com.example.quizaiapp.data.local.dao.NganHangDao
import com.example.quizaiapp.data.local.entity.CauHoi
import com.example.quizaiapp.data.local.entity.PhuongAnTraLoi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

class WordParserService(
    private val context: Context,
    private val nganHangDao: NganHangDao
) {

    suspend fun parseAndImportFile(uri: Uri, subjectName: String): Result<Int> {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: throw Exception("Không thể mở tệp tài liệu.")

                val reader = BufferedReader(InputStreamReader(inputStream))
                val lines = mutableListOf<String>()
                var line: String? = reader.readLine()
                while (line != null) {
                    val trimmed = line.trim()
                    if (trimmed.isNotEmpty()) {
                        lines.add(trimmed)
                    }
                    line = reader.readLine()
                }
                reader.close()
                inputStream.close()

                val parsedQuestions = mutableListOf<Pair<CauHoi, List<PhuongAnTraLoi>>>()

                var currentQuestionText = ""
                var currentA = ""
                var currentB = ""
                var currentC = ""
                var currentD = ""
                var currentCorrect = "A"

                fun saveCurrentIfValid() {
                    if (currentQuestionText.isNotEmpty() && currentA.isNotEmpty() && currentB.isNotEmpty()) {
                        val cauHoi = CauHoi(
                            idChuong = 0,
                            noiDungCauHoi = currentQuestionText,
                            mucDoKho = 1,
                            dapAnDung = currentCorrect
                        )
                        val phuongAnList = listOf(
                            PhuongAnTraLoi(idCauHoi = 0, nhanPhuongAn = "A. $currentA"),
                            PhuongAnTraLoi(idCauHoi = 0, nhanPhuongAn = "B. $currentB"),
                            PhuongAnTraLoi(idCauHoi = 0, nhanPhuongAn = "C. $currentC"),
                            PhuongAnTraLoi(idCauHoi = 0, nhanPhuongAn = "D. $currentD")
                        )
                        parsedQuestions.add(Pair(cauHoi, phuongAnList))

                        currentQuestionText = ""
                        currentA = ""
                        currentB = ""
                        currentC = ""
                        currentD = ""
                        currentCorrect = "A"
                    }
                }

                for (l in lines) {
                    val lower = l.lowercase()
                    if (lower.startsWith("câu ") || lower.startsWith("cau ") || lower.startsWith("question ")) {
                        saveCurrentIfValid()
                        currentQuestionText = l.substringAfter(":").trim().ifEmpty { l }
                    } else if (l.startsWith("A.") || l.startsWith("A/")) {
                        currentA = l.substring(2).trim()
                    } else if (l.startsWith("B.") || l.startsWith("B/")) {
                        currentB = l.substring(2).trim()
                    } else if (l.startsWith("C.") || l.startsWith("C/")) {
                        currentC = l.substring(2).trim()
                    } else if (l.startsWith("D.") || l.startsWith("D/")) {
                        currentD = l.substring(2).trim()
                    } else if (lower.contains("dáp án:") || lower.contains("đáp án:") || lower.contains("ans:")) {
                        val ansChar = l.substringAfter(":").trim().uppercase()
                        if (ansChar in listOf("A", "B", "C", "D")) {
                            currentCorrect = ansChar
                        }
                    }
                }

                saveCurrentIfValid()

                if (parsedQuestions.isNotEmpty()) {
                    nganHangDao.luuBoCauHoiVaoNganHang(
                        tenMonHoc = subjectName,
                        tenChuong = "Tài liệu đính kèm - $subjectName",
                        danhSachCauHoi = parsedQuestions
                    )
                }

                Result.success(parsedQuestions.size)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}