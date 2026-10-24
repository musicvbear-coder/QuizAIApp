package com.example.quizaiapp.data.presentation.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quizaiapp.data.repository.GeminiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AiQuizUiState {
    object Idle : AiQuizUiState()
    object Loading : AiQuizUiState()
    data class Success(val idDeThi: String) : AiQuizUiState()
    data class Error(val message: String) : AiQuizUiState()
}

class AiQuizViewModel(
    private val geminiRepository: GeminiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AiQuizUiState>(AiQuizUiState.Idle)
    val uiState: StateFlow<AiQuizUiState> = _uiState.asStateFlow()

    fun taoDeThiBangAi(chuDe: String, soLuongCau: Int) {
        if (chuDe.isBlank()) {
            _uiState.value = AiQuizUiState.Error("Chủ đề kiểm tra không được để trống!")
            return
        }

        viewModelScope.launch {
            _uiState.value = AiQuizUiState.Loading
            val result = geminiRepository.taoVaLuuDeThiAi(chuDe, soLuongCau)
            if (result.isSuccess) {
                val idDeThi = result.getOrNull() ?: ""
                _uiState.value = AiQuizUiState.Success(idDeThi)
            } else {
                val errorMsg = result.exceptionOrNull()?.localizedMessage ?: "Lỗi không xác định khi gọi AI API."
                _uiState.value = AiQuizUiState.Error(errorMsg)
            }
        }
    }
}