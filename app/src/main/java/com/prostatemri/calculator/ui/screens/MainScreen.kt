package com.prostatemri.calculator.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.prostatemri.calculator.data.models.ClinicalCategory
import com.prostatemri.calculator.data.models.VolumeResult
import com.prostatemri.calculator.ui.theme.*
import com.prostatemri.calculator.viewmodel.MainViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavController,
    viewModel: MainViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // FocusRequester for each measurement field
    val wFocusRequester = remember { FocusRequester() }
    val hFocusRequester = remember { FocusRequester() }
    val dFocusRequester = remember { FocusRequester() }

    // 수동 입력 요청 토스트 + 포커스 이동
    LaunchedEffect(uiState.showManualInputToast) {
        if (uiState.showManualInputToast) {
            Toast.makeText(context, "수동으로 입력해주세요", Toast.LENGTH_SHORT).show()
            viewModel.clearManualInputToast()
            try {
                when {
                    uiState.wError -> wFocusRequester.requestFocus()
                    uiState.hError -> hFocusRequester.requestFocus()
                    uiState.dError -> dFocusRequester.requestFocus()
                }
            } catch (_: Exception) {}
        }
    }

    // 결과 카드로 자동 스크롤
    LaunchedEffect(uiState.scrollToResult) {
        if (uiState.scrollToResult) {
            kotlinx.coroutines.delay(150)
            scrollState.animateScrollTo(scrollState.maxValue)
            viewModel.clearScrollToResult()
        }
    }

    // Camera temp URI
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }

    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.setImageUri(it) }
    }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) viewModel.setImageUri(cameraImageUri)
    }

    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraImageUri = createCameraUri(context)
            cameraImageUri?.let { cameraLauncher.launch(it) }
        }
    }

    // Gallery permission launcher (for Android < 13)
    val galleryPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) galleryLauncher.launch("image/*")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "전립선 MRI 부피 계산기",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                actions = {
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "설정",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface
                )
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ─── Image Selection Section ───────────────────────────────
            SectionCard {
                Text(
                    text = "MRI 이미지 선택",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES)
                                    == PackageManager.PERMISSION_GRANTED
                                ) {
                                    galleryLauncher.launch("image/*")
                                } else {
                                    galleryPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                                }
                            } else {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
                                    == PackageManager.PERMISSION_GRANTED
                                ) {
                                    galleryLauncher.launch("image/*")
                                } else {
                                    galleryPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp, MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PhotoLibrary,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("갤러리")
                    }

                    OutlinedButton(
                        onClick = {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                                == PackageManager.PERMISSION_GRANTED
                            ) {
                                cameraImageUri = createCameraUri(context)
                                cameraImageUri?.let { cameraLauncher.launch(it) }
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.secondary
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp, MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("카메라")
                    }
                }

                // Image Preview
                AnimatedVisibility(visible = uiState.selectedImageUri != null) {
                    uiState.selectedImageUri?.let { uri ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                                .height(220.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, DarkOutline, RoundedCornerShape(8.dp))
                        ) {
                            AsyncImage(
                                model = uri,
                                contentDescription = "선택된 MRI 이미지",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }
            }

            // ─── AI Analysis Section ───────────────────────────────────
            SectionCard {
                Text(
                    text = "AI 자동 분석",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Button(
                    onClick = { viewModel.analyzeImage() },
                    enabled = uiState.selectedImageUri != null && !uiState.isAnalyzing,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = DarkBackground,
                        disabledContainerColor = DarkSurfaceVariant,
                        disabledContentColor = Color.Gray
                    )
                ) {
                    if (uiState.isAnalyzing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = DarkBackground,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("분석 중...")
                    } else {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("AI 분석", fontWeight = FontWeight.Bold)
                    }
                }

                // Extracted text result
                AnimatedVisibility(
                    visible = uiState.extractedText.isNotEmpty(),
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut()
                ) {
                    Column(modifier = Modifier.padding(top = 10.dp)) {
                        Text(
                            text = "추출된 텍스트",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.Gray
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = uiState.extractedText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFFFD54F),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkSurfaceVariant, RoundedCornerShape(6.dp))
                                .padding(10.dp)
                        )
                    }
                }
            }

            // ─── Measurement Input Section ─────────────────────────────
            SectionCard {
                Text(
                    text = "측정값 입력 (mm)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MeasurementField(
                        label = "가로 W",
                        value = uiState.wValue,
                        onValueChange = viewModel::setWValue,
                        isError = uiState.wError,
                        focusRequester = wFocusRequester,
                        modifier = Modifier.weight(1f)
                    )
                    MeasurementField(
                        label = "세로 H",
                        value = uiState.hValue,
                        onValueChange = viewModel::setHValue,
                        isError = uiState.hError,
                        focusRequester = hFocusRequester,
                        modifier = Modifier.weight(1f)
                    )
                    MeasurementField(
                        label = "높이 D",
                        value = uiState.dValue,
                        onValueChange = viewModel::setDValue,
                        isError = uiState.dError,
                        focusRequester = dFocusRequester,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = { viewModel.calculateVolume() },
                    enabled = uiState.wValue.isNotEmpty() &&
                            uiState.hValue.isNotEmpty() &&
                            uiState.dValue.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SecondaryTeal,
                        contentColor = DarkBackground,
                        disabledContainerColor = DarkSurfaceVariant,
                        disabledContentColor = Color.Gray
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Calculate,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("부피 계산", fontWeight = FontWeight.Bold)
                }
            }

            // ─── Result Card ───────────────────────────────────────────
            AnimatedVisibility(
                visible = uiState.volumeResult != null,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut()
            ) {
                uiState.volumeResult?.let { result ->
                    VolumeResultCard(result = result)
                }
            }

            // ─── Error Snackbar ────────────────────────────────────────
            AnimatedVisibility(visible = uiState.errorMessage != null) {
                uiState.errorMessage?.let { error ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF3E1A1A)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ErrorOutline,
                                contentDescription = null,
                                tint = ColorSevereBPH,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = error,
                                color = ColorSevereBPH,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { viewModel.clearError() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "닫기",
                                    tint = ColorSevereBPH,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MeasurementField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean = false,
    focusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier
) {
    val fieldModifier = if (focusRequester != null) {
        modifier.focusRequester(focusRequester)
    } else {
        modifier
    }

    OutlinedTextField(
        value = value,
        onValueChange = { newValue ->
            if (newValue.isEmpty() || newValue.matches(Regex("^-?\\d*\\.?\\d*$"))) {
                onValueChange(newValue)
            }
        },
        label = { Text(label, fontSize = 12.sp) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        isError = isError,
        modifier = fieldModifier,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = DarkOutline,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = Color.Gray,
            focusedTextColor = OnDarkBackground,
            unfocusedTextColor = OnDarkSurface,
            cursorColor = MaterialTheme.colorScheme.primary,
            errorBorderColor = Color.Red,
            errorLabelColor = Color.Red,
            errorCursorColor = Color.Red
        ),
        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center)
    )
}

@Composable
private fun VolumeResultCard(result: VolumeResult) {
    val (categoryColor, bgColor) = when (result.category) {
        ClinicalCategory.BELOW_NORMAL -> Pair(ColorBelowNormal, Color(0xFF0D2035))
        ClinicalCategory.NORMAL -> Pair(ColorNormal, Color(0xFF0D2818))
        ClinicalCategory.MILD_BPH -> Pair(ColorMildBPH, Color(0xFF2A2000))
        ClinicalCategory.MODERATE_BPH -> Pair(ColorModerateBPH, Color(0xFF2A1200))
        ClinicalCategory.SEVERE_BPH -> Pair(ColorSevereBPH, Color(0xFF2A0D0D))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, categoryColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "전립선 부피",
                style = MaterialTheme.typography.labelLarge,
                color = Color.Gray
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = String.format("%.1f mL", result.volume),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = categoryColor
            )

            Spacer(Modifier.height(12.dp))

            Divider(color = categoryColor.copy(alpha = 0.3f), thickness = 1.dp)

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(categoryColor, shape = androidx.compose.foundation.shape.CircleShape)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = result.category.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = categoryColor,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(4.dp))

            Text(
                text = result.category.rangeDescription,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )

            Spacer(Modifier.height(16.dp))

            ClinicalReferenceTable(currentVolume = result.volume)
        }
    }
}

@Composable
private fun ClinicalReferenceTable(currentVolume: Double) {
    val ranges = listOf(
        Triple("< 20 mL", "정상 이하", ColorBelowNormal),
        Triple("20–30 mL", "정상", ColorNormal),
        Triple("30–50 mL", "경도 비대 (BPH)", ColorMildBPH),
        Triple("50–80 mL", "중등도 비대", ColorModerateBPH),
        Triple("> 80 mL", "중증 비대", ColorSevereBPH)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurfaceVariant, RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "임상 해석 기준",
            style = MaterialTheme.typography.labelLarge,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        ranges.forEach { (range, label, color) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(color, shape = androidx.compose.foundation.shape.CircleShape)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = range,
                    style = MaterialTheme.typography.bodyMedium,
                    color = color,
                    modifier = Modifier.width(80.dp),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnDarkSurface
                )
            }
        }
    }
}

private fun createCameraUri(context: Context): Uri {
    val imageFile = File(context.cacheDir, "images").also { it.mkdirs() }
        .let { File(it, "camera_photo_${System.currentTimeMillis()}.jpg") }
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        imageFile
    )
}
