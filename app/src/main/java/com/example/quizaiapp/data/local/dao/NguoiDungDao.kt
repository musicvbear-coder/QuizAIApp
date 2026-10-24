package com.example.quizaiapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.quizaiapp.data.local.entity.NguoiDung

@Dao
interface NguoiDungDao {
    @Insert
    suspend fun dangKyTaiKhoan(nguoiDung: NguoiDung)

    @Query("SELECT * FROM bang_nguoi_dung WHERE email = :email AND matKhau = :matKhau LIMIT 1")
    suspend fun dangNhap(email: String, matKhau: String): NguoiDung?

    @Query("SELECT EXISTS(SELECT 1 FROM bang_nguoi_dung WHERE email = :email)")
    suspend fun kiemTraEmailDaTonTai(email: String): Boolean
}