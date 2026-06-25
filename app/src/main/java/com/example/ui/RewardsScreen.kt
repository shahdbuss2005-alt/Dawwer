package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*

data class LuxuryBadge(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val descriptionAr: String,
    val descriptionEn: String,
    val emoji: String,
    val themeColor: Color,
    val levelRequired: Int,
    val milestoneRefId: String,
    val targetVal: Double,
    val currentVal: Double,
    val rankTitleAr: String,
    val rankTitleEn: String,
    val auraGlowColors: List<Color>
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RewardsScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val profile by viewModel.userProfile.collectAsState()
    val digitalAssets by viewModel.digitalAssets.collectAsState()
    val isAr = viewModel.currentLanguage.value == "ar"

    val activeProfile = profile ?: com.example.data.InitialData.defaultProfile

    // Animation states
    var showBonusEarned by remember { mutableStateOf(false) }
    var bonusPointsEarned by remember { mutableStateOf(0) }
    
    // Dialog details state
    var selectedMilestoneDetail by remember { mutableStateOf<RecyclingMilestone?>(null) }
    var selectedLuxuryBadgeDetail by remember { mutableStateOf<LuxuryBadge?>(null) }
    var selectedAssetDetail by remember { mutableStateOf<MainViewModel.DigitalAsset?>(null) }
    var showingRedemptionSuccess by remember { mutableStateOf<MainViewModel.DigitalAsset?>(null) }

    // List of premium, luxury-themed badges
    val luxuryBadges = remember(activeProfile) {
        listOf(
            LuxuryBadge(
                id = "badge_emerald",
                nameAr = "وسام الزمرد الإمبراطوري 🛡️💚",
                nameEn = "Imperial Emerald Insignia 🛡️💚",
                descriptionAr = "يُمنح للأوصياء البيئيين الذين نجحوا في منع انبعاثات الكربون الضارة بمقدار 1000 جرام أو أكثر عبر الفرز التلقائي.",
                descriptionEn = "Awarded to ecological guardians who successfully mitigated more than 1000g of greenhouse CO2 emissions through automated sorting.",
                emoji = "🛡️💚",
                themeColor = Color(0xFF0F9D58),
                levelRequired = 1,
                milestoneRefId = "m_co2",
                targetVal = 1000.0,
                currentVal = activeProfile.co2SavedGrams,
                rankTitleAr = "حارس الزمرد الفخري",
                rankTitleEn = "Noble Emerald Sentinel",
                auraGlowColors = listOf(Color(0xFF00FF87), Color(0xFF60EFFF))
            ),
            LuxuryBadge(
                id = "badge_sapphire",
                nameAr = "قلادة الياقوت الأزرق الملكي 🏆💙",
                nameEn = "Royal Sapphire Medallion 🏆💙",
                descriptionAr = "تُمنح للنخبة الذين حافظوا على موارد المياه العذبة وحموها من الهدر الدائري بـ 50 لتراً أو أكثر.",
                descriptionEn = "Bestowed upon the elite who conserved fresh water resources and protected them from circular waste by 50L or more.",
                emoji = "🏆💙",
                themeColor = Color(0xFF1A73E8),
                levelRequired = 2,
                milestoneRefId = "m_water",
                targetVal = 50.0,
                currentVal = activeProfile.waterSavedLiters,
                rankTitleAr = "سيد مياه الشرق الفاخر",
                rankTitleEn = "Grand Hydrology Sovereign",
                auraGlowColors = listOf(Color(0xFF00C6FF), Color(0xFF0072FF))
            ),
            LuxuryBadge(
                id = "badge_ruby",
                nameAr = "شعلة الياقوت الأحمر الملكية ⚜️❤️",
                nameEn = "Monarch Ruby Phare ⚜️❤️",
                descriptionAr = "يُمنح للمبتكرين الذين وفروا طاقة تشغيلية بمقدار 20 كيلوواط ساعة من خلال تدوير المواد الخام بكفاءة.",
                descriptionEn = "Earned by pioneers who conserved 20 kWh of operational grid electricity by recycling crucial raw elements.",
                emoji = "⚜️❤️",
                themeColor = Color(0xFFEA4335),
                levelRequired = 3,
                milestoneRefId = "m_energy",
                targetVal = 20.0,
                currentVal = activeProfile.energySavedKwh,
                rankTitleAr = "كيميائي الطاقة اللامع",
                rankTitleEn = "Luminous Energy Alchemist",
                auraGlowColors = listOf(Color(0xFFFF416C), Color(0xFFFF4B2B))
            ),
            LuxuryBadge(
                id = "badge_diamond",
                nameAr = "تاج الألماس السماوي 💎✨",
                nameEn = "Celestial Diamond Crown 💎✨",
                descriptionAr = "أرقى وسام فخري يمنح لمن تجاوزت نقاطهم 3000 XP ليصبحوا قدوة مجتمعية مشعة في جامعاتهم.",
                descriptionEn = "The highest tier honor granted to leaders whose experience surpasses 3000 XP, shining as dynamic eco-exemplars.",
                emoji = "💎✨",
                themeColor = Color(0xFF800080),
                levelRequired = 4,
                milestoneRefId = "m_points",
                targetVal = 3000.0,
                currentVal = activeProfile.points.toDouble(),
                rankTitleAr = "حليف الطبيعة الماسي الأبدي",
                rankTitleEn = "Eternal Diamond Eco-Sovereign",
                auraGlowColors = listOf(Color(0xFF8A2387), Color(0xFFE94057), Color(0xFFF27121))
            )
        )
    }

    // Daily Eco-Trivia State
    var triviaAnsweredCorrectly by remember { mutableStateOf<Boolean?>(null) }
    var selectedTriviaOption by remember { mutableStateOf<Int?>(null) }

    // Map list of milestones to match user active profile
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
                color = GoldEarth
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
                color = GoldEarth
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
                color = GoldEarth
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
                color = GoldEarth
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
                color = GoldEarth
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
                color = GoldEarth
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(EcoBlack)
    ) {
        // Luxury Subtle Background Glows
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(350.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(ForestDeep.copy(alpha = 0.8f), Color.Transparent),
                        radius = 800f
                    )
                )
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 24.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Screen Header: Luxury Brand Title
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isAr) "لوحة المكافآت الملكية" else "Imperial Rewards Vault",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = CreamPaper,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = if (isAr) "استبدل نقاطك بامتيازات وهدايا بيئية فاخرة" else "Convert eco-points into premier sustainability privileges",
                            fontSize = 11.sp,
                            color = MistWhite.copy(alpha = 0.7f)
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Share Button
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(ForestMid, RoundedCornerShape(12.dp))
                                .border(1.dp, GoldEarth.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .clickable {
                                    val unlockedBadges = luxuryBadges.filter { it.currentVal >= it.targetVal }
                                    
                                    val shareText = if (isAr) {
                                        val badgesText = if (unlockedBadges.isEmpty()) {
                                            "• لا توجد أوسمة مذهبة مفتوحة بعد. قيد التقدم والعمل الجاد! 🔒"
                                        } else {
                                            unlockedBadges.joinToString("\n") { badge ->
                                                "• ${badge.nameAr} (${badge.rankTitleAr})"
                                            }
                                        }
                                        
                                        """
                                        🌍 خزنة الإنجازات البيئية الملكية من دَوِّر 💎
                                        
                                        أنا فخور بمشاركة إنجازاتي الاستثنائية للحفاظ على الكوكب عبر تطبيق دَوِّر الذكي! 🛡️💚
                                        إليك تقرير الأثر البيئي الخاص بي:
                                        • المستوى: المستوى ${activeProfile.level}
                                        • النقاط المكتسبة: ${activeProfile.points} XP 🏆
                                        • انبعاثات CO₂ الموفرة: ${activeProfile.co2SavedGrams} جرام 🌿
                                        • المياه المحمية من الهدر: ${activeProfile.waterSavedLiters} لتر 💧
                                        • طاقة شبكة التشغيل الموفرة: ${activeProfile.energySavedKwh} كيلوواط ساعة ⚡
                                        
                                        🎖️ أوسمتي الملكية المفتوحة:
                                        $badgesText
                                        
                                        انضم إليّ الآن في فرز النفايات، واكتسب جوائز فاخرة وحافظ على الموارد الطبيعية باستخدام ذكاء دَوِّر الاصطناعي! 👑🌱
                                        """.trimIndent()
                                    } else {
                                        val badgesText = if (unlockedBadges.isEmpty()) {
                                            "• No luxury badges unlocked yet. Active journey in progress! 🔒"
                                        } else {
                                            unlockedBadges.joinToString("\n") { badge ->
                                                "• ${badge.nameEn} (${badge.rankTitleEn})"
                                            }
                                        }
                                        
                                        """
                                        🌍 Dawwer Imperial Eco-Achievements Vault 💎
                                        
                                        I am proud to share my exceptional environmental impact milestones via the Dawwer Smart AI app! 🛡️💚
                                        Here is my imperial ecological impact report:
                                        • Level: Level ${activeProfile.level}
                                        • Eco-Points: ${activeProfile.points} XP 🏆
                                        • CO₂ Saved: ${activeProfile.co2SavedGrams}g 🌿
                                        • Water Conserved: ${activeProfile.waterSavedLiters}L 💧
                                        • Energy Saved: ${activeProfile.energySavedKwh} kWh ⚡
                                        
                                        🎖️ My Unlocked Imperial Badges:
                                        $badgesText
                                        
                                        Join me in sorting waste, earning premium rewards, and conserving resources with Dawwer AI! 👑🌱
                                        """.trimIndent()
                                    }

                                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(android.content.Intent.EXTRA_SUBJECT, if (isAr) "إنجازاتي البيئية الملكية - دَوِّر" else "My Imperial Eco-Achievements - Dawwer")
                                        putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                                    }
                                    context.startActivity(android.content.Intent.createChooser(intent, if (isAr) "مشاركة الإنجازات البيئية" else "Share Eco-Achievements"))
                                }
                                .testTag("rewards_share_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = if (isAr) "مشاركة الإنجازات" else "Share Achievements",
                                tint = GoldEarth,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Decorative Icon Box
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(ForestMid, RoundedCornerShape(12.dp))
                                .border(1.dp, GoldEarth.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = GoldEarth,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // LEVEL PROGRESS & GOLD CARD
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rewards_gold_level_card"),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.5.dp, GoldEarth),
                    colors = CardDefaults.cardColors(containerColor = ForestDeep)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(ForestDeep, CardBackground)
                                )
                            )
                            .padding(20.dp)
                    ) {
                        // Decorative Background Gold Gradients
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(120.dp)
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(GoldEarth.copy(alpha = 0.08f), Color.Transparent)
                                    )
                                )
                        )

                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // User Info Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Laurel Wreath Level Emblem
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .background(
                                                brush = Brush.sweepGradient(
                                                    colors = listOf(GoldEarth, LeafGreen, GoldEarth)
                                                ),
                                                CircleShape
                                            )
                                            .padding(2.dp)
                                            .clip(CircleShape)
                                            .background(CardBackground),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "${activeProfile.level}",
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Black,
                                                color = GoldEarth
                                            )
                                            Text(
                                                text = if (isAr) "مستوى" else "LVL",
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = CreamPaper
                                            )
                                        }
                                    }

                                    Column {
                                        Text(
                                            text = activeProfile.name,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CreamPaper
                                        )
                                        Text(
                                            text = if (isAr) "رتبة: بطل البيئة الفخري 🛡️" else "Rank: Noble Eco-Guardian 🛡️",
                                            fontSize = 11.sp,
                                            color = GoldEarth,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                // Total Points Pill
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = ForestMid),
                                    border = BorderStroke(1.dp, GoldEarth.copy(alpha = 0.6f)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Stars,
                                            contentDescription = null,
                                            tint = GoldEarth,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "${activeProfile.points} XP",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Black,
                                            color = CreamPaper
                                        )
                                    }
                                }
                            }

                            // Dynamic Point Gauge to Next Level
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                val currentLvlPoints = activeProfile.points % 1000
                                val progressPercent = (currentLvlPoints.toFloat() / 1000f).coerceIn(0f, 1f)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isAr) "التقدم للمستوى التالي" else "Next Level Progress",
                                        fontSize = 11.sp,
                                        color = MistWhite.copy(alpha = 0.7f)
                                    )
                                    Text(
                                        text = "$currentLvlPoints / 1000 XP",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GoldEarth
                                        )
                                }

                                // Custom Shiny Gold-Emerald progress track
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(10.dp)
                                        .clip(CircleShape)
                                        .background(ForestMid)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(progressPercent)
                                            .clip(CircleShape)
                                            .background(
                                                brush = Brush.horizontalGradient(
                                                    colors = listOf(LeafGreen, GoldEarth)
                                                )
                                            )
                                    )
                                }
                            }

                            Divider(color = ForestMid, modifier = Modifier.padding(vertical = 4.dp))

                            // Claim Daily Eco-Bonus Interactive Button
                            Button(
                                onClick = {
                                    viewModel.awardPoints(100)
                                    bonusPointsEarned = 100
                                    showBonusEarned = true
                                    Toast.makeText(context, if (isAr) "تم إضافة +100 نقطة خبرة ملكية بنجاح! 👑" else "Earned +100 Imperial XP Points! 👑", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = GoldEarth),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("rewards_claim_bonus_button")
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Diamond,
                                        contentDescription = null,
                                        tint = ForestDeep,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = if (isAr) "المطالبة بالهدية اليومية البيئية (+100 XP)" else "Claim Daily Eco-Bonus (+100 XP)",
                                        fontSize = 12.sp,
                                        color = ForestDeep,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Circular progress indicator to Next Milestone (Recharts-inspired Radial gauge)
            item {
                val nextMilestone = milestones.firstOrNull { it.currentVal < it.targetVal } ?: milestones.lastOrNull()
                if (nextMilestone != null) {
                    val progress = (nextMilestone.currentVal / nextMilestone.targetVal).toFloat().coerceIn(0f, 1f)
                    val percent = (progress * 100).toInt()
                    val milestoneName = if (isAr) nextMilestone.nameAr else nextMilestone.nameEn
                    val unit = if (isAr) nextMilestone.unitAr else nextMilestone.unitEn
                    val currentStr = if (nextMilestone.currentVal % 1.0 == 0.0) nextMilestone.currentVal.toInt().toString() else String.format("%.1f", nextMilestone.currentVal)
                    val targetStr = if (nextMilestone.targetVal % 1.0 == 0.0) nextMilestone.targetVal.toInt().toString() else String.format("%.1f", nextMilestone.targetVal)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("milestone_recharts_circular_progress"),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, GoldEarth.copy(alpha = 0.4f)),
                        colors = CardDefaults.cardColors(containerColor = CardBackground)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1.3f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(GoldEarth.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (isAr) "🎯 الهدف البيئي القادم" else "🎯 Next Eco-Milestone Goal",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GoldEarth
                                    )
                                }
                                Text(
                                    text = milestoneName,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CreamPaper
                                )
                                Text(
                                    text = if (isAr) {
                                        "حققت $currentStr من أصل $targetStr $unit لتجاوز هذا الهدف والحفاظ على بيئة أنظف 🌱"
                                    } else {
                                        "Completed $currentStr out of $targetStr $unit to surpass this goal and protect the ecosystem 🌱"
                                    },
                                    fontSize = 11.sp,
                                    color = MistWhite.copy(alpha = 0.7f),
                                    lineHeight = 15.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = if (isAr) "الحالة:" else "Status:",
                                        fontSize = 11.sp,
                                        color = MistWhite.copy(alpha = 0.5f)
                                    )
                                    Text(
                                        text = if (progress >= 1f) {
                                            if (isAr) "مكتملة ✨" else "Completed ✨"
                                        } else {
                                            if (isAr) "قيد التقدم ($percent%)" else "In Progress ($percent%)"
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (progress >= 1f) LeafGreen else GoldEarth
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            // Custom Radial Circular Ring (Inspired by Recharts concentric progress chart)
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .weight(0.7f, fill = false),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.size(90.dp)) {
                                    // 1. Draw outer track
                                    drawArc(
                                        color = ForestMid.copy(alpha = 0.4f),
                                        startAngle = 0f,
                                        sweepAngle = 360f,
                                        useCenter = false,
                                        style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                                    )

                                    // 2. Draw active progress gradient arc
                                    drawArc(
                                        brush = Brush.sweepGradient(
                                            colors = listOf(LeafGreen, GoldEarth, LeafGreen)
                                        ),
                                        startAngle = -90f,
                                        sweepAngle = progress * 360f,
                                        useCenter = false,
                                        style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                                    )
                                }

                                // Center content: large emoji and percentage
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = nextMilestone.emoji,
                                        fontSize = 22.sp
                                    )
                                    Text(
                                        text = "$percent%",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        color = CreamPaper
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ECO-MILESTONES TITLE
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isAr) "🏆 أوسمة الاستدامة الفخرية" else "🏆 Golden Eco-Milestones",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = CreamPaper
                    )

                    val unlockedCount = milestones.count { it.currentVal >= it.targetVal }
                    Box(
                        modifier = Modifier
                            .background(GoldEarth.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .border(1.dp, GoldEarth.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isAr) "تم فتح $unlockedCount من أصل ${milestones.size}" else "$unlockedCount / ${milestones.size} Unlocked",
                            fontSize = 10.sp,
                            color = GoldEarth,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // HORIZONTAL SCROLL OR GRID OF MILSTONES
            item {
                androidx.compose.foundation.lazy.LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(milestones) { milestone ->
                        val isUnlocked = milestone.currentVal >= milestone.targetVal
                        val progress = (milestone.currentVal / milestone.targetVal).toFloat().coerceIn(0f, 1f)

                        Card(
                            onClick = { selectedMilestoneDetail = milestone },
                            colors = CardDefaults.cardColors(containerColor = CardBackground),
                            border = BorderStroke(1.dp, if (isUnlocked) GoldEarth else ForestMid.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .width(160.dp)
                                .height(160.dp)
                                .testTag("milestone_card_${milestone.id}")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(
                                                if (isUnlocked) GoldEarth.copy(alpha = 0.15f) else ForestMid.copy(alpha = 0.3f),
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = milestone.emoji, fontSize = 18.sp)
                                    }

                                    if (isUnlocked) {
                                        Icon(
                                            imageVector = Icons.Default.WorkspacePremium,
                                            contentDescription = null,
                                            tint = GoldEarth,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = MistWhite.copy(alpha = 0.4f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = if (isAr) milestone.nameAr else milestone.nameEn,
                                        color = CreamPaper,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    val progressPct = (progress * 100).toInt()
                                    Text(
                                        text = if (isUnlocked) (if (isAr) "تم الإنجاز 🎉" else "Completed 🎉") else "$progressPct%",
                                        color = if (isUnlocked) GoldEarth else MistWhite.copy(alpha = 0.6f),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    // Mini Progress bar
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(5.dp)
                                            .clip(CircleShape)
                                            .background(ForestMid)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(progress)
                                                .clip(CircleShape)
                                                .background(if (isUnlocked) GoldEarth else LeafGreen)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // LUXURY THEMED ACHIEVEMENT BADGES SECTION
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isAr) "💎 الأوسمة الملكية والنياشين الفاخرة" else "💎 Imperial Luxury Badges",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = CreamPaper
                            )
                            Text(
                                text = if (isAr) "افتح نياشين النخبة المذهبة بإنجازاتك المستدامة" else "Unlock gold-plated elite insignia via sustainable milestones",
                                fontSize = 10.sp,
                                color = MistWhite.copy(alpha = 0.6f)
                            )
                        }

                        val unlockedBadgesCount = luxuryBadges.count { it.currentVal >= it.targetVal }
                        Box(
                            modifier = Modifier
                                .background(GoldEarth.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .border(1.dp, GoldEarth, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isAr) "$unlockedBadgesCount مذهب" else "$unlockedBadgesCount Unlocked",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldEarth
                            )
                        }
                    }

                    androidx.compose.foundation.lazy.LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(luxuryBadges) { badge ->
                            val isUnlocked = badge.currentVal >= badge.targetVal
                            val progress = (badge.currentVal / badge.targetVal).coerceIn(0.0, 1.0).toFloat()

                            Card(
                                onClick = { selectedLuxuryBadgeDetail = badge },
                                colors = CardDefaults.cardColors(containerColor = CardBackground),
                                border = BorderStroke(
                                    width = 1.5.dp,
                                    brush = if (isUnlocked) {
                                        Brush.sweepGradient(badge.auraGlowColors)
                                    } else {
                                        Brush.linearGradient(listOf(ForestMid, ForestMid.copy(alpha = 0.3f)))
                                    }
                                ),
                                shape = RoundedCornerShape(22.dp),
                                modifier = Modifier
                                    .width(140.dp)
                                    .height(180.dp)
                                    .testTag("luxury_badge_card_${badge.id}")
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    // Ambient Radial Background Glow for unlocked badges
                                    if (isUnlocked) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    brush = Brush.radialGradient(
                                                        colors = listOf(badge.themeColor.copy(alpha = 0.15f), Color.Transparent)
                                                    )
                                                )
                                        )
                                    }

                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        // Hexagonal/Circular badge frame
                                        Box(
                                            modifier = Modifier
                                                .size(64.dp)
                                                .padding(2.dp)
                                                .background(
                                                    brush = if (isUnlocked) {
                                                        Brush.sweepGradient(badge.auraGlowColors)
                                                    } else {
                                                        Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                                                    },
                                                    CircleShape
                                                )
                                                .padding(1.5.dp)
                                                .clip(CircleShape)
                                                .background(if (isUnlocked) ForestDeep else Color(0x33101010)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isUnlocked) {
                                                Text(text = badge.emoji, fontSize = 28.sp)
                                            } else {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = badge.emoji,
                                                        fontSize = 28.sp,
                                                        modifier = Modifier.scale(0.85f).alpha(0.3f)
                                                    )
                                                    Icon(
                                                        imageVector = Icons.Default.Lock,
                                                        contentDescription = null,
                                                        tint = GoldEarth.copy(alpha = 0.8f),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text(
                                                text = if (isAr) badge.nameAr.replace(badge.emoji, "").trim() else badge.nameEn.replace(badge.emoji, "").trim(),
                                                color = if (isUnlocked) CreamPaper else MistWhite.copy(alpha = 0.4f),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = if (isUnlocked) {
                                                    if (isAr) badge.rankTitleAr else badge.rankTitleEn
                                                } else {
                                                    if (isAr) "مغلق" else "Incomplete"
                                                },
                                                color = if (isUnlocked) badge.themeColor else MistWhite.copy(alpha = 0.3f),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Medium,
                                                textAlign = TextAlign.Center,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        // Progress Track
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(4.dp)
                                                    .clip(CircleShape)
                                                    .background(ForestMid.copy(alpha = 0.5f))
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxHeight()
                                                        .fillMaxWidth(progress)
                                                        .clip(CircleShape)
                                                        .background(if (isUnlocked) badge.themeColor else GoldEarth.copy(alpha = 0.5f))
                                                )
                                            }

                                            val percent = (progress * 100).toInt()
                                            Text(
                                                text = if (isUnlocked) (if (isAr) "مكتمل مذهب ✨" else "Gilded ✨") else "$percent%",
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isUnlocked) GoldEarth else MistWhite.copy(alpha = 0.5f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            // REDEEMABLE ROYAL PRIVILEGES
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isAr) "🏛️ متجر الامتيازات والجوائز البيئية" else "🏛️ Sustainable Privileges & Gifts",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = CreamPaper
                    )

                    Text(
                        text = if (isAr) "اضغط للاستبدال" else "Tap to claim",
                        fontSize = 10.sp,
                        color = GoldEarth
                    )
                }
            }

            // Digital store items list
            items(digitalAssets) { asset ->
                val isAffordable = activeProfile.points >= asset.cost
                val hasLevel = activeProfile.level >= asset.requiredLevel
                val canRedeem = !asset.unlocked && isAffordable && hasLevel

                Card(
                    onClick = {
                        if (!asset.unlocked) {
                            selectedAssetDetail = asset
                        } else {
                            Toast.makeText(context, if (isAr) "قمت بفتح هذا بالفعل! يمكنك استعراضه في لوحتك." else "Already unlocked!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    border = BorderStroke(1.dp, if (asset.unlocked) GoldEarth.copy(alpha = 0.7f) else ForestMid),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("digital_asset_store_${asset.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Icon Circle
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .background(
                                    if (asset.unlocked) GoldEarth.copy(alpha = 0.15f) else ForestMid.copy(alpha = 0.2f),
                                    RoundedCornerShape(14.dp)
                                )
                                .border(
                                    1.dp,
                                    if (asset.unlocked) GoldEarth.copy(alpha = 0.4f) else Color.Transparent,
                                    RoundedCornerShape(14.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = asset.icon, fontSize = 24.sp)
                        }

                        // Text content
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = if (isAr) asset.nameAr else asset.nameEn,
                                    color = CreamPaper,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                if (asset.unlocked) {
                                    Box(
                                        modifier = Modifier
                                            .background(GoldEarth, RoundedCornerShape(6.dp))
                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (isAr) "مملوك" else "Owned",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Black,
                                            color = ForestDeep
                                        )
                                    }
                                }
                            }

                            Text(
                                text = if (isAr) asset.descriptionAr else asset.descriptionEn,
                                color = MistWhite.copy(alpha = 0.6f),
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Point price
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Stars,
                                        contentDescription = null,
                                        tint = GoldEarth,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "${asset.cost} XP",
                                        color = GoldEarth,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // Level Requirement
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LockOpen,
                                        contentDescription = null,
                                        tint = if (hasLevel) LeafGreen else DangerRust,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = if (isAr) "مستوى ${asset.requiredLevel}" else "Lvl ${asset.requiredLevel}",
                                        color = if (hasLevel) LeafGreen else DangerRust,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        // Right Arrow or Lock indicator
                        if (asset.unlocked) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = GoldEarth,
                                modifier = Modifier.size(20.dp)
                            )
                        } else if (!hasLevel) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = DangerRust,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = null,
                                tint = if (isAffordable) GoldEarth else MistWhite.copy(alpha = 0.3f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // DAILY INTERACTIVE ECO-TRIVIA CHALLENGE
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rewards_eco_trivia_card"),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, GoldEarth.copy(alpha = 0.4f)),
                    colors = CardDefaults.cardColors(containerColor = CardBackground)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Quiz,
                                    contentDescription = null,
                                    tint = GoldEarth,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = if (isAr) "🧠 تحدي اللغز البيئي اليومي" else "🧠 Daily Green Trivia",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldEarth
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .background(LeafGreen.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "+50 XP",
                                    color = LeafGreen,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Text(
                            text = if (isAr) 
                                "أي من المواد التالية يمكن إعادة تدويرها لعدد غير محدود من المرات دون تدهور جودتها على الإطلاق؟" 
                            else 
                                "Which of these materials can be recycled infinitely many times without any loss in its pure quality?",
                            fontSize = 12.sp,
                            color = CreamPaper,
                            lineHeight = 18.sp
                        )

                        val options = if (isAr) {
                            listOf("الأكياس البلاستيكية الخفيفة", "الورق والكرتون المقوى", "الزجاج النقي والألومنيوم 💎", "الألياف الصناعية والملابس")
                        } else {
                            listOf("Thin Plastic Bags", "Paper & Cardboards", "Pure Glass & Aluminum 💎", "Synthetic Fibers")
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            options.forEachIndexed { index, option ->
                                val isSelected = selectedTriviaOption == index
                                val isCorrect = index == 2
                                val borderCol = when {
                                    isSelected && triviaAnsweredCorrectly == true -> LeafGreen
                                    isSelected && triviaAnsweredCorrectly == false -> DangerRust
                                    isSelected -> GoldEarth
                                    else -> ForestMid
                                }

                                Card(
                                    onClick = {
                                        if (triviaAnsweredCorrectly == null) {
                                            selectedTriviaOption = index
                                            if (isCorrect) {
                                                triviaAnsweredCorrectly = true
                                                viewModel.awardPoints(50)
                                                Toast.makeText(context, if (isAr) "إجابة ملكية صحيحة! حصلت على +50 XP 🎉" else "Perfect! You earned +50 XP! 🎉", Toast.LENGTH_SHORT).show()
                                            } else {
                                                triviaAnsweredCorrectly = false
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    border = BorderStroke(1.dp, borderCol),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) ForestDeep else Color.Transparent
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .background(
                                                    if (isSelected) GoldEarth else ForestMid.copy(alpha = 0.5f),
                                                    CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${index + 1}",
                                                fontSize = 9.sp,
                                                color = if (isSelected) ForestDeep else MistWhite,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Text(
                                            text = option,
                                            fontSize = 12.sp,
                                            color = if (isSelected) CreamPaper else MistWhite.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }

                        // Reset Button or status
                        if (triviaAnsweredCorrectly != null) {
                            Text(
                                text = if (triviaAnsweredCorrectly == true) {
                                    if (isAr) "🎉 مذهل! الزجاج والألومنيوم يحافظان على تركيبهما الكيميائي للابد عند تدويرهما." else "🎉 Outstanding! Glass and aluminum retain pure quality forever when melted down."
                                } else {
                                    if (isAr) "❌ غير صحيح تماماً. حاول مرة أخرى غداً لتتعلم المزيد!" else "❌ Not quite correct. Keep recycling and try again tomorrow!"
                                },
                                fontSize = 11.sp,
                                color = if (triviaAnsweredCorrectly == true) GoldEarth else DangerRust,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // LEVEL UP OR BONUS CELEBRATION FLOATING WINDOW
        if (showBonusEarned) {
            Dialog(onDismissRequest = { showBonusEarned = false }) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.5.dp, GoldEarth),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    modifier = Modifier
                        .padding(16.dp)
                        .testTag("bonus_points_celebration_dialog")
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(GoldEarth.copy(alpha = 0.15f), CircleShape)
                                .border(1.dp, GoldEarth, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.WorkspacePremium,
                                contentDescription = null,
                                tint = GoldEarth,
                                modifier = Modifier.size(42.dp)
                            )
                        }

                        Text(
                            text = if (isAr) "تم استحقاق مكافأة فخرية!" else "Royal Reward Claimed!",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldEarth,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = if (isAr) "تمت إضافة $bonusPointsEarned نقطة إلى خزنتك الملكية بنجاح لفرزك الذكي المتواصل." else "Successfully deposited $bonusPointsEarned premium XP into your vault.",
                            fontSize = 12.sp,
                            color = CreamPaper,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        Button(
                            onClick = { showBonusEarned = false },
                            colors = ButtonDefaults.buttonColors(containerColor = LeafGreen),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (isAr) "استلام الكنز البيئي 🛡️" else "Accept Imperial XP 🛡️",
                                color = CreamPaper,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // MILESTONE DETAIL DIALOG
        selectedMilestoneDetail?.let { milestone ->
            val isUnlocked = milestone.currentVal >= milestone.targetVal
            Dialog(onDismissRequest = { selectedMilestoneDetail = null }) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.5.dp, GoldEarth),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    modifier = Modifier
                        .padding(16.dp)
                        .testTag("milestone_vault_dialog")
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = milestone.emoji,
                            fontSize = 54.sp
                        )

                        Text(
                            text = if (isAr) milestone.nameAr else milestone.nameEn,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldEarth,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = if (isAr) milestone.descriptionAr else milestone.descriptionEn,
                            fontSize = 12.sp,
                            color = CreamPaper,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        Divider(color = ForestMid, modifier = Modifier.padding(vertical = 4.dp))

                        // Progress Info
                        val unit = if (isAr) milestone.unitAr else milestone.unitEn
                        val currentFormatted = if (milestone.currentVal % 1.0 == 0.0) milestone.currentVal.toInt().toString() else String.format("%.1f", milestone.currentVal)
                        val targetFormatted = if (milestone.targetVal % 1.0 == 0.0) milestone.targetVal.toInt().toString() else String.format("%.1f", milestone.targetVal)

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = if (isAr) "مستوى مساهمتك الحالي:" else "Your Current Footprint Level:",
                                fontSize = 11.sp,
                                color = MistWhite.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "$currentFormatted / $targetFormatted $unit",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = GoldEarth
                            )
                        }

                        Button(
                            onClick = { selectedMilestoneDetail = null },
                            colors = ButtonDefaults.buttonColors(containerColor = ForestMid),
                            border = BorderStroke(1.dp, GoldEarth.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (isAr) "إغلاق الخزانة" else "Close Ledger",
                                color = GoldEarth,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // LUXURY BADGE DETAIL DIALOG
        selectedLuxuryBadgeDetail?.let { badge ->
            val isUnlocked = badge.currentVal >= badge.targetVal
            Dialog(onDismissRequest = { selectedLuxuryBadgeDetail = null }) {
                Card(
                    shape = RoundedCornerShape(26.dp),
                    border = BorderStroke(2.dp, Brush.sweepGradient(badge.auraGlowColors)),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    modifier = Modifier
                        .padding(16.dp)
                        .testTag("luxury_badge_detail_dialog")
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Glowing Emblem with background pulse
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(badge.themeColor.copy(alpha = 0.25f), Color.Transparent)
                                    )
                                )
                                .border(1.5.dp, Brush.sweepGradient(badge.auraGlowColors), CircleShape)
                                .padding(4.dp)
                                .clip(CircleShape)
                                .background(ForestDeep),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = badge.emoji, fontSize = 42.sp)
                        }

                        Text(
                            text = if (isAr) badge.nameAr else badge.nameEn,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = GoldEarth,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = if (isUnlocked) {
                                if (isAr) "🏆 رتبة معتمدة: ${badge.rankTitleAr}" else "🏆 Verified Rank: ${badge.rankTitleEn}"
                            } else {
                                if (isAr) "🔒 مغلق مؤقتاً في خزانتك" else "🔒 Locked in Imperial Vault"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isUnlocked) LeafGreen else MistWhite.copy(alpha = 0.4f),
                            textAlign = TextAlign.Center
                        )

                        Divider(color = ForestMid, modifier = Modifier.padding(vertical = 4.dp))

                        Text(
                            text = if (isAr) badge.descriptionAr else badge.descriptionEn,
                            fontSize = 12.sp,
                            color = CreamPaper,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        // Requirement detail
                        val progress = (badge.currentVal / badge.targetVal).coerceIn(0.0, 1.0).toFloat()
                        val percent = (progress * 100).toInt()
                        val currentStr = if (badge.currentVal % 1.0 == 0.0) badge.currentVal.toInt().toString() else String.format("%.1f", badge.currentVal)
                        val targetStr = if (badge.targetVal % 1.0 == 0.0) badge.targetVal.toInt().toString() else String.format("%.1f", badge.targetVal)

                        Column(
                            modifier = Modifier.fillMaxWidth().background(ForestMid.copy(alpha = 0.3f), RoundedCornerShape(12.dp)).padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = if (isAr) "التقدم المالي لطلب الوسام:" else "Royal Progress for Claim:",
                                fontSize = 10.sp,
                                color = MistWhite.copy(alpha = 0.5f)
                            )
                            Text(
                                text = "$currentStr / $targetStr (${percent}%)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isUnlocked) badge.themeColor else GoldEarth
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    val shareText = if (isAr) {
                                        if (isUnlocked) {
                                            """
                                            🏆 لقد حصلت للتو على وسام ملكي في دَوِّر!
                                            
                                            وسام: ${badge.nameAr}
                                            رتبتي المعتمدة: ${badge.rankTitleAr}
                                            
                                            ${badge.descriptionAr}
                                            
                                            انضم إليّ في فرز النفايات والحد من الانبعاثات مع دَوِّر الذكي! 👑🌱
                                            """.trimIndent()
                                        } else {
                                            """
                                            🎯 أنا قيد طريقي لفتح وسام ملكي في دَوِّر!
                                            
                                            وسام الهدف: ${badge.nameAr}
                                            التقدم الحالي: $currentStr من أصل $targetStr
                                            
                                            انضم إليّ لنحمي بيئتنا ونربح هدايا متميزة مع دَوِّر! 👑🌱
                                            """.trimIndent()
                                        }
                                    } else {
                                        if (isUnlocked) {
                                            """
                                            🏆 I just unlocked a royal insignia in Dawwer!
                                            
                                            Badge: ${badge.nameEn}
                                            My Verified Rank: ${badge.rankTitleEn}
                                            
                                            ${badge.descriptionEn}
                                            
                                            Join me in sorting waste and saving the environment with Dawwer AI! 👑🌱
                                            """.trimIndent()
                                        } else {
                                            """
                                            🎯 I am on my way to unlocking a royal insignia in Dawwer!
                                            
                                            Target Badge: ${badge.nameEn}
                                            Current Progress: $currentStr / $targetStr
                                            
                                            Join me to protect our planet and earn premium rewards with Dawwer! 👑🌱
                                            """.trimIndent()
                                        }
                                    }

                                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(android.content.Intent.EXTRA_SUBJECT, if (isAr) "وسام ملكي جديد - دَوِّر" else "New Royal Badge - Dawwer")
                                        putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                                    }
                                    context.startActivity(android.content.Intent.createChooser(intent, if (isAr) "مشاركة الوسام" else "Share Badge"))
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ForestMid),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, GoldEarth.copy(alpha = 0.5f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                    tint = GoldEarth,
                                    modifier = Modifier.size(16.dp).padding(end = 4.dp)
                                )
                                Text(
                                    text = if (isAr) "مشاركة" else "Share",
                                    color = CreamPaper,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }

                            Button(
                                onClick = { selectedLuxuryBadgeDetail = null },
                                colors = ButtonDefaults.buttonColors(containerColor = badge.themeColor),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = if (isAr) "الرجوع" else "Return",
                                    color = CreamPaper,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // REDEEM ASSET CONFIRMATION DIALOG
        selectedAssetDetail?.let { asset ->
            val isAffordable = activeProfile.points >= asset.cost
            val hasLevel = activeProfile.level >= asset.requiredLevel

            Dialog(onDismissRequest = { selectedAssetDetail = null }) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.5.dp, GoldEarth),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    modifier = Modifier
                        .padding(16.dp)
                        .testTag("asset_claim_confirmation_dialog")
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = asset.icon,
                            fontSize = 48.sp
                        )

                        Text(
                            text = if (isAr) "تأكيد اقتناء الجائزة" else "Acquire Eco-Prize?",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldEarth
                        )

                        Text(
                            text = if (isAr) "هل أنت متأكد من رغبتك في استبدال ${asset.cost} XP مقابل الحصول على:" else "Are you sure you want to trade ${asset.cost} XP in exchange for:",
                            fontSize = 12.sp,
                            color = MistWhite,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = if (isAr) asset.nameAr else asset.nameEn,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = CreamPaper,
                            textAlign = TextAlign.Center
                        )

                        Divider(color = ForestMid, modifier = Modifier.padding(vertical = 4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Cancel Button
                            OutlinedButton(
                                onClick = { selectedAssetDetail = null },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CreamPaper),
                                border = BorderStroke(1.dp, ForestMid),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(text = if (isAr) "تراجع" else "Cancel", fontSize = 12.sp)
                            }

                            // Confirm Button
                            Button(
                                onClick = {
                                    val assetToRedeem = selectedAssetDetail
                                    selectedAssetDetail = null
                                    if (assetToRedeem != null) {
                                        viewModel.redeemAsset(
                                            assetToRedeem.id,
                                            onSuccess = {
                                                showingRedemptionSuccess = assetToRedeem
                                            },
                                            onError = { error ->
                                                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                                            }
                                        )
                                    }
                                },
                                enabled = isAffordable && hasLevel,
                                colors = ButtonDefaults.buttonColors(containerColor = GoldEarth),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1.2f)
                            ) {
                                Text(
                                    text = if (isAr) "تأكيد الشراء 🪙" else "Redeem Now 🪙",
                                    color = ForestDeep,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // REDEMPTION SUCCESS DIALOG (GOLD RECEIPT LOOK)
        showingRedemptionSuccess?.let { asset ->
            Dialog(onDismissRequest = { showingRedemptionSuccess = null }) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(2.dp, GoldEarth),
                    colors = CardDefaults.cardColors(containerColor = ForestDeep),
                    modifier = Modifier
                        .padding(16.dp)
                        .testTag("redemption_receipt_dialog")
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Success Fireworks Icon
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(GoldEarth.copy(alpha = 0.2f), CircleShape)
                                .border(1.5.dp, GoldEarth, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = GoldEarth,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Text(
                            text = if (isAr) "تذكرة اقتناء حائز البيئة" else "Sustainability Voucher Printed",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldEarth
                        )

                        Text(
                            text = if (isAr) "تهانينا! لقد قمت بتبديل نقاطك بنجاح مقابل الجائزة التالية:" else "Congratulations! You have successfully purchased this eco-prize:",
                            fontSize = 12.sp,
                            color = CreamPaper,
                            textAlign = TextAlign.Center
                        )

                        // Ticket details box
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CardBackground),
                            border = BorderStroke(1.dp, ForestMid),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(text = asset.icon, fontSize = 36.sp)
                                Text(
                                    text = if (isAr) asset.nameAr else asset.nameEn,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldEarth,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = if (isAr) asset.descriptionAr else asset.descriptionEn,
                                    color = MistWhite.copy(alpha = 0.7f),
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "SECURE VOUCHER ID: ECO-${asset.id.uppercase()}",
                                    fontSize = 9.sp,
                                    color = GoldEarth,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Button(
                            onClick = { showingRedemptionSuccess = null },
                            colors = ButtonDefaults.buttonColors(containerColor = LeafGreen),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (isAr) "حفظ التذكرة" else "Dismiss Receipt",
                                color = CreamPaper,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
