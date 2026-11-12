package com.example.quizaiapp

import com.example.quizaiapp.data.local.dao.TruyVanDeThiDao
import com.example.quizaiapp.data.local.entity.ChiTietCauHoiAi
import com.example.quizaiapp.data.local.entity.LichSuDeThi
import com.example.quizaiapp.data.repository.GeminiRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiRepositoryTest {

    @Test
    fun testTaoVaLuuDeThiAi_withoutApiKey_returnsFailure() = runBlocking {
        val dummyDao = DummyTruyVanDeThiDao()
        val repo = GeminiRepository(dummyDao, apiKey = "")

        val result = repo.taoVaLuuDeThiAi(chuDe = "Lịch sử Việt Nam", soLuongCau = 2)

        assertTrue(result.isFailure)
        assertEquals("Chưa cấu hình GEMINI_API_KEY", result.exceptionOrNull()?.message)
    }

    @Test
    fun testTaoVaLuuDeThiMauOffline_withDocContent_createsSampleQuestions() = runBlocking {
        val dummyDao = DummyTruyVanDeThiDao()
        val repo = GeminiRepository(dummyDao, apiKey = "")

        val docText = "Chiến dịch Hồ Chí Minh là chiến dịch cuối cùng của cuộc Tổng tiến công và nổi dậy mùa Xuân năm 1975."

        val result = repo.taoVaLuuDeThiMauOffline(chuDe = "Lịch sử Việt Nam", soLuongCau = 2, docContent = docText)

        assertTrue(result.isSuccess)
        assertTrue(dummyDao.savedQuestions.isNotEmpty())
        assertEquals(2, dummyDao.savedQuestions.size)

        val firstQ = dummyDao.savedQuestions[0].noiDungCauHoi
        assertTrue(firstQ.contains("Chiến dịch Hồ Chí Minh") || firstQ.contains("1975") || firstQ.contains("Tổng tiến công"))

        val savedExam = dummyDao.savedExam
        assertTrue(savedExam != null && savedExam.chuDeKiemsTra.contains("(Đề mẫu, không phải do AI tạo)"))
    }
}

private class DummyTruyVanDeThiDao : TruyVanDeThiDao {
    val savedQuestions = mutableListOf<ChiTietCauHoiAi>()
    var savedExam: LichSuDeThi? = null

    override suspend fun luuLichSuDeThi(deThi: LichSuDeThi) {
        savedExam = deThi
    }

    override suspend fun luuDanhSachCauHoiAi(danhSachCauHoi: List<ChiTietCauHoiAi>) {
        savedQuestions.addAll(danhSachCauHoi)
    }

    override fun layToanBoLichSuDeThi(): Flow<List<LichSuDeThi>> = flowOf(emptyList())

    override suspend fun layDeThiTheoId(idDeThi: String): LichSuDeThi? = savedExam

    override suspend fun layDanhSachCauHoiTheoDeThi(idDeThi: String): List<ChiTietCauHoiAi> = savedQuestions

    override suspend fun capNhatDiemSo(idDeThi: String, diemMoi: Float) {}
}
