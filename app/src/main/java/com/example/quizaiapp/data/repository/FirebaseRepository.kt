package com.example.quizaiapp.data.repository

import com.example.quizaiapp.data.local.entity.CauHoi
import com.example.quizaiapp.data.local.entity.ChiTietCauHoiAi
import com.example.quizaiapp.data.local.entity.LichSuDeThi
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

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