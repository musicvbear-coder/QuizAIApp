package com.example.quizaiapp.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "bang_mon_hoc")
data class MonHoc(
    @PrimaryKey(autoGenerate = true)
    val idMonHoc: Int = 0,
    val tenMonHoc: String
)

@Entity(
    tableName = "bang_chuong_hoc",
    foreignKeys = [
        ForeignKey(
            entity = MonHoc::class,
            parentColumns = ["idMonHoc"],
            childColumns = ["idMonHoc"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ChuongHoc(
    @PrimaryKey(autoGenerate = true)
    val idChuong: Int = 0,
    val idMonHoc: Int,
    val tenChuong: String
)

@Entity(
    tableName = "bang_cau_hoi",
    foreignKeys = [
        ForeignKey(
            entity = ChuongHoc::class,
            parentColumns = ["idChuong"],
            childColumns = ["idChuong"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class CauHoi(
    @PrimaryKey(autoGenerate = true)
    val idCauHoi: Int = 0,
    val idChuong: Int,
    val noiDungCauHoi: String,
    val mucDoKho: Int,
    val dapAnDung: String
)

@Entity(
    tableName = "bang_phuong_an",
    foreignKeys = [
        ForeignKey(
            entity = CauHoi::class,
            parentColumns = ["idCauHoi"],
            childColumns = ["idCauHoi"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PhuongAnTraLoi(
    @PrimaryKey(autoGenerate = true)
    val idPhuongAn: Int = 0,
    val idCauHoi: Int,
    val nhanPhuongAn: String
)