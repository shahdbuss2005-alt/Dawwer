package com.example.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ScanHistoryItem
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.delay

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigateToScan: () -> Unit
) {
    val profile by viewModel.userProfile.collectAsState()
    val history by viewModel.scanHistory.collectAsState()
    val challengesList by viewModel.challenges.collectAsState()

    val currentLang by viewModel.currentLanguage.collectAsState()
    val selectedUni by viewModel.selectedUniversity.collectAsState()

    val activeProfile = profile ?: com.example.data.InitialData.defaultProfile
    val completedCount = challengesList.count { it.completed }

    // Dynamic Arabic Eco-Friendly Living Tips states
    var selectedTipCategory by remember { mutableStateOf("all") }
    var completedTipsToday by remember { mutableStateOf(setOf<String>()) }

    // Dialog & Simulation States
    var depositStep by remember { mutableStateOf(0) }
    var selectedMaterialType by remember { mutableStateOf("plastic") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 0.5. Inactivity Reminder Alert Banner
        item {
            val showReminder by viewModel.showReminderBanner.collectAsState()
            val context = LocalContext.current
            
            LaunchedEffect(Unit) {
                viewModel.checkInactivity()
            }
            
            AnimatedVisibility(visible = showReminder) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .border(1.5.dp, DangerRust, RoundedCornerShape(16.dp))
                        .testTag("inactivity_reminder_banner"),
                    colors = CardDefaults.cardColors(containerColor = DangerRust.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("⏰", fontSize = 22.sp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (currentLang == "ar") "تنبيه غياب الاستدامة! 🔥" else "Inactivity Alert! 🔥",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DangerRust
                                )
                                Text(
                                    text = if (currentLang == "ar") "مرت أكثر من ٤٨ ساعة دون تدوير أي عنصر. سارع بالحفاظ على بيئتك وشعلة نشاطك!" else "More than 48 hours have passed without scanning! Scan now to keep your eco streak.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                                    lineHeight = 15.sp
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { 
                                    viewModel.resetInactivity() 
                                    Toast.makeText(context, if (currentLang == "ar") "تم تمديد المهلة بنجاح! 👍" else "Reminder Snoozed! 👍", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Text(text = if (currentLang == "ar") "تجاهل" else "Dismiss", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 11.sp)
                            }
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Button(
                                onClick = { 
                                    viewModel.resetInactivity()
                                    onNavigateToScan() 
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DangerRust, contentColor = EcoBlack),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = if (currentLang == "ar") "افرز الآن" else "Recycle Now", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // 1. Welcome Header Section
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = viewModel.translate("welcome"),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = activeProfile.name,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Quick Language Selector
                    IconButton(
                        onClick = {
                            val newLang = if (currentLang == "ar") "en" else "ar"
                            viewModel.setLanguage(newLang)
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), CircleShape)
                            .testTag("home_language_toggle")
                    ) {
                        Text(
                            text = if (currentLang == "ar") "EN" else "عربي",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Level Badge with Gold Glow
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            .border(2.dp, GoldEarth, CircleShape)
                            .testTag("level_badge"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🌱", fontSize = 14.sp)
                            Text(
                                text = "${viewModel.translate("level")} ${activeProfile.level}",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldEarth
                            )
                        }
                    }
                }
            }
        }

        // 1.5. Daily Eco-Tip Module
        item {
            val dailyTip by viewModel.dailyEcoTip.collectAsState()
            val isGeneratingTip by viewModel.isGeneratingTip.collectAsState()
            val context = LocalContext.current

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                    .testTag("eco_tip_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("💡", fontSize = 18.sp)
                            Text(
                                text = if (currentLang == "ar") "نصيحة دَوِّر اليومية للاستدامة" else "Dawwer Daily Eco-Tip",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = LeafGreen
                            )
                        }
                        
                        if (isGeneratingTip) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = LeafGreen)
                        } else {
                            Text(
                                text = "AI",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldEarth,
                                modifier = Modifier
                                    .border(1.dp, GoldEarth, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = dailyTip,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { viewModel.shuffleEcoTip() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(36.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = if (currentLang == "ar") "نصيحة أخرى" else "Next Tip", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = { 
                                viewModel.generateAiEcoTip()
                                Toast.makeText(context, if (currentLang == "ar") "جاري استشارة الذكاء الاصطناعي... 🤖" else "Consulting Gemini AI... 🤖", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LeafGreen.copy(alpha = 0.2f),
                                contentColor = LeafGreen
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1.2f).height(36.dp),
                            enabled = !isGeneratingTip,
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp), tint = LeafGreen)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = if (currentLang == "ar") "توليد بالذكاء الاصطناعي" else "Gemini Generate", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 2. Animated Progress Bar
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val levelNamesAr = listOf("بذرة 🌱", "شتلة 🌿", "شجيرة 🌳", "غابة صغيرة 🌲", "حارس البيئة 🛡️", "بطل الأرض 🌍")
                    val levelNamesEn = listOf("Seedling 🌱", "Sprout 🌿", "Sapling 🌳", "Mini Forest 🌲", "Eco Guardian 🛡️", "Earth Hero 🌍")
                    val levelNames = if (currentLang == "ar") levelNamesAr else levelNamesEn
                    val currentLevelName = levelNames.getOrElse(activeProfile.level - 1) { if (currentLang == "ar") "حامي الأرض 🌍" else "Earth Savior 🌍" }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = viewModel.translate("current_title_prefix") + currentLevelName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = LeafGreen
                        )
                        Text(
                            text = "${activeProfile.points} / ${activeProfile.level * 3000} XP",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }

                    // Progress Slider
                    val maxPointsForLevel = (activeProfile.level * 3000).toFloat()
                    val progressRatio = (activeProfile.points.toFloat() / maxPointsForLevel).coerceIn(0f, 1f)

                    LinearProgressIndicator(
                        progress = progressRatio,
                        color = LeafGreen,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                }
            }
        }

        // 3. User Statistics Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    value = "${history.size}",
                    label = viewModel.translate("total_scans"),
                    icon = Icons.Default.QrCodeScanner,
                    color = LeafGreen,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    value = "$completedCount",
                    label = viewModel.translate("completed_challenges"),
                    icon = Icons.Default.EmojiEvents,
                    color = GoldEarth,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    value = "${activeProfile.streak}",
                    label = viewModel.translate("streak_days"),
                    icon = Icons.Default.LocalFireDepartment,
                    color = LimePulse,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 3.5. Dynamic Arabic Eco-Friendly Living Tips (Educational Sustainable Practices)
        item {
            val context = LocalContext.current
            val filteredTips = remember(selectedTipCategory) {
                if (selectedTipCategory == "all") {
                    arabicEcoTipsList
                } else {
                    arabicEcoTipsList.filter { it.category == selectedTipCategory }
                }
            }

            var currentTipIndex by remember(filteredTips) { mutableStateOf(0) }
            val tipIndexClamped = if (filteredTips.isEmpty()) 0 else currentTipIndex.coerceIn(0, filteredTips.size - 1)
            val currentTip = filteredTips.getOrNull(tipIndexClamped)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header with title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("💡", fontSize = 22.sp)
                        Text(
                            text = if (currentLang == "ar") "نصائح المعيشة المستدامة" else "Sustainable Living Tips",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    // A badge showing total completed tips today
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(LeafGreen.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (currentLang == "ar") "تم تطبيق: ${completedTipsToday.size}" else "Applied: ${completedTipsToday.size}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = LeafGreen
                        )
                    }
                }

                // Educational context description in Arabic / English
                Text(
                    text = if (currentLang == "ar") {
                        "تعلم ممارسات يومية بسيطة وذكية للحفاظ على كوكبنا، وطبقها مباشرة لتربح نقاط XP حقيقية!"
                    } else {
                        "Learn simple & smart daily practices to preserve our planet, apply them to earn real XP points!"
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    lineHeight = 16.sp
                )

                // Category selection row (Horizontal scroll of chic pills)
                val categories = listOf(
                    "all" to ("الكل 🌍" to "All 🌍"),
                    "recycling" to ("تدوير ♻️" to "Recycling ♻️"),
                    "water" to ("مياه 💧" to "Water 💧"),
                    "energy" to ("طاقة ⚡" to "Energy ⚡"),
                    "shopping" to ("تسوق 🛍️" to "Shopping 🛍️"),
                    "transit" to ("نقل 🚌" to "Transit 🚌")
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(categories) { (catId, labels) ->
                            val isSelected = selectedTipCategory == catId
                            val label = if (currentLang == "ar") labels.first else labels.second
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50.dp))
                                    .background(
                                        if (isSelected) LeafGreen else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) Color.Transparent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                        RoundedCornerShape(50.dp)
                                    )
                                    .clickable { selectedTipCategory = catId }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) EcoBlack else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }

                // Main Tip Card
                if (currentTip != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = if (currentTip.id in completedTipsToday) LeafGreen.copy(alpha = 0.6f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(20.dp)
                            ),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Badge metadata row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = currentTip.icon,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        text = if (currentLang == "ar") currentTip.categoryAr else currentTip.category.replaceFirstChar { it.uppercase() },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = LeafGreen
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Difficulty badge
                                    val diffColor = when(currentTip.difficulty) {
                                        "سهل" -> LeafGreen
                                        "متوسط" -> GoldEarth
                                        else -> DangerRust
                                    }
                                    Text(
                                        text = if (currentLang == "ar") "مستوى: ${currentTip.difficulty}" else "Diff: ${currentTip.difficulty}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = diffColor,
                                        modifier = Modifier
                                            .background(diffColor.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )

                                    // Reward points badge
                                    Text(
                                        text = "+${currentTip.xpReward} XP",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EcoBlack,
                                        modifier = Modifier
                                            .background(GoldEarth, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            // Tip Title
                            Text(
                                text = currentTip.title,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = CreamPaper
                            )

                            // Concrete Action description (highly educational)
                            Text(
                                text = currentTip.action,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 18.sp
                            )

                            // Elegant Callout Box for Direct Environmental Impact
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(LeafGreen.copy(alpha = 0.08f))
                                    .border(1.dp, LeafGreen.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text("🌍", fontSize = 16.sp)
                                    Column {
                                        Text(
                                            text = if (currentLang == "ar") "الأثر البيئي والتعليمي:" else "Eco-Impact & Learning:",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = LeafGreen
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = currentTip.impact,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                            lineHeight = 15.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Action buttons row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // "Next Tip" Button
                                Button(
                                    onClick = {
                                        currentTipIndex = (currentTipIndex + 1) % filteredTips.size
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp),
                                    contentPadding = PaddingValues(0.dp),
                                    enabled = filteredTips.size > 1
                                ) {
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (currentLang == "ar") "نصيحة أخرى" else "Next Tip",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // Interactive "Mark as Completed today!" Button
                                val isCompleted = currentTip.id in completedTipsToday
                                Button(
                                    onClick = {
                                        if (!isCompleted) {
                                            viewModel.awardPoints(currentTip.xpReward)
                                            completedTipsToday = completedTipsToday + currentTip.id
                                            Toast.makeText(
                                                context,
                                                if (currentLang == "ar") "أحسنت! لقد ربحت +${currentTip.xpReward} نقطة لاهتمامك بالبيئة 🌱" else "Excellent! You earned +${currentTip.xpReward} XP for supporting Earth 🌱",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isCompleted) LeafGreen else LeafGreen.copy(alpha = 0.2f),
                                        contentColor = if (isCompleted) EcoBlack else LeafGreen
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1.3f)
                                        .height(40.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(
                                        if (isCompleted) Icons.Default.CheckCircle else Icons.Default.Done,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isCompleted) {
                                            if (currentLang == "ar") "تم التطبيق اليوم 🌿" else "Applied Today 🌿"
                                        } else {
                                            if (currentLang == "ar") "طبقّت النصيحة اليوم!" else "I Applied This Today!"
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                    ) {
                        Text(
                            text = if (currentLang == "ar") "لا توجد نصائح متوفرة في هذا القسم حالياً." else "No tips available in this category.",
                            modifier = Modifier.padding(16.0.dp),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // 4. Environmental Impact Calculator
        item {
            Text(
                text = viewModel.translate("impact"),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val co2Val = if (currentLang == "ar") String.format("%.1f كجم", activeProfile.co2SavedGrams / 1000.0) else String.format("%.1f kg", activeProfile.co2SavedGrams / 1000.0)
                    ImpactRow(
                        title = viewModel.translate("saved_carbon_title"),
                        value = co2Val,
                        subtitle = viewModel.translate("saved_carbon_sub"),
                        color = LeafGreen
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), thickness = 0.5.dp)
                    
                    val waterVal = if (currentLang == "ar") String.format("%.0f لتر", activeProfile.waterSavedLiters) else String.format("%.0f L", activeProfile.waterSavedLiters)
                    ImpactRow(
                        title = viewModel.translate("saved_water_title"),
                        value = waterVal,
                        subtitle = viewModel.translate("saved_water_sub"),
                        color = InfoTeal
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), thickness = 0.5.dp)
                    
                    val energyVal = if (currentLang == "ar") String.format("%.1f كيلوواط", activeProfile.energySavedKwh) else String.format("%.1f kWh", activeProfile.energySavedKwh)
                    ImpactRow(
                        title = viewModel.translate("saved_energy_title"),
                        value = energyVal,
                        subtitle = viewModel.translate("saved_energy_sub"),
                        color = LeafGreen
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), thickness = 0.5.dp)
                    
                    val treesVal = if (currentLang == "ar") String.format("%.2f شجرة", activeProfile.treesPlanted) else String.format("%.2f trees", activeProfile.treesPlanted)
                    ImpactRow(
                        title = viewModel.translate("saved_trees_title"),
                        value = treesVal,
                        subtitle = viewModel.translate("saved_trees_sub"),
                        color = GoldEarth
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), thickness = 0.5.dp)

                    // DOWNLOAD REPORT BUTTON & DIALOG TRIGGER
                    var showReportDialog by remember { mutableStateOf(false) }
                    var reportTextResult by remember { mutableStateOf("") }
                    val isGeneratingReport by viewModel.isGeneratingReport.collectAsState()
                    val reportProgress by viewModel.reportProgress.collectAsState()
                    val context = LocalContext.current

                    Button(
                        onClick = {
                            showReportDialog = true
                            viewModel.generateReportPdf { text ->
                                reportTextResult = text
                                try {
                                    val downloadsDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
                                    val reportFile = java.io.File(downloadsDir, "Dawwer_Eco_Report.txt")
                                    reportFile.writeText(text)
                                } catch (e: Exception) {
                                    // backup fallback
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LeafGreen.copy(alpha = 0.12f), contentColor = LeafGreen),
                        border = androidx.compose.foundation.BorderStroke(1.dp, LeafGreen.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .testTag("download_report_button")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (currentLang == "ar") "تحميل التقرير البيئي المعتمد (PDF)" else "Download Certified Eco Report (PDF)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (showReportDialog) {
                        AlertDialog(
                            onDismissRequest = { if (!isGeneratingReport) showReportDialog = false },
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.Verified, contentDescription = null, tint = LeafGreen)
                                    Text(
                                        text = if (currentLang == "ar") "التقرير البيئي الموثق" else "Certified Ecological Report",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            },
                            text = {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    if (isGeneratingReport) {
                                        CircularProgressIndicator(
                                            progress = reportProgress,
                                            color = LeafGreen,
                                            modifier = Modifier.size(64.dp)
                                        )
                                        Text(
                                            text = if (currentLang == "ar") "جاري توليد التقرير المعتمد وتنسيق ملف الـ PDF... ${(reportProgress * 100).toInt()}%" else "Generating certified PDF ecological report... ${(reportProgress * 100).toInt()}%",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                            textAlign = TextAlign.Center
                                        )
                                        LinearProgressIndicator(
                                            progress = reportProgress,
                                            color = LeafGreen,
                                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
                                        )
                                    } else {
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(1.5.dp, GoldEarth, RoundedCornerShape(12.dp)),
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1511)),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(14.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text("🌍", fontSize = 32.sp)
                                                Text(
                                                    text = if (currentLang == "ar") "شهادة حارس الأرض المعتمدة" else "Certified Earth Protector",
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = GoldEarth,
                                                    textAlign = TextAlign.Center
                                                )
                                                HorizontalDivider(color = GoldEarth.copy(alpha = 0.3f), thickness = 1.dp)
                                                
                                                Text(
                                                    text = if (currentLang == "ar") "تشهد منصة دَوِّر للاستدامة بأن البطل البيئي:" else "The Dawwer Sustainability Platform certifies that:",
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                                    textAlign = TextAlign.Center
                                                )
                                                Text(
                                                    text = activeProfile.name,
                                                    fontSize = 18.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = LeafGreen,
                                                    textAlign = TextAlign.Center
                                                )
                                                Text(
                                                    text = if (currentLang == "ar") "قد ساهم بشكل استثنائي في خفض الانبعاثات الكربونية وحفظ الموارد الطبيعية وحقق رصيداً مميزاً قدره ${activeProfile.points} نقطة تقدير (XP) وبذلك يُمنح اللقب الشرفي:" else "Has contributed exceptionally to CO2 emission savings and is awarded the eco honor title:",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                                    textAlign = TextAlign.Center,
                                                    lineHeight = 15.sp
                                                )
                                                Text(
                                                    text = if (currentLang == "ar") "المستوى ${activeProfile.level} 🌱 حارس مستدام" else "Level ${activeProfile.level} 🌱 Eco Guardian",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = GoldEarth,
                                                    textAlign = TextAlign.Center
                                                )
                                                
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceAround
                                                ) {
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Text(text = if (currentLang == "ar") String.format("%.1f كجم", activeProfile.co2SavedGrams / 1000.0) else String.format("%.1f kg", activeProfile.co2SavedGrams / 1000.0), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LeafGreen)
                                                        Text(text = if (currentLang == "ar") "كربون موفر" else "CO2 Saved", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                                    }
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Text(text = if (currentLang == "ar") String.format("%.0f لتر", activeProfile.waterSavedLiters) else String.format("%.0f L", activeProfile.waterSavedLiters), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = InfoTeal)
                                                        Text(text = if (currentLang == "ar") "مياه محفوظة" else "Water Saved", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = if (currentLang == "ar") "✓ شهادة إلكترونية مشفرة برقم: DWR-${System.currentTimeMillis().toString().takeLast(6)}" else "✓ Digital Certificate: DWR-${System.currentTimeMillis().toString().takeLast(6)}",
                                                    fontSize = 8.sp,
                                                    color = LeafGreen.copy(alpha = 0.8f)
                                                )
                                            }
                                        }
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    val shareIntent = Intent().apply {
                                                        action = Intent.ACTION_SEND
                                                        putExtra(Intent.EXTRA_TEXT, reportTextResult)
                                                        type = "text/plain"
                                                    }
                                                    context.startActivity(Intent.createChooser(shareIntent, if (currentLang == "ar") "شارك تقريرك البيئي" else "Share Eco Report"))
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), contentColor = MaterialTheme.colorScheme.onSurface),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.weight(1f).height(40.dp)
                                            ) {
                                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(text = if (currentLang == "ar") "مشاركة" else "Share", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                            
                                            Button(
                                                onClick = {
                                                    try {
                                                        val pdfDocument = android.graphics.pdf.PdfDocument()
                                                        val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
                                                        val page = pdfDocument.startPage(pageInfo)
                                                        val canvas = page.canvas
                                                        val paint = android.graphics.Paint()
                                                        paint.textSize = 14f
                                                        paint.color = android.graphics.Color.BLACK
                                                        
                                                        var y = 50f
                                                        val lines = reportTextResult.split("\n")
                                                        for (line in lines) {
                                                            canvas.drawText(line, 40f, y, paint)
                                                            y += 20f
                                                        }
                                                        
                                                        pdfDocument.finishPage(page)
                                                        
                                                        val dir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS)
                                                        val file = java.io.File(dir, "EcoScanner_Report.pdf")
                                                        pdfDocument.writeTo(java.io.FileOutputStream(file))
                                                        pdfDocument.close()
                                                        
                                                        val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                            type = "application/pdf"
                                                            putExtra(Intent.EXTRA_STREAM, uri)
                                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                        }
                                                        context.startActivity(Intent.createChooser(shareIntent, if (currentLang == "ar") "شارك التقرير (PDF)" else "Share PDF Report"))
                                                        
                                                        showReportDialog = false
                                                        Toast.makeText(context, if (currentLang == "ar") "تم توليد ملف PDF بنجاح 📄" else "PDF Report generated! 📄", Toast.LENGTH_LONG).show()
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                        Toast.makeText(context, "Error generating PDF", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = LeafGreen, contentColor = EcoBlack),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.weight(1.2f).height(40.dp)
                                            ) {
                                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(text = if (currentLang == "ar") "تحميل PDF" else "Download PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            },
                            confirmButton = {},
                            dismissButton = {
                                if (!isGeneratingReport) {
                                    TextButton(onClick = { showReportDialog = false }) {
                                        Text(text = if (currentLang == "ar") "إغلاق" else "Close", color = LeafGreen)
                                    }
                                }
                            },
                            containerColor = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }
            }
        }

        // 4.5. Recharts-Style Environmental Impact Chart
        item {
            RechartsStyleEnvironmentalChart(history = history, currentLang = currentLang)
        }

        // 5. AI Generated Weekly Report
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, GoldEarth.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = GoldEarth)
                        Text(
                            text = viewModel.translate("ai_report_title"),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldEarth
                        )
                    }

                    val historyCount = history.size
                    val reportText = when {
                        historyCount == 0 -> viewModel.translate("ai_report_empty")
                        historyCount < 3 -> String.format(viewModel.translate("ai_report_few"), historyCount)
                        else -> viewModel.translate("ai_report_many")
                    }

                    Text(
                        text = reportText,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        // 6. Quick Action Buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onNavigateToScan,
                    colors = ButtonDefaults.buttonColors(containerColor = LeafGreen, contentColor = EcoBlack),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("scan_waste_action")
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(viewModel.translate("scan_now"), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                var showNfcHelpDialog by remember { mutableStateOf(false) }
                if (showNfcHelpDialog) {
                    AlertDialog(
                        onDismissRequest = { showNfcHelpDialog = false },
                        title = { Text(viewModel.translate("nfc_dialog_title"), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                        text = {
                            Text(
                                text = viewModel.translate("nfc_dialog_desc"),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 18.sp
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = { showNfcHelpDialog = false },
                                colors = ButtonDefaults.buttonColors(containerColor = LeafGreen, contentColor = EcoBlack)
                            ) {
                                Text(viewModel.translate("understand"))
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(16.dp)
                    )
                }

                Button(
                    onClick = { showNfcHelpDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), contentColor = MaterialTheme.colorScheme.onSurface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("nfc_help_action")
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = LeafGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(viewModel.translate("nfc_guide"), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }





        // 7. Recent Scans Title
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = viewModel.translate("recent_activity"),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Box(
                    modifier = Modifier
                        .background(LeafGreen.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .border(1.dp, LeafGreen.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(LeafGreen)
                        )
                        Text(
                            text = if (currentLang == "ar") "آمن دون اتصال بالإنترنت" else "Offline Safe",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = LeafGreen
                        )
                    }
                }
            }
        }

        // 8. Recent Scans List
        if (history.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = viewModel.translate("empty_history"),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        } else {
            items(history.take(6)) { item ->
                HistoryItemRow(item = item, viewModel = viewModel)
            }
        }

        // 9. Simulation & Report Controls
        item {
            Text(
                text = if (currentLang == "ar") "أدوات محاكاة دَوِّر البيئية" else "Smart Dawwer Simulation Tools",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        item {
            val context = LocalContext.current
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (currentLang == "ar") "استخدم هذه الأزرار لاختبار تذكيرات الخمول لمدة ٤٨ ساعة والتقرير البيئي الموثق بشكل فوري:" else "Use these triggers to instantly test inactivity reminders and certified PDF reports:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        lineHeight = 16.sp
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { 
                                viewModel.simulateInactivity()
                                Toast.makeText(context, if (currentLang == "ar") "تم تفعيل محاكاة الخمول ٤٨ ساعة! تظهر لوحة التنبيه الآن بالطلب." else "Inactivity simulated! Alert banner visible at the top.", Toast.LENGTH_LONG).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(40.dp)
                        ) {
                            Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp), tint = GoldEarth)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = if (currentLang == "ar") "محاكاة خمول ⏰" else "Simulate 48h", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { 
                                viewModel.resetInactivity()
                                Toast.makeText(context, if (currentLang == "ar") "تم إعادة ضبط المؤقت وحالة النشاط بنجاح." else "Inactivity state reset successfully.", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(40.dp)
                        ) {
                            Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(16.dp), tint = LeafGreen)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = if (currentLang == "ar") "إعادة ضبط" else "Reset", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RechartsStyleEnvironmentalChart(
    history: List<ScanHistoryItem>,
    currentLang: String
) {
    var activeMetric by remember { mutableStateOf("co2") } // co2, water, energy
    var activeViewType by remember { mutableStateOf("cumulative") } // weekly, cumulative
    var selectedIndex by remember { mutableStateOf(-1) }

    val daysOfWeekAr = listOf("السبت", "الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة")
    val daysOfWeekEn = listOf("Sat", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri")
    val days = if (currentLang == "ar") daysOfWeekAr else daysOfWeekEn

    // Colors & Units depending on metric
    val metricColor = when (activeMetric) {
        "co2" -> LeafGreen
        "water" -> Color(0xFF2196F3)
        "energy" -> GoldEarth
        else -> LeafGreen
    }
    val metricUnit = when (activeMetric) {
        "co2" -> if (currentLang == "ar") "جرام CO2" else "g CO2"
        "water" -> if (currentLang == "ar") "لتر مياه" else "L Water"
        "energy" -> if (currentLang == "ar") "ك.و.س" else "kWh"
        else -> ""
    }

    // 1. Weekly periodic data calculation
    val weeklyData = remember(history, activeMetric) {
        val data = FloatArray(7) { index ->
            // Dynamic premium baselines
            when (activeMetric) {
                "co2" -> 150f + (index * 35f)
                "water" -> 10f + (index * 1.2f)
                "energy" -> 3f + (index * 0.4f)
                else -> 10f
            }
        }
        history.forEachIndexed { index, item ->
            val dayIndex = (index + 4) % 7
            val valueToAdd = when (activeMetric) {
                "co2" -> item.co2SavedGrams.toFloat()
                "water" -> item.waterSavedLiters.toFloat()
                "energy" -> item.energySavedKwh.toFloat()
                else -> 0f
            }
            data[dayIndex] = data[dayIndex] + valueToAdd
        }
        data
    }

    // 2. Cumulative growth data calculation (Chronological history)
    val sortedHistory = remember(history) { history.sortedBy { it.timestamp } }
    val cumulativeData = remember(sortedHistory, activeMetric) {
        val points = mutableListOf<Float>()
        var runningSum = when (activeMetric) {
            "co2" -> 350f
            "water" -> 15f
            "energy" -> 5f
            else -> 10f
        }
        points.add(runningSum)
        sortedHistory.forEach { item ->
            val value = when (activeMetric) {
                "co2" -> item.co2SavedGrams.toFloat()
                "water" -> item.waterSavedLiters.toFloat()
                "energy" -> item.energySavedKwh.toFloat()
                else -> 0f
            }
            runningSum += value
            points.add(runningSum)
        }
        // Fallback or pad to ensure at least 7 points for a beautiful Area Chart line
        while (points.size < 7) {
            val offset = when (activeMetric) {
                "co2" -> 95f
                "water" -> 4.5f
                "energy" -> 1.5f
                else -> 5f
            }
            runningSum += offset + (points.size * 5.5f)
            points.add(runningSum)
        }
        points
    }

    val currentDataList = if (activeViewType == "weekly") weeklyData.toList() else cumulativeData

    // Custom labels
    val xLabels = remember(activeViewType, sortedHistory, currentDataList.size) {
        if (activeViewType == "weekly") {
            days
        } else {
            val list = mutableListOf<String>()
            list.add(if (currentLang == "ar") "البداية 🌱" else "Start 🌱")
            sortedHistory.forEachIndexed { i, item ->
                list.add("${item.itemEmoji} ${i + 1}")
            }
            while (list.size < currentDataList.size) {
                list.add("🌱 ${list.size}")
            }
            list
        }
    }

    // Reset selected index when view type or metric changes
    LaunchedEffect(activeViewType, activeMetric) {
        selectedIndex = -1
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (currentLang == "ar") "تحليلات الأثر البيئي التفاعلية (Recharts)" else "Environmental Impact Analytics (Recharts-Style)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (currentLang == "ar") "رسم بياني ذكي يوضح إنجازاتك المستدامة عبر الزمن" else "Smart visualization showing sustainable wins over time",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(metricColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = metricColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tab toggles for View Type (Weekly vs Cumulative)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val views = listOf(
                    "cumulative" to (if (currentLang == "ar") "الأثر التراكمي المستمر 📈" else "Cumulative Trend 📈"),
                    "weekly" to (if (currentLang == "ar") "الأداء الأسبوعي الدوري 📊" else "Weekly Periodic 📊")
                )
                views.forEach { (viewKey, viewLabel) ->
                    val isSelected = activeViewType == viewKey
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent)
                            .clickable { activeViewType = viewKey }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = viewLabel,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) metricColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Metric Chip selection row using Box elements (consistent with Tip categories style)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val metrics = listOf(
                    Triple("co2", if (currentLang == "ar") "أثر الكربون 🍃" else "CO2 Offset 🍃", LeafGreen),
                    Triple("water", if (currentLang == "ar") "توفير المياه 💧" else "Water Saved 💧", Color(0xFF2196F3)),
                    Triple("energy", if (currentLang == "ar") "حفظ الطاقة ⚡" else "Energy Saved ⚡", GoldEarth)
                )
                metrics.forEach { (metricKey, label, color) ->
                    val isSelected = activeMetric == metricKey
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50.dp))
                            .background(
                                if (isSelected) color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                            .border(
                                1.dp,
                                if (isSelected) color else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                RoundedCornerShape(50.dp)
                            )
                            .clickable { activeMetric = metricKey }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) color else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Interactive Chart Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                androidx.compose.foundation.Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(currentDataList, activeViewType) {
                            detectTapGestures { offset ->
                                val width = size.width
                                val totalPoints = currentDataList.size
                                val spacing = width / if (activeViewType == "weekly") 7f else (totalPoints - 1).toFloat()
                                
                                val tappedIndex = if (activeViewType == "weekly") {
                                    (offset.x / (width / 7f)).toInt().coerceIn(0, 6)
                                } else {
                                    ((offset.x + spacing / 2f) / spacing).toInt().coerceIn(0, totalPoints - 1)
                                }
                                selectedIndex = if (selectedIndex == tappedIndex) -1 else tappedIndex
                            }
                        }
                ) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    
                    val maxVal = (currentDataList.maxOrNull() ?: 100f).coerceAtLeast(10f) * 1.15f
                    val topMargin = canvasHeight * 0.1f
                    val bottomMargin = canvasHeight * 0.85f
                    val chartHeight = bottomMargin - topMargin

                    // Draw Horizontal Gridlines (classic Recharts)
                    val gridLinesCount = 5
                    for (i in 0 until gridLinesCount) {
                        val y = topMargin + chartHeight * (i / (gridLinesCount - 1).toFloat())
                        drawLine(
                            color = Color(0xFF8E9B8B).copy(alpha = 0.12f),
                            start = Offset(0f, y),
                            end = Offset(canvasWidth, y),
                            strokeWidth = 1f
                        )
                    }

                    if (activeViewType == "weekly") {
                        // BAR CHART (Weekly periodic view)
                        val barWidth = (canvasWidth / 7f) * 0.5f
                        val spacing = canvasWidth / 7f
                        
                        currentDataList.forEachIndexed { index, value ->
                            val barHeight = (value / maxVal) * chartHeight
                            val x = index * spacing + (spacing - barWidth) / 2f
                            val y = bottomMargin - barHeight
                            
                            val isSelected = index == selectedIndex
                            val colorStart = if (isSelected) metricColor else metricColor.copy(alpha = 0.7f)
                            val colorEnd = if (isSelected) metricColor.copy(alpha = 0.4f) else metricColor.copy(alpha = 0.15f)

                            // Background Track Bar
                            drawRoundRect(
                                color = Color(0xFF1E2820).copy(alpha = 0.1f),
                                topLeft = Offset(x, topMargin),
                                size = Size(barWidth, chartHeight),
                                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                            )

                            // Fill Bar
                            drawRoundRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(colorStart, colorEnd)
                                ),
                                topLeft = Offset(x, y),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                            )

                            if (isSelected) {
                                // Draw glowing alignment ring above selected bar
                                drawCircle(
                                    color = metricColor,
                                    radius = 5.dp.toPx(),
                                    center = Offset(x + barWidth / 2f, y)
                                )
                                drawCircle(
                                    color = metricColor.copy(alpha = 0.3f),
                                    radius = 9.dp.toPx(),
                                    center = Offset(x + barWidth / 2f, y)
                                )
                            }
                        }
                    } else {
                        // AREA CHART (Cumulative trend)
                        val totalPoints = currentDataList.size
                        val spacing = canvasWidth / (totalPoints - 1).toFloat()
                        
                        val path = Path()
                        val areaPath = Path()
                        
                        val coordinates = currentDataList.mapIndexed { index, value ->
                            val x = index * spacing
                            val y = bottomMargin - (value / maxVal) * chartHeight
                            Offset(x, y)
                        }

                        // Build Line and Area paths
                        coordinates.forEachIndexed { index, point ->
                            if (index == 0) {
                                path.moveTo(point.x, point.y)
                                areaPath.moveTo(point.x, point.y)
                            } else {
                                path.lineTo(point.x, point.y)
                                areaPath.lineTo(point.x, point.y)
                            }
                        }

                        // Close area path along bottom boundary
                        areaPath.lineTo(canvasWidth, bottomMargin)
                        areaPath.lineTo(0f, bottomMargin)
                        areaPath.close()

                        // 1. Draw Shaded Gradient Area Under the Curve (Classic Recharts Area)
                        drawPath(
                            path = areaPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(metricColor.copy(alpha = 0.3f), Color.Transparent),
                                startY = topMargin,
                                endY = bottomMargin
                            )
                        )

                        // 2. Draw Spline/Connecting Line
                        drawPath(
                            path = path,
                            color = metricColor,
                            style = Stroke(width = 3.dp.toPx(), miter = 1f)
                        )

                        // 3. Draw alignment indicator line & nodes
                        if (selectedIndex != -1 && selectedIndex < totalPoints) {
                            val selPoint = coordinates[selectedIndex]
                            
                            // Recharts-style vertical dashed/dotted crosshair line
                            drawLine(
                                color = metricColor.copy(alpha = 0.4f),
                                start = Offset(selPoint.x, topMargin),
                                end = Offset(selPoint.x, bottomMargin),
                                strokeWidth = 1.5f
                            )

                            // Glowing Node
                            drawCircle(
                                color = Color.White,
                                radius = 6.dp.toPx(),
                                center = selPoint
                            )
                            drawCircle(
                                color = metricColor,
                                radius = 4.dp.toPx(),
                                center = selPoint
                            )
                            drawCircle(
                                color = metricColor.copy(alpha = 0.25f),
                                radius = 10.dp.toPx(),
                                center = selPoint
                            )
                        } else {
                            // Draw mini dots on all points for visual cue
                            coordinates.forEach { point ->
                                drawCircle(
                                    color = metricColor,
                                    radius = 3.dp.toPx(),
                                    center = point
                                )
                            }
                        }
                    }
                }

                // X-Axis Labels Row (Aligned cleanly under the canvas)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(top = 138.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    xLabels.forEachIndexed { index, label ->
                        val isSelected = index == selectedIndex
                        Text(
                            text = label,
                            fontSize = 8.sp,
                            color = if (isSelected) metricColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(36.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Details interactive tooltip card
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                if (selectedIndex != -1 && selectedIndex < currentDataList.size) {
                    val amount = currentDataList[selectedIndex]
                    val labelName = xLabels[selectedIndex]

                    // Environmental equivalent math
                    val equivalentText = when (activeMetric) {
                        "co2" -> {
                            val km = amount * 0.005
                            if (currentLang == "ar") {
                                String.format("توفير %.1f جرام يعادل تقليل عوادم سيارة ركاب لمسافة %.2f كم! 🚗", amount, km)
                            } else {
                                String.format("Saving %.1fg is equivalent to avoiding %.2f km of passenger car emissions! 🚗", amount, km)
                            }
                        }
                        "water" -> {
                            val cups = amount * 4.2
                            if (currentLang == "ar") {
                                String.format("حفظ %.1f لتر يعادل كفاية مياه لـ %.0f كوب شاي دافئ! ☕", amount, cups)
                            } else {
                                String.format("Conserving %.1fL is equivalent to water needed for %.0f cups of warm tea! ☕", amount, cups)
                            }
                        }
                        "energy" -> {
                            val phoneHours = amount * 80.0
                            if (currentLang == "ar") {
                                String.format("توفير %.1f ك.و.س يكفي لشحن هاتف ذكي بالكامل لـ %.0f ساعة متواصلة! 📱", amount, phoneHours)
                            } else {
                                String.format("Saving %.1f kWh is enough to power and charge a smartphone for %.0f full hours! 📱", amount, phoneHours)
                            }
                        }
                        else -> ""
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, metricColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = metricColor.copy(alpha = 0.08f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(text = if (activeViewType == "weekly") "📅" else "📈", fontSize = 14.sp)
                                    Text(
                                        text = if (currentLang == "ar") "تفاصيل: $labelName" else "Details: $labelName",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = String.format("%.1f %s", amount, metricUnit),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = metricColor
                                )
                            }
                            
                            HorizontalDivider(color = metricColor.copy(alpha = 0.15f), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 4.dp))

                            Text(
                                text = equivalentText,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                lineHeight = 14.sp
                            )
                        }
                    }
                } else {
                    // Instruction cue when nothing is tapped
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (currentLang == "ar") "انقر على أي نقطة أو عمود لعرض الأثر البيئي المقابل" else "Tap any data node or bar to inspect corresponding eco impact",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatCard(
    value: String,
    label: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = label,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ImpactRow(
    title: String,
    value: String,
    subtitle: String,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Text(text = subtitle, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
        Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun HistoryItemRow(item: ScanHistoryItem, viewModel: MainViewModel) {
    val currentLang = viewModel.currentLanguage.value
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(item.itemEmoji, fontSize = 20.sp)
            }

            Column(modifier = Modifier.weight(1f)) {
                val localizedName = if (currentLang == "ar") item.itemName else {
                    item.itemName
                        .replace("زجاجة بلاستيكية فارغة", "Empty Plastic Bottle")
                        .replace("عبوة مياه معدنية", "Mineral Water Bottle")
                        .replace("صندوق كرتون جاف", "Dry Cardboard Box")
                        .replace("عبوة ألومنيوم كانز", "Aluminum Soda Can")
                        .replace("ورق مقوى تالف", "Damaged Cardboard Paper")
                }
                Text(
                    text = localizedName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val matName = if (currentLang == "ar") item.materialType else {
                        item.materialType
                            .replace("بلاستيك", "Plastic")
                            .replace("ألومنيوم", "Aluminum")
                            .replace("معدن", "Metal")
                            .replace("ورق كرتون", "Cardboard")
                            .replace("ورق", "Paper")
                            .replace("إلكترونيات", "E-Waste")
                    }
                    Text(
                        text = matName,
                        fontSize = 11.sp,
                        color = LeafGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "•",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                    val dateStr = try {
                        val sdf = SimpleDateFormat("dd/MM HH:mm", Locale(currentLang))
                        sdf.format(Date(item.timestamp))
                    } catch (e: Exception) {
                        if (currentLang == "ar") "اليوم" else "Today"
                    }
                    Text(
                        text = dateStr,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "+${item.pointsEarned} XP",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = LeafGreen
                )
                Text(
                    text = if (currentLang == "ar") String.format("%.0fج كربون", item.co2SavedGrams) else String.format("%.0fg CO2", item.co2SavedGrams),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

data class ArabicEcoTip(
    val id: String,
    val category: String, // "recycling", "water", "energy", "shopping", "transit"
    val categoryAr: String,
    val title: String,
    val action: String,
    val impact: String,
    val difficulty: String,
    val icon: String,
    val xpReward: Int
)

val arabicEcoTipsList = listOf(
    ArabicEcoTip(
        id = "tip_recycle_smart",
        category = "recycling",
        categoryAr = "النفايات والتدوير",
        title = "إعادة تدوير البلاستيك الذكي ♻️",
        action = "افصل الأغطية البلاستيكية عن الزجاجات واغسل العبوة بالماء سريعاً قبل وضعها في سلة إعادة التدوير.",
        impact = "يسرع عملية الفرز في المصنع بنسبة ٤٠٪ ويمنع تعفن بقايا السوائل وجذب الحشرات.",
        difficulty = "سهل",
        icon = "♻️",
        xpReward = 25
    ),
    ArabicEcoTip(
        id = "tip_compost_veg",
        category = "recycling",
        categoryAr = "النفايات والتدوير",
        title = "تسميد بقايا الخضروات والمطبخ 🌱",
        action = "اجمع قشور الخضار والفواكه، وتفل الشاي، وتفل القهوة في وعاء لإنشاء سماد عضوي مغذٍ ومجاني لتربة شرفتك.",
        impact = "يقلل من النفايات العضوية المنزلية بنسبة ٣٠٪ ويغني عن الأسمدة الكيميائية الضارة.",
        difficulty = "متوسط",
        icon = "🌱",
        xpReward = 35
    ),
    ArabicEcoTip(
        id = "tip_box_flat",
        category = "recycling",
        categoryAr = "النفايات والتدوير",
        title = "إنقاذ وتسطيح الصناديق الكرتونية 📦",
        action = "قم بفك وتسطيح كل علب الكرتون الكبيرة الواردة من الطرود قبل رميها في حاوية جمع الورق لتوفير المساحة.",
        impact = "يقلل حجم النفايات الورقية في الشوارع وحجم شاحنات النقل بنسبة ٨٠٪!",
        difficulty = "سهل",
        icon = "📦",
        xpReward = 20
    ),
    ArabicEcoTip(
        id = "tip_water_brush",
        category = "water",
        categoryAr = "ترشيد المياه",
        title = "مراقبة الصنبور أثناء تنظيف الأسنان 💧",
        action = "أغلق الصنبور تماماً أثناء غسل الأسنان بالفرشاة أو فرك اليدين بالصابون، واستخدم كأساً لشطف الفم.",
        impact = "يوفر أكثر من ١٢ لترًا من المياه النظيفة المهدرة في كل دقيقة!",
        difficulty = "سهل",
        icon = "💧",
        xpReward = 25
    ),
    ArabicEcoTip(
        id = "tip_water_reuse",
        category = "water",
        categoryAr = "ترشيد المياه",
        title = "إعادة استخدام مياه غسل الطعام 🚿",
        action = "اغسل الفواكه والخضروات في وعاء مملوء بالماء بدلاً من الصنبور الجاري المباشر، ثم استخدم الماء لري النباتات المنزلية.",
        impact = "يحد من الهدر غير المبرر للمياه العذبة ويعطي حياة ثانية للمياه لري مزروعاتك.",
        difficulty = "متوسط",
        icon = "🚿",
        xpReward = 30
    ),
    ArabicEcoTip(
        id = "tip_shower_minute",
        category = "water",
        categoryAr = "ترشيد المياه",
        title = "تقليص وقت الاستحمام دقيقة واحدة 🛁",
        action = "اضبط مؤقتاً لتقليل وقت استحمامك المعتاد بمقدار دقيقة واحدة فقط يومياً وتجنب هدر المياه الساخنة.",
        impact = "يوفر حوالي ٩ إلى ١٥ لتر من المياه النقية والكهرباء والغاز المستخدم في التدفئة.",
        difficulty = "سهل",
        icon = "🛁",
        xpReward = 25
    ),
    ArabicEcoTip(
        id = "tip_energy_vampire",
        category = "energy",
        categoryAr = "ترشيد الطاقة",
        title = "نزع قوابس الشواحن غير المستخدمة 🔌",
        action = "افصل شاحن الهاتف واللابتوب والتلفزيون من المقبس بمجرد الانتهاء من استخدامها أو شحنها تماماً.",
        impact = "يقضي على 'الطاقة الشبحيّة' التي تمثل نحو ١٠٪ من قيمة فاتورة الكهرباء في منزلك دون داعٍ.",
        difficulty = "سهل",
        icon = "🔌",
        xpReward = 20
    ),
    ArabicEcoTip(
        id = "tip_energy_sun",
        category = "energy",
        categoryAr = "ترشيد الطاقة",
        title = "الاعتماد الكامل على الإضاءة الطبيعية ☀️",
        action = "افتح ستائر النوافذ نهاراً ودع أشعة الشمس تملأ أرجاء المنزل بدلاً من تشغيل المصابيح الكهربائية.",
        impact = "يقلل استهلاك مصابيح الإنارة، ويوفر طاقة التدفئة الطبيعية في الأيام الباردة ويحسن الصحة النفسية.",
        difficulty = "سهل",
        icon = "☀️",
        xpReward = 20
    ),
    ArabicEcoTip(
        id = "tip_energy_ac",
        category = "energy",
        categoryAr = "ترشيد الطاقة",
        title = "ضبط مكيف الهواء على درجة ٢٤ ❄️",
        action = "اضبط جهاز تكييف الهواء على ٢٤ أو ٢٥ درجة مئوية دائماً، واستخدم المروحة لتوزيع الهواء بالتساوي.",
        impact = "يوفر ما يصل إلى ١٢٪ من استهلاك الطاقة الكهربائية الإجمالي للمكيف لكل درجة حرارة إضافية ترفعها!",
        difficulty = "سهل",
        icon = "❄️",
        xpReward = 30
    ),
    ArabicEcoTip(
        id = "tip_shop_cloth",
        category = "shopping",
        categoryAr = "التسوق الأخضر",
        title = "حقائب التسوق القماشية الدائمة 🛍️",
        action = "احتفظ بحقيبة قماشية مستدامة وقابلة للغسل في حقيبتك أو سيارتك، واستعملها عند الشراء بدلاً من الأكياس البلاستيكية.",
        impact = "يمنع تلوث البيئة بأكياس بلاستيكية أحادية الاستخدام تحتاج إلى ٥٠٠ عام للتحلل الكامل.",
        difficulty = "سهل",
        icon = "🛍️",
        xpReward = 25
    ),
    ArabicEcoTip(
        id = "tip_shop_local",
        category = "shopping",
        categoryAr = "التسوق الأخضر",
        title = "شراء المنتجات المحلية والموسمية 🍎",
        action = "اختر المنتجات الغذائية المزروعة والمصنوعة محلياً والمقترنة بموسمها بدلاً من السلع المستوردة المغلفة.",
        impact = "يقلل البصمة الكربونية الضخمة الناتجة عن شحن البضائع جواً وبحراً وتغليفها الاصطناعي الطويل.",
        difficulty = "سهل",
        icon = "🍎",
        xpReward = 25
    ),
    ArabicEcoTip(
        id = "tip_shop_no_plastic",
        category = "shopping",
        categoryAr = "التسوق الأخضر",
        title = "تجنب المنتجات ذات التغليف البلاستيكي المفرط 🚫",
        action = "اشترِ الفواكه والخضار السائبة بدلاً من المعبأة في أطباق بلاستيكية ومغلفة بالنايلون.",
        impact = "يقلل من إنتاج وتراكم النفايات البلاستيكية الصلبة في البيئة بنسبة ٢٠٪.",
        difficulty = "متوسط",
        icon = "🚫",
        xpReward = 30
    ),
    ArabicEcoTip(
        id = "tip_transit_walk",
        category = "transit",
        categoryAr = "النقل المستدام",
        title = "المشي وركوب الدراجة للمسافات القريبة 🚶‍♂️",
        action = "إذا كانت وجهتك تبعد أقل من ٢ كيلومتر، اختر المشي الرياضي أو الدراجة الهوائية وتجنب ركوب السيارة.",
        impact = "يمنع انبعاثات ثاني أكسيد الكربون الضارة بالجو، ويحسن صحة جهاز الدوران والقلب.",
        difficulty = "متوسط",
        icon = "🚶‍♂️",
        xpReward = 35
    ),
    ArabicEcoTip(
        id = "tip_transit_group",
        category = "transit",
        categoryAr = "النقل المستدام",
        title = "استخدام وسائل النقل الجماعي أو التشاركي 🚌",
        action = "استخدم الحافلات العامة أو المترو أو النقل التشاركي للذهاب لعملك أو جامعتك مرة واحدة على الأسبوع على الأعل.",
        impact = "يقلل البصمة الكربونية الفردية لرحلتك بمقدار النصف ويقلل الازدحام المروري.",
        difficulty = "متوسط",
        icon = "🚌",
        xpReward = 30
    )
)
