package com.prostatemri.calculator.data.models

data class AnalysisResult(
    val extractedText: String,
    val w: Double?,
    val h: Double?,
    val d: Double?
)

enum class ClinicalCategory(
    val label: String,
    val rangeDescription: String
) {
    BELOW_NORMAL("정상 이하", "< 20 mL"),
    NORMAL("정상", "20–30 mL"),
    MILD_BPH("경도 비대 (BPH)", "30–50 mL"),
    MODERATE_BPH("중등도 비대", "50–80 mL"),
    SEVERE_BPH("중증 비대", "> 80 mL")
}

data class VolumeResult(
    val volume: Double,
    val category: ClinicalCategory
)
