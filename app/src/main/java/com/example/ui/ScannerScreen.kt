package com.example.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CreativeReuse
import com.example.data.GeminiScanResult
import com.example.data.RecyclingMethod
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import android.speech.SpeechRecognizer
import android.speech.RecognizerIntent
import android.speech.RecognitionListener
import android.speech.tts.TextToSpeech
import android.content.Intent
import android.os.Bundle
import java.util.Locale
import android.util.Log
import androidx.compose.ui.draw.scale

@Composable
fun ScannerScreen(
    viewModel: MainViewModel
) {
    val isScanning by viewModel.isScanning.collectAsState()
    val activeResult by viewModel.activeScanResult.collectAsState()

    var showVoiceAssistant by remember { mutableStateOf(false) }
    var showOnboardingModal by remember { mutableStateOf(true) }

    val autoTriggerVoice by viewModel.autoTriggerVoiceAssistant.collectAsState()
    LaunchedEffect(autoTriggerVoice) {
        if (autoTriggerVoice) {
            showVoiceAssistant = true
            val isAr = viewModel.currentLanguage.value == "ar"
            viewModel.queryVoiceAssistant(
                if (isAr) "أعطني نصيحة سريعة لإعادة التدوير في ساعة الذروة الحالية!"
                else "Give me a quick peak hour recycling strategy reminder!"
            )
            viewModel.consumeAutoTriggerVoiceAssistant()
        }
    }
    var selectedSimulatedItem by remember { mutableStateOf<String?>(null) }
    var showScanningAnimation by remember { mutableStateOf(false) }
    var scanStepText by remember { mutableStateOf("🔍 جاري الفحص...") }
    var scannerMode by remember { mutableStateOf(0) } // 0 = Camera, 1 = Barcode/QR
    var manualBarcodeText by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Helper function to decode URI to Bitmap
    fun uriToBitmap(uri: android.net.Uri): Bitmap? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                showScanningAnimation = true
                val isAr = viewModel.currentLanguage.value == "ar"
                scanStepText = if (isAr) "🔍 جاري معالجة الصورة..." else "🔍 Processing gallery image..."
                val bitmap = uriToBitmap(uri)
                if (bitmap != null) {
                    scanStepText = if (isAr) "🔍 جاري الفحص بالذكاء الاصطناعي..." else "🔍 AI Classification in progress..."
                    viewModel.triggerScan(bitmap, null)
                } else {
                    Toast.makeText(context, if (isAr) "فشل قراءة الصورة" else "Failed to read image", Toast.LENGTH_SHORT).show()
                }
                showScanningAnimation = false
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            coroutineScope.launch {
                showScanningAnimation = true
                val isAr = viewModel.currentLanguage.value == "ar"
                scanStepText = if (isAr) "🔍 جاري الفحص بالذكاء الاصطناعي..." else "🔍 AI Classification in progress..."
                viewModel.triggerScan(bitmap, null)
                showScanningAnimation = false
            }
        } else {
            Toast.makeText(context, if (viewModel.currentLanguage.value == "ar") "لم يتم التقاط أي صورة" else "No photo taken", Toast.LENGTH_SHORT).show()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            cameraLauncher.launch(null)
        } else {
            Toast.makeText(context, if (viewModel.currentLanguage.value == "ar") "إذن الكاميرا مطلوب لمسح النفايات" else "Camera permission is required to scan waste", Toast.LENGTH_LONG).show()
        }
    }

    // Laser scan animation values
    val infiniteTransition = rememberInfiniteTransition(label = "laser")
    val laserYPercent by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_pos"
    )

    // Deposit Simulator Dialog & States removed for campus bins cleanup



    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (activeResult != null) {
            // Display Analysis Results Screen
            ScanResultsView(
                result = activeResult!!,
                onDismiss = {
                    viewModel.clearScanResult()
                    selectedSimulatedItem = null
                },
                viewModel = viewModel
            )
        } else if (showScanningAnimation || isScanning) {
            // Processing Scanning Screen
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Spinning Eco-Loader
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .drawBehind {
                            drawCircle(
                                color = ForestMid,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 8.dp.toPx())
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val angleTransition = rememberInfiniteTransition(label = "loader")
                    val rotationAngle by angleTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1500, easing = LinearEasing)
                        ),
                        label = "rotation"
                    )

                    Icon(
                        imageVector = Icons.Default.Eco,
                        contentDescription = null,
                        tint = LeafGreen,
                        modifier = Modifier
                            .size(54.dp)
                            .drawBehind {
                                // Rotate effect
                            }
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = scanStepText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = LimePulse,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "نستخدم الذكاء الاصطناعي لتصنيف المواد وابتكار حلول إعادة التدوير من أجلك.",
                    fontSize = 12.sp,
                    color = MistWhite.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
            }

            // Update loading stages
            LaunchedEffect(showScanningAnimation) {
                scanStepText = "🔍 جاري التعرف على المخلف..."
                delay(1200)
                scanStepText = "⚗️ تحليل المواد وتركيبها..."
                delay(1200)
                scanStepText = "♻️ ابتكار طرق تدوير وحلول DIY..."
            }
        } else {
            // 1. Live Camera / Simulation Viewfinder
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = viewModel.translate("scan_waste_ai"),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        // Elegant luxury info badge to show onboarding modal
                        IconButton(
                            onClick = { showOnboardingModal = true },
                            modifier = Modifier
                                .size(28.dp)
                                .background(ForestMid.copy(alpha = 0.5f), CircleShape)
                                .border(0.8.dp, GoldEarth.copy(alpha = 0.4f), CircleShape)
                                .testTag("help_onboarding_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.HelpOutline,
                                contentDescription = "إرشادات",
                                tint = GoldEarth,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Glowing pulse-like luxury voice assistant button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(ForestMid, LeafGreen.copy(alpha = 0.8f))
                                )
                            )
                            .border(1.dp, GoldEarth.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .clickable {
                                showVoiceAssistant = true
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("voice_assistant_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardVoice,
                                contentDescription = null,
                                tint = GoldEarth,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "المساعد البيئي الصوتي",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CreamPaper
                            )
                        }
                    }
                }

                // Elegant Segmented Mode Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(ForestMid.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                        .border(1.dp, GoldEarth.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isAr = viewModel.currentLanguage.value == "ar"
                    val cameraText = if (isAr) "📸 الكاميرا البيئية" else "📸 Eco Camera"
                    val barcodeText = if (isAr) "🏷️ قارئ الباركود والـ QR" else "🏷️ Barcode & QR"

                    // Camera Tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (scannerMode == 0) LeafGreen else Color.Transparent)
                            .clickable { scannerMode = 0 }
                            .testTag("eco_camera_tab"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = cameraText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (scannerMode == 0) CreamPaper else MistWhite.copy(alpha = 0.7f)
                        )
                    }

                    // Barcode Tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (scannerMode == 1) LeafGreen else Color.Transparent)
                            .clickable { scannerMode = 1 }
                            .testTag("barcode_qr_tab"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = barcodeText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (scannerMode == 1) CreamPaper else MistWhite.copy(alpha = 0.7f)
                        )
                    }
                }

                if (scannerMode == 0) {
                    // Viewfinder Frame
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                    ) {
                        // Viewfinder corners decoration (pulsing)
                        val alphaScale by rememberInfiniteTransition(label = "pulse").animateFloat(
                            initialValue = 0.4f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1200, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "alpha"
                        )

                        // simulated viewfinder lines
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp)
                                .border(1.5.dp, LeafGreen.copy(alpha = alphaScale), RoundedCornerShape(16.dp))
                        ) {
                            // Moving laser scanning line
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .offset(y = (24.dp + (laserYPercent * 300).dp))
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(Color.Transparent, LimePulse, Color.Transparent)
                                        )
                                    )
                            )
                        }

                        // Simulated Camera Feed Text
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = MistWhite.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = if (selectedSimulatedItem != null) {
                                    val itemKey = when (selectedSimulatedItem) {
                                        "بلاستيك" -> "sample_plastic"
                                        "كرتون" -> "sample_cardboard"
                                        "معدن" -> "sample_aluminum"
                                        "طعام" -> "sample_food"
                                        else -> "sample_plastic"
                                    }
                                    "${viewModel.translate("selected") ?: ""} ${viewModel.translate(itemKey)}"
                                } else {
                                    viewModel.translate("camera_viewfinder_instruction")
                                },
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Quick Simulation selector
                    Text(
                        text = viewModel.translate("choose_sample_for_scan"),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SimulatedItemChip(
                            emoji = "🧴",
                            label = viewModel.translate("sample_plastic"),
                            isSelected = selectedSimulatedItem == "بلاستيك",
                            onClick = { selectedSimulatedItem = "بلاستيك" },
                            modifier = Modifier.weight(1f)
                        )
                        SimulatedItemChip(
                            emoji = "📦",
                            label = viewModel.translate("sample_cardboard"),
                            isSelected = selectedSimulatedItem == "كرتون",
                            onClick = { selectedSimulatedItem = "كرتون" },
                            modifier = Modifier.weight(1f)
                        )
                        SimulatedItemChip(
                            emoji = "🥤",
                            label = viewModel.translate("sample_aluminum"),
                            isSelected = selectedSimulatedItem == "معدن",
                            onClick = { selectedSimulatedItem = "معدن" },
                            modifier = Modifier.weight(1f)
                        )
                        SimulatedItemChip(
                            emoji = "🌿",
                            label = viewModel.translate("sample_food"),
                            isSelected = selectedSimulatedItem == "طعام",
                            onClick = { selectedSimulatedItem = "طعام" },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Action Controls
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 80.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Photo library action
                        OutlinedButton(
                            onClick = {
                                galleryLauncher.launch("image/*")
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onBackground),
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1.5f)
                                .height(54.dp)
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = LeafGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(viewModel.translate("from_gallery"), fontSize = 13.sp)
                        }

                        // Big Round Capture Trigger
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(LeafGreen)
                                .clickable {
                                    val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                                        context,
                                        android.Manifest.permission.CAMERA
                                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                    if (hasPermission) {
                                        cameraLauncher.launch(null)
                                    } else {
                                        permissionLauncher.launch(android.Manifest.permission.CAMERA)
                                    }
                                }
                                .testTag("capture_photo_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.background)
                                    .border(2.dp, LeafGreen, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(LimePulse)
                                )
                            }
                        }

                        // Flash trigger mock
                        OutlinedButton(
                            onClick = {
                                viewModel.triggerScan(null, selectedSimulatedItem ?: "بلاستيك")
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CreamPaper),
                            border = BorderStroke(1.5.dp, ForestMid),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                        ) {
                            Icon(Icons.Default.FlashOn, contentDescription = null, tint = GoldEarth)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("وميض تلقائي", fontSize = 12.sp)
                        }
                    }
                } else {
                    // Barcode & QR Code Reader Layout
                    BarcodeScannerLayout(
                        viewModel = viewModel,
                        laserYPercent = laserYPercent,
                        onScanBarcode = { code ->
                            viewModel.triggerBarcodeScan(code)
                        }
                    )
                }
            }
        }

        if (showVoiceAssistant) {
            VoiceAssistantOverlay(
                viewModel = viewModel,
                onDismiss = { showVoiceAssistant = false }
            )
        }

        if (showOnboardingModal) {
            ScannerOnboardingModal(
                onDismiss = { showOnboardingModal = false }
            )
        }
    }
}

@Composable
fun SimulatedItemChip(
    emoji: String,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .border(
                width = 1.5.dp,
                color = if (isSelected) LeafGreen else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(emoji, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ScanResultsView(
    result: GeminiScanResult,
    onDismiss: () -> Unit,
    viewModel: MainViewModel
) {
    var showSuccessNotification by remember { mutableStateOf(false) }
    
    LaunchedEffect(result) {
        showSuccessNotification = true
        delay(3500)
        showSuccessNotification = false
    }
    
    val currentLang = viewModel.currentLanguage.collectAsState().value
    val isAr = currentLang == "ar"

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentPadding = PaddingValues(bottom = 100.dp, top = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title Bar with Back arrow
            item {
                AnimatedEntryContainer(delayMillis = 50) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .testTag("dismiss_results_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = viewModel.translate("back"),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = viewModel.translate("scan_result_title"),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    val context = androidx.compose.ui.platform.LocalContext.current
                    IconButton(
                        onClick = {
                            val shareIntent = android.content.Intent().apply {
                                action = android.content.Intent.ACTION_SEND
                                val text = if (isAr) {
                                    "لقد قمت بإعادة تدوير ${result.itemName} ${result.itemEmoji} باستخدام EcoScanner وفرت ${String.format(java.util.Locale.US, "%.1f", result.co2SavedGrams)} جم من انبعاثات الكربون! 🌍♻️"
                                } else {
                                    "I just recycled ${result.itemName} ${result.itemEmoji} using EcoScanner and saved ${String.format(java.util.Locale.US, "%.1f", result.co2SavedGrams)}g of CO2! 🌍♻️"
                                }
                                putExtra(android.content.Intent.EXTRA_TEXT, text)
                                type = "text/plain"
                            }
                            context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Result"))
                        },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = LeafGreen
                        )
                    }
                }
            }
        }

        // Header Card with Name, emoji and score
        item {
            AnimatedEntryContainer(delayMillis = 150) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, LeafGreen, RoundedCornerShape(24.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(result.itemEmoji, fontSize = 36.sp)
                        }

                        Text(
                            text = result.itemName,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = result.materialType,
                                color = LeafGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text("•", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                            Text(
                                text = if (result.recyclable) "قابل لإعادة التدوير ⭐" else "غير قابل للتدوير ⚠️",
                                color = if (result.recyclable) LeafGreen else DangerRust,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Score Progress
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(viewModel.translate("sustainability_index"), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                            Text("${result.recyclabilityScore}/10", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LeafGreen)
                        }
                        LinearProgressIndicator(
                            progress = result.recyclabilityScore / 10f,
                            color = LeafGreen,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                    }
                }
            }
        }

        // Environmental Impact Stats
        item {
            AnimatedEntryContainer(delayMillis = 250) {
                Text(
                    text = viewModel.translate("immediate_environmental_impact"),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        item {
            AnimatedEntryContainer(delayMillis = 350) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ImpactStatsWidget(
                        value = result.decompositionYears,
                        label = viewModel.translate("decomposition_time"),
                        color = DangerRust,
                        modifier = Modifier.weight(1f)
                    )
                    ImpactStatsWidget(
                        value = String.format("%.0fg", result.co2SavedGrams),
                        label = viewModel.translate("co2_saving"),
                        color = LeafGreen,
                        modifier = Modifier.weight(1f)
                    )
                    ImpactStatsWidget(
                        value = String.format("%.1fL", result.waterSavedLiters),
                        label = viewModel.translate("water_saving_label"),
                        color = InfoTeal,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Environmental Impact Description text
        item {
            AnimatedEntryContainer(delayMillis = 450) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = result.environmentalImpact,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        // Fun Fact Banner
        item {
            AnimatedEntryContainer(delayMillis = 550) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, GoldEarth.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Lightbulb, contentDescription = null, tint = GoldEarth)
                        Text(
                            text = result.funFact,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 18.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Steps & Recycling Methods (Module 2)
        item {
            AnimatedEntryContainer(delayMillis = 650) {
                Text(
                    text = viewModel.translate("suggested_recycling_methods"),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        itemsIndexed(result.recyclingMethods) { idx, method ->
            AnimatedEntryContainer(delayMillis = 750 + idx * 100) {
                RecycleMethodCard(method = method, viewModel = viewModel)
            }
        }

        // Creative Reuse (DIY) Ideas
        item {
            val baseDelay = 850 + result.recyclingMethods.size * 100
            AnimatedEntryContainer(delayMillis = baseDelay) {
                Text(
                    text = viewModel.translate("creative_reuse_diy_ideas"),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        item {
            val baseDelay = 950 + result.recyclingMethods.size * 100
            AnimatedEntryContainer(delayMillis = baseDelay) {
                CreativeMindMap(
                    itemName = result.itemName,
                    itemEmoji = result.itemEmoji,
                    ideas = result.creativeReuse,
                    viewModel = viewModel
                )
            }
        }

        // Reward and Actions
        item {
            val baseDelay = 1050 + result.recyclingMethods.size * 100 + result.creativeReuse.size * 100
            AnimatedEntryContainer(delayMillis = baseDelay) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(2.dp, GoldEarth, RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(viewModel.translate("congrats_earned_reward"), fontSize = 13.sp, color = GoldEarth, fontWeight = FontWeight.Bold)
                        Text(
                            text = "+${result.pointsEarned} XP",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = LeafGreen
                        )
                        Text(
                            text = viewModel.translate("points_registered_success"),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onDismiss,
                                colors = ButtonDefaults.buttonColors(containerColor = LeafGreen, contentColor = EcoBlack),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1.2f)
                                    .height(46.dp)
                                    .testTag("navigate_from_results_button")
                            ) {
                                Icon(Icons.Default.Home, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isAr) "الرئيسية" else "Home", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { /* Share Simulation */ },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onBackground),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, tint = LeafGreen)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(viewModel.translate("share_impact"), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        } // End of item
        
    } // End of LazyColumn

    // Elegant floating notification for successful logging
    androidx.compose.animation.AnimatedVisibility(
            visible = showSuccessNotification,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
                .zIndex(10f)
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(30.dp))
                    .background(LeafGreen.copy(alpha = 0.9f))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = CreamPaper,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = if (isAr) "تم تسجيل المخلف بنجاح وحفظه محلياً!" else "Item successfully logged and saved locally!",
                    color = CreamPaper,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ImpactStatsWidget(
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = label, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
    }
}

@Composable
fun RecycleMethodCard(method: RecyclingMethod, viewModel: MainViewModel) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🛠️", fontSize = 11.sp)
                    }
                    Text(text = method.method, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isEasy = method.difficulty.contains("سهل") || method.difficulty.lowercase().contains("easy")
                    Text(
                        text = method.difficulty,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isEasy) LeafGreen else GoldEarth,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = viewModel.translate("scientific_steps"), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LeafGreen)
                    method.steps.forEachIndexed { idx, step ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("${idx + 1}-", color = LeafGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(text = step, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, lineHeight = 18.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${viewModel.translate("target_product")} ${method.result}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${viewModel.translate("amazing_info")} ${method.wowFact}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun CreativeMindMap(
    itemName: String,
    itemEmoji: String,
    ideas: List<CreativeReuse>,
    viewModel: MainViewModel
) {
    if (ideas.isEmpty()) return
    
    var activeIdeaIndex by remember { mutableIntStateOf(0) }
    val isGenerating by viewModel.isGeneratingAlternativeIdea.collectAsState()
    
    // Automatically jump to the newest idea when a new alternative is generated
    LaunchedEffect(ideas.size) {
        if (ideas.isNotEmpty()) {
            activeIdeaIndex = ideas.size - 1
        }
    }
    
    val currentIdea = if (activeIdeaIndex in ideas.indices) ideas[activeIdeaIndex] else ideas.first()
    val currentLang = viewModel.currentLanguage.collectAsState().value

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
            .border(1.dp, LeafGreen.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Central Item
        Box(
            modifier = Modifier
                .size(70.dp)
                .clip(CircleShape)
                .background(LeafGreen.copy(alpha = 0.15f))
                .border(2.dp, LeafGreen, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(itemEmoji, fontSize = 32.sp)
        }
        Text(itemName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 8.dp))
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Connector Line
        Box(modifier = Modifier.width(2.dp).height(20.dp).background(LeafGreen.copy(alpha = 0.5f)))
        
        // Idea Card (The "Mind Map Node") with high-end entry transition animations
        AnimatedContent(
            targetState = currentIdea,
            transitionSpec = {
                (fadeIn(animationSpec = tween(450, easing = LinearOutSlowInEasing)) +
                 slideInVertically(initialOffsetY = { 30 }, animationSpec = tween(450, easing = LinearOutSlowInEasing)))
                    .togetherWith(fadeOut(animationSpec = tween(200, easing = LinearOutSlowInEasing)))
            },
            label = "idea_transition"
        ) { targetIdea ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GoldEarth.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "💡 ${targetIdea.idea}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = targetIdea.difficulty,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldEarth,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(GoldEarth.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    
                    Text(
                        text = "${if (currentLang == "ar") "مواد إضافية:" else "Additional materials:"} ${targetIdea.materialsNeeded.joinToString(", ")}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )

                    if (targetIdea.steps.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 8.dp)) {
                            targetIdea.steps.forEachIndexed { idx, step ->
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("${idx + 1}-", color = LeafGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text(step, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                    
                    if (targetIdea.benefit.isNotEmpty()) {
                        Text(
                            text = "✨ ${targetIdea.benefit}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = LeafGreen,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Changer & alternative generation buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (ideas.size > 1) {
                OutlinedButton(
                    onClick = { 
                        activeIdeaIndex = (activeIdeaIndex + 1) % ideas.size 
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = LeafGreen),
                    border = androidx.compose.foundation.BorderStroke(1.dp, LeafGreen),
                    modifier = Modifier.weight(1f).height(44.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (currentLang == "ar") "فكرة أخرى" else "Another Idea",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Button(
                onClick = { viewModel.fetchAlternativeIdea() },
                enabled = !isGenerating,
                colors = ButtonDefaults.buttonColors(containerColor = LeafGreen, contentColor = EcoBlack),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(44.dp).testTag("change_idea_button")
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(color = EcoBlack, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (currentLang == "ar") "جاري الابتكار..." else "Generating...",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (currentLang == "ar") "فكرة بديلة" else "Change Idea",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceAssistantOverlay(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val voiceResponse by viewModel.voiceAssistantResponse.collectAsState()
    val isLoading by viewModel.isVoiceAssistantLoading.collectAsState()

    var isListening by remember { mutableStateOf(false) }
    var speechText by remember { mutableStateOf("") }
    var manualInputQuery by remember { mutableStateOf("") }

    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }
    var textToSpeech by remember { mutableStateOf<TextToSpeech?>(null) }
    var isTtsInitialized by remember { mutableStateOf(false) }

    val speechRecognizerIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-EG")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ar")
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "ar")
        }
    }

    // Function to speak text out loud using Android TTS
    fun speakText(text: String) {
        if (isTtsInitialized && textToSpeech != null) {
            textToSpeech?.stop()
            val cleanText = text.replace(Regex("[^\\p{L}\\p{N}\\s،؟!]"), "") // strip emojis for smoother pronunciation
            textToSpeech?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, "voice_assistant_utterance")
        }
    }

    // Initialize Recognition and Text-To-Speech
    DisposableEffect(Unit) {
        // Init SpeechRecognizer
        val hasSpeech = SpeechRecognizer.isRecognitionAvailable(context)
        if (hasSpeech) {
            try {
                val recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            isListening = true
                            speechText = "جاري الاستماع... تحدث الآن"
                        }

                        override fun onBeginningOfSpeech() {
                            speechText = "جاري التقاط صوتك..."
                        }

                        override fun onRmsChanged(rmsdB: Float) {}

                        override fun onBufferReceived(buffer: ByteArray?) {}

                        override fun onEndOfSpeech() {
                            isListening = false
                        }

                        override fun onError(error: Int) {
                            isListening = false
                            val errStr = when (error) {
                                SpeechRecognizer.ERROR_AUDIO -> "خطأ في تسجيل الصوت"
                                SpeechRecognizer.ERROR_CLIENT -> "خطأ في الاتصال بالهاتف"
                                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "صلاحيات الميكروفون مطلوبة"
                                SpeechRecognizer.ERROR_NETWORK -> "خطأ في اتصال الإنترنت"
                                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "انتهت مهلة اتصال الإنترنت"
                                SpeechRecognizer.ERROR_NO_MATCH -> "لم يتم التعرف على الكلمات"
                                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "خدمة الصوت مشغولة"
                                SpeechRecognizer.ERROR_SERVER -> "خطأ من خادم التعرف"
                                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "انتهت مهلة التحدث"
                                else -> "فشل التقاط الصوت"
                            }
                            speechText = "تنبيه: $errStr"
                        }

                        override fun onResults(results: Bundle?) {
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            if (!matches.isNullOrEmpty()) {
                                val query = matches[0]
                                speechText = query
                                viewModel.queryVoiceAssistant(query)
                            } else {
                                speechText = "لم أستمع لأي كلمة بوضوح، أعد المحاولة."
                            }
                        }

                        override fun onPartialResults(partialResults: Bundle?) {}

                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }
                speechRecognizer = recognizer
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            speechText = "ميزة التعرف على الصوت غير مدعومة على هذا الجهاز."
        }

        // Init TextToSpeech
        val tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = textToSpeech?.setLanguage(Locale("ar"))
                if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                    isTtsInitialized = true
                }
            }
        }
        textToSpeech = tts

        onDispose {
            speechRecognizer?.destroy()
            textToSpeech?.stop()
            textToSpeech?.shutdown()
            viewModel.clearVoiceAssistantResponse()
        }
    }

    fun startListening() {
        textToSpeech?.stop()
        speechRecognizer?.let { recognizer ->
            try {
                recognizer.startListening(speechRecognizerIntent)
            } catch (e: Exception) {
                speechText = "خطأ في تفعيل الميكروفون"
            }
        }
    }

    // Auto-speak response when it changes
    LaunchedEffect(voiceResponse) {
        voiceResponse?.let {
            speakText(it)
        }
    }

    // Microphone Permission Launcher
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startListening()
        } else {
            Toast.makeText(context, "إذن الميكروفون مطلوب لاستخدام المساعد البيئي", Toast.LENGTH_LONG).show()
        }
    }

    // Pulse animation for visual waveform
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_anim")
    val pulseScale1 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse1"
    )
    val pulseScale2 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 2.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse2"
    )

    // Layout representation
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.88f))
            .clickable(enabled = true, onClick = {}), // Prevent taps passing through
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .border(
                    BorderStroke(
                        1.5.dp,
                        Brush.verticalGradient(listOf(GoldEarth, Color.Transparent))
                    ),
                    RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                ),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Top Header indicator and Close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            textToSpeech?.stop()
                            onDismiss()
                        },
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
                            .size(36.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = DangerRust, modifier = Modifier.size(18.dp))
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isListening) LimePulse else GoldEarth)
                        )
                        Text(
                            text = "المساعد الصوتي دَوِّر",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Reset button
                    TextButton(
                        onClick = {
                            viewModel.clearVoiceAssistantResponse()
                            speechText = ""
                            manualInputQuery = ""
                        }
                    ) {
                        Text("مسح", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                    }
                }

                // AI Visual pulsing core
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isListening || isLoading) {
                        // Outermost pulse circle
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .scale(pulseScale2)
                                .clip(CircleShape)
                                .background(LeafGreen.copy(alpha = 0.15f))
                        )
                        // Middle pulse circle
                        Box(
                            modifier = Modifier
                                .size(70.dp)
                                .scale(pulseScale1)
                                .clip(CircleShape)
                                .background(LimePulse.copy(alpha = 0.25f))
                        )
                    }

                    // Central Mic trigger button
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(LimePulse, ForestMid)
                                )
                            )
                            .border(1.5.dp, GoldEarth, CircleShape)
                            .clickable {
                                micPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "استمع",
                            tint = CreamPaper,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Text(
                    text = if (isListening) "جاري الاستماع إليك... تكلّم الآن" else if (isLoading) "جاري استشارة الذكاء الاصطناعي..." else "اضغط على الميكروفون للتحدث بالعربية",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Medium
                )

                // Speech recognized text or fallback query display
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (speechText.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "🗣️ سألتني: \"$speechText\"",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(12.dp),
                                textAlign = TextAlign.Start
                            )
                        }
                    }

                    // Display assistant advice
                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = LeafGreen, modifier = Modifier.size(24.dp))
                        }
                    } else if (voiceResponse != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, LeafGreen.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                            colors = CardDefaults.cardColors(containerColor = ForestMid.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("🌿", fontSize = 14.sp)
                                    Text(
                                        text = "المساعد الذكي دَوِّر:",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = LeafGreen
                                    )
                                }
                                Text(
                                    text = voiceResponse!!,
                                    fontSize = 14.sp,
                                    color = CreamPaper,
                                    lineHeight = 22.sp,
                                    textAlign = TextAlign.Start
                                )
                                
                                // Replay audio button
                                OutlinedButton(
                                    onClick = { speakText(voiceResponse!!) },
                                    modifier = Modifier.align(Alignment.End).height(32.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    border = BorderStroke(1.dp, LeafGreen.copy(alpha = 0.3f))
                                ) {
                                    Text("🔊", fontSize = 12.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("أعد تشغيل الصوت", fontSize = 10.sp, color = LeafGreen)
                                }
                            }
                        }
                    }

                    // Quick Suggestion chips (staggered display)
                    Text(
                        text = "أسئلة شائعة يمكنك طرحها:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.align(Alignment.Start)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SuggestionChip(
                            onClick = {
                                val q = "كيف أعيد تدوير زجاجة زجاجية؟"
                                speechText = q
                                viewModel.queryVoiceAssistant(q)
                            },
                            label = { Text("🧴 تدوير الزجاج؟", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface) },
                            modifier = Modifier.weight(1f)
                        )
                        SuggestionChip(
                            onClick = {
                                val q = "أين أضع الصناديق الكرتونية المستعملة؟"
                                speechText = q
                                viewModel.queryVoiceAssistant(q)
                            },
                            label = { Text("📦 صناديق الكرتون؟", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SuggestionChip(
                            onClick = {
                                val q = "ما فائدة تدوير علب الصودا المعدنية؟"
                                speechText = q
                                viewModel.queryVoiceAssistant(q)
                            },
                            label = { Text("🥤 علب الألومنيوم؟", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface) },
                            modifier = Modifier.weight(1f)
                        )
                        SuggestionChip(
                            onClick = {
                                val q = "هل البلاستيك الأسود ضار بالبيئة؟"
                                speechText = q
                                viewModel.queryVoiceAssistant(q)
                            },
                            label = { Text("⚠️ بلاستيك أسود؟", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Fallback manual writing area
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = manualInputQuery,
                        onValueChange = { manualInputQuery = it },
                        placeholder = { Text("اكتب سؤالك بالعربية هنا تفصيلاً...", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LeafGreen,
                            unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        ),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            if (manualInputQuery.isNotBlank()) {
                                speechText = manualInputQuery
                                viewModel.queryVoiceAssistant(manualInputQuery)
                                manualInputQuery = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LeafGreen, contentColor = EcoBlack),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "إرسال")
                    }
                }
            }
        }
    }
}

@Composable
fun ScannerOnboardingModal(
    onDismiss: () -> Unit
) {
    var currentStep by remember { mutableStateOf(0) }
    
    val steps = listOf(
        OnboardingStep(
            title = "الكاميرا البيئية الذكية 📸",
            subtitle = "AI Smart Eco-Scanner",
            description = "قم بتوجيه الكاميرا نحو أي مادة (زجاج، بلاستيك، كرتون) أو اختر صورة من المعرض. سيقوم نموذج الذكاء الاصطناعي الفائق بتحليلها تلقائياً وتحديد قابليتها للتدوير ونقاط الـ XP المكتسبة، بالإضافة إلى ابتكار حلول إعادة تدوير يدوية إبداعية ومخصصة لك!",
            icon = Icons.Default.CameraAlt,
            color = LeafGreen
        ),
        OnboardingStep(
            title = "المساعد الصوتي العربي 🎙️",
            subtitle = "Bilingual Voice Eco-Assistant",
            description = "ميزة صوتية فريدة تمكنك من التحدث مع الذكاء الاصطناعي مباشرة بالعربية الفصحى أو الإنجليزية! اضغط على الميكروفون واسأله بصوتك مثل: 'كيف أعيد تدوير هذه الزجاجة؟' وسيجيبك فوراً بتعليمات صوتية مسهبة لتوجيهك خطوة بخطوة.",
            icon = Icons.Default.KeyboardVoice,
            color = GoldEarth
        ),
        OnboardingStep(
            title = "قارئ الباركود والـ QR السريع 🏷️",
            subtitle = "Barcode & QR Eco-Decoder",
            description = "ميزة المسح الفوري لرموز الباركود والـ QR على أغلفة المنتجات الغذائية والاستهلاكية (مثل كوكاكولا، حليب جهينة، كريم إيفا وغيرها)! احصل فوراً على المكونات التفصيلية للغلاف، ونسب المواد، وإرشادات فرز دقيقة وموثوقة محلياً بمصر.",
            icon = Icons.Default.QrCode,
            color = GoldEarth
        ),
        OnboardingStep(
            title = "افرز، اكسب، واحمي الأرض 🌍",
            subtitle = "Sort, Earn & Protect Earth",
            description = "بعد فحص المواد، يمكنك الاتصال الذكي بالصناديق المخصصة عبر الـ NFC لفتح الباب تلقائياً وتفريغ المواد. احصل على نقاط مضاعفة، وارتقِ بمستواك البيئي من 'بذرة' إلى 'بطل الأرض' للحصول على أوسمة فخرية ومكافآت حصرية!",
            icon = Icons.Default.EmojiEvents,
            color = LimePulse
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.88f))
            .clickable(enabled = true, onClick = {}), // Prevent click pass through
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight()
                .border(
                    BorderStroke(
                        1.5.dp,
                        Brush.verticalGradient(
                            listOf(GoldEarth, ForestMid)
                        )
                    ),
                    RoundedCornerShape(24.dp)
                ),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Skip Button on the top corner
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = "تخطي ✕",
                            fontSize = 12.sp,
                            color = MistWhite.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                val step = steps[currentStep]

                // Glowing Icon container
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(step.color.copy(alpha = 0.12f), CircleShape)
                        .border(1.5.dp, step.color, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = step.icon,
                        contentDescription = null,
                        tint = step.color,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Title & Subtitle
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = step.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = CreamPaper,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = step.subtitle,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = GoldEarth,
                        textAlign = TextAlign.Center
                    )
                }

                // Description
                Text(
                    text = step.description,
                    fontSize = 13.sp,
                    color = MistWhite,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Stepper Dots Indicators
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    steps.forEachIndexed { index, _ ->
                        val isSelected = index == currentStep
                        Box(
                            modifier = Modifier
                                .size(if (isSelected) 10.dp else 6.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) GoldEarth else MistWhite.copy(alpha = 0.3f))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom CTA Button (Next / Finish)
                Button(
                    onClick = {
                        if (currentStep < steps.lastIndex) {
                            currentStep++
                        } else {
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = step.color,
                        contentColor = EcoBlack
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("onboarding_next_button")
                ) {
                    Text(
                        text = if (currentStep < steps.lastIndex) "التالي 🌿" else "ابدأ رحلة الاستدامة 🌍",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun AnimatedEntryContainer(
    delayMillis: Int,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(delayMillis.toLong())
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(durationMillis = 800, easing = LinearOutSlowInEasing)) +
                slideInVertically(
                    initialOffsetY = { 80 },
                    animationSpec = tween(durationMillis = 800, easing = LinearOutSlowInEasing)
                ),
        exit = fadeOut(animationSpec = tween(durationMillis = 300))
    ) {
        content()
    }
}

data class OnboardingStep(
    val title: String,
    val subtitle: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color
)

@Composable
fun BarcodeScannerLayout(
    viewModel: MainViewModel,
    laserYPercent: Float,
    onScanBarcode: (String) -> Unit
) {
    val isAr = viewModel.currentLanguage.value == "ar"
    var manualCode by remember { mutableStateOf("") }
    val context = LocalContext.current

    val products = listOf(
        BarcodeProduct(
            name = if (isAr) "كوكاكولا مصر (PET)" else "Coca-Cola PET Bottle",
            brand = if (isAr) "بلاستيك PET قابلة للتدوير" else "Recyclable PET Plastic",
            emoji = "🧴",
            barcode = "5449000000996",
            points = 40
        ),
        BarcodeProduct(
            name = if (isAr) "علبة حليب جهينة" else "Juhayna Milk Carton",
            brand = if (isAr) "كرتون متعدد الطبقات" else "Multi-layer Tetra Pak",
            emoji = "🍼",
            barcode = "6221007011400",
            points = 50
        ),
        BarcodeProduct(
            name = if (isAr) "شوكولاتة كورونا" else "Corona Wrapper",
            brand = if (isAr) "غلاف ألومنيوم رقيق" else "Thin Aluminum Foil",
            emoji = "🍫",
            barcode = "6221003001221",
            points = 20
        ),
        BarcodeProduct(
            name = if (isAr) "كريم إيفا الزجاجي" else "Eva Glass Jar",
            brand = if (isAr) "زجاج نقي فاخر" else "Premium Eco Glass",
            emoji = "🧴",
            barcode = "6221014022307",
            points = 65
        ),
        BarcodeProduct(
            name = if (isAr) "كرتون أمازون مصر" else "Amazon Egypt Box",
            brand = if (isAr) "ورق مموج معاد تدويره" else "Recycled Cardboard",
            emoji = "📦",
            barcode = "193248102431",
            points = 55
        ),
        BarcodeProduct(
            name = if (isAr) "عصير المراعي (HDPE)" else "Al-Marai HDPE Bottle",
            brand = if (isAr) "بلاستيك HDPE متين" else "Heavy Duty HDPE Plastic",
            emoji = "🥫",
            barcode = "6281007130234",
            points = 45
        )
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Futuristic Barcode/QR Laser Target Frame
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(ForestMid.copy(alpha = 0.15f))
                .border(2.dp, GoldEarth.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                .testTag("barcode_laser_frame")
        ) {
            // Neon Scanning Corner Brackets
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .border(1.5.dp, GoldEarth.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            )

            // Scanning laser animation line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .offset(y = (16.dp + (laserYPercent * 140).dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, LimePulse, GoldEarth, LimePulse, Color.Transparent)
                        )
                    )
            )

            // Viewfinder Icon and hint
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CropFree,
                    contentDescription = null,
                    tint = GoldEarth.copy(alpha = 0.6f),
                    modifier = Modifier.size(54.dp)
                )
                Text(
                    text = if (isAr) "قم بمحاذاة الباركود داخل الإطار الفوسفوري" else "Align barcode inside the glowing target",
                    fontSize = 11.sp,
                    color = CreamPaper.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Manual Entry Input Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, ForestMid.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (isAr) "🖊️ إدخال الرقم التسلسلي أو الباركود يدوياً:" else "🖊️ Enter Barcode Manually:",
                    fontSize = 12.sp,
                    color = GoldEarth,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = manualCode,
                        onValueChange = { manualCode = it },
                        placeholder = {
                            Text(
                                text = if (isAr) "مثال: 5449000000996" else "e.g., 5449000000996",
                                fontSize = 12.sp,
                                color = MistWhite.copy(alpha = 0.4f)
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = CreamPaper,
                            unfocusedTextColor = CreamPaper,
                            focusedBorderColor = GoldEarth,
                            unfocusedBorderColor = ForestMid,
                            focusedContainerColor = ForestMid.copy(alpha = 0.2f),
                            unfocusedContainerColor = ForestMid.copy(alpha = 0.1f)
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("manual_barcode_input")
                    )

                    Button(
                        onClick = {
                            if (manualCode.isNotBlank()) {
                                onScanBarcode(manualCode)
                            } else {
                                Toast.makeText(
                                    context,
                                    if (isAr) "الرجاء كتابة رمز صحيح أولاً" else "Please enter a valid code first",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LeafGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(50.dp)
                            .testTag("manual_barcode_submit_button")
                    ) {
                        Text(
                            text = if (isAr) "مسح" else "Scan",
                            color = CreamPaper,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Egyptian Products Simulate Scanner Shelf
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = if (isAr) "🥫 رف منتجات مصرية حقيقية (اضغط للمحاكاة):" else "🥫 Popular packaging (tap to simulate scan):",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = CreamPaper
            )

            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(products) { product ->
                    Card(
                        onClick = { onScanBarcode(product.barcode) },
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        border = BorderStroke(1.dp, GoldEarth.copy(alpha = 0.25f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .width(180.dp)
                            .height(135.dp)
                            .testTag("product_sim_${product.barcode}")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .background(ForestMid.copy(alpha = 0.3f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = product.emoji, fontSize = 18.sp)
                                }
                                
                                Box(
                                    modifier = Modifier
                                        .background(LeafGreen.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "+${product.points} XP",
                                        color = LeafGreen,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = product.name,
                                    color = CreamPaper,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = product.brand,
                                    color = MistWhite.copy(alpha = 0.6f),
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = product.barcode,
                                    color = GoldEarth,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

data class BarcodeProduct(
    val name: String,
    val brand: String,
    val emoji: String,
    val barcode: String,
    val points: Int
)

