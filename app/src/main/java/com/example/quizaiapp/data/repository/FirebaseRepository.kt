package com.example.quizaiapp.data.repository

import com.example.quizaiapp.data.local.entity.CauHoi
import com.example.quizaiapp.data.local.entity.ChiTietCauHoiAi
import com.example.quizaiapp.data.local.entity.LichSuDeThi
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

data class SubmissionItem(
    val userName: String = "",
    val score: Float = 0f,
    val correctCount: Int = 0,
    val totalQuestions: Int = 0,
    val submittedAt: Long = 0L
)

class FirebaseRepository {

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    fun layUidHienTai(): String? {
        return auth.currentUser?.uid
    }

    suspend fun dangKyFirebase(hoTen: String, email: String, matKhau: String): Result<String> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, matKhau).await()
            val user = authResult.user ?: throw Exception("Không thể tạo tài khoản trên Firebase Auth.")
            val uid = user.uid

            val userMap = hashMapOf<String, Any>(
                "uid" to uid,
                "hoTen" to hoTen,
                "email" to email,
                "createdAt" to System.currentTimeMillis()
            )

            firestore.collection("users").document(uid).set(userMap).await()
            Result.success(uid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun dangNhapFirebase(email: String, matKhau: String): Result<Pair<String, String>> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, matKhau).await()
            val user = authResult.user ?: throw Exception("Đăng nhập thất bại.")
            val uid = user.uid

            val doc = firestore.collection("users").document(uid).get().await()
            val hoTen = doc.getString("hoTen") ?: "Người dùng"

            Result.success(Pair(uid, hoTen))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun taoPhongThi(deThi: LichSuDeThi, danhSachCauHoi: List<ChiTietCauHoiAi>): String {
        val roomCode = (100000..999999).random().toString()
        try {
            val questionsList = danhSachCauHoi.map { q ->
                hashMapOf(
                    "noiDungCauHoi" to q.noiDungCauHoi,
                    "phuongAnA" to q.phuongAnA,
                    "phuongAnB" to q.phuongAnB,
                    "phuongAnC" to q.phuongAnC,
                    "phuongAnD" to q.phuongAnD,
                    "dapAnChinhXac" to q.dapAnChinhXac,
                    "loiGiaiThich" to q.loiGiaiThich
                )
            }

            val roomMap = hashMapOf<String, Any>(
                "roomCode" to roomCode,
                "idDeThi" to deThi.idDeThi,
                "chuDeKiemsTra" to deThi.chuDeKiemsTra,
                "tongSoCau" to deThi.tongSoCau,
                "diemMoiCau" to deThi.diemMoiCau,
                "thoiGianTao" to deThi.thoiGianTao,
                "createdBy" to (layUidHienTai() ?: "Anonymous"),
                "danhSachCauHoi" to questionsList
            )

            firestore.collection("rooms").document(roomCode).set(roomMap).await()
        } catch (ignored: Exception) {
        }
        return roomCode
    }

    suspend fun thamGiaPhongThi(roomCode: String): Pair<LichSuDeThi, List<ChiTietCauHoiAi>>? {
        return try {
            val doc = firestore.collection("rooms").document(roomCode.trim()).get().await()
            if (!doc.exists()) return null

            val idDeThi = doc.getString("idDeThi") ?: "ROOM_$roomCode"
            val chuDe = doc.getString("chuDeKiemsTra") ?: "Đề thi phòng $roomCode"
            val tongSoCau = (doc.getLong("tongSoCau") ?: 10L).toInt()
            val diemMoiCau = (doc.getDouble("diemMoiCau") ?: 1.0).toFloat()
            val thoiGianTao = doc.getLong("thoiGianTao") ?: System.currentTimeMillis()

            val deThi = LichSuDeThi(
                idDeThi = idDeThi,
                chuDeKiemsTra = chuDe,
                tongSoCau = tongSoCau,
                diemMoiCau = diemMoiCau,
                thoiGianTao = thoiGianTao,
                diemDatDuoc = 0.0f
            )

            val questionsList = mutableListOf<ChiTietCauHoiAi>()
            val rawList = doc.get("danhSachCauHoi") as? List<Map<String, Any>>
            rawList?.forEach { map ->
                questionsList.add(
                    ChiTietCauHoiAi(
                        idDeThi = idDeThi,
                        noiDungCauHoi = map["noiDungCauHoi"] as? String ?: "",
                        phuongAnA = map["phuongAnA"] as? String ?: "",
                        phuongAnB = map["phuongAnB"] as? String ?: "",
                        phuongAnC = map["phuongAnC"] as? String ?: "",
                        phuongAnD = map["phuongAnD"] as? String ?: "",
                        dapAnChinhXac = map["dapAnChinhXac"] as? String ?: "A",
                        loiGiaiThich = map["loiGiaiThich"] as? String ?: ""
                    )
                )
            }

            Pair(deThi, questionsList)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun luuKetQuaPhongThi(roomCode: String, userName: String, score: Float, correctCount: Int, totalQuestions: Int) {
        if (roomCode.isBlank()) return
        try {
            val subMap = hashMapOf<String, Any>(
                "userName" to userName,
                "score" to score,
                "correctCount" to correctCount,
                "totalQuestions" to totalQuestions,
                "submittedAt" to System.currentTimeMillis()
            )

            firestore.collection("rooms")
                .document(roomCode.trim())
                .collection("submissions")
                .add(subMap)
                .await()
        } catch (ignored: Exception) {
        }
    }

    suspend fun layBangXepHang(roomCode: String): List<SubmissionItem> {
        if (roomCode.isBlank()) return emptyList()
        return try {
            val snapshot = firestore.collection("rooms")
                .document(roomCode.trim())
                .collection("submissions")
                .orderBy("score", Query.Direction.DESCENDING)
                .get()
                .await()

            val result = mutableListOf<SubmissionItem>()
            for (doc in snapshot.documents) {
                val userName = doc.getString("userName") ?: "Thí sinh"
                val score = (doc.getDouble("score") ?: 0.0).toFloat()
                val correctCount = (doc.getLong("correctCount") ?: 0L).toInt()
                val totalQuestions = (doc.getLong("totalQuestions") ?: 0L).toInt()
                val submittedAt = doc.getLong("submittedAt") ?: 0L

                result.add(SubmissionItem(userName, score, correctCount, totalQuestions, submittedAt))
            }
            result
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun luuLichSuDeThiLenCloud(deThi: LichSuDeThi, danhSachCauHoi: List<ChiTietCauHoiAi>) {
        val uid = layUidHienTai() ?: return
        try {
            val questionsList = danhSachCauHoi.map { q ->
                hashMapOf(
                    "noiDungCauHoi" to q.noiDungCauHoi,
                    "phuongAnA" to q.phuongAnA,
                    "phuongAnB" to q.phuongAnB,
                    "phuongAnC" to q.phuongAnC,
                    "phuongAnD" to q.phuongAnD,
                    "dapAnChinhXac" to q.dapAnChinhXac,
                    "loiGiaiThich" to q.loiGiaiThich
                )
            }

            val examMap = hashMapOf<String, Any>(
                "idDeThi" to deThi.idDeThi,
                "chuDeKiemsTra" to deThi.chuDeKiemsTra,
                "tongSoCau" to deThi.tongSoCau,
                "diemMoiCau" to deThi.diemMoiCau,
                "thoiGianTao" to deThi.thoiGianTao,
                "diemDatDuoc" to deThi.diemDatDuoc,
                "danhSachCauHoi" to questionsList
            )

            firestore.collection("users")
                .document(uid)
                .collection("exams")
                .document(deThi.idDeThi)
                .set(examMap)
                .await()
        } catch (ignored: Exception) {
        }
    }

    suspend fun luuCauHoiNganHangLenCloud(cauHoi: CauHoi) {
        val uid = layUidHienTai() ?: return
        try {
            val questionMap = hashMapOf<String, Any>(
                "idCauHoi" to cauHoi.idCauHoi,
                "noiDungCauHoi" to cauHoi.noiDungCauHoi,
                "mucDoKho" to cauHoi.mucDoKho,
                "dapAnDung" to cauHoi.dapAnDung
            )

            firestore.collection("users")
                .document(uid)
                .collection("bank_questions")
                .document(cauHoi.idCauHoi.toString())
                .set(questionMap)
                .await()
        } catch (ignored: Exception) {
        }
    }
}