package com.prostatemri.calculator.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.prostatemri.calculator.data.models.AnalysisResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val MAX_FILE_SIZE = 7 * 1024 * 1024L  // 7MB
private const val MAX_IMAGE_SIZE = 1280

class OcrService {

    suspend fun analyzeImage(context: Context, uri: Uri): AnalysisResult =
        withContext(Dispatchers.Default) {
            // 파일 크기 확인
            val fileSize = try {
                context.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: 0L
            } catch (e: Exception) {
                0L
            }
            if (fileSize > MAX_FILE_SIZE) {
                throw Exception("이미지 파일 크기가 7MB를 초과합니다.")
            }

            // 비트맵 로드
            val originalBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            } ?: throw Exception("이미지를 읽을 수 없습니다.")

            // 긴 쪽 최대 1280px로 리사이즈
            val resizedBitmap = resizeBitmap(originalBitmap, MAX_IMAGE_SIZE)

            // 노란색 픽셀 마스킹 → 흑백 이미지
            val maskedBitmap = applyYellowMask(resizedBitmap)

            // ML Kit OCR
            val extractedText = recognizeText(maskedBitmap)

            // mm 단위 숫자 파싱 → 큰 순서로 W, H, D 배정
            val mmValues = parseMmValues(extractedText)
            val topThree = mmValues.sortedDescending().take(3)

            AnalysisResult(
                extractedText = extractedText,
                w = topThree.getOrNull(0),
                h = topThree.getOrNull(1),
                d = topThree.getOrNull(2)
            )
        }

    private fun resizeBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val longerSide = maxOf(width, height)
        if (longerSide <= maxSize) return bitmap
        val scale = maxSize.toFloat() / longerSide
        return Bitmap.createScaledBitmap(
            bitmap,
            (width * scale).toInt(),
            (height * scale).toInt(),
            true
        )
    }

    // 노란색 픽셀: R>180, G>180, B<80 → 흰색 / 나머지 → 검정
    private fun applyYellowMask(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = Color.red(pixel)
            val g = Color.green(pixel)
            val b = Color.blue(pixel)
            pixels[i] = if (r > 180 && g > 180 && b < 80) Color.WHITE else Color.BLACK
        }

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        result.setPixels(pixels, 0, width, 0, 0, width, height)
        return result
    }

    private suspend fun recognizeText(bitmap: Bitmap): String =
        suspendCancellableCoroutine { continuation ->
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            continuation.invokeOnCancellation { recognizer.close() }

            recognizer.process(inputImage)
                .addOnSuccessListener { result ->
                    recognizer.close()
                    continuation.resume(result.text)
                }
                .addOnFailureListener { e ->
                    recognizer.close()
                    continuation.resumeWithException(Exception("텍스트 인식 실패: ${e.message}"))
                }
        }

    // mm 단위 숫자 추출
    // - 앞쪽 오염 문자(--, -t, : 등)는 regex 스캔 시 자동 skip
    // - 정수 1~3자리 + 선택적 소수점 1~2자리
    // - 유효 범위: 0 초과 100 이하, 중복 제거
    private fun parseMmValues(text: String): List<Double> {
        val regex = Regex("""(\d{1,3}(?:\.\d{1,2})?)\s*mm""", RegexOption.IGNORE_CASE)
        return regex.findAll(text)
            .mapNotNull { it.groupValues[1].toDoubleOrNull() }
            .filter { it > 0.0 && it <= 100.0 }
            .distinct()
            .toList()
    }
}
