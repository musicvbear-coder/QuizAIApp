package com.example.quizaiapp.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "bang_lich_su_de_thi")
data class LichSuDeThi(
    @PrimaryKey
    val idDeThi: String,
    val chuDeKiemsTra: String,
    val tongSoCau: Int,
    val diemMoiCau: Float,
    val thoiGianTao: Long,
    val diemDatDuoc: Float
)

@Entity(
    tableName = "bang_chi_tiet_cau_hoi_ai",
    foreignKeys = [
        ForeignKey(
            entity = LichSuDeThi::class,
            parentColumns = ["idDeThi"],
            childColumns = ["idDeThi"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ChiTietCauHoiAi(
    @PrimaryKey(autoGenerate = true)
    val idCauHoiChiTiet: Long = 0,
    val idDeThi: String,
    val noiDungCauHoi: String,
    val phuongAnA: String,
    val phuongAnB: String,
    val phuongAnC: String,
    val phuongAnD: String,
    val dapAnChinhXac: String,
    val loiGiaiThich: String
)