package com.example.quizaiapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.quizaiapp.data.local.entity.ChiTietCauHoiAi
import com.example.quizaiapp.data.local.entity.LichSuDeThi
import kotlinx.coroutines.flow.Flow

@Dao
interface TruyVanDeThiDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun luuLichSuDeThi(deThi: LichSuDeThi)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun luuDanhSachCauHoiAi(danhSachCauHoi: List<ChiTietCauHoiAi>)

    @Transaction
    suspend fun luuTronBoDeThiAi(deThi: LichSuDeThi, danhSachCauHoi: List<ChiTietCauHoiAi>) {
        luuLichSuDeThi(deThi)
        luuDanhSachCauHoiAi(danhSachCauHoi)
    }

    @Query("SELECT * FROM bang_lich_su_de_thi ORDER BY thoiGianTao DESC")
    fun layToanBoLichSuDeThi(): Flow<List<LichSuDeThi>>

    @Query("SELECT * FROM bang_lich_su_de_thi WHERE idDeThi = :idDeThi LIMIT 1")
    suspend fun layDeThiTheoId(idDeThi: String): LichSuDeThi?

    @Query("SELECT * FROM bang_chi_tiet_cau_hoi_ai WHERE idDeThi = :idDeThi")
    suspend fun layDanhSachCauHoiTheoDeThi(idDeThi: String): List<ChiTietCauHoiAi>

    @Query("UPDATE bang_lich_su_de_thi SET diemDatDuoc = :diemMoi WHERE idDeThi = :idDeThi")
    suspend fun capNhatDiemSo(idDeThi: String, diemMoi: Float)
}