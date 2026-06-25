package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSettingsScreen(viewModel: MainViewModel) {
    val profile by viewModel.userProfile.collectAsState()
    val activeLanguage by viewModel.currentLanguage.collectAsState()

    val activeProfile = profile ?: com.example.data.InitialData.defaultProfile

    // Editing states
    var name by remember(activeProfile) { mutableStateOf(activeProfile.name) }
    var email by remember(activeProfile) { mutableStateOf(activeProfile.email) }
    var phone by remember(activeProfile) { mutableStateOf(activeProfile.phone) }
    var country by remember(activeProfile) { mutableStateOf(activeProfile.country) }

    var isEditingProfile by remember { mutableStateOf(false) }
    var expandedSection by remember { mutableStateOf<String?>(null) } // lang, appearance, privacy, help, about

    val isDailyReminderEnabled by viewModel.isDailyReminderEnabled.collectAsState()
    val reminderTime by viewModel.reminderTime.collectAsState()
    val isLevelUpAlertEnabled by viewModel.isLevelUpAlertEnabled.collectAsState()
    val isPeakHourReminderEnabled by viewModel.isPeakHourReminderEnabled.collectAsState()
    val recentAlerts by viewModel.recentAlerts.collectAsState()

    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.triggerTestNotification()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Title
        item {
            Text(
                text = viewModel.translate("settings_title"),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // 2. Profile Summary Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Profile Avatar
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🌿", fontSize = 36.sp)
                    }

                    if (isEditingProfile) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text(viewModel.translate("name"), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            textStyle = TextStyle(color = MaterialTheme.colorScheme.onBackground),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("edit_name_input"),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text(viewModel.translate("email"), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            textStyle = TextStyle(color = MaterialTheme.colorScheme.onBackground),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("edit_email_input"),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text(viewModel.translate("phone"), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            textStyle = TextStyle(color = MaterialTheme.colorScheme.onBackground),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.updateProfile(name, email, phone, country)
                                    isEditingProfile = false
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier.weight(1f).testTag("save_profile_button")
                            ) {
                                Text(viewModel.translate("save"), fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { isEditingProfile = false },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onBackground),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(viewModel.translate("cancel"))
                            }
                        }
                    } else {
                        Text(text = activeProfile.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        Text(text = activeProfile.email, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Text(text = "${viewModel.translate("country")}: ${activeProfile.country}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))

                        Button(
                            onClick = { isEditingProfile = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(36.dp).testTag("edit_profile_button")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(viewModel.translate("edit_profile"), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 3. Settings List
        item {
            Text(
                text = viewModel.translate("general_settings"),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Appearance Mode Setting
        item {
            SettingsCollapseRow(
                title = "🎨 " + viewModel.translate("theme_and_colors"),
                icon = Icons.Default.Palette,
                isExpanded = expandedSection == "appearance",
                onClick = { expandedSection = if (expandedSection == "appearance") null else "appearance" }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val currentMode = activeProfile.themeMode
                    AppearanceChip(label = viewModel.translate("dark"), isSelected = currentMode == "dark", onClick = { viewModel.setThemeMode("dark") }, modifier = Modifier.weight(1f))
                    AppearanceChip(label = viewModel.translate("light"), isSelected = currentMode == "light", onClick = { viewModel.setThemeMode("light") }, modifier = Modifier.weight(1f))
                    AppearanceChip(label = viewModel.translate("auto"), isSelected = currentMode == "auto", onClick = { viewModel.setThemeMode("auto") }, modifier = Modifier.weight(1f))
                }
            }
        }

        // Multi Language Setting
        item {
            SettingsCollapseRow(
                title = "🌐 " + viewModel.translate("language"),
                icon = Icons.Default.Language,
                isExpanded = expandedSection == "lang",
                onClick = { expandedSection = if (expandedSection == "lang") null else "lang" }
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LanguageRow(label = "العربية (Arabic)", isSelected = activeLanguage == "ar", onClick = { viewModel.setLanguage("ar") })
                    LanguageRow(label = "English", isSelected = activeLanguage == "en", onClick = { viewModel.setLanguage("en") })
                    LanguageRow(label = "Español (Spanish)", isSelected = activeLanguage == "es", onClick = { viewModel.setLanguage("es") })
                    LanguageRow(label = "Français (French)", isSelected = activeLanguage == "fr", onClick = { viewModel.setLanguage("fr") })
                    LanguageRow(label = "Türkçe (Turkish)", isSelected = activeLanguage == "tr", onClick = { viewModel.setLanguage("tr") })
                }
            }
        }

        // Reminders & Alerts Settings
        item {
            SettingsCollapseRow(
                title = "🔔 " + viewModel.translate("reminders_alerts"),
                icon = Icons.Default.NotificationsActive,
                isExpanded = expandedSection == "reminders",
                onClick = { expandedSection = if (expandedSection == "reminders") null else "reminders" }
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. Daily Recycling Reminder Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = viewModel.translate("daily_reminder"),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = viewModel.translate("daily_reminder_desc"),
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                lineHeight = 14.sp
                            )
                        }
                        Switch(
                            checked = isDailyReminderEnabled,
                            onCheckedChange = { viewModel.setDailyReminderEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = LeafGreen,
                                checkedTrackColor = LeafGreen.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.testTag("toggle_daily_reminder")
                        )
                    }

                    // 2. Daily Reminder Time Selector (Horizontal chips)
                    if (isDailyReminderEnabled) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = viewModel.translate("reminder_time"),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = LeafGreen
                            )
                            
                            val times = listOf("09:00", "12:00", "15:00", "18:00", "21:00")
                            val displayTimes = mapOf(
                                "09:00" to (if (activeLanguage == "ar") "٠٩:٠٠ ص" else "09:00 AM"),
                                "12:00" to (if (activeLanguage == "ar") "١٢:٠٠ م" else "12:00 PM"),
                                "15:00" to (if (activeLanguage == "ar") "٠٣:٠٠ م" else "03:00 PM"),
                                "18:00" to (if (activeLanguage == "ar") "٠٦:٠٠ م" else "06:00 PM"),
                                "21:00" to (if (activeLanguage == "ar") "٠٩:٠٠ م" else "09:00 PM")
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                times.forEach { t ->
                                    val isSelected = reminderTime == t
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                            .border(
                                                1.dp,
                                                if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable { viewModel.setReminderTime(t) }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = displayTimes[t] ?: t,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), thickness = 0.5.dp)

                    // 2.5 Peak Hour Smart Reminder Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = viewModel.translate("peak_hour_reminder"),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = viewModel.translate("peak_hour_reminder_desc"),
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                lineHeight = 14.sp
                            )
                        }
                        Switch(
                            checked = isPeakHourReminderEnabled,
                            onCheckedChange = { viewModel.setPeakHourReminderEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = LeafGreen,
                                checkedTrackColor = LeafGreen.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.testTag("toggle_peak_hour_reminder")
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), thickness = 0.5.dp)

                    // 3. Level-up Alerts Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = viewModel.translate("levelup_alerts"),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = viewModel.translate("levelup_alerts_desc"),
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                lineHeight = 14.sp
                            )
                        }
                        Switch(
                            checked = isLevelUpAlertEnabled,
                            onCheckedChange = { viewModel.setLevelUpAlertEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = LeafGreen,
                                checkedTrackColor = LeafGreen.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.testTag("toggle_levelup_alerts")
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), thickness = 0.5.dp)

                    // 4. Test Notification Button
                    Button(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                val hasPermission = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) == PackageManager.PERMISSION_GRANTED
                                if (hasPermission) {
                                    viewModel.triggerTestNotification()
                                } else {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            } else {
                                viewModel.triggerTestNotification()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(38.dp).testTag("test_notification_button")
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(viewModel.translate("test_notification"), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // 5. Recent Alerts List Log
                    if (recentAlerts.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = viewModel.translate("notification_history"),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = LeafGreen
                            )
                            Text(
                                text = viewModel.translate("clear_history"),
                                fontSize = 11.sp,
                                color = DangerRust,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { viewModel.clearAlertLogs() }.testTag("clear_alerts_button")
                            )
                        }

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            recentAlerts.forEach { alert ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = alert.title,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = alert.message,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                            lineHeight = 14.sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        val timeStr = android.text.format.DateFormat.format("hh:mm a", alert.timestamp).toString()
                                        Text(
                                            text = timeStr,
                                            fontSize = 8.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                            modifier = Modifier.align(Alignment.End)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = viewModel.translate("no_history"),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // 4. Total Recycling History Setting/Section
        item {
            val scanHistory by viewModel.scanHistory.collectAsState(initial = emptyList())
            val isAr = activeLanguage == "ar"

            SettingsCollapseRow(
                title = "🌿 " + (if (isAr) "سجل إعادة التدوير الإجمالي" else "Total Recycling History"),
                icon = Icons.Default.History,
                isExpanded = expandedSection == "recycling_history",
                onClick = { expandedSection = if (expandedSection == "recycling_history") null else "recycling_history" }
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Offline Storage Info Banner
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = LeafGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = if (isAr) "تخزين محلي آمن ومستدام 💾" else "Secure Offline Local Storage 💾",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isAr) "يتم تخزين كافة إحصاءات وسجل تدوير مخلفاتك محلياً بشكل آمن في قاعدة بيانات جهازك، لتبقى متاحة بالكامل وتعمل بسلاسة حتى دون اتصال بالإنترنت." else "All your recycling logs and environmental impact records are stored securely in your device's offline database, completely accessible without internet.",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                lineHeight = 13.sp
                            )
                        }
                    }

                    if (scanHistory.isEmpty()) {
                        Text(
                            text = viewModel.translate("empty_history"),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                        )
                    } else {
                        // Summary Cards Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Total count
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "${scanHistory.size}",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = LeafGreen
                                    )
                                    Text(
                                        text = if (isAr) "عدد العناصر" else "Items Scanned",
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            // CO2 saved
                            val totalCo2 = scanHistory.sumOf { it.co2SavedGrams } / 1000.0
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = String.format("%.2f kg", totalCo2),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = LeafGreen
                                    )
                                    Text(
                                        text = if (isAr) "CO₂ الموفر" else "CO₂ Saved",
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            // Points earned
                            val totalPoints = scanHistory.sumOf { it.pointsEarned }
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "+$totalPoints XP",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = LeafGreen
                                    )
                                    Text(
                                        text = if (isAr) "النقاط المكتسبة" else "Points Earned",
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        // List of Scans
                        RecyclingHistorySummaryComponent(
                            scanHistory = scanHistory,
                            viewModel = viewModel
                        )
                    }
                }
            }
        }

        // Privacy and Security settings
        item {
            SettingsCollapseRow(
                title = "🛡️ " + viewModel.translate("privacy_security"),
                icon = Icons.Default.VerifiedUser,
                isExpanded = expandedSection == "privacy",
                onClick = { expandedSection = if (expandedSection == "privacy") null else "privacy" }
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(viewModel.translate("privacy_desc1"), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text(viewModel.translate("privacy_desc2"), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }

        // Help Center / FAQ
        item {
            SettingsCollapseRow(
                title = "❓ " + viewModel.translate("help_center"),
                icon = Icons.Default.HelpOutline,
                isExpanded = expandedSection == "help",
                onClick = { expandedSection = if (expandedSection == "help") null else "help" }
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FaqRow(
                        question = viewModel.translate("faq_q1"),
                        answer = viewModel.translate("faq_a1")
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), thickness = 0.5.dp)
                    FaqRow(
                        question = viewModel.translate("faq_q2"),
                        answer = viewModel.translate("faq_a2")
                    )
                }
            }
        }

        // About / Version info
        item {
            SettingsCollapseRow(
                title = "ℹ️ " + viewModel.translate("about_app"),
                icon = Icons.Default.Info,
                isExpanded = expandedSection == "about",
                onClick = { expandedSection = if (expandedSection == "about") null else "about" }
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(viewModel.translate("about_desc1"), fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground)
                    Text(viewModel.translate("about_desc2"), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                }
            }
        }

        // Danger zone reset database
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DangerRust.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("⚠️ " + viewModel.translate("danger_zone"), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DangerRust)
                    Text(viewModel.translate("danger_zone_desc"), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))

                    Button(
                        onClick = { viewModel.clearScanHistory() },
                        colors = ButtonDefaults.buttonColors(containerColor = DangerRust, contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(38.dp).testTag("reset_data_button")
                    ) {
                        Text(viewModel.translate("reset_db"), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Logout action
        item {
            OutlinedButton(
                onClick = { viewModel.logout() },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRust),
                border = BorderStroke(1.5.dp, DangerRust.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("logout_button")
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(viewModel.translate("logout"), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SettingsCollapseRow(
    title: String,
    icon: ImageVector,
    isExpanded: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = LeafGreen, modifier = Modifier.size(20.dp))
                    Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    content()
                }
            }
        }
    }
}

@Composable
fun AppearanceChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
            .border(0.5.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun LanguageRow(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 13.sp, color = if (isSelected) LeafGreen else MaterialTheme.colorScheme.onBackground, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
        if (isSelected) {
            Icon(Icons.Default.Check, contentDescription = null, tint = LeafGreen, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun FaqRow(question: String, answer: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = "• $question", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LeafGreen)
        Text(text = answer, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface, lineHeight = 16.sp)
    }
}

@Composable
fun RecyclingHistorySummaryComponent(
    scanHistory: List<com.example.data.ScanHistoryItem>,
    viewModel: MainViewModel
) {
    var expanded by remember { mutableStateOf(false) }
    val displayList = if (expanded) scanHistory else scanHistory.take(5)
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        displayList.forEach { item ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(item.itemEmoji, fontSize = 20.sp)
                        }

                        Column {
                            Text(
                                text = item.itemName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "${item.materialType} • ${String.format(java.util.Locale.US, "%.1f g CO₂", item.co2SavedGrams)}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .background(LeafGreen.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "+${item.pointsEarned} XP",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = LeafGreen
                        )
                    }
                }
            }
        }
        
        if (scanHistory.size > 5) {
            TextButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (expanded) 
                        (if (viewModel.currentLanguage.value == "ar") "عرض أقل" else "Show Less")
                    else 
                        (if (viewModel.currentLanguage.value == "ar") "عرض كل السجل (${scanHistory.size})" else "Show All History (${scanHistory.size})"),
                    color = LeafGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
