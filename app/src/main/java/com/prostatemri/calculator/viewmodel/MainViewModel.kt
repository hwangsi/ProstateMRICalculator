package com.prostatemri.calculator.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.prostatemri.calculator.data.OcrService
import com.prostatemri.calculator.data.models.ClinicalCategory
import com.prostatemri.calculator.data.models.VolumeResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.PI

data class MainUiState(
    val selectedImageUri: Uri? = null,
    val extractedText: String = "",
    val wValue: String = "",
    val hValue: String = "",
    val dValue: String = "",
    val isAnalyzing: Boolean = false,
    val volumeResult: VolumeResult? = null,
    val errorMessage: String? = null,
    // 파싱 실패 필드 표시
    val wError: Boolean = false,
    val hError: Boolean = false,
    val dError: Boolean = false,
    // 자동 흐름 트리거 (one-shot)
    val showManualInputToast: Boolean = false,
    val scrollToResult: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val ocrService = OcrService()

    // 이미지 선택/촬영 완료 → 자동으로 OCR 실행
    fun setImageUri(uri: Uri?) {
        _uiState.value = _uiState.value.copy(
            selectedImageUri = uri,
            volumeResult = null,
            errorMessage = null,
            extractedText = "",
            wError = false, hError = false, dError = false
        )
        if (uri != null) analyzeImage()
    }

    fun setWValue(value: String) {
        _uiState.value = _uiState.value.copy(wValue = value, wError = false)
    }
    fun setHValue(value: String) {
        _uiState.value = _uiState.value.copy(hValue = value, hError = false)
    }
    fun setDValue(value: String) {
        _uiState.value = _uiState.value.copy(dValue = value, dError = false)
    }
    fun clearError() { _uiState.value = _uiState.value.copy(errorMessage = null) }
    fun clearManualInputToast() { _uiState.value = _uiState.value.copy(showManualInputToast = false) }
    fun clearScrollToResult() { _uiState.value = _uiState.value.copy(scrollToResult = false) }

    fun analyzeImage() {
        val uri = _uiState.value.selectedImageUri ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAnalyzing = true, errorMessage = null)

            try {
                val context = getApplication<Application>()
                val result = ocrService.analyzeImage(context, uri)

                val wMissing = result.w == null
                val hMissing = result.h == null
                val dMissing = result.d == null

                _uiState.value = _uiState.value.copy(
                    isAnalyzing = false,
                    extractedText = result.extractedText,
                    wValue = result.w?.let { formatDouble(it) } ?: "",
                    hValue = result.h?.let { formatDouble(it) } ?: "",
                    dValue = result.d?.let { formatDouble(it) } ?: "",
                    wError = wMissing,
                    hError = hMissing,
                    dError = dMissing,
                    showManualInputToast = wMissing || hMissing || dMissing
                )

                // W/H/D 모두 파싱 성공 시 자동으로 부피 계산
                if (!wMissing && !hMissing && !dMissing) {
                    calculateVolume()
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isAnalyzing = false,
                    errorMessage = "분석 실패: ${e.message}"
                )
            }
        }
    }

    fun calculateVolume() {
        val w = _uiState.value.wValue.toDoubleOrNull()
        val h = _uiState.value.hValue.toDoubleOrNull()
        val d = _uiState.value.dValue.toDoubleOrNull()

        if (w == null || h == null || d == null) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "W, H, D 값을 모두 올바르게 입력해주세요."
            )
            return
        }

        if (w <= 0 || h <= 0 || d <= 0) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "측정값은 0보다 커야 합니다."
            )
            return
        }

        val volume = w * h * d * PI / 6.0 / 1000.0
        val category = getClinicalCategory(volume)

        _uiState.value = _uiState.value.copy(
            volumeResult = VolumeResult(volume, category),
            errorMessage = null,
            scrollToResult = true
        )
    }

    private fun getClinicalCategory(volume: Double): ClinicalCategory = when {
        volume < 20.0 -> ClinicalCategory.BELOW_NORMAL
        volume <= 30.0 -> ClinicalCategory.NORMAL
        volume <= 50.0 -> ClinicalCategory.MILD_BPH
        volume <= 80.0 -> ClinicalCategory.MODERATE_BPH
        else -> ClinicalCategory.SEVERE_BPH
    }

    private fun formatDouble(value: Double): String {
        return if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            String.format("%.1f", value)
        }
    }
}
