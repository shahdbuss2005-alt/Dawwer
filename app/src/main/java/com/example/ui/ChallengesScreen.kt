package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BadgeItem
import com.example.data.Challenge
import com.example.ui.theme.*

@Composable
fun ChallengesScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val challengesList by viewModel.challenges.collectAsState()
    val badgesList by viewModel.badges.collectAsState()
    val profile by viewModel.userProfile.collectAsState()
    val leaderboard by viewModel.leaderboardUsers.collectAsState()
    val digitalAssets by viewModel.digitalAssets.collectAsState()
    val currentLang by viewModel.currentLanguage.collectAsState()

    var activeTab by remember { mutableStateOf("daily") } // daily, weekly, event, rewards, badges

    val filteredChallenges = remember(challengesList, activeTab) {
        if (activeTab == "badges" || activeTab == "rewards") {
            emptyList()
        } else {
            challengesList.filter { it.type == activeTab }
        }
    }

    val activeProfile = profile ?: com.example.data.InitialData.defaultProfile

    var previousLevel by remember { mutableStateOf(activeProfile.level) }
    var showLevelUpCelebration by remember { mutableStateOf(false) }
    var selectedMilestone by remember { mutableStateOf<RecyclingMilestone?>(null) }

    val milestones = remember(activeProfile) {
        listOf(
            RecyclingMilestone(
                id = "m_co2",
                nameAr = "قاهر الانبعاثات الكربوني 🍃",
                nameEn = "Carbon Crusader 🍃",
                emoji = "🍃",
                descriptionAr = "لقد منعت انبعاثات الغازات الدفيئة التي تضر بالبيئة عبر فرز نفاياتك الذكية.",
                descriptionEn = "Prevented greenhouse gas emissions that harm the environment by smart sorting your waste.",
                currentVal = activeProfile.co2SavedGrams,
                targetVal = 1000.0,
                unitAr = "جرام CO2",
                unitEn = "g CO2",
                category = "co2",
                color = Color(0xFF4CAF50)
            ),
            RecyclingMilestone(
                id = "m_water",
                nameAr = "حارس المياه العذبة 💧",
                nameEn = "Hydration Guardian 💧",
                emoji = "💧",
                descriptionAr = "قمت بترشيد وحفظ المياه العذبة الكافية عبر دعم عمليات التدوير الدائرية.",
                descriptionEn = "Conserved fresh water by supporting circular recycling loops.",
                currentVal = activeProfile.waterSavedLiters,
                targetVal = 50.0,
                unitAr = "لتر مياه",
                unitEn = "L Water",
                category = "water",
                color = Color(0xFF2196F3)
            ),
            RecyclingMilestone(
                id = "m_energy",
                nameAr = "بطل توفير الطاقة ⚡",
                nameEn = "Energy Champion ⚡",
                emoji = "⚡",
                descriptionAr = "أنقذت الطاقة الكهربائية اللازمة لتشغيل الأجهزة عبر توفير المواد الخام.",
                descriptionEn = "Saved electricity needed to run appliances by recycling raw materials.",
                currentVal = activeProfile.energySavedKwh,
                targetVal = 20.0,
                unitAr = "كيلوواط ساعة",
                unitEn = "kWh",
                category = "energy",
                color = Color(0xFFFFC107)
            ),
            RecyclingMilestone(
                id = "m_trees",
                nameAr = "مُشجِّر الكوكب الأخضر 🌲",
                nameEn = "Forest Planter 🌲",
                emoji = "🌲",
                descriptionAr = "تعادل مساهماتك في التدوير زراعة ورعاية نصف شجرة أرز حقيقية.",
                descriptionEn = "Your recycling contribution is equivalent to planting and nurturing half a real cedar tree.",
                currentVal = activeProfile.treesPlanted,
                targetVal = 0.5,
                unitAr = "شجرة",
                unitEn = "Tree",
                category = "trees",
                color = Color(0xFF8BC34A)
            ),
            RecyclingMilestone(
                id = "m_points",
                nameAr = "خبير التدوير الفائق 🏆",
                nameEn = "Recycling Master 🏆",
                emoji = "🏆",
                descriptionAr = "جمعت نقاط خبرة قياسية وأصبحت قدوة يحتذى بها في مجتمع التدوير لجامعتك.",
                descriptionEn = "Collected high experience points (XP) and became a role model in your campus recycling community.",
                currentVal = activeProfile.points.toDouble(),
                targetVal = 3000.0,
                unitAr = "نقطة XP",
                unitEn = "XP",
                category = "points",
                color = Color(0xFFFF9800)
            ),
            RecyclingMilestone(
                id = "m_streak",
                nameAr = "الملتزم البيئي الدائم 🔥",
                nameEn = "Loyal Recycler 🔥",
                emoji = "🔥",
                descriptionAr = "حافظت على تدوير يومي مستمر لثلاثة أيام متتالية لتعتاد العادات المستدامة.",
                descriptionEn = "Maintained a continuous daily recycling streak for 3 days to build eco-friendly habits.",
                currentVal = activeProfile.streak.toDouble(),
                targetVal = 3.0,
                unitAr = "أيام متتالية",
                unitEn = "days",
                category = "streak",
                color = Color(0xFFFF5722)
            )
        )
    }

    LaunchedEffect(activeProfile.level) {
        if (activeProfile.level > previousLevel) {
            showLevelUpCelebration = true
        }
        previousLevel = activeProfile.level
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Screen Title
        item {
            Text(
                text = viewModel.translate("challenges_rewards_title"),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // 2. Navigation Tabs Row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TabButton(
                    label = viewModel.translate("daily_tab"),
                    selected = activeTab == "daily",
                    onClick = { activeTab = "daily" },
                    modifier = Modifier.weight(1f)
                )
                TabButton(
                    label = viewModel.translate("weekly_tab"),
                    selected = activeTab == "weekly",
                    onClick = { activeTab = "weekly" },
                    modifier = Modifier.weight(1f)
                )
                TabButton(
                    label = viewModel.translate("event_tab"),
                    selected = activeTab == "event",
                    onClick = { activeTab = "event" },
                    modifier = Modifier.weight(1f)
                )
                TabButton(
                    label = viewModel.translate("rewards_tab"),
                    selected = activeTab == "rewards",
                    onClick = { activeTab = "rewards" },
                    modifier = Modifier.weight(1.2f)
                )
                TabButton(
                    label = viewModel.translate("badges_tab"),
                    selected = activeTab == "badges",
                    onClick = { activeTab = "badges" },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 3. Conditional Content
        if (activeTab == "badges") {
            // Level-up progress section
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, GoldEarth.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("🌱", fontSize = 20.sp)
                            Text(
                                text = if (currentLang == "ar") "مستوى التقدم والترقي" else "Level-up & Milestone Progress",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldEarth
                            )
                        }
                        
                        val maxPoints = activeProfile.level * 3000
                        val progress = (activeProfile.points.toFloat() / maxPoints.toFloat()).coerceIn(0f, 1f)
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                text = if (currentLang == "ar") "المستوى الحالي: ${activeProfile.level}" else "Current Level: ${activeProfile.level}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${activeProfile.points} / $maxPoints XP",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                        
                        LinearProgressIndicator(
                            progress = progress,
                            color = GoldEarth,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                        
                        Text(
                            text = if (currentLang == "ar") "احصل على المزيد من نقاط XP لإلغاء قفل مكافآت الجوائز الرقمية أدناه!" else "Earn more XP by recycling items to unlock the digital premium assets below!",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            lineHeight = 14.sp
                        )
                    }
                }
            }

            // Digital Assets Rewards Vault Section
            item {
                Text(
                    text = if (currentLang == "ar") "خزانة المكافآت والأصول الرقمية البيئية" else "Eco Digital Assets Rewards Vault",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            
            if (digitalAssets.isEmpty()) {
                item {
                    Text(
                        text = "No assets available.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            } else {
                items(digitalAssets) { asset ->
                    val assetName = if (currentLang == "ar") asset.nameAr else asset.nameEn
                    val assetDesc = if (currentLang == "ar") asset.descriptionAr else asset.descriptionEn
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = if (asset.active) LeafGreen else if (asset.unlocked) GoldEarth else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(16.dp)
                            ),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(if (asset.unlocked) GoldEarth.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(asset.icon, fontSize = 26.sp)
                            }
                            
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = assetName,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (asset.active) LeafGreen else if (asset.unlocked) GoldEarth else MaterialTheme.colorScheme.onBackground
                                    )
                                    if (asset.active) {
                                        Text(
                                            text = if (currentLang == "ar") "مفعّل" else "Active",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = EcoBlack,
                                            modifier = Modifier
                                                .background(LeafGreen, RoundedCornerShape(4.dp))
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = assetDesc,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    lineHeight = 15.sp
                                )
                                Text(
                                    text = if (currentLang == "ar") "التكلفة: ${asset.cost} XP | يتطلب مستوى ${asset.requiredLevel}" else "Cost: ${asset.cost} XP | Requires Lvl ${asset.requiredLevel}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = GoldEarth
                                )
                            }
                            
                            val isLevelMet = activeProfile.level >= asset.requiredLevel
                            val isAffordable = activeProfile.points >= asset.cost
                            
                            Button(
                                onClick = {
                                    if (!asset.unlocked) {
                                        viewModel.redeemAsset(
                                            assetId = asset.id,
                                            onSuccess = {
                                                Toast.makeText(context, if (currentLang == "ar") "تم الشراء بنجاح! 🎉" else "Redeemed successfully! 🎉", Toast.LENGTH_SHORT).show()
                                            },
                                            onError = { error ->
                                                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                                            }
                                        )
                                    } else {
                                        viewModel.activateAsset(asset.id)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (asset.active) LeafGreen else if (asset.unlocked) GoldEarth else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    contentColor = if (asset.unlocked || asset.active) EcoBlack else MaterialTheme.colorScheme.onSurface
                                ),
                                shape = RoundedCornerShape(8.dp),
                                enabled = asset.unlocked || (isLevelMet && isAffordable),
                                modifier = Modifier
                                    .width(88.dp)
                                    .height(34.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(
                                    text = if (asset.active) {
                                        if (currentLang == "ar") "مفعّل" else "Active"
                                    } else if (asset.unlocked) {
                                        if (currentLang == "ar") "تفعيل" else "Activate"
                                    } else {
                                        if (currentLang == "ar") "شراء" else "Redeem"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
            
            // 2. Recycling Milestones Achievements Section
            item {
                Column(
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (currentLang == "ar") "🏆 معالم التدوير والأوسمة الفائقة" else "🏆 Recycling Milestones & Badges",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldEarth
                    )
                    
                    val unlockedCount = milestones.count { it.currentVal >= it.targetVal }
                    Text(
                        text = if (currentLang == "ar") {
                            "تم إلغاء قفل $unlockedCount من أصل ${milestones.size} أوسمة فخرية"
                        } else {
                            "Unlocked $unlockedCount of ${milestones.size} special honors"
                        },
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            
            items(milestones) { milestone ->
                MilestoneCard(
                    milestone = milestone,
                    currentLang = currentLang,
                    onCardClick = { selectedMilestone = milestone }
                )
            }

            // Unlocked Badges Title
            item {
                Text(
                    text = viewModel.translate("unlocked_badges_title"),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoldEarth,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            item {
                BadgesListGrid(badges = badgesList, viewModel = viewModel)
            }

        } else if (activeTab == "rewards") {
            // Point counter smooth animation
            item {
                val animatedPoints by animateIntAsState(
                    targetValue = activeProfile.points,
                    animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
                    label = "points_roll"
                )

                // Shimmer gold/emerald gradient brush
                val infiniteShimmer = rememberInfiniteTransition(label = "shimmer")
                val shimmerX by infiniteShimmer.animateFloat(
                    initialValue = 0f,
                    targetValue = 1000f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 1800, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "shimmer_pos"
                )
                val luxuryBrush = Brush.linearGradient(
                    colors = listOf(ForestDeep, CardBackground, ForestMid, ForestDeep),
                    start = androidx.compose.ui.geometry.Offset(shimmerX, 0f),
                    end = androidx.compose.ui.geometry.Offset(shimmerX + 300f, 300f)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            BorderStroke(
                                1.5.dp,
                                Brush.sweepGradient(listOf(GoldEarth, LeafGreen, LimePulse, GoldEarth))
                            ),
                            RoundedCornerShape(24.dp)
                        ),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .background(luxuryBrush)
                            .padding(20.dp)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(
                                        Icons.Default.EmojiEvents,
                                        contentDescription = null,
                                        tint = GoldEarth,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Text(
                                        text = if (currentLang == "ar") "المحفظة البيئية الفاخرة" else "Luxury Eco Vault",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CreamPaper
                                    )
                                }
                                
                                Text(
                                    text = if (currentLang == "ar") "مستوى ${activeProfile.level}" else "Lvl ${activeProfile.level}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = EcoBlack,
                                    modifier = Modifier
                                        .background(GoldEarth, RoundedCornerShape(12.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }

                            // Big animated points score
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = if (currentLang == "ar") "رصيد النقاط الفاخرة" else "PREMIUM BALANCE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MistWhite.copy(alpha = 0.7f),
                                    letterSpacing = 1.5.sp
                                )
                                
                                Row(
                                    verticalAlignment = Alignment.Bottom,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "$animatedPoints",
                                        fontSize = 44.sp,
                                        fontWeight = FontWeight.Black,
                                        color = GoldEarth,
                                        style = MaterialTheme.typography.headlineLarge
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (currentLang == "ar") "نقطة" else "XP",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = LeafGreen,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                }
                            }

                            // XP Progress line with custom gold gradient
                            val maxPoints = activeProfile.level * 3000
                            val rawProgress = (activeProfile.points.toFloat() / maxPoints.toFloat()).coerceIn(0f, 1f)
                            val animatedProgress by animateFloatAsState(
                                targetValue = rawProgress,
                                animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
                                label = "progress_anim"
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (currentLang == "ar") "الترقية للمستوى التالي" else "Next Level Progress",
                                        fontSize = 10.sp,
                                        color = MistWhite.copy(alpha = 0.8f)
                                    )
                                    Text(
                                        text = "${activeProfile.points} / $maxPoints XP",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GoldEarth
                                    )
                                }
                                
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(Color.White.copy(alpha = 0.08f))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(animatedProgress)
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(LeafGreen, GoldEarth, LimePulse)
                                                )
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Unlocked coupon show dialog
            // We can display custom eco reward vouchers to redeem
            item {
                Text(
                    text = if (currentLang == "ar") "قسائم ومكافآت نمط الحياة المستدام ✨" else "Eco-Lifestyle Exclusive Rewards ✨",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = CreamPaper,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (digitalAssets.isEmpty()) {
                item {
                    Text(
                        text = "No assets available.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            } else {
                items(digitalAssets) { asset ->
                    val assetName = if (currentLang == "ar") asset.nameAr else asset.nameEn
                    val assetDesc = if (currentLang == "ar") asset.descriptionAr else asset.descriptionEn
                    
                    var showCouponCodeDialog by remember { mutableStateOf(false) }
                    
                    if (showCouponCodeDialog) {
                        val generatedCoupon = remember(asset.id) {
                            "ECO-" + asset.id.uppercase().take(6) + "-" + (1000..9999).random()
                        }
                        AlertDialog(
                            onDismissRequest = { showCouponCodeDialog = false },
                            confirmButton = {
                                Button(
                                    onClick = { showCouponCodeDialog = false },
                                    colors = ButtonDefaults.buttonColors(containerColor = LeafGreen)
                                ) {
                                    Text(if (currentLang == "ar") "موافق" else "Done")
                                }
                            },
                            icon = { Text(asset.icon, fontSize = 48.sp) },
                            title = {
                                Text(
                                    text = if (currentLang == "ar") "رمز الخصم الخاص بك 🎫" else "Your Redeem Voucher 🎫",
                                    color = GoldEarth,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            text = {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = assetName,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = if (currentLang == "ar") "اعرض هذا الرمز لموظف الخدمة لدى المتجر الشريك:" else "Present this code at any of our partner green branches:",
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                    )
                                    
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                            .border(1.dp, GoldEarth.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                            .padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = generatedCoupon,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = LimePulse,
                                            letterSpacing = 2.sp
                                        )
                                    }
                                }
                            },
                            containerColor = ForestDeep,
                            shape = RoundedCornerShape(20.dp)
                        )
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = if (asset.active) LeafGreen else if (asset.unlocked) GoldEarth else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(20.dp)
                            ),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(if (asset.unlocked) GoldEarth.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(asset.icon, fontSize = 28.sp)
                            }
                            
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = assetName,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (asset.active) LeafGreen else if (asset.unlocked) GoldEarth else MaterialTheme.colorScheme.onBackground
                                    )
                                    if (asset.active) {
                                        Text(
                                            text = if (currentLang == "ar") "مفعّل" else "Active",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = EcoBlack,
                                            modifier = Modifier
                                                .background(LeafGreen, RoundedCornerShape(4.dp))
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = assetDesc,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    lineHeight = 15.sp
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = if (currentLang == "ar") "التكلفة: ${asset.cost} XP" else "Cost: ${asset.cost} XP",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = GoldEarth
                                    )
                                    Text(
                                        text = "|",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                    )
                                    Text(
                                        text = if (currentLang == "ar") "يتطلب مستوى ${asset.requiredLevel}" else "Requires Lvl ${asset.requiredLevel}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (activeProfile.level >= asset.requiredLevel) LeafGreen else DangerRust
                                    )
                                }
                            }
                            
                            val isLevelMet = activeProfile.level >= asset.requiredLevel
                            val isAffordable = activeProfile.points >= asset.cost
                            
                            Button(
                                onClick = {
                                    if (!asset.unlocked) {
                                        viewModel.redeemAsset(
                                            assetId = asset.id,
                                            onSuccess = {
                                                Toast.makeText(context, if (currentLang == "ar") "تم الشراء بنجاح! 🎉" else "Redeemed successfully! 🎉", Toast.LENGTH_SHORT).show()
                                            },
                                            onError = { error ->
                                                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                                            }
                                        )
                                    } else {
                                        if (asset.type == "voucher") {
                                            // Vouchers present a coupon popup
                                            showCouponCodeDialog = true
                                        } else {
                                            viewModel.activateAsset(asset.id)
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (asset.active) LeafGreen else if (asset.unlocked) GoldEarth else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    contentColor = if (asset.unlocked || asset.active) EcoBlack else MaterialTheme.colorScheme.onSurface
                                ),
                                shape = RoundedCornerShape(10.dp),
                                enabled = asset.unlocked || (isLevelMet && isAffordable),
                                modifier = Modifier
                                    .width(88.dp)
                                    .height(36.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(
                                    text = if (asset.unlocked) {
                                        if (asset.type == "voucher") {
                                            if (currentLang == "ar") "عرض الكود" else "Show Code"
                                        } else {
                                            if (asset.active) {
                                                if (currentLang == "ar") "مفعّل" else "Active"
                                            } else {
                                                if (currentLang == "ar") "تفعيل" else "Activate"
                                            }
                                        }
                                    } else {
                                        if (currentLang == "ar") "شراء" else "Redeem"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Challenges listings
            if (activeTab == "weekly") {
                item {
                    WeeklyRotatingChallengeHub(viewModel = viewModel)
                }
            }

            item {
                Text(
                    text = when (activeTab) {
                        "daily" -> viewModel.translate("challenges_available_daily")
                        "weekly" -> viewModel.translate("challenges_available_weekly")
                        else -> viewModel.translate("challenges_available_event")
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = LeafGreen
                )
            }

            if (filteredChallenges.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Text(
                            text = viewModel.translate("challenges_empty"),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                }
            } else {
                items(filteredChallenges) { challenge ->
                    ChallengeItemCard(
                        challenge = challenge,
                        onCompleteDirect = { viewModel.completeChallengeDirectly(challenge.id) },
                        viewModel = viewModel
                    )
                }
            }
        }

        // 4. Monthly Community Leaderboard
        item {
            Text(
                text = viewModel.translate("leaderboard_title"),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        item {
            val pointsLabel = viewModel.translate("points")
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    leaderboard.sortedByDescending { it.points }.forEachIndexed { idx, user ->
                        val rankEmoji = when (idx) {
                            0 -> "🥇"
                            1 -> "🥈"
                            2 -> "🥉"
                            else -> "🎖️"
                        }
                        
                        val isCurrentUser = user.isCurrentUser
                        val displayName = if (isCurrentUser) {
                            if (currentLang == "ar") "${user.name} (أنت)" else "${user.name} (You)"
                        } else {
                            user.name
                        }
                        
                        LeaderboardRow(
                            rank = rankEmoji,
                            name = displayName,
                            score = "${user.points} $pointsLabel",
                            isCurrentUser = isCurrentUser
                        )
                        
                        if (idx < leaderboard.size - 1) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))

                    // Dynamic User rank details
                    val currentUserRank = leaderboard.sortedByDescending { it.points }.indexOfFirst { it.isCurrentUser } + 1
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(viewModel.translate("user_rank_label"), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            Text(
                                text = if (currentLang == "ar") "المركز #$currentUserRank من أصل ${leaderboard.size}" else "Rank #$currentUserRank of ${leaderboard.size}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = LeafGreen
                            )
                        }

                        Text(
                            text = if (currentUserRank == 1) {
                                if (currentLang == "ar") "أنت البطل الحالي! 🏆" else "You are the top champion! 🏆"
                            } else {
                                viewModel.translate("work_hard_up")
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldEarth
                        )
                    }
                }
            }
        }
    }

    if (showLevelUpCelebration) {
        AlertDialog(
            onDismissRequest = { showLevelUpCelebration = false },
            confirmButton = {
                Button(
                    onClick = { showLevelUpCelebration = false },
                    colors = ButtonDefaults.buttonColors(containerColor = LeafGreen)
                ) {
                    Text(if (currentLang == "ar") "رائع! استمر بالعمل الأخضر 🌱" else "Awesome! Keep it green 🌱")
                }
            },
            icon = {
                // Rotating and pulsing trophy with floating stars
                val infiniteTransition = rememberInfiniteTransition(label = "trophy")
                val scale by infiniteTransition.animateFloat(
                    initialValue = 0.9f,
                    targetValue = 1.15f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "trophy_scale"
                )
                Box(
                    modifier = Modifier
                        .scale(scale)
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(GoldEarth.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🏆", fontSize = 44.sp)
                }
            },
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (currentLang == "ar") "ارتفع مستواك البيئي! 🎉" else "New Eco Level Unlocked! 🎉",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = GoldEarth,
                        textAlign = TextAlign.Center
                    )
                }
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (currentLang == "ar") "المستوى الجديد: ${activeProfile.level}" else "New Milestone Level: ${activeProfile.level}",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = LeafGreen,
                        textAlign = TextAlign.Center
                    )
                    
                    Text(
                        text = if (currentLang == "ar") {
                            "لقد ارتفع تصنيفك نتيجة لجهودك المستمرة في تدوير النفايات بشكل صحيح! تفقّد محفظة المكافآت لتفعيل أصول بيئية جديدة."
                        } else {
                            "Your continued dedication to sustainable recycling has leveled up your environmental impact! Check out the Rewards Vault to claim exclusive assets."
                        },
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        color = MistWhite,
                        lineHeight = 18.sp
                    )
                }
            },
            containerColor = ForestDeep,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.border(2.dp, GoldEarth, RoundedCornerShape(24.dp))
        )
    }

    selectedMilestone?.let { milestone ->
        MilestoneDetailDialog(
            milestone = milestone,
            currentLang = currentLang,
            onDismiss = { selectedMilestone = null }
        )
    }
}

@Composable
fun TabButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) LeafGreen else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) EcoBlack else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ChallengeItemCard(
    challenge: Challenge,
    onCompleteDirect: () -> Unit,
    viewModel: MainViewModel
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = challenge.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = challenge.description,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        lineHeight = 16.sp
                    )
                }

                // Points Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        .border(0.5.dp, GoldEarth, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("+${challenge.rewardPoints} XP", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GoldEarth)
                }
            }

            // Progress Indicators
            val progressRatio = (challenge.progress.toFloat() / challenge.target.toFloat()).coerceIn(0f, 1f)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LinearProgressIndicator(
                    progress = progressRatio,
                    color = LeafGreen,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "${challenge.progress}/${challenge.target}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⏰ ${challenge.timeLeftLabel}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )

                if (!challenge.completed) {
                    Text(
                        text = viewModel.translate("simulate_completion"),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = LeafGreen,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            .clickable(onClick = onCompleteDirect)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("simulate_challenge_${challenge.id}")
                    )
                } else {
                    Text(
                        text = viewModel.translate("completed_badge"),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = LeafGreen
                    )
                }
            }
        }
    }
}

@Composable
fun LeaderboardRow(
    rank: String,
    name: String,
    score: String,
    isCurrentUser: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isCurrentUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = rank, fontSize = 16.sp)
            Text(
                text = name,
                fontSize = 13.sp,
                fontWeight = if (isCurrentUser) FontWeight.Bold else FontWeight.Medium,
                color = if (isCurrentUser) LeafGreen else MaterialTheme.colorScheme.onBackground
            )
        }
        Text(text = score, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
fun BadgesListGrid(badges: List<BadgeItem>, viewModel: MainViewModel) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        val chunked = badges.chunked(2)
        chunked.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowItems.forEach { badge ->
                    BadgeCard(badge = badge, modifier = Modifier.weight(1f), viewModel = viewModel)
                }
                if (rowItems.size < 2) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun BadgeCard(badge: BadgeItem, modifier: Modifier = Modifier, viewModel: MainViewModel) {
    Card(
        modifier = modifier
            .border(
                width = 1.dp,
                color = if (badge.unlocked) GoldEarth else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(if (badge.unlocked) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                    .border(
                        1.dp,
                        if (badge.unlocked) GoldEarth else Color.Transparent,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (badge.unlocked) {
                    Text(badge.emoji, fontSize = 28.sp)
                } else {
                    Icon(
                        Icons.Default.Lock, 
                        contentDescription = if (viewModel.currentLanguage.value == "ar") "مغلق" else "Locked", 
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }
            }

            Text(
                text = badge.name,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (badge.unlocked) GoldEarth else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )

            Text(
                text = badge.description,
                fontSize = 10.sp,
                color = if (badge.unlocked) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                textAlign = TextAlign.Center,
                lineHeight = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

data class RecyclingMilestone(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val emoji: String,
    val descriptionAr: String,
    val descriptionEn: String,
    val currentVal: Double,
    val targetVal: Double,
    val unitAr: String,
    val unitEn: String,
    val category: String, // co2, water, energy, trees, points, streak
    val color: Color
)

@Composable
fun MilestoneCard(
    milestone: RecyclingMilestone,
    currentLang: String,
    onCardClick: () -> Unit
) {
    val isUnlocked = milestone.currentVal >= milestone.targetVal
    val progress = (milestone.currentVal / milestone.targetVal).toFloat().coerceIn(0f, 1f)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onCardClick() }
            .border(
                width = 1.dp,
                color = if (isUnlocked) milestone.color.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnlocked) milestone.color.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Milestone Icon
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(if (isUnlocked) milestone.color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                    .border(
                        width = 1.5.dp,
                        color = if (isUnlocked) milestone.color else Color.Transparent,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isUnlocked) {
                    Text(milestone.emoji, fontSize = 28.sp)
                } else {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Milestone Details
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (currentLang == "ar") milestone.nameAr else milestone.nameEn,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isUnlocked) milestone.color else MaterialTheme.colorScheme.onBackground
                    )
                    if (isUnlocked) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(milestone.color.copy(alpha = 0.2f))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = if (currentLang == "ar") "مكتمل" else "Unlocked",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = milestone.color
                            )
                        }
                    }
                }
                
                Text(
                    text = if (currentLang == "ar") milestone.descriptionAr else milestone.descriptionEn,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    lineHeight = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Progress Bar
                LinearProgressIndicator(
                    progress = progress,
                    color = milestone.color,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                )

                // Progress Text
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val unit = if (currentLang == "ar") milestone.unitAr else milestone.unitEn
                    val currentFormatted = if (milestone.currentVal % 1.0 == 0.0) milestone.currentVal.toInt().toString() else String.format("%.1f", milestone.currentVal)
                    val targetFormatted = if (milestone.targetVal % 1.0 == 0.0) milestone.targetVal.toInt().toString() else String.format("%.1f", milestone.targetVal)
                    
                    Text(
                        text = "$currentFormatted / $targetFormatted $unit",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = milestone.color
                    )
                }
            }
        }
    }
}

@Composable
fun MilestoneDetailDialog(
    milestone: RecyclingMilestone,
    currentLang: String,
    onDismiss: () -> Unit
) {
    val isUnlocked = milestone.currentVal >= milestone.targetVal
    val isAr = currentLang == "ar"
    
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = milestone.color,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (isAr) "موافق ورائع 👍" else "Awesome 👍",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (isUnlocked) milestone.emoji else "🔒",
                    fontSize = 28.sp
                )
                Text(
                    text = if (isAr) milestone.nameAr else milestone.nameEn,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isUnlocked) milestone.color else MaterialTheme.colorScheme.onSurface
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isAr) milestone.descriptionAr else milestone.descriptionEn,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    lineHeight = 16.sp
                )
                
                // Detailed eco insights
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = milestone.color.copy(alpha = 0.08f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (isAr) "🌱 الأثر البيئي لهذا الإنجاز:" else "🌱 Eco Impact of this milestone:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = milestone.color
                        )
                        
                        val impactText = when (milestone.id) {
                            "m_co2" -> if (isAr) {
                                "تقليل انبعاثات الكربون بمقدار ١٠٠٠ جرام يعادل وقف قيادة سيارة عادية لمسافة ٥ كيلومترات كاملة! يساهم هذا في حماية غلافنا الجوي في القاهرة وتقليل الاحتباس الحراري."
                            } else {
                                "Reducing 1,000g of CO2 emissions is equivalent to stopping a regular passenger car from driving 5 full kilometers! This helps protect the Cairo atmosphere and mitigates global warming."
                            }
                            "m_water" -> if (isAr) {
                                "حفظ ٥٠ لتر من المياه النقية يحمي الموارد المائية الثمينة. لتر واحد يتم توفيره يغذي مئات النباتات أو يحافظ على المياه الجوفية العذبة للأجيال القادمة."
                            } else {
                                "Conserving 50 liters of water protects precious water resources. Every single saved liter can nourish hundreds of university plants or preserve clean groundwater."
                            }
                            "m_energy" -> if (isAr) {
                                "توفير ٢٠ كيلوواط ساعة من الكهرباء يكفي لتشغيل شاشة كمبيوتر في معمل الجامعة لمدة تزيد عن ١٥٠ ساعة متواصلة! التدوير يوفر الطاقة الأولية بشكل هائل."
                            } else {
                                "Saving 20 kWh of electricity is enough to power a computer screen in the campus labs for over 150 hours continuously! Recycling saves immense primary power."
                            }
                            "m_trees" -> if (isAr) {
                                "مكافأة زراعة نصف شجرة أرز ناضجة تعني إنتاج أكسجين كافي لشخصين يومياً، وتبريد الهواء المحيط بالحرم الجامعي بشكل طبيعي وصديق للبيئة."
                            } else {
                                "An offset equivalent to half a mature cedar tree means producing enough oxygen for two people daily, and naturally cooling the air surrounding the university campus."
                            }
                            "m_points" -> if (isAr) {
                                "جمع ٣٠٠٠ نقطة خبرة دليل على التزامك المطلق كنموذج ريادي. أنت تلهم عائلتك وزملائك في جامعة القاهرة لاتباع أسلوب حياة مستدام ومسؤول."
                            } else {
                                "Amassing 3,000 XP points proves your absolute commitment as a green leader. You inspire classmates and family to pursue a sustainable lifestyle."
                            }
                            "m_streak" -> if (isAr) {
                                "سلسلة ٣ أيام متتالية هي العتبة الأساسية لبناء عادة يومية مستدامة تدوم طويلاً. الالتزام اليومي البسيط يجمع نتائج بيئية مذهلة مع الوقت."
                            } else {
                                "A 3-day consecutive recycling streak is the essential threshold to forming long-lasting daily eco habits. Small daily efforts multiply into major ecological victories."
                            }
                            else -> ""
                        }
                        
                        Text(
                            text = impactText,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 14.sp
                        )
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val unit = if (isAr) milestone.unitAr else milestone.unitEn
                    val currentFormatted = if (milestone.currentVal % 1.0 == 0.0) milestone.currentVal.toInt().toString() else String.format("%.1f", milestone.currentVal)
                    val targetFormatted = if (milestone.targetVal % 1.0 == 0.0) milestone.targetVal.toInt().toString() else String.format("%.1f", milestone.targetVal)
                    
                    Text(
                        text = if (isAr) "التقدم الحالي:" else "Current Progress:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "$currentFormatted / $targetFormatted $unit",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = milestone.color
                    )
                }
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.padding(16.dp).testTag("milestone_detail_dialog")
    )
}

data class RotatingChallenge(
    val id: String,
    val titleAr: String,
    val titleEn: String,
    val descriptionAr: String,
    val descriptionEn: String,
    val target: Int,
    val rewardPoints: Int,
    val emoji: String,
    val stepsAr: List<String>,
    val stepsEn: List<String>,
    val color: Color
)

@Composable
fun WeeklyRotatingChallengeHub(viewModel: MainViewModel) {
    val currentLang by viewModel.currentLanguage.collectAsState()
    val context = LocalContext.current
    val isAr = currentLang == "ar"

    val rotatingChallenges = remember {
        listOf(
            RotatingChallenge(
                id = "rot_plastic",
                titleAr = "أسبوع بلا بلاستيك 🚫🥤",
                titleEn = "Zero Plastic Week 🚫🥤",
                descriptionAr = "تجنب تماماً استخدام الأكياس والزجاجات البلاستيكية أحادية الاستخدام واستبدلها ببدائل مستدامة لحماية البحار والمحيطات.",
                descriptionEn = "Strictly avoid single-use plastic bags and bottles and use sustainable alternatives to protect marine life.",
                target = 7,
                rewardPoints = 500,
                emoji = "🚫",
                stepsAr = listOf(
                    "تجنب الأكياس البلاستيكية بالمتجر 🛍️",
                    "استخدام زجاجة مياه قابلة لإعادة التعبئة 💧",
                    "فرز عبوة بلاستيكية في صندوق الذكي 📥",
                    "شراء منتج بدون غلاف بلاستيكي 🍎",
                    "رفض استخدام الماصّة البلاستيكية 🥤",
                    "إرشاد صديق لأضرار البلاستيك 📢",
                    "تنظيف مساحة صغيرة من القمامة 🌿"
                ),
                stepsEn = listOf(
                    "Avoid plastic bags at store 🛍️",
                    "Use reusable water bottle 💧",
                    "Deposit plastic container in Smart Bin 📥",
                    "Buy unpackaged fresh products 🍎",
                    "Say no to plastic straws 🥤",
                    "Educate a friend on plastic waste 📢",
                    "Do a mini cleanup in your area 🌿"
                ),
                color = LeafGreen
            ),
            RotatingChallenge(
                id = "rot_paper",
                titleAr = "أسبوع التدوير الورقي الذكي 📦📰",
                titleEn = "Smart Paper Recycling Week 📦📰",
                descriptionAr = "تجميع أوراق الكتب القديمة والكرتون والصحف وفرزها بشكل صحيح لدعم الحفاظ على الغابات والأشجار.",
                descriptionEn = "Collect and properly recycle old textbooks, cardboard boxes, and newspapers to conserve forests and trees.",
                target = 5,
                rewardPoints = 400,
                emoji = "📦",
                stepsAr = listOf(
                    "جمع الدفاتر والأوراق القديمة 📓",
                    "فرز الصناديق الكرتونية المفككة 📦",
                    "قراءة كتاب إلكتروني بدلاً من الورقي 📱",
                    "إعادة استخدام كرتونة لتخزين الأغراض 🔄",
                    "التبرع بالكتب الخارجية لطلاب آخرين 📚"
                ),
                stepsEn = listOf(
                    "Gather old papers and notebooks 📓",
                    "Sort flattened cardboard boxes 📦",
                    "Read an e-book instead of paper 📱",
                    "Reuse a cardboard box for storage 🔄",
                    "Donate textbook to other students 📚"
                ),
                color = GoldEarth
            ),
            RotatingChallenge(
                id = "rot_water",
                titleAr = "أسبوع حماية الموارد المائية 💧🧼",
                titleEn = "Water Resource Protection Week 💧🧼",
                descriptionAr = "تبني عادات يومية حكيمة لترشيد استهلاك المياه والمحافظة على النقاء البيئي للمسطحات المائية.",
                descriptionEn = "Adopt conscious daily habits to reduce clean water consumption and preserve our blue planet.",
                target = 6,
                rewardPoints = 450,
                emoji = "💧",
                stepsAr = listOf(
                    "تقليل وقت الاستحمام بدقيقتين ⏱️",
                    "إغلاق الصنبور أثناء غسيل الأسنان 🪥",
                    "استخدام نصف سعة غسالة الصحون 🍽️",
                    "ري النباتات المنزلية قبل الغروب 🌿",
                    "ملاحظة وإصلاح تسريب مائي صغير 🔧",
                    "إعادة استخدام مياه غسل الخضار للري ♻️"
                ),
                stepsEn = listOf(
                    "Reduce shower time by 2 minutes ⏱️",
                    "Close faucet while brushing teeth 🪥",
                    "Run dishwasher only when full 🍽️",
                    "Water household plants near sunset 🌿",
                    "Locate and fix a small leak 🔧",
                    "Reuse vegetable washing water for plants ♻️"
                ),
                color = Color(0xFF2196F3)
            ),
            RotatingChallenge(
                id = "rot_energy",
                titleAr = "أسبوع خفض البصمة الطاقية ⚡🔌",
                titleEn = "Energy Footprint Reduction Week ⚡🔌",
                descriptionAr = "تقليل استهلاك الكهرباء في الغرف ومكاتب الجامعة لخفض انبعاثات ثاني أكسيد الكربون غير المباشرة.",
                descriptionEn = "Minimize electrical energy usage in rooms and campus offices to lower indirect CO2 emissions.",
                target = 5,
                rewardPoints = 350,
                emoji = "⚡",
                stepsAr = listOf(
                    "إطفاء مصابيح الغرف غير المستعملة 💡",
                    "فصل الشواحن والأجهزة ليلاً 🔌",
                    "استخدام الدرج بدلاً من المصعد مرتين 🏃",
                    "تفعيل وضع توفير الطاقة بالهاتف 🔋",
                    "الاعتماد على ضوء الشمس الطبيعي صباحاً ☀️"
                ),
                stepsEn = listOf(
                    "Turn off lights in unused rooms 💡",
                    "Unplug chargers and devices overnight 🔌",
                    "Take stairs instead of elevator twice 🏃",
                    "Enable battery-saving mode on phone 🔋",
                    "Rely on natural daylight in morning ☀️"
                ),
                color = Color(0xFFE91E63)
            )
        )
    }

    var challengeIndex by remember { mutableStateOf(0) }
    val currentChallenge = rotatingChallenges[challengeIndex]

    // Local tracking state for completed step indices for each challenge ID
    val completedStepsMap = remember { mutableStateMapOf<String, Set<Int>>() }
    val activeCompletedSteps = completedStepsMap[currentChallenge.id] ?: emptySet()

    // Local tracking state for claimed grand reward for each challenge ID
    val claimedRewardsMap = remember { mutableStateMapOf<String, Boolean>() }
    val isClaimed = claimedRewardsMap[currentChallenge.id] ?: false

    val progressCount = activeCompletedSteps.size
    val totalTarget = currentChallenge.target
    val progressFraction = if (totalTarget > 0) progressCount.toFloat() / totalTarget.toFloat() else 0f
    val isChallengeFinished = progressCount >= totalTarget

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                border = BorderStroke(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            currentChallenge.color.copy(alpha = 0.5f),
                            GoldEarth.copy(alpha = 0.3f)
                        )
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .testTag("weekly_challenge_hub"),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Goal type pill and Rotator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Goal Pill Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .background(currentChallenge.color.copy(alpha = 0.15f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (isAr) "تحدي أسبوعي دوّار ⚡" else "Weekly Rotating Challenge ⚡",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = currentChallenge.color
                    )
                }

                // Rotator Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            challengeIndex = (challengeIndex - 1 + rotatingChallenges.size) % rotatingChallenges.size
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .testTag("weekly_challenge_rotate_prev")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Previous Goal",
                            tint = MistWhite,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Text(
                        text = "${challengeIndex + 1}/${rotatingChallenges.size}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MistWhite
                    )

                    IconButton(
                        onClick = {
                            challengeIndex = (challengeIndex + 1) % rotatingChallenges.size
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .testTag("weekly_challenge_rotate_next")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Next Goal",
                            tint = MistWhite,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Challenge Title and Description
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = if (isAr) currentChallenge.titleAr else currentChallenge.titleEn,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = CreamPaper
                )
                Text(
                    text = if (isAr) currentChallenge.descriptionAr else currentChallenge.descriptionEn,
                    fontSize = 11.sp,
                    color = MistWhite.copy(alpha = 0.8f),
                    lineHeight = 15.sp
                )
            }

            // Progress Bar and text
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isAr) {
                            "التقدم نحو الاكتمال: $progressCount من أصل $totalTarget متطلبات"
                        } else {
                            "Progress: $progressCount of $totalTarget completed"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MistWhite
                    )

                    Text(
                        text = "${(progressFraction * 100).toInt()}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = currentChallenge.color
                    )
                }

                LinearProgressIndicator(
                    progress = progressFraction,
                    color = currentChallenge.color,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                thickness = 1.dp
            )

            // Steps Checklist Section
            Text(
                text = if (isAr) "العادات والمهام اليومية المطلوبة:" else "Required Daily Habits & Tasks:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = CreamPaper
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val steps = if (isAr) currentChallenge.stepsAr else currentChallenge.stepsEn
                steps.forEachIndexed { stepIdx, stepText ->
                    val isStepCompleted = activeCompletedSteps.contains(stepIdx)
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isStepCompleted) {
                                    currentChallenge.color.copy(alpha = 0.08f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                                }
                            )
                            .border(
                                width = 1.dp,
                                color = if (isStepCompleted) {
                                    currentChallenge.color.copy(alpha = 0.25f)
                                } else {
                                    Color.Transparent
                                },
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                val currentSet = completedStepsMap[currentChallenge.id] ?: emptySet()
                                val isNowCompleted = !currentSet.contains(stepIdx)
                                
                                val nextSet = if (isNowCompleted) {
                                    currentSet + stepIdx
                                } else {
                                    currentSet - stepIdx
                                }
                                completedStepsMap[currentChallenge.id] = nextSet

                                if (isNowCompleted) {
                                    // Award sub-step XP
                                    viewModel.awardPoints(25)
                                    val pointsMsg = if (isAr) {
                                        "رائع! أنجزت مهمة بيئية وربحت +25 XP! 🌱"
                                    } else {
                                        "Great job! Task completed, earned +25 XP! 🌱"
                                    }
                                    Toast.makeText(context, pointsMsg, Toast.LENGTH_SHORT).show()
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                            .testTag("weekly_challenge_step_$stepIdx"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Custom interactive check box
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isStepCompleted) currentChallenge.color else Color.Transparent
                                )
                                .border(
                                    width = 1.5.dp,
                                    color = if (isStepCompleted) Color.Transparent else MistWhite.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(6.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isStepCompleted) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = EcoBlack,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }

                        Text(
                            text = stepText,
                            fontSize = 11.sp,
                            color = if (isStepCompleted) CreamPaper else MistWhite.copy(alpha = 0.9f),
                            fontWeight = if (isStepCompleted) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }

            // Grand Reward claim area
            if (isChallengeFinished) {
                Spacer(modifier = Modifier.height(4.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    GoldEarth.copy(alpha = 0.15f),
                                    currentChallenge.color.copy(alpha = 0.15f)
                                )
                            )
                        )
                        .border(1.dp, GoldEarth.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .padding(12.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("🏆", fontSize = 20.sp)
                            Text(
                                text = if (isAr) "تهانينا! اكتمل التحدي بنجاح! 🎉" else "Congratulations! Challenge Completed! 🎉",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldEarth
                            )
                        }

                        Text(
                            text = if (isAr) {
                                "لقد نجحت في حماية البيئة والتزمت بكافة عادات هذا الأسبوع الدوار."
                            } else {
                                "You have successfully protected resources and stuck to all this week's habits."
                            },
                            fontSize = 10.sp,
                            color = MistWhite.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )

                        Button(
                            onClick = {
                                if (!isClaimed) {
                                    claimedRewardsMap[currentChallenge.id] = true
                                    viewModel.awardPoints(currentChallenge.rewardPoints)
                                    val rewardMsg = if (isAr) {
                                        "مبروك! حصلت على جائزة التحدي الكبرى: +${currentChallenge.rewardPoints} XP! 🌟🏆"
                                    } else {
                                        "Success! Claimed Grand Reward: +${currentChallenge.rewardPoints} XP! 🌟🏆"
                                    }
                                    Toast.makeText(context, rewardMsg, Toast.LENGTH_LONG).show()
                                }
                            },
                            enabled = !isClaimed,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GoldEarth,
                                contentColor = EcoBlack,
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                disabledContentColor = MistWhite.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .testTag("weekly_challenge_claim_button")
                        ) {
                            Text(
                                text = if (isClaimed) {
                                    if (isAr) "تم استلام مكافأة التحدي بنجاح ✅" else "Reward Claimed Successfully ✅"
                                } else {
                                    if (isAr) "استلام مكافأة التحدي (+${currentChallenge.rewardPoints} XP) 🎁" else "Claim Grand Reward (+${currentChallenge.rewardPoints} XP) 🎁"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
