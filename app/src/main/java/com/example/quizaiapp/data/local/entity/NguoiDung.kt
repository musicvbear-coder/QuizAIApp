package com.example.quizaiapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bang_nguoi_dung")
data class NguoiDung(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val hoTen: String,
    val email: String,
    val matKhau: String
)