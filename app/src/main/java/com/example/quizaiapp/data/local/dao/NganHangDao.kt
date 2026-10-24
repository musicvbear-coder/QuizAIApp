package com.example.quizaiapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.quizaiapp.data.local.entity.CauHoi
import com.example.quizaiapp.data.local.entity.ChuongHoc
import com.example.quizaiapp.data.local.entity.MonHoc
import com.example.quizaiapp.data.local.entity.PhuongAnTraLoi

@Dao
interface NganHangDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun luuMonHoc(monHoc: MonHoc): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun luuChuongHoc(chuongHoc: ChuongHoc): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun luuCauHoi(cauHoi: CauHoi): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun luuDanhSachPhuongAn(danhSachPhuongAn: List<PhuongAnTraLoi>)

    @Query("SELECT * FROM bang_mon_hoc WHERE tenMonHoc = :tenMonHoc LIMIT 1")
    suspend fun layMonHocTheoTen(tenMonHoc: String): MonHoc?

    @Query("SELECT * FROM bang_chuong_hoc WHERE idMonHoc = :idMonHoc AND tenChuong = :tenChuong LIMIT 1")
    suspend fun layChuongHocTheoTen(idMonHoc: Int, tenChuong: String): ChuongHoc?

    @Query("SELECT * FROM bang_cau_hoi ORDER BY idCauHoi DESC")
    suspend fun layToanBoCauHoi(): List<CauHoi>

    @Query("DELETE FROM bang_cau_hoi WHERE idCauHoi = :idCauHoi")
    suspend fun xoaCauHoi(idCauHoi: Int)

    @Transaction
    suspend fun luuBoCauHoiVaoNganHang(
        tenMonHoc: String,
        tenChuong: String,
        danhSachCauHoi: List<Pair<CauHoi, List<PhuongAnTraLoi>>>
    ) {
        val monHoc = layMonHocTheoTen(tenMonHoc)
        val idMonHoc = monHoc?.idMonHoc ?: luuMonHoc(MonHoc(tenMonHoc = tenMonHoc)).toInt()

        val chuongHoc = layChuongHocTheoTen(idMonHoc, tenChuong)
        val idChuong = chuongHoc?.idChuong ?: luuChuongHoc(ChuongHoc(idMonHoc = idMonHoc, tenChuong = tenChuong)).toInt()

        for ((cauHoi, phuongAnList) in danhSachCauHoi) {
            val cauHoiCapNhat = cauHoi.copy(idChuong = idChuong)
            val idCauHoiMoi = luuCauHoi(cauHoiCapNhat).toInt()
            val phuongAnCapNhat = phuongAnList.map { it.copy(idCauHoi = idCauHoiMoi) }
            luuDanhSachPhuongAn(phuongAnCapNhat)
        }
    }
}