package com.example.quizaiapp.data.service

import android.content.ContentValues
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.quizaiapp.data.local.entity.ChiTietCauHoiAi
import com.example.quizaiapp.data.local.entity.LichSuDeThi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream

class PdfGeneratorService(private val context: Context) {

    suspend fun xuatFilePdfDeThiVaDapAn(
        deThi: LichSuDeThi,
        danhSachCauHoi: List<ChiTietCauHoiAi>
    ): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                taoVaLuuPdf(
                    fileName = "De_Thi_${deThi.idDeThi}.pdf",
                    tieuDe = "ĐỀ THI TRẮC NGHIỆM: ${deThi.chuDeKiemsTra}",
                    danhSachCauHoi = danhSachCauHoi,
                    hienThiDapAn = false
                )

                taoVaLuuPdf(
                    fileName = "Dap_An_${deThi.idDeThi}.pdf",
                    tieuDe = "ĐÁP ÁN & LỜI GIẢI: ${deThi.chuDeKiemsTra}",
                    danhSachCauHoi = danhSachCauHoi,
                    hienThiDapAn = true
                )

                Result.success(true)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun taoVaLuuPdf(
        fileName: String,
        tieuDe: String,
        danhSachCauHoi: List<ChiTietCauHoiAi>,
        hienThiDapAn: Boolean
    ) {
        val pdfDocument = PdfDocument()

        val textPaint = TextPaint().apply {
            textSize = 12f
            isAntiAlias = true
            color = Color.BLACK
        }
        val titlePaint = TextPaint().apply {
            textSize = 16f
            isFakeBoldText = true
            isAntiAlias = true
            color = Color.BLACK
        }
        val boldPaint = TextPaint().apply {
            textSize = 12f
            isFakeBoldText = true
            isAntiAlias = true
            color = Color.BLACK
        }

        val pageWidth = 595
        val pageHeight = 842
        val margin = 40f
        val contentWidth = (pageWidth - 2 * margin).toInt()

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas: Canvas = page.canvas
        var yPosition = 50f

        fun createLayout(text: String, paint: TextPaint, width: Int): StaticLayout {
            return StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1f)
                .setIncludePad(false)
                .build()
        }

        fun drawLayoutBlock(layout: StaticLayout, xPosition: Float, spacingAfter: Float) {
            if (yPosition + layout.height > pageHeight - margin) {
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                yPosition = margin
            }

            canvas.save()
            canvas.translate(xPosition, yPosition)
            layout.draw(canvas)
            canvas.restore()

            yPosition += layout.height + spacingAfter
        }

        val titleLayout = createLayout(tieuDe, titlePaint, contentWidth)
        drawLayoutBlock(titleLayout, margin, 20f)

        for ((index, cauHoi) in danhSachCauHoi.withIndex()) {
            val cauHoiText = "Câu ${index + 1}: ${cauHoi.noiDungCauHoi}"
            val cauHoiLayout = createLayout(cauHoiText, boldPaint, contentWidth)
            drawLayoutBlock(cauHoiLayout, margin, 8f)

            val optionWidth = contentWidth - 20
            val optionA = createLayout("A. ${cauHoi.phuongAnA}", textPaint, optionWidth)
            drawLayoutBlock(optionA, margin + 20f, 4f)

            val optionB = createLayout("B. ${cauHoi.phuongAnB}", textPaint, optionWidth)
            drawLayoutBlock(optionB, margin + 20f, 4f)

            val optionC = createLayout("C. ${cauHoi.phuongAnC}", textPaint, optionWidth)
            drawLayoutBlock(optionC, margin + 20f, 4f)

            val optionD = createLayout("D. ${cauHoi.phuongAnD}", textPaint, optionWidth)
            drawLayoutBlock(optionD, margin + 20f, 8f)

            if (hienThiDapAn) {
                val dapAnText = "Đáp án đúng: ${cauHoi.dapAnChinhXac}"
                val dapAnLayout = createLayout(dapAnText, boldPaint, optionWidth)
                drawLayoutBlock(dapAnLayout, margin + 20f, 4f)

                val loiGiaiText = "Giải thích: ${cauHoi.loiGiaiThich}"
                val loiGiaiLayout = createLayout(loiGiaiText, textPaint, optionWidth)
                drawLayoutBlock(loiGiaiLayout, margin + 20f, 12f)
            } else {
                yPosition += 8f
            }
        }

        pdfDocument.finishPage(page)

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)

        uri?.let {
            val outputStream: OutputStream? = resolver.openOutputStream(it)
            if (outputStream != null) {
                pdfDocument.writeTo(outputStream)
                outputStream.close()
            }
        }
        pdfDocument.close()
    }
}