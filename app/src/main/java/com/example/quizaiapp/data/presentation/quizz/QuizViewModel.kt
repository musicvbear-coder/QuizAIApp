package com.example.quizaiapp.data.presentation.quizz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quizaiapp.data.local.dao.TruyVanDeThiDao
import com.example.quizaiapp.data.local.entity.ChiTietCauHoiAi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class QuizViewModel(
    private val dao: TruyVanDeThiDao,
    private val idDeThi: String
) : ViewModel() {

    private val _cauHoiList = MutableStateFlow<List<ChiTietCauHoiAi>>(emptyList())
    val cauHoiList: StateFlow<List<ChiTietCauHoiAi>> = _cauHoiList.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val userAnswers = mutableMapOf<Int, String>()

    init {
        loadCauHoi()
    }

    private fun loadCauHoi() {
        viewModelScope.launch {
            val list = dao.layDanhSachCauHoiTheoDeThi(idDeThi)
            _cauHoiList.value = list
        }
    }

    fun chonDapAn(position: Int, answer: String) {
        userAnswers[position] = answer
    }

    fun layDapAnUser(position: Int): String? {
        return userAnswers[position]
    }

    fun setIndex(index: Int) {
        if (index in 0 until _cauHoiList.value.size) {
            _currentIndex.value = index
        }
    }

    fun tinhKetQua(): Pair<Int, Float> {
        val questions = _cauHoiList.value
        if (questions.isEmpty()) return Pair(0, 0f)

        var soCauDung = 0
        questions.forEachIndexed { index, cauHoi ->
            val userAns = userAnswers[index] ?: ""
            if (userAns.equals(cauHoi.dapAnChinhXac, ignoreCase = true)) {
                soCauDung++
            }
        }
        val diemMoiCau = 10.0f / questions.size
        val tongDiem = soCauDung * diemMoiCau
        return Pair(soCauDung, tongDiem)
    }
}