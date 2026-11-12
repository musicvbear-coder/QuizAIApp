package com.example.quizaiapp.data.repository

import android.util.Log
import com.example.quizaiapp.data.local.dao.TruyVanDeThiDao
import com.example.quizaiapp.data.local.entity.ChiTietCauHoiAi
import com.example.quizaiapp.data.local.entity.LichSuDeThi
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

class GeminiRepository(
    private val truyVanDeThiDao: TruyVanDeThiDao,
    private val apiKey: String
) {

    companion object {
        private const val TAG = "GeminiRepository"

        private fun logError(tag: String, msg: String, throwable: Throwable? = null) {
            try {
                Log.e(tag, msg, throwable)
            } catch (ignored: Throwable) {
                println("[$tag ERROR] $msg")
            }
        }

        private fun logDebug(tag: String, msg: String) {
            try {
                Log.d(tag, msg)
            } catch (ignored: Throwable) {
                println("[$tag DEBUG] $msg")
            }
        }
    }

    suspend fun taoVaLuuDeThiAi(chuDe: String, soLuongCau: Int, docContent: String = ""): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val cleanKey = apiKey.trim()
                val diemMoiCau = 10.0f / soLuongCau
                val effectiveSubject = if (chuDe.isNotBlank()) chuDe else "Nội dung tệp tài liệu"

                val promptText = if (docContent.isNotBlank()) {
                    val userRequirement = if (chuDe.isNotBlank()) {
                        "Hãy thực hiện ĐÚNG THEO YÊU CẦU/HƯỚNG DẪN BỔ SUNG của người dùng: \"$chuDe\""
                    } else {
                        "Tự động phân tích toàn bộ nội dung tài liệu và tạo các câu hỏi trắc nghiệm trọng tâm nhất"
                    }

                    """
                        Bạn là một chuyên gia giáo dục xuất sắc.
                        Dựa TRỰC TIẾP và BÁM SÁT vào nội dung văn bản tài liệu dưới đây, hãy biên soạn đúng $soLuongCau câu hỏi trắc nghiệm khách quan chuẩn sư phạm.
                        YÊU CẦU CỦA NGƯỜI DÙNG: $userRequirement

                        NỘI DUNG TÀI LIỆU THAM KHẢO:
                        \"\"\"
                        $docContent
                        \"\"\"

                        Yêu cầu sư phạm khắt khe:
                        1. Mọi câu hỏi và đáp án phải bám sát chính xác kiến thức có trong đoạn tài liệu tham khảo trên.
                        2. 4 phương án A, B, C, D có độ nhiễu chất lượng. Tuyệt đối KHÔNG dùng câu "Tất cả các đáp án trên".
                        3. Chỉ ra đáp án chính xác (A, B, C hoặc D).
                        4. Lời giải thích chi tiết, trích dẫn nguyên văn câu văn từ tài liệu.

                        Yêu cầu định dạng đầu ra:
                        Trả về duy nhất một mảng JSON (JSONArray) hợp lệ, KHÔNG chứa bất kỳ văn bản ngoài JSON.
                        [
                          {
                            "noiDungCauHoi": "Nội dung câu hỏi cụ thể?",
                            "phuongAnA": "Nội dung đáp án A",
                            "phuongAnB": "Nội dung đáp án B",
                            "phuongAnC": "Nội dung đáp án C",
                            "phuongAnD": "Nội dung đáp án D",
                            "dapAnChinhXac": "A",
                            "loiGiaiThich": "Lời giải thích chi tiết trích dẫn từ tài liệu"
                          }
                        ]
                    """.trimIndent()
                } else {
                    """
                        Bạn là một chuyên gia giáo dục xuất sắc am hiểu mọi lĩnh vực kiến thức, chuyên biên soạn đề thi trắc nghiệm khách quan chuẩn Bộ Giáo Dục và Đào Tạo Việt Nam với tư duy phân tích sâu sắc.
                        Hãy biên soạn đúng $soLuongCau câu hỏi trắc nghiệm khách quan cực kỳ thông minh, sát thực tế và đúng trọng tâm về chủ đề: "$effectiveSubject".

                        Yêu cầu sư phạm khắt khe:
                        1. Câu hỏi phải rõ ràng, mang tính ứng dụng thực tế hoặc kiểm tra tư duy phân tích sâu sắc về "$effectiveSubject".
                        2. 4 phương án A, B, C, D phải có độ nhiễu chất lượng. Tuyệt đối KHÔNG dùng câu "Tất cả các đáp án trên".
                        3. Chỉ ra đáp án chính xác (A, B, C hoặc D).
                        4. Lời giải thích phải vô cùng chi tiết, phân tích rõ lý do tại sao đúng và tại sao các câu khác sai.

                        Yêu cầu định dạng đầu ra:
                        Trả về duy nhất một mảng JSON (JSONArray) hợp lệ, KHÔNG chứa văn bản ngoài JSON.
                        [
                          {
                            "noiDungCauHoi": "Nội dung câu hỏi cụ thể?",
                            "phuongAnA": "Nội dung đáp án A",
                            "phuongAnB": "Nội dung đáp án B",
                            "phuongAnC": "Nội dung đáp án C",
                            "phuongAnD": "Nội dung đáp án D",
                            "dapAnChinhXac": "A",
                            "loiGiaiThich": "Lời giải thích chi tiết, thuyết phục từng bước"
                          }
                        ]
                    """.trimIndent()
                }

                var responseText: String? = null
                var lastException: Exception? = null

                val isValidKey = cleanKey.isNotBlank() && cleanKey != "YOUR_GEMINI_API_KEY_HERE"

                if (isValidKey) {
                    // 1. Thử gọi qua Google Generative AI SDK (Cấu hình ưu tiên Gemini 3.0 / 2.0)
                    val sdkModels = listOf("gemini-3.0-flash", "gemini-3-flash", "gemini-2.0-flash", "gemini-1.5-flash")
                    for (modelName in sdkModels) {
                        try {
                            val generativeModel = GenerativeModel(
                                modelName = modelName,
                                apiKey = cleanKey
                            )
                            val response = generativeModel.generateContent(promptText)
                            val sdkText = response.text
                            if (!sdkText.isNull_or_blank()) {
                                responseText = sdkText
                                logDebug(TAG, "Gọi Gemini SDK thành công với model $modelName!")
                                break
                            }
                        } catch (e: Exception) {
                            lastException = e
                            logError(TAG, "Lỗi khi gọi Gemini SDK ($modelName): ${e.message}", e)
                        }
                    }

                    // 2. Thử gọi qua REST API trực tiếp đối với các Endpoint Gemini 3.0 / 2.0 / 1.5
                    if (responseText.isNull_or_blank()) {
                        val apiEndpoints = listOf(
                            "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.0-flash:generateContent?key=$cleanKey",
                            "https://generativelanguage.googleapis.com/v1beta/models/gemini-3-flash:generateContent?key=$cleanKey",
                            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$cleanKey",
                            "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$cleanKey"
                        )

                        for (endpoint in apiEndpoints) {
                            try {
                                val result = goiApiEndpoint(endpoint, promptText)
                                if (result.isNotBlank()) {
                                    responseText = result
                                    logDebug(TAG, "Gọi Gemini REST API thành công!")
                                    break
                                }
                            } catch (e: Exception) {
                                lastException = e
                                logError(TAG, "Lỗi khi gọi Gemini REST endpoint $endpoint: ${e.message}")
                            }
                        }
                    }
                }

                // 3. Dự phòng nếu API không có sẵn hoặc gặp lỗi kết nối
                if (responseText.isNull_or_blank()) {
                    responseText = sinhDeThiPhongPhuTheoChuDe(effectiveSubject, soLuongCau, docContent)
                }

                val safeResponseText = responseText ?: ""
                val jsonStart = safeResponseText.indexOf('[')
                val jsonEnd = safeResponseText.lastIndexOf(']')
                val jsonClean = if (jsonStart != -1 && jsonEnd != -1 && jsonStart < jsonEnd) {
                    safeResponseText.substring(jsonStart, jsonEnd + 1)
                } else {
                    sinhDeThiPhongPhuTheoChuDe(effectiveSubject, soLuongCau, docContent)
                }

                val jsonArray = JSONArray(jsonClean)
                val idDeThi = "EXAM_" + UUID.randomUUID().toString().substring(0, 8)
                val thoiGianTao = System.currentTimeMillis()

                val danhSachCauHoiChiTiet = mutableListOf<ChiTietCauHoiAi>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    danhSachCauHoiChiTiet.add(
                        ChiTietCauHoiAi(
                            idDeThi = idDeThi,
                            noiDungCauHoi = obj.optString("noiDungCauHoi", "Câu hỏi ${i + 1}"),
                            phuongAnA = obj.optString("phuongAnA", "Phương án A"),
                            phuongAnB = obj.optString("phuongAnB", "Phương án B"),
                            phuongAnC = obj.optString("phuongAnC", "Phương án C"),
                            phuongAnD = obj.optString("phuongAnD", "Phương án D"),
                            dapAnChinhXac = obj.optString("dapAnChinhXac", "A"),
                            loiGiaiThich = obj.optString("loiGiaiThich", "Lời giải chi tiết cho câu hỏi ${i + 1}.")
                        )
                    )
                }

                val thongTinDeThi = LichSuDeThi(
                    idDeThi = idDeThi,
                    chuDeKiemsTra = effectiveSubject,
                    tongSoCau = jsonArray.length(),
                    diemMoiCau = diemMoiCau,
                    thoiGianTao = thoiGianTao,
                    diemDatDuoc = 0.0f
                )

                truyVanDeThiDao.luuTronBoDeThiAi(thongTinDeThi, danhSachCauHoiChiTiet)

                Result.success(idDeThi)
            } catch (e: Exception) {
                logError(TAG, "Lỗi tổng thể taoVaLuuDeThiAi: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    private fun goiApiEndpoint(urlString: String, prompt: String): String {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        connection.doOutput = true
        connection.connectTimeout = 30000
        connection.readTimeout = 30000

        val requestBody = JSONObject().apply {
            val genConfig = JSONObject().apply {
                put("temperature", 0.7)
                put("topK", 40)
                put("topP", 0.95)
                put("maxOutputTokens", 4096)
            }
            put("generationConfig", genConfig)

            val contents = JSONArray().apply {
                val contentObj = JSONObject().apply {
                    val parts = JSONArray().apply {
                        val partObj = JSONObject().apply {
                            put("text", prompt)
                        }
                        put(partObj)
                    }
                    put("parts", parts)
                }
                put(contentObj)
            }
            put("contents", contents)
        }

        val writer = OutputStreamWriter(connection.outputStream, "UTF-8")
        writer.write(requestBody.toString())
        writer.flush()
        writer.close()

        val responseCode = connection.responseCode
        if (responseCode == HttpURLConnection.HTTP_OK) {
            val reader = BufferedReader(InputStreamReader(connection.inputStream, "UTF-8"))
            val responseBuilder = StringBuilder()
            var line: String? = reader.readLine()
            while (line != null) {
                responseBuilder.append(line)
                line = reader.readLine()
            }
            reader.close()

            val jsonResponse = JSONObject(responseBuilder.toString())
            val candidates = jsonResponse.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    return parts.getJSONObject(0).optString("text", "")
                }
            }
        } else {
            val errorStream = connection.errorStream
            val errorText = if (errorStream != null) {
                BufferedReader(InputStreamReader(errorStream, "UTF-8")).use { it.readText() }
            } else ""
            logError(TAG, "Gemini API HTTP Error $responseCode: $errorText")
            throw Exception("HTTP $responseCode: $errorText")
        }
        return ""
    }

    private fun sinhDeThiPhongPhuTheoChuDe(chuDe: String, soLuongCau: Int, docContent: String = ""): String {
        val jsonArray = JSONArray()

        if (docContent.isNotBlank()) {
            val cleanDoc = docContent.replace(Regex("\\s+"), " ").trim()
            val rawSentences = cleanDoc.split(Regex("[.?!\\r\\n]+"))
                .map { it.trim() }
                .filter { sentence ->
                    sentence.length >= 12 &&
                            !sentence.lowercase().contains("pdf-") &&
                            !sentence.lowercase().contains("endobj") &&
                            !sentence.lowercase().contains("flatedecode")
                }

            val sentences = if (rawSentences.isNotEmpty()) {
                rawSentences
            } else {
                // Tách đoạn thành các phần nhỏ 40-100 ký tự từ văn bản
                cleanDoc.chunked(80).filter { it.length >= 10 }
            }

            if (sentences.isNotEmpty()) {
                val dapAnList = listOf("A", "B", "C", "D")
                for (i in 0 until soLuongCau) {
                    val sentence = sentences[i % sentences.size]
                    val correctPos = dapAnList[i % 4]

                    val optionA = if (correctPos == "A") sentence else "Phân tích không tương thích với trích dẫn tài liệu"
                    val optionB = if (correctPos == "B") sentence else "Nội dung mở rộng ngoài phạm vi tài liệu"
                    val optionC = if (correctPos == "C") sentence else "Khái niệm tương đương nhưng chưa chính xác"
                    val optionD = if (correctPos == "D") sentence else "Giả thuyết không được đề cập trong tệp đính kèm"

                    val obj = JSONObject().apply {
                        put("noiDungCauHoi", "Câu ${i + 1}: Căn cứ vào nội dung tệp tài liệu đính kèm, khẳng định nào sau đây là ĐÚNG về: \"${sentence.take(90)}\"?")
                        put("phuongAnA", optionA)
                        put("phuongAnB", optionB)
                        put("phuongAnC", optionC)
                        put("phuongAnD", optionD)
                        put("dapAnChinhXac", correctPos)
                        put("loiGiaiThich", "Căn cứ trực tiếp từ trích đoạn trong tài liệu đính kèm: \"$sentence\".")
                    }
                    jsonArray.put(obj)
                }
                return jsonArray.toString()
            }
        }

        val lower = chuDe.lowercase()
        val questionTemplates = when {
            lower.contains("tiếng anh") || lower.contains("english") -> listEnglishQuestions(chuDe)
            lower.contains("hàm số") || lower.contains("toán") || lower.contains("math") -> listMathQuestions(chuDe)
            lower.contains("mạng") || lower.contains("tin học") || lower.contains("cntt") -> listItQuestions(chuDe)
            lower.contains("sử") || lower.contains("lịch sử") || lower.contains("kháng chiến") -> listHistoryQuestions(chuDe)
            lower.contains("hóa") || lower.contains("hoá") || lower.contains("chemistry") -> listChemistryQuestions(chuDe)
            else -> listGeneralQuestions(chuDe)
        }

        for (i in 0 until soLuongCau) {
            val q = questionTemplates[i % questionTemplates.size]
            val obj = JSONObject().apply {
                put("noiDungCauHoi", "Câu ${i + 1}: ${q.noiDung}")
                put("phuongAnA", q.a)
                put("phuongAnB", q.b)
                put("phuongAnC", q.c)
                put("phuongAnD", q.d)
                put("dapAnChinhXac", q.correct)
                put("loiGiaiThich", q.explain)
            }
            jsonArray.put(obj)
        }

        return jsonArray.toString()
    }

    private fun String?.isNull_or_blank(): Boolean {
        return this == null || this.trim().isEmpty()
    }

    private data class SampleQ(val noiDung: String, val a: String, val b: String, val c: String, val d: String, val correct: String, val explain: String)

    private fun listEnglishQuestions(topic: String) = listOf(
        SampleQ("Choose the word whose underlined part is pronounced differently.", "A. school", "B. scholar", "C. chemistry", "D. children", "D", "Chữ 'ch' trong 'children' phát âm là /tʃ/, các từ còn lại là /k/."),
        SampleQ("She has been working here _____ 2018.", "A. since", "B. for", "C. in", "D. from", "A", "Thì hiện tại hoàn thành dùng 'since' đi với mốc thời gian cụ thể."),
        SampleQ("If I _____ you, I would take that opportunity.", "A. am", "B. was", "C. were", "D. have been", "C", "Câu điều kiện loại 2 dùng 'were' cho tất cả các ngôi."),
        SampleQ("Choose the synonym of the word 'IMPORTANT'.", "A. Vital", "B. Trivial", "C. Minor", "D. Optional", "A", "'Vital' (quan trọng, thiết yếu) đồng nghĩa với 'Important'."),
        SampleQ("He enjoys _____ books in his free time.", "A. read", "B. reading", "C. to read", "D. reads", "B", "Động từ theo sau 'enjoy' luôn ở dạng V-ing.")
    )

    private fun listMathQuestions(topic: String) = listOf(
        SampleQ("Tập xác định của hàm số y = 1 / (x - 2) là gì?", "A. D = R \\ {2}", "B. D = R", "C. D = (2; +∞)", "D. D = R \\ {0}", "A", "Mẫu số x - 2 phải khác 0 => x khác 2."),
        SampleQ("Đạo hàm của hàm số y = x^3 - 3x là:", "A. y' = 3x^2 - 3", "B. y' = x^2 - 3", "C. y' = 3x^2", "D. y' = 3x - 3", "A", "Áp dụng công thức (x^n)' = n*x^(n-1)."),
        SampleQ("Hàm số y = x^4 - 2x^2 + 1 có bao nhiêu điểm cực trị?", "A. 3 điểm", "B. 1 điểm", "C. 2 điểm", "D. 0 điểm", "A", "Phương trình y' = 4x^3 - 4x = 0 có 3 nghiệm phân biệt x = 0, x = 1, x = -1."),
        SampleQ("Đồ thị hàm số y = (2x + 1) / (x - 1) có tiệm cận đứng là:", "A. x = 1", "B. y = 2", "C. x = -1", "D. y = 1", "A", "Tiệm cận đứng là nghiệm của mẫu số: x - 1 = 0 => x = 1."),
        SampleQ("Giá trị lớn nhất của hàm số y = sin(x) + 2 là:", "A. 3", "B. 1", "C. 2", "D. 0", "A", "Do -1 <= sin(x) <= 1 nên max y = 1 + 2 = 3.")
    )

    private fun listItQuestions(topic: String) = listOf(
        SampleQ("Giao thức nào được sử dụng để truyền tải trang web an toàn?", "A. HTTPS", "B. HTTP", "C. FTP", "D. SMTP", "A", "HTTPS mã hóa dữ liệu qua SSL/TLS đảm bảo an toàn."),
        SampleQ("Địa chỉ IPv4 bao gồm bao nhiêu bit?", "A. 32 bit", "B. 64 bit", "C. 128 bit", "D. 16 bit", "A", "IPv4 gồm 4 byte = 32 bit."),
        SampleQ("Thành phần nào điều khiển và xử lý các lệnh trong máy tính?", "A. CPU", "B. RAM", "C. SSD", "D. GPU", "A", "CPU (Central Processing Unit) là bộ xử lý trung tâm."),
        SampleQ("Trong mô hình OSI, tầng nào chịu trách nhiệm định tuyến dữ liệu?", "A. Network", "B. Transport", "C. Data Link", "D. Application", "A", "Tầng Network (tầng 3) chịu trách nhiệm định tuyến gói tin (IP)."),
        SampleQ("Cấu trúc dữ liệu nào hoạt động theo nguyên tắc LIFO (Last In First Out)?", "A. Stack", "B. Queue", "C. Array", "D. Linked List", "A", "Stack (Ngăn xếp) hoạt động theo cơ chế Vào sau Ra trước (LIFO).")
    )

    private fun listHistoryQuestions(topic: String) = listOf(
        SampleQ("Nguyễn Tất Thành ra đi tìm đường cứu nước vào năm nào và từ đâu?", "A. 1911 tại Cảng Nhà Rồng", "B. 1920 tại Pháp", "C. 1930 tại Quảng Châu", "D. 1941 tại Pác Bó", "A", "Ngày 5/6/1911, Nguyễn Tất Thành ra đi tìm đường cứu nước tại Cảng Nhà Rồng (Sài Gòn)."),
        SampleQ("Đảng Cộng sản Việt Nam được thành lập vào thời gian nào?", "A. 03/02/1930", "B. 19/08/1945", "C. 02/09/1945", "D. 07/05/1954", "A", "Hội nghị thành lập Đảng diễn ra từ ngày 6/1 đến đầu tháng 2/1930 tại Cửu Long (Hương Cảng, Trung Quốc) dưới sự trì của Nguyễn Ái Quốc."),
        SampleQ("Sự kiện nào đánh dấu sự bùng nổ của Cách mạng tháng Tám năm 1945?", "A. Tổng khởi nghĩa giành chính quyền tại Hà Nội (19/08/1945)", "B. Khởi nghĩa Bắc Sơn", "C. Nam Kỳ khởi nghĩa", "D. Bác Hồ đọc Tuyên ngôn Độc lập", "A", "Ngày 19/08/1945, nhân nhân Hà Nội khởi nghĩa giành chính quyền thắng lợi, mở đầu cho làn sóng Tổng khởi nghĩa toàn quốc."),
        SampleQ("Chiến dịch Điện Biên Phủ toàn thắng vào ngày tháng năm nào?", "A. 07/05/1954", "B. 20/07/1954", "C. 13/03/1954", "D. 30/04/1975", "A", "Chiều ngày 7/5/1954, lá cờ 'Quyết chiến Quyết thắng' tung bay trên nóc hầm De Castries, chiến dịch Điện Biên Phủ hoàn toàn thắng lợi."),
        SampleQ("Hiệp định Giơ-ne-vơ năm 1954 lấy vĩ tuyến nào làm ranh giới quân sự tạm thời ở Việt Nam?", "A. Vĩ tuyến 17", "B. Vĩ tuyến 16", "C. Vĩ tuyến 18", "D. Vĩ tuyến 20", "A", "Hiệp định Giơ-ne-vơ chọn sông Bến Hải ở vĩ tuyến 17 làm ranh giới quân sự tạm thời phân chia hai miền Nam - Bắc.")
    )

    private fun listChemistryQuestions(topic: String) = listOf(
        SampleQ("Este C2H4O2 (etyl fomat/metyl fomat) có tên gọi thay thế là gì?", "A. Metyl fomat", "B. Etyl axetat", "C. Axit axetic", "D. Ancol etylic", "A", "Công thức HCOOCH3 là metyl fomat."),
        SampleQ("Kim loại nào sau đây có tính dẫn điện tốt nhất?", "A. Bạc (Ag)", "B. Đồng (Cu)", "C. Vàng (Au)", "D. Nhôm (Al)", "A", "Thứ tự dẫn điện giảm dần của kim loại là: Ag > Cu > Au > Al > Fe."),
        SampleQ("Số oxi hóa của sắt trong hợp chất Fe2O3 là bao nhiêu?", "A. +3", "B. +2", "C. +1", "D. 0", "A", "Trong Fe2O3, oxi có số oxi hóa -2 => 2*Fe + 3*(-2) = 0 => Fe = +3."),
        SampleQ("Chất nào sau đây làm quỳ tím chuyển sang màu đỏ?", "A. Axit axetic (CH3COOH)", "B. Ancol etylic", "C. Glucozơ", "D. Nước cất", "A", "Axit axetic CH3COOH có tính axit yếu làm quỳ tím hóa đỏ nhẹ."),
        SampleQ("Dung dịch chất nào sau đây hòa tan được Cu(OH)2 ở nhiệt độ thường tạo phức màu xanh lam?", "A. Glucozơ", "B. Ancol etylic", "C. Benzen", "D. Etan", "A", "Glucozơ có nhiều nhóm -OH liền kề nên hòa tan được Cu(OH)2 tạo dung dịch xanh lam đặc trưng.")
    )

    private fun listGeneralQuestions(topic: String) = listOf(
        SampleQ("Kiến thức cốt lõi nào sau đây thuộc chủ đề '$topic'?", "A. Định lý và nguyên lý căn bản của $topic", "B. Phương pháp thực hành ứng dụng $topic", "C. Các quy tắc chuẩn hóa trong $topic", "D. Tất cả các phương án trên đều đúng", "D", "Chủ đề '$topic' bao gồm cả lý thuyết căn bản, phương pháp thực hành và chuẩn hóa."),
        SampleQ("Đâu là ứng dụng thực tế phổ biến nhất của '$topic'?", "A. Tối ưu hóa quy trình làm việc", "B. Giải quyết bài toán phân tích dữ liệu", "C. Nâng cao hiệu suất xử lý hệ thống", "D. Cả A, B và C", "D", "Tất cả các phương án trên đều là ứng dụng thực tế quan trọng của $topic."),
        SampleQ("Khi học và làm bài tập về '$topic', bước đầu tiên cần làm là gì?", "A. Nắm vững khái niệm và công thức nền tảng", "B. Làm ngay bài tập nâng cao", "C. Bỏ qua lý thuyết", "D. Chỉ học thuộc đáp án", "A", "Hiểu rõ khái niệm nền tảng là bước bắt buộc để làm tốt bài tập $topic.")
    )
}