package com.example.quizaiapp

import com.example.quizaiapp.data.local.dao.NganHangDao
import com.example.quizaiapp.data.local.entity.CauHoi
import com.example.quizaiapp.data.local.entity.ChuongHoc
import com.example.quizaiapp.data.local.entity.MonHoc
import com.example.quizaiapp.data.local.entity.PhuongAnTraLoi
import com.example.quizaiapp.data.service.WordParserService
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class WordParserServiceTest {

    private val dummyService = WordParserService(
        context = DummyContext(),
        nganHangDao = DummyNganHangDao()
    )

    @Test
    fun testExtractDocxFromBytes_returnsCorrectParagraphText() {
        val bos = ByteArrayOutputStream()
        val zos = ZipOutputStream(bos)

        val xmlContent = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
                <w:body>
                    <w:p>
                        <w:t>Thủ đô của Việt Nam là Hà Nội.</w:t>
                    </w:p>
                    <w:p>
                        <w:t>Cách mạng tháng Tám diễn ra năm 1945.</w:t>
                    </w:p>
                </w:body>
            </w:document>
        """.trimIndent()

        zos.putNextEntry(ZipEntry("word/document.xml"))
        zos.write(xmlContent.toByteArray(StandardCharsets.UTF_8))
        zos.closeEntry()
        zos.close()

        val docxBytes = bos.toByteArray()
        val extractedText = dummyService.extractDocxFromBytes(docxBytes)
        assertTrue(extractedText.contains("Thủ đô của Việt Nam là Hà Nội."))
        assertTrue(extractedText.contains("Cách mạng tháng Tám diễn ra năm 1945."))
    }

    @Test
    fun testExtractPlainTextFromBytes_returnsCorrectText() {
        val text = "Bài học về Chiến dịch Điện Biên Phủ năm 1954 kéo dài 56 ngày đêm."
        val bytes = text.toByteArray(StandardCharsets.UTF_8)

        val extracted = dummyService.extractPlainTextFromBytes(bytes)
        assertTrue(extracted.contains("Chiến dịch Điện Biên Phủ"))
    }

    @Test
    fun testExtractPdfFromBytes_extractsTextFromDecompressedStream() {
        val pdfStreamContent = "BT /F1 12 Tf (Kỳ thi tốt nghiệp THPT Quốc Gia năm 2025) Tj ET"
        val rawPdf = """
            1 0 obj
            << /Type /Catalog >>
            endobj
            2 0 obj
            << /Length ${pdfStreamContent.length} >>
            stream
            $pdfStreamContent
            endstream
            endobj
        """.trimIndent()

        val pdfBytes = rawPdf.toByteArray(StandardCharsets.UTF_8)
        val extracted = dummyService.extractPdfFromBytes(pdfBytes)
        assertTrue(extracted.contains("Kỳ thi tốt nghiệp THPT Quốc Gia năm 2025"))
    }
}

private class DummyContext : android.content.ContextWrapper(null)

private class DummyNganHangDao : NganHangDao {
    override suspend fun luuMonHoc(monHoc: MonHoc): Long = 1
    override suspend fun luuChuongHoc(chuongHoc: ChuongHoc): Long = 1
    override suspend fun luuCauHoi(cauHoi: CauHoi): Long = 1
    override suspend fun luuDanhSachPhuongAn(danhSachPhuongAn: List<PhuongAnTraLoi>) {}
    override suspend fun layMonHocTheoTen(tenMonHoc: String): MonHoc? = null
    override suspend fun layChuongHocTheoTen(idMonHoc: Int, tenChuong: String): ChuongHoc? = null
    override suspend fun layToanBoCauHoi(): List<CauHoi> = emptyList()
    override suspend fun xoaCauHoi(idCauHoi: Int) {}
}
