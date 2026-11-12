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
                        <w:t>Thu do cua Viet Nam la Ha Noi.</w:t>
                    </w:p>
                    <w:p>
                        <w:t>Cach mang thang Tam dien ra nam 1945.</w:t>
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
        assertTrue(extractedText.contains("Thu do cua Viet Nam la Ha Noi."))
        assertTrue(extractedText.contains("Cach mang thang Tam dien ra nam 1945."))
    }

    @Test
    fun testExtractDocxFromBytes_handlesTabAndBrTags() {
        val bos = ByteArrayOutputStream()
        val zos = ZipOutputStream(bos)

        val xmlContent = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
                <w:body>
                    <w:p>
                        <w:t>Tieu de</w:t>
                        <w:tab/>
                        <w:t>Noi dung</w:t>
                        <w:br/>
                        <w:t>Dong moi</w:t>
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
        assertTrue(extractedText.contains("Tieu de"))
        assertTrue(extractedText.contains("Noi dung"))
        assertTrue(extractedText.contains("Dong moi"))
    }

    @Test
    fun testExtractPlainTextFromBytes_returnsCorrectText() {
        val text = "Bai hoc ve Chien dich Dien Bien Phu nam 1954 keo dai 56 ngay dem."
        val bytes = text.toByteArray(StandardCharsets.UTF_8)

        val extracted = dummyService.extractPlainTextFromBytes(bytes)
        assertTrue(extracted.contains("Chien dich Dien Bien Phu"))
    }

    @Test
    fun testExtractPdfFromBytes_extractsTextFromDecompressedStream() {
        val pdfStreamContent = "BT /F1 12 Tf (Ky thi tot nghiep THPT Quoc Gia nam 2025) Tj ET"
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
        assertTrue(extracted.contains("Ky thi tot nghiep THPT Quoc Gia nam 2025"))
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
