package com.example.quizaiapp.data.service

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.example.quizaiapp.data.local.dao.NganHangDao
import com.example.quizaiapp.data.local.entity.CauHoi
import com.example.quizaiapp.data.local.entity.PhuongAnTraLoi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern
import java.util.zip.Inflater
import java.util.zip.InflaterInputStream
import java.util.zip.ZipInputStream

class WordParserService(
    private val context: Context,
    private val nganHangDao: NganHangDao
) {

    companion object {
        private const val TAG = "WordParserService"
    }

    suspend fun readBytesFromUri(uri: Uri, maxSizeBytes: Long = 15 * 1024 * 1024): ByteArray? {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
                val bytes = inputStream.use { it.readBytes() }
                if (bytes.size > maxSizeBytes) {
                    Log.e(TAG, "Lỗi: Tệp vượt quá kích thước cho phép (${maxSizeBytes / (1024 * 1024)}MB)")
                    null
                } else {
                    bytes
                }
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi khi đọc tệp từ Uri: ${e.message}")
                null
            }
        }
    }

    suspend fun extractRawTextFromUri(uri: Uri, maxChars: Int = 150000): String {
        return withContext(Dispatchers.IO) {
            try {
                val (fileName, mimeType) = getFileInfo(uri)
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: throw Exception("Không thể mở tệp từ đường dẫn đã chọn.")

                val bytes = inputStream.use { it.readBytes() }

                if (bytes.isEmpty()) {
                    throw Exception("Tệp rỗng hoặc không có dữ liệu.")
                }

                val nameLower = fileName.lowercase()
                val mimeLower = mimeType.lowercase()
                val isPdf = nameLower.endsWith(".pdf") || mimeLower.contains("pdf") || isPdfBytes(bytes)

                val rawText = when {
                    nameLower.endsWith(".docx") || mimeLower.contains("word") || mimeLower.contains("officedocument.wordprocessingml") -> {
                        extractDocxFromBytes(bytes)
                    }
                    isPdf -> {
                        extractPdfFromBytes(bytes)
                    }
                    else -> {
                        if (isZipBytes(bytes)) {
                            extractDocxFromBytes(bytes)
                        } else {
                            extractPlainTextFromBytes(bytes)
                        }
                    }
                }

                val cleaned = cleanTextContent(rawText, maxChars)
                if (cleaned.isBlank()) {
                    if (isPdf) {
                        throw Exception("Không thể trích xuất văn bản từ PDF (có thể là PDF dạng ảnh quét).")
                    } else {
                        throw Exception("Không tìm thấy nội dung văn bản trong tệp hoặc định dạng không được hỗ trợ.")
                    }
                }

                cleaned
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi khi trích xuất văn bản từ Uri: ${e.message}")
                throw e
            }
        }
    }

    fun getFileInfo(uri: Uri): Pair<String, String> {
        var fileName = ""
        val mimeType = context.contentResolver.getType(uri) ?: ""

        try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        fileName = cursor.getString(index) ?: ""
                    }
                }
            }
        } catch (ignored: Exception) {
        }

        if (fileName.isBlank()) {
            fileName = uri.lastPathSegment ?: ""
        }

        return Pair(fileName, mimeType)
    }

    private fun isZipBytes(bytes: ByteArray): Boolean {
        return bytes.size >= 4 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte() && bytes[2] == 0x03.toByte() && bytes[3] == 0x04.toByte()
    }

    private fun isPdfBytes(bytes: ByteArray): Boolean {
        if (bytes.size < 5) return false
        val header = String(bytes, 0, minOf(bytes.size, 20), StandardCharsets.ISO_8859_1)
        return header.contains("%PDF-")
    }

    fun extractDocxFromBytes(bytes: ByteArray): String {
        val sb = StringBuilder()
        try {
            val zip = ZipInputStream(ByteArrayInputStream(bytes))
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name == "word/document.xml" || entry.name.startsWith("word/header") || entry.name.startsWith("word/footer")) {
                    val reader = BufferedReader(InputStreamReader(zip, StandardCharsets.UTF_8))
                    val xmlSb = StringBuilder()
                    var line: String? = reader.readLine()
                    while (line != null) {
                        xmlSb.append(line).append("\n")
                        line = reader.readLine()
                    }

                    val xmlContent = xmlSb.toString()
                    val paragraphs = xmlContent.split(Regex("</w:p>"))

                    for (p in paragraphs) {
                        val matcher = Pattern.compile("<w:t(?:\\s+[^>]*)?>(.*?)</w:t>|<w:tab\\s*/>|<w:br\\s*/>", Pattern.DOTALL).matcher(p)
                        val pSb = StringBuilder()
                        while (matcher.find()) {
                            val matched = matcher.group()
                            if (matched.startsWith("<w:t")) {
                                val text = matcher.group(1) ?: ""
                                if (text.isNotEmpty()) {
                                    pSb.append(text)
                                }
                            } else if (matched.contains("tab")) {
                                pSb.append("\t")
                            } else if (matched.contains("br")) {
                                pSb.append("\n")
                            }
                        }
                        val cleanedParagraph = unescapeXml(pSb.toString()).trim()
                        if (cleanedParagraph.isNotEmpty()) {
                            sb.append(cleanedParagraph).append("\n")
                        }
                    }
                }
                entry = zip.nextEntry
            }
            zip.close()
        } catch (ignored: Exception) {
        }
        return sb.toString().trim()
    }

    private fun unescapeXml(text: String): String {
        return text
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&#39;", "'")
    }

    fun extractPdfFromBytes(bytes: ByteArray): String {
        val sb = StringBuilder()
        try {
            val rawString = String(bytes, StandardCharsets.ISO_8859_1)

            val streamMatcher = Pattern.compile("(?s)(?:<<.*?>\\s*)?stream\\r?\\n(.*?)\\r?\\nendstream").matcher(rawString)
            val decompressedStreams = mutableListOf<String>()

            var searchStart = 0
            while (streamMatcher.find(searchStart)) {
                val streamStart = streamMatcher.start(1)
                val streamEnd = streamMatcher.end(1)
                searchStart = streamMatcher.end()

                val headerStart = maxOf(0, streamMatcher.start() - 300)
                val headerSnippet = rawString.substring(headerStart, streamMatcher.start())

                val streamBytes = bytes.copyOfRange(streamStart, streamEnd)
                val isFlate = headerSnippet.contains("/FlateDecode")

                val decompressedText = if (isFlate) {
                    decompressFlate(streamBytes)
                } else {
                    String(streamBytes, StandardCharsets.UTF_8)
                }

                if (decompressedText.isNotBlank()) {
                    decompressedStreams.add(decompressedText)
                }
            }

            for (streamText in decompressedStreams) {
                val textFromStream = parsePdfTextOperators(streamText)
                if (textFromStream.isNotBlank()) {
                    sb.append(textFromStream).append("\n")
                }
            }

            if (sb.length < 50) {
                val fallbackText = parsePdfTextOperators(rawString)
                if (fallbackText.isNotBlank()) {
                    sb.append(fallbackText).append("\n")
                }
            }

            if (sb.length < 50) {
                val lineMatcher = Pattern.compile("\\(([^()]+)\\)").matcher(rawString)
                while (lineMatcher.find()) {
                    val candidate = lineMatcher.group(1) ?: ""
                    val decoded = decodePdfEscapes(candidate)
                    if (isCleanHumanText(decoded)) {
                        sb.append(decoded).append(" ")
                    }
                }
            }
        } catch (ignored: Exception) {
        }

        return sb.toString().trim()
    }

    private fun decompressFlate(data: ByteArray): String {
        try {
            val inflater = Inflater(false)
            inflater.setInput(data)
            val outputStream = ByteArrayOutputStream()
            val buffer = ByteArray(4096)
            while (!inflater.finished()) {
                val count = inflater.inflate(buffer)
                if (count == 0) {
                    if (inflater.needsInput() || inflater.needsDictionary()) break
                }
                outputStream.write(buffer, 0, count)
            }
            inflater.end()
            val decompressedBytes = outputStream.toByteArray()
            return tryUtf8OrIso(decompressedBytes)
        } catch (e: Exception) {
            try {
                val ais = InflaterInputStream(ByteArrayInputStream(data))
                val bos = ByteArrayOutputStream()
                val buf = ByteArray(4096)
                var len: Int
                while (ais.read(buf).also { len = it } > 0) {
                    bos.write(buf, 0, len)
                }
                return tryUtf8OrIso(bos.toByteArray())
            } catch (ignored: Exception) {
            }
        }
        return ""
    }

    private fun tryUtf8OrIso(bytes: ByteArray): String {
        val utf8 = String(bytes, StandardCharsets.UTF_8)
        if (utf8.contains('\uFFFD')) {
            return String(bytes, StandardCharsets.ISO_8859_1)
        }
        return utf8
    }

    private fun parsePdfTextOperators(content: String): String {
        val sb = StringBuilder()

        val tjMatcher = Pattern.compile("\\(([^()\\\\]*(?:\\\\.[^()\\\\]*)*)\\)\\s*(?:Tj|'|\")").matcher(content)
        while (tjMatcher.find()) {
            val matched = tjMatcher.group(1) ?: ""
            val decoded = decodePdfEscapes(matched)
            if (isCleanHumanText(decoded)) {
                sb.append(decoded).append(" ")
            }
        }

        val arrayTjMatcher = Pattern.compile("\\[((?:\\([^()\\\\]*(?:\\\\.[^()\\\\]*)*\\)|<[0-9A-Fa-f\\s]+>|-?\\d+\\s*)*)\\]\\s*TJ").matcher(content)
        while (arrayTjMatcher.find()) {
            val arrayContent = arrayTjMatcher.group(1) ?: ""
            val elemMatcher = Pattern.compile("\\(([^()\\\\]*(?:\\\\.[^()\\\\]*)*)\\)|<([0-9A-Fa-f\\s]+)>").matcher(arrayContent)
            val pSb = StringBuilder()
            while (elemMatcher.find()) {
                val strLiteral = elemMatcher.group(1)
                val hexLiteral = elemMatcher.group(2)
                if (strLiteral != null) {
                    pSb.append(decodePdfEscapes(strLiteral))
                } else if (hexLiteral != null) {
                    pSb.append(decodeHex(hexLiteral))
                }
            }
            val text = pSb.toString()
            if (isCleanHumanText(text)) {
                sb.append(text).append(" ")
            }
        }

        return sb.toString().trim()
    }

    private fun decodePdfEscapes(str: String): String {
        val byteList = mutableListOf<Byte>()
        var i = 0
        val len = str.length
        val charArray = str.toCharArray()

        while (i < len) {
            val c = charArray[i]
            if (c == '\\' && i + 1 < len) {
                val next = charArray[i + 1]
                when (next) {
                    'n' -> { byteList.add('\n'.code.toByte()); i += 2 }
                    'r' -> { byteList.add('\r'.code.toByte()); i += 2 }
                    't' -> { byteList.add('\t'.code.toByte()); i += 2 }
                    'b' -> { byteList.add('\b'.code.toByte()); i += 2 }
                    'f' -> { byteList.add(12.toByte()); i += 2 }
                    '(' -> { byteList.add('('.code.toByte()); i += 2 }
                    ')' -> { byteList.add(')'.code.toByte()); i += 2 }
                    '\\' -> { byteList.add('\\'.code.toByte()); i += 2 }
                    in '0'..'7' -> {
                        var octalStr = "" + next
                        var j = i + 2
                        while (j < len && j < i + 4 && charArray[j] in '0'..'7') {
                            octalStr += charArray[j]
                            j++
                        }
                        try {
                            val octVal = octalStr.toInt(8)
                            byteList.add(octVal.toByte())
                        } catch (e: Exception) {
                            byteList.add(next.code.toByte())
                        }
                        i = j
                    }
                    else -> {
                        byteList.add(next.code.toByte())
                        i += 2
                    }
                }
            } else {
                byteList.add(c.code.toByte())
                i++
            }
        }

        val byteArray = byteList.toByteArray()
        val utf8Str = String(byteArray, StandardCharsets.UTF_8)
        return if (!utf8Str.contains('\uFFFD')) utf8Str else String(byteArray, StandardCharsets.ISO_8859_1)
    }

    private fun decodeHex(hex: String): String {
        val cleanHex = hex.replace("\\s+".toRegex(), "")
        if (cleanHex.isBlank()) return ""
        val bytes = ByteArray(cleanHex.length / 2)
        try {
            for (i in bytes.indices) {
                val index = i * 2
                bytes[i] = cleanHex.substring(index, index + 2).toInt(16).toByte()
            }
            val utf8 = String(bytes, StandardCharsets.UTF_8)
            return if (!utf8.contains('\uFFFD')) utf8 else String(bytes, StandardCharsets.ISO_8859_1)
        } catch (e: Exception) {
            return ""
        }
    }

    fun extractPlainTextFromBytes(bytes: ByteArray): String {
        val utf8 = String(bytes, StandardCharsets.UTF_8)
        val textToUse = if (!utf8.contains('\uFFFD')) utf8 else String(bytes, StandardCharsets.ISO_8859_1)

        val sb = StringBuilder()
        val lines = textToUse.split(Regex("[\\r\\n]+"))
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isNotEmpty() && isCleanHumanText(trimmed)) {
                sb.append(trimmed).append("\n")
            }
        }
        return sb.toString().trim()
    }

    private fun isCleanHumanText(str: String): Boolean {
        if (str.length < 3) return false
        val lower = str.lowercase()

        val pdfKeywords = listOf(
            "pdf-", "endobj", "flatedecode",
            "skia pdf", "mediabox"
        )

        for (kw in pdfKeywords) {
            if (lower.contains(kw)) return false
        }

        var validCharCount = 0
        for (c in str) {
            if (c.isLetterOrDigit() || c.isWhitespace() || Character.isLetter(c) || c in ",.:;?!-_()\"'/[]=+#$%&*@~") {
                validCharCount++
            }
        }

        return (validCharCount.toDouble() / str.length) > 0.60
    }

    fun cleanTextContent(raw: String, maxChars: Int = 150000): String {
        val cleaned = raw.replace(Regex("[ \\t]+"), " ")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
        return if (cleaned.length > maxChars) cleaned.substring(0, maxChars) else cleaned
    }

    suspend fun parseAndImportFile(uri: Uri, subjectName: String): Result<Int> {
        return withContext(Dispatchers.IO) {
            try {
                val fullText = extractRawTextFromUri(uri, maxChars = 8000)
                if (fullText.isBlank()) throw Exception("Không thể trích xuất văn bản từ tệp.")

                val lines = fullText.split(Regex("[\\r\\n]+"))
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

                for (line in lines) {
                    val l = line.trim()
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
