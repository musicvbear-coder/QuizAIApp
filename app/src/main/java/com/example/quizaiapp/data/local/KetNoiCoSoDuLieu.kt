package com.example.quizaiapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.quizaiapp.data.local.dao.NganHangDao
import com.example.quizaiapp.data.local.dao.NguoiDungDao
import com.example.quizaiapp.data.local.dao.TruyVanDeThiDao
import com.example.quizaiapp.data.local.entity.CauHoi
import com.example.quizaiapp.data.local.entity.ChiTietCauHoiAi
import com.example.quizaiapp.data.local.entity.ChuongHoc
import com.example.quizaiapp.data.local.entity.LichSuDeThi
import com.example.quizaiapp.data.local.entity.MonHoc
import com.example.quizaiapp.data.local.entity.NguoiDung
import com.example.quizaiapp.data.local.entity.PhuongAnTraLoi

@Database(
    entities = [
        MonHoc::class,
        ChuongHoc::class,
        CauHoi::class,
        PhuongAnTraLoi::class,
        LichSuDeThi::class,
        ChiTietCauHoiAi::class,
        NguoiDung::class
    ],
    version = 1,
    exportSchema = false
)
abstract class KetNoiCoSoDuLieu : RoomDatabase() {

    abstract fun truyVanDeThiDao(): TruyVanDeThiDao
    abstract fun nguoiDungDao(): NguoiDungDao
    abstract fun nganHangDao(): NganHangDao

    companion object {
        @Volatile
        private var INSTANCE: KetNoiCoSoDuLieu? = null

        fun layInstance(context: Context): KetNoiCoSoDuLieu {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    KetNoiCoSoDuLieu::class.java,
                    "quiz_ai_database.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}