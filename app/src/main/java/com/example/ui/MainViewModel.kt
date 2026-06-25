package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.data.BadgeItem
import com.example.data.Challenge
import com.example.data.DawwerRepository
import com.example.data.GeminiScanResult
import com.example.data.InitialData
import com.example.data.UserProfile
import com.example.data.RecyclingMethod
import com.example.data.CreativeReuse
import com.example.network.GeminiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.example.BuildConfig

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database: AppDatabase by lazy {
        Room.databaseBuilder(
            application.applicationContext,
            AppDatabase::class.java,
            "dawwer_database"
        ).fallbackToDestructiveMigration().build()
    }

    private val repository: DawwerRepository by lazy {
        DawwerRepository(database)
    }

    private val geminiService = GeminiService()

    // Database reactive Flows
    val userProfile: StateFlow<UserProfile?> = repository.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val scanHistory = repository.scanHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val challenges = repository.challenges
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val badges = repository.badges
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI Loading & Temporary States
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _activeScanResult = MutableStateFlow<GeminiScanResult?>(null)
    val activeScanResult: StateFlow<GeminiScanResult?> = _activeScanResult.asStateFlow()

    private val _isGeneratingAlternativeIdea = MutableStateFlow(false)
    val isGeneratingAlternativeIdea: StateFlow<Boolean> = _isGeneratingAlternativeIdea.asStateFlow()

    private val _voiceAssistantResponse = MutableStateFlow<String?>(null)
    val voiceAssistantResponse: StateFlow<String?> = _voiceAssistantResponse.asStateFlow()

    private val _isVoiceAssistantLoading = MutableStateFlow(false)
    val isVoiceAssistantLoading: StateFlow<Boolean> = _isVoiceAssistantLoading.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(true) // Simulating user session logged in by default
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()
    
    private val appPrefs = application.getSharedPreferences("app_main_prefs", android.content.Context.MODE_PRIVATE)
    
    private val _hasSeenOnboarding = MutableStateFlow(appPrefs.getBoolean("has_seen_onboarding", false))
    val hasSeenOnboarding: StateFlow<Boolean> = _hasSeenOnboarding.asStateFlow()

    private val _selectedUniversity = MutableStateFlow(appPrefs.getString("selected_university", "cairo_university") ?: "cairo_university")
    val selectedUniversity: StateFlow<String> = _selectedUniversity.asStateFlow()

    fun completeOnboarding() {
        appPrefs.edit().putBoolean("has_seen_onboarding", true).apply()
        _hasSeenOnboarding.value = true
    }

    fun setUniversity(uniId: String) {
        appPrefs.edit().putString("selected_university", uniId).apply()
        _selectedUniversity.value = uniId
    }

    // Multi-Language Support Translations (ar, en, es, fr, tr)
    private val _currentLanguage = MutableStateFlow("ar")
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    // --- Leaderboard User Model & State ---
    data class LeaderboardUser(
        val rank: String,
        val name: String,
        val points: Int,
        val isCurrentUser: Boolean,
        val cheered: Boolean = false
    )

    private val _leaderboardUsers = MutableStateFlow<List<LeaderboardUser>>(emptyList())
    val leaderboardUsers: StateFlow<List<LeaderboardUser>> = _leaderboardUsers.asStateFlow()

    // --- Daily Eco-Tip & AI Tip State ---
    private val defaultEcoTips = listOf(
        "افرز البلاستيك والورق دائماً بشكل منفصل لتجنب تلويث المواد القابلة للتدوير! 🌿",
        "قم بغسل عبوات العصير والحليب بالماء سريعاً قبل الفرز لمنع تعفنها وجذب الحشرات والروائح الكريهة. 🧴",
        "انزع الأغطية البلاستيكية والحلقات من الزجاجات، فالمصانع تعيد تدويرها بشكل منفصل تماماً! 🪙",
        "تجنب كبس الورق والكرتون المبلل، لأن الرطوبة العالية تضعف ألياف السيليولوز وتمنع تصنيعها مجدداً. 📦",
        "هل تعلم؟ إعادة تدوير علبة ألومنيوم واحدة توفر طاقة كافية لتشغيل شاشة تلفازك لمدة ٣ ساعات كاملة! 🥤",
        "تخلص من البطاريات والأسلاك التالفة في حاوية الإلكترونيات المخصصة لمنع تسرب المعادن الثقيلة السامة للتربة. 💻",
        "استخدم بقايا قشور الفواكه والخضروات لعمل سماد منزلي عضوي (كمبوست) يغذي مزروعات شرفتك مجاناً! 🌿",
        "أكياس البلاستيك الشفافة تأخذ مئات السنين لتتحلل؛ استبدلها دائماً بحقيبة تسوق قماشية مستدامة. 🛍️",
        "عند استلام الطرود الكرتونية، قم بتفكيكها لتصبح مسطحة، فهذا يقلل من مساحة التخزين وحجم النقل بنسبة ٨٠٪! 📦",
        "احتفظ بزجاجات الأدوية والزجاج الفارغ بعيداً عن النفايات العادية وسلمها للصيدليات أو مراكز التجميع المعملي. 🍶"
    )

    private val _dailyEcoTip = MutableStateFlow(defaultEcoTips[0])
    val dailyEcoTip: StateFlow<String> = _dailyEcoTip.asStateFlow()

    private val _isGeneratingTip = MutableStateFlow(false)
    val isGeneratingTip: StateFlow<Boolean> = _isGeneratingTip.asStateFlow()

    // --- Reminder system & Notification Settings ---
    private val _showReminderBanner = MutableStateFlow(false)
    val showReminderBanner: StateFlow<Boolean> = _showReminderBanner.asStateFlow()

    private val prefs = application.getSharedPreferences("eco_reminders_prefs", android.content.Context.MODE_PRIVATE)

    private val _isDailyReminderEnabled = MutableStateFlow(prefs.getBoolean("daily_reminder_enabled", true))
    val isDailyReminderEnabled: StateFlow<Boolean> = _isDailyReminderEnabled.asStateFlow()

    private val _reminderTime = MutableStateFlow(prefs.getString("reminder_time", "18:00") ?: "18:00")
    val reminderTime: StateFlow<String> = _reminderTime.asStateFlow()

    private val _isLevelUpAlertEnabled = MutableStateFlow(prefs.getBoolean("levelup_alert_enabled", true))
    val isLevelUpAlertEnabled: StateFlow<Boolean> = _isLevelUpAlertEnabled.asStateFlow()

    private val _isPeakHourReminderEnabled = MutableStateFlow(prefs.getBoolean("peak_hour_reminder_enabled", true))
    val isPeakHourReminderEnabled: StateFlow<Boolean> = _isPeakHourReminderEnabled.asStateFlow()

    private val _autoTriggerVoiceAssistant = MutableStateFlow(false)
    val autoTriggerVoiceAssistant: StateFlow<Boolean> = _autoTriggerVoiceAssistant.asStateFlow()

    data class AlertLog(
        val id: String,
        val title: String,
        val message: String,
        val timestamp: Long
    )

    private val _recentAlerts = MutableStateFlow<List<AlertLog>>(emptyList())
    val recentAlerts: StateFlow<List<AlertLog>> = _recentAlerts.asStateFlow()

    private fun loadRecentAlerts(): List<AlertLog> {
        val jsonString = prefs.getString("recent_alerts_json", "[]") ?: "[]"
        return try {
            val arr = org.json.JSONArray(jsonString)
            val list = mutableListOf<AlertLog>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    AlertLog(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        message = obj.getString("message"),
                        timestamp = obj.getLong("timestamp")
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveRecentAlerts(alerts: List<AlertLog>) {
        val arr = org.json.JSONArray()
        for (alert in alerts) {
            val obj = org.json.JSONObject()
            obj.put("id", alert.id)
            obj.put("title", alert.title)
            obj.put("message", alert.message)
            obj.put("timestamp", alert.timestamp)
            arr.put(obj)
        }
        prefs.edit().putString("recent_alerts_json", arr.toString()).apply()
        _recentAlerts.value = alerts
    }

    fun addAlertLog(title: String, message: String) {
        val newAlert = AlertLog(
            id = java.util.UUID.randomUUID().toString(),
            title = title,
            message = message,
            timestamp = System.currentTimeMillis()
        )
        val updated = (listOf(newAlert) + _recentAlerts.value).take(10)
        saveRecentAlerts(updated)
    }

    fun clearAlertLogs() {
        saveRecentAlerts(emptyList())
    }

    fun setDailyReminderEnabled(enabled: Boolean) {
        _isDailyReminderEnabled.value = enabled
        prefs.edit().putBoolean("daily_reminder_enabled", enabled).apply()
        if (enabled) {
            val timeParts = _reminderTime.value.split(":")
            val hour = timeParts.getOrNull(0)?.toIntOrNull() ?: 18
            val minute = timeParts.getOrNull(1)?.toIntOrNull() ?: 0
            NotificationHelper.scheduleDailyReminder(getApplication(), hour, minute)
            addAlertLog(
                if (_currentLanguage.value == "ar") "تفعيل التذكير اليومي 🌱" else "Daily Reminder Enabled 🌱",
                if (_currentLanguage.value == "ar") "تم تفعيل المنبه اليومي لإعادة التدوير في الساعة ${_reminderTime.value}" else "Daily recycling alarm enabled at ${_reminderTime.value}"
            )
        } else {
            NotificationHelper.cancelDailyReminder(getApplication())
            addAlertLog(
                if (_currentLanguage.value == "ar") "إلغاء التذكير اليومي 🔕" else "Daily Reminder Disabled 🔕",
                if (_currentLanguage.value == "ar") "تم إيقاف تذكير التدوير اليومي التلقائي." else "Automatic daily recycling reminder has been stopped."
            )
        }
    }

    fun setReminderTime(time: String) {
        _reminderTime.value = time
        prefs.edit().putString("reminder_time", time).apply()
        if (_isDailyReminderEnabled.value) {
            val timeParts = time.split(":")
            val hour = timeParts.getOrNull(0)?.toIntOrNull() ?: 18
            val minute = timeParts.getOrNull(1)?.toIntOrNull() ?: 0
            NotificationHelper.scheduleDailyReminder(getApplication(), hour, minute)
            addAlertLog(
                if (_currentLanguage.value == "ar") "تحديث وقت التذكير ⏰" else "Reminder Time Updated ⏰",
                if (_currentLanguage.value == "ar") "تم تغيير موعد التذكير اليومي إلى الساعة $time" else "Daily reminder time changed to $time"
            )
        }
    }

    fun setLevelUpAlertEnabled(enabled: Boolean) {
        _isLevelUpAlertEnabled.value = enabled
        prefs.edit().putBoolean("levelup_alert_enabled", enabled).apply()
    }

    fun setPeakHourReminderEnabled(enabled: Boolean) {
        _isPeakHourReminderEnabled.value = enabled
        prefs.edit().putBoolean("peak_hour_reminder_enabled", enabled).apply()
        if (enabled) {
            NotificationHelper.schedulePeakHourReminders(getApplication())
            addAlertLog(
                if (_currentLanguage.value == "ar") "تفعيل تذكير ساعات الذروة 🎙️" else "Peak Hour Reminders Enabled 🎙️",
                if (_currentLanguage.value == "ar") "تم تفعيل تنبيهات ساعات الذروة الذكية باستخدام المساعد الصوتي." else "Smart peak-hour reminders with AI Voice Assistant enabled."
            )
        } else {
            NotificationHelper.cancelPeakHourReminders(getApplication())
            addAlertLog(
                if (_currentLanguage.value == "ar") "إلغاء تذكير ساعات الذروة 🔕" else "Peak Hour Reminders Disabled 🔕",
                if (_currentLanguage.value == "ar") "تم إلغاء تنبيهات ساعات الذروة." else "Peak-hour reminders have been disabled."
            )
        }
    }

    fun triggerVoiceAssistantAutoQuery() {
        _autoTriggerVoiceAssistant.value = true
    }

    fun consumeAutoTriggerVoiceAssistant() {
        _autoTriggerVoiceAssistant.value = false
    }

    fun triggerTestNotification() {
        val isAr = _currentLanguage.value == "ar"
        val title = if (isAr) "اختبار نظام دَوِّر الذكي 🔔" else "Dawwer Alert System Test 🔔"
        val message = if (isAr) "مرحباً بك! نظام الإشعارات والتذكير يعمل بشكل ممتاز الآن. استمر في فرز مخلفاتك لربح نقاط إضافية 🌱" else "Welcome! The notification and reminder system is fully operational. Continue sorting your waste to earn extra points 🌱"
        NotificationHelper.showNotification(getApplication(), title, message, 999)
        addAlertLog(title, message)
    }

    fun sendLevelUpNotification(newLevel: Int) {
        val isAr = _currentLanguage.value == "ar"
        val title = if (isAr) "🎉 تهانينا! لقد وصلت إلى مستوى جديد!" else "🎉 Congratulations! New Level Unlocked!"
        val message = if (isAr) "رائع جداً! تم ترقيتك بنجاح إلى المستوى $newLevel في تطبيق دَوِّر. واصل العمل الرائع وافتح مزيداً من الجوائز الرقمية المذهلة! 🏆" else "Wonderful! You've been successfully promoted to Level $newLevel in Dawwer. Keep up the awesome work to unlock digital rewards! 🏆"
        NotificationHelper.showNotification(getApplication(), title, message, 2000 + newLevel)
        addAlertLog(title, message)
    }

    // --- Download Report State ---
    private val _isGeneratingReport = MutableStateFlow(false)
    val isGeneratingReport: StateFlow<Boolean> = _isGeneratingReport.asStateFlow()

    private val _reportProgress = MutableStateFlow(0f)
    val reportProgress: StateFlow<Float> = _reportProgress.asStateFlow()

    // --- Rewards Vault Digital Assets ---
    data class DigitalAsset(
        val id: String,
        val nameAr: String,
        val nameEn: String,
        val descriptionAr: String,
        val descriptionEn: String,
        val cost: Int,
        val requiredLevel: Int,
        val icon: String,
        val unlocked: Boolean = false,
        val active: Boolean = false,
        val type: String = "digital"
    )

    private val _digitalAssets = MutableStateFlow<List<DigitalAsset>>(emptyList())
    val digitalAssets: StateFlow<List<DigitalAsset>> = _digitalAssets.asStateFlow()

    init {
        viewModelScope.launch {
            repository.checkAndSeedDatabase()
            initDigitalAssets()
            _recentAlerts.value = loadRecentAlerts()

            // Schedule active reminders on app startup
            if (_isDailyReminderEnabled.value) {
                val timeParts = _reminderTime.value.split(":")
                val hour = timeParts.getOrNull(0)?.toIntOrNull() ?: 18
                val minute = timeParts.getOrNull(1)?.toIntOrNull() ?: 0
                NotificationHelper.scheduleDailyReminder(getApplication(), hour, minute)
            }
            if (_isPeakHourReminderEnabled.value) {
                NotificationHelper.schedulePeakHourReminders(getApplication())
            }
            
            var previousLevel: Int? = null
            userProfile.collect { profile ->
                profile?.let {
                    _currentLanguage.value = it.language
                    // Update leaderboard list dynamically
                    updateLeaderboard(it.points, it.name)
                    
                    // Track level-up
                    val prev = previousLevel
                    if (prev != null && it.level > prev) {
                        if (_isLevelUpAlertEnabled.value) {
                            sendLevelUpNotification(it.level)
                        }
                    }
                    previousLevel = it.level
                }
            }
        }
    }

    fun updateLeaderboard(currentPoints: Int, currentName: String) {
        val baseUsers = listOf(
            Triple("فاطمة علي 🇪🇬", 1450, false),
            Triple("محمود حسن 🇪🇬", 1200, false),
            Triple("يوسف أحمد 🇸🇦", 2800, false),
            Triple("سلمى كريم 🇦🇪", 950, false),
            Triple("مريم علي 🇪🇬", 1950, false)
        )
        
        val currentCheeredMap = _leaderboardUsers.value.associate { it.name to it.cheered }

        val allUsers = (baseUsers.map { (name, pts, isCurr) ->
            LeaderboardUser(
                rank = "",
                name = name,
                points = pts,
                isCurrentUser = isCurr,
                cheered = currentCheeredMap[name] ?: false
            )
        } + LeaderboardUser(
            rank = "",
            name = "$currentName (أنت)",
            points = currentPoints,
            isCurrentUser = true,
            cheered = false
        )).sortedByDescending { it.points }

        val rankedUsers = allUsers.mapIndexed { idx, user ->
            val medal = when (idx) {
                0 -> "🥇"
                1 -> "🥈"
                2 -> "🥉"
                else -> "${idx + 1}"
            }
            user.copy(rank = medal)
        }
        _leaderboardUsers.value = rankedUsers
    }

    fun cheerUser(name: String) {
        _leaderboardUsers.value = _leaderboardUsers.value.map { user ->
            if (user.name == name) {
                val nextCheered = !user.cheered
                if (nextCheered) {
                    // Reward a small bonus points for social cheer
                    viewModelScope.launch {
                        val profile = userProfile.value ?: InitialData.defaultProfile
                        repository.saveUserProfile(profile.copy(points = profile.points + 10))
                    }
                }
                user.copy(cheered = nextCheered)
            } else {
                user
            }
        }
    }

    fun shuffleEcoTip() {
        val currentIndex = defaultEcoTips.indexOf(_dailyEcoTip.value)
        val nextIndex = if (currentIndex == -1) 0 else (currentIndex + 1) % defaultEcoTips.size
        _dailyEcoTip.value = defaultEcoTips[nextIndex]
    }

    fun generateAiEcoTip() {
        viewModelScope.launch {
            _isGeneratingTip.value = true
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                    _dailyEcoTip.value = defaultEcoTips.shuffled().first()
                } else {
                    val prompt = "ولد نصيحة بيئية متقدمة وقصيرة جداً (بحد أقصى سطرين) باللغة العربية الفصحى عن إعادة تدوير المخلفات المنزلية بطريقة علمية مبتكرة ومحفزة وعملية. ابدأ بـ 'نصيحة دَوِّر الذكية: ' ثم الرمز التعبيري 🌿."
                    val response = geminiService.generateText(prompt, "أنت مساعد بيئي ذكي ودود وبسيط.")
                    if (response.isNotEmpty() && response.length > 5) {
                        _dailyEcoTip.value = response
                    } else {
                        _dailyEcoTip.value = defaultEcoTips.shuffled().first()
                    }
                }
            } catch (e: Exception) {
                _dailyEcoTip.value = defaultEcoTips.shuffled().first()
            } finally {
                _isGeneratingTip.value = false
            }
        }
    }

    fun awardPoints(amount: Int) {
        viewModelScope.launch {
            repository.rewardPoints(amount)
            userProfile.value?.let { profile ->
                updateLeaderboard(profile.points + amount, profile.name)
            }
        }
    }

    fun checkInactivity() {
        viewModelScope.launch {
            val profile = userProfile.value ?: InitialData.defaultProfile
            val elapsedMs = System.currentTimeMillis() - profile.lastActive
            // 48 hours is 172800000 ms
            if (elapsedMs > 172800000L) {
                _showReminderBanner.value = true
            }
        }
    }

    fun simulateInactivity() {
        viewModelScope.launch {
            val profile = userProfile.value ?: InitialData.defaultProfile
            val threeDaysAgo = System.currentTimeMillis() - (3 * 24 * 3600 * 1000L)
            repository.saveUserProfile(profile.copy(lastActive = threeDaysAgo))
            _showReminderBanner.value = true
        }
    }

    fun resetInactivity() {
        viewModelScope.launch {
            val profile = userProfile.value ?: InitialData.defaultProfile
            repository.saveUserProfile(profile.copy(lastActive = System.currentTimeMillis()))
            _showReminderBanner.value = false
        }
    }

    fun generateReportPdf(onComplete: (String) -> Unit) {
        viewModelScope.launch {
            _isGeneratingReport.value = true
            _reportProgress.value = 0f
            for (i in 1..100) {
                delay(12)
                _reportProgress.value = i / 100f
            }
            
            val profile = userProfile.value ?: InitialData.defaultProfile
            val historyCount = scanHistory.value.size
            
            val reportText = """
                =======================================
                        دَوِّر: تقرير الأثر البيئي الفاخر
                =======================================
                الاسم الكودى: ${profile.name}
                اللقب البيئي: المستوى ${profile.level} 🌱
                إجمالي عمليات الفرز بالذكاء الاصطناعي: $historyCount
                إجمالي نقاط التقدير المكتسبة (XP): ${profile.points} XP
                ---------------------------------------
                المساهمة البيئية والموارد المحفوظة:
                ---------------------------------------
                - انبعاثات ثاني أكسيد الكربون (CO2) الموفرة: ${String.format("%.1f", profile.co2SavedGrams / 1000.0)} كجم
                - مياه عذبة محفوظة من الهدر: ${String.format("%.1f", profile.waterSavedLiters)} لتر
                - طاقة كهربائية موفرة: ${String.format("%.1f", profile.energySavedKwh)} كيلوواط/ساعة
                - معادل غابة أشجار مزروعة: ${String.format("%.2f", profile.treesPlanted)} شجرة
                ---------------------------------------
                نشهد بأن ${profile.name} أحد حماة الأرض المعتمدين لدى منصة تدوير المستدامة (دَوِّر).
                تم إصدار هذا التقرير البيئي لغرض المشاركة الاجتماعية وإلهام الآخرين.
                =======================================
            """.trimIndent()
            
            _isGeneratingReport.value = false
            onComplete(reportText)
        }
    }

    fun initDigitalAssets() {
        val savedPreferences = getApplication<Application>().getSharedPreferences("rewards_vault", android.content.Context.MODE_PRIVATE)
        val defaultAssets = listOf(
            DigitalAsset("theme_emerald", "مظهر الزبرجد الفاخر 💚", "Emerald Luxury Theme 💚", "مظهر داكن أخضر ملكي مميز يعزز هويتك الخضراء.", "A royal dark green theme that premiumizes your app interface.", 200, 3, "🎨", type = "theme"),
            DigitalAsset("wallpaper_forest", "خلفية الغابة الماطرة 🌱", "Rainforest Live Wallpaper 🌱", "خلفية حية بجودة HD لشاشة هاتفك مستوحاة من غابات الأمازون.", "A high-fidelity HD ambient live wallpaper for your phone screen.", 100, 2, "🏞️", type = "wallpaper"),
            DigitalAsset("cert_guardian", "شهادة حارس البيئة المعتمدة 📜", "Certified Eco-Guardian 📜", "نسخة رقمية موقعة تثبت تميزك الاستثنائي في الاستدامة لدعم سيرتك الذاتية.", "A signed, beautiful digital diploma showing your commitment to sustainability.", 400, 4, "📜", type = "diploma"),
            DigitalAsset("voucher_coffee", "كوب قهوة عضوي مجاني ☕", "Free Organic Coffee ☕", "احصل على كوب قهوة عضوي فاخر مجاناً من المقاهي الشريكة للصفر نفايات.", "Claim a premium hot organic coffee from our zero-waste partner cafes.", 150, 2, "☕", type = "voucher"),
            DigitalAsset("voucher_transit", "تذكرة تنقل خضراء مجانية 🚌", "Green Transit City Pass 🚌", "بطاقة مرور مجانية لوسائل النقل العام الهجينة والصديقة للبيئة.", "A full day travel pass on premium eco-friendly hybrid city buses.", 300, 3, "🚌", type = "voucher"),
            DigitalAsset("voucher_solar", "خصم ٥٠٪ على شاحن شمسي 🔋", "50% Solar Powerbank Discount 🔋", "قسيمة خصم لشراء شاحن متنقل يعمل كلياً بالطاقة الشمسية النظيفة.", "Get a half-price discount coupon for an ultra-efficient smart solar powerbank.", 600, 4, "🔋", type = "voucher"),
            DigitalAsset("virtual_cedar", "شجرة أرز افتراضية باسمك 🌲", "Virtual Cedar Tree 🌲", "قم بزراعة شجرة أرز حقيقية في محميتنا وتتبع نموها رقمياً عبر التطبيق!", "Plant a real cedar tree in our nature reserve and track its growth digitally!", 800, 5, "🌲", type = "virtual_item")
        )
        
        val loadedAssets = defaultAssets.map { asset ->
            val isUnlocked = savedPreferences.getBoolean("unlocked_${asset.id}", false)
            val isActive = savedPreferences.getBoolean("active_${asset.id}", false)
            asset.copy(unlocked = isUnlocked, active = isActive)
        }
        _digitalAssets.value = loadedAssets
    }

    fun redeemAsset(assetId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val profile = userProfile.value ?: InitialData.defaultProfile
            val assets = _digitalAssets.value
            val asset = assets.find { it.id == assetId }
            
            if (asset == null) {
                onError("الأصل غير موجود!")
                return@launch
            }
            
            if (asset.unlocked) {
                onError("لقد قمت بفتح هذا العنصر بالفعل!")
                return@launch
            }
            
            if (profile.level < asset.requiredLevel) {
                val errorMsg = if (_currentLanguage.value == "ar") {
                    "عذراً! هذا العنصر يتطلب الوصول للمستوى ${asset.requiredLevel} أولاً! 🔒"
                } else {
                    "Sorry! This item requires reaching Level ${asset.requiredLevel} first! 🔒"
                }
                onError(errorMsg)
                return@launch
            }
            
            if (profile.points < asset.cost) {
                val errorMsg = if (_currentLanguage.value == "ar") {
                    "نقاطك الحالية غير كافية! العنصر يتطلب ${asset.cost} نقطة بينما تملك ${profile.points} نقطة. 🪙"
                } else {
                    "Insufficient points! Requires ${asset.cost} XP, but you have ${profile.points} XP. 🪙"
                }
                onError(errorMsg)
                return@launch
            }
            
            // Deduct points from profile and update
            val updatedProfile = profile.copy(points = profile.points - asset.cost)
            repository.saveUserProfile(updatedProfile)
            
            // Save to Shared Preferences
            val preferences = getApplication<Application>().getSharedPreferences("rewards_vault", android.content.Context.MODE_PRIVATE)
            preferences.edit()
                .putBoolean("unlocked_${assetId}", true)
                .apply()
                
            // Refresh list
            _digitalAssets.value = _digitalAssets.value.map {
                if (it.id == assetId) it.copy(unlocked = true) else it
            }
            onSuccess()
        }
    }

    fun activateAsset(assetId: String) {
        val preferences = getApplication<Application>().getSharedPreferences("rewards_vault", android.content.Context.MODE_PRIVATE)
        val editor = preferences.edit()
        
        val nextAssets = _digitalAssets.value.map { asset ->
            if (asset.id == assetId) {
                val nextActive = !asset.active
                editor.putBoolean("active_${asset.id}", nextActive)
                asset.copy(active = nextActive)
            } else {
                // if same type, deactivate other active ones
                val item = _digitalAssets.value.find { it.id == assetId }
                if (item != null && item.type == asset.type) {
                    editor.putBoolean("active_${asset.id}", false)
                    asset.copy(active = false)
                } else {
                    asset
                }
            }
        }
        editor.apply()
        _digitalAssets.value = nextAssets
    }

    fun login(email: String, name: String) {
        viewModelScope.launch {
            val profile = userProfile.value ?: InitialData.defaultProfile
            repository.saveUserProfile(profile.copy(email = email, name = name))
            _isLoggedIn.value = true
        }
    }

    fun logout() {
        _isLoggedIn.value = false
    }

    fun setLanguage(lang: String) {
        viewModelScope.launch {
            _currentLanguage.value = lang
            val profile = userProfile.value ?: InitialData.defaultProfile
            repository.saveUserProfile(profile.copy(language = lang))
        }
    }

    fun updateProfile(name: String, email: String, phone: String, country: String) {
        viewModelScope.launch {
            val profile = userProfile.value ?: InitialData.defaultProfile
            repository.saveUserProfile(profile.copy(name = name, email = email, phone = phone, country = country))
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            val profile = userProfile.value ?: InitialData.defaultProfile
            repository.saveUserProfile(profile.copy(themeMode = mode))
        }
    }

    fun triggerScan(bitmap: Bitmap?, customText: String?) {
        viewModelScope.launch {
            _isScanning.value = true
            try {
                val result = geminiService.analyzeWaste(bitmap, customText, _currentLanguage.value)
                if (result != null) {
                    _activeScanResult.value = result
                    // Commit to repository (which saves scan item, awards points, updates streaks, and challenges!)
                    repository.addScanResult(result)
                } else {
                    Log.e("MainViewModel", "Analysis returned null scan result.")
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error scanning item: ${e.message}", e)
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun triggerBarcodeScan(barcode: String) {
        viewModelScope.launch {
            _isScanning.value = true
            delay(1500) // High-tech scanner decode simulation delay
            try {
                val isAr = _currentLanguage.value == "ar"
                val result = when (barcode.trim()) {
                    "5449000000996" -> {
                        GeminiScanResult(
                            itemName = if (isAr) "زجاجة كوكاكولا مصر (PET)" else "Coca-Cola Egypt PET Bottle",
                            itemEmoji = "🧴",
                            materialType = if (isAr) "بلاستيك" else "Plastic",
                            recyclable = true,
                            recyclabilityScore = 9,
                            environmentalImpact = if (isAr) {
                                "تحتاج العبوات البلاستيكية لقرون لتتحلل، مسببة تلوثاً حاداً للأنهار ومجاري المياه المصرية إذا لم يتم فرزها."
                            } else {
                                "Plastic bottles take centuries to decompose, causing severe pollution to rivers and Egyptian waterways if not sorted."
                            },
                            co2SavedGrams = 32.0,
                            waterSavedLiters = 2.1,
                            energySavedKwh = 0.12,
                            decompositionYears = if (isAr) "٤٥٠ سنة" else "450 Years",
                            recyclingMethods = listOf(
                                RecyclingMethod(
                                    method = if (isAr) "غسل وتجفيف وضغط" else "Rinse, Dry & Crush",
                                    difficulty = if (isAr) "سهل" else "Easy",
                                    steps = if (isAr) {
                                        listOf("شطف الزجاجة للتخلص من بقايا المشروبات", "نزع الغلاف الخارجي والغطاء البلاستيكي", "كبس الزجاجة رأسياً لتقليص المساحة بنسبة ٨٠٪")
                                    } else {
                                        listOf("Rinse bottle to clear any soda residue", "Remove label wrap and plastic cap", "Crush bottle vertically to compress space by 80%")
                                    },
                                    result = if (isAr) "مادة PET خام معاد تدويرها لمصانع الغزل والنسيج والعبوات" else "Recycled PET raw flakes for textile and packaging factories",
                                    wowFact = if (isAr) "إعادة تدوير زجاجة بلاستيكية واحدة توفر طاقة لتشغيل لمبة ليد لمدة ٢٤ ساعة!" else "Recycling one plastic bottle saves enough energy to run a LED bulb for 24 hours!"
                                )
                            ),
                            creativeReuse = listOf(
                                CreativeReuse(
                                    idea = if (isAr) "نظام ري ذاتي للنباتات" else "Self-Watering Plant System",
                                    difficulty = if (isAr) "سهل" else "Easy",
                                    materialsNeeded = if (isAr) {
                                        listOf("مقص طبيعي", "خيط قطني سميك", "طين خفيف")
                                    } else {
                                        listOf("Scissors", "Thick cotton wick", "Light potting soil")
                                    },
                                    timeMinutes = 10,
                                    steps = if (isAr) {
                                        listOf("قص الزجاجة نصفين", "عمل ثقب بالغطاء وتمرير الخيط القطني منه", "وضع النصف العلوي مقلوباً داخل السفلي المملوء بالماء")
                                    } else {
                                        listOf("Cut bottle in half", "Make a hole in cap and pass cotton wick", "Place upper half inverted inside lower half filled with water")
                                    },
                                    benefit = if (isAr) "ري تلقائي للنباتات المنزلية لمدة أسبوع كامل دون تدخل" else "Automatic irrigation for household plants for a week without intervention"
                                )
                            ),
                            funFact = if (isAr) {
                                "كوكاكولا تسعى لإعادة تدوير عبوة مقابل كل عبوة تبيعها عالمياً بحلول عام ٢٠٣٠!"
                            } else {
                                "Coca-Cola aims to recycle a bottle for every one it sells globally by 2030!"
                            },
                            pointsEarned = 40
                        )
                    }
                    "6221003001221" -> {
                        GeminiScanResult(
                            itemName = if (isAr) "غلاف شوكولاتة كورونا المعدني" else "Corona Chocolate Foil Wrapper",
                            itemEmoji = "🍫",
                            materialType = if (isAr) "معدن" else "Metal/Foil",
                            recyclable = false,
                            recyclabilityScore = 3,
                            environmentalImpact = if (isAr) {
                                "الأغلفة متعددة الطبقات من الألومنيوم والبلاستيك صعبة التدوير لعدم إمكانية فصل مكوناتها بسهولة، ويفضل تحويلها لأعمال فنية."
                            } else {
                                "Multi-layered foil and plastic wrappers are hard to recycle due to bonding, and are preferred for artistic upcycling."
                            },
                            co2SavedGrams = 5.0,
                            waterSavedLiters = 0.1,
                            energySavedKwh = 0.02,
                            decompositionYears = if (isAr) "٨٠-١٠٠ سنة" else "80-100 Years",
                            recyclingMethods = listOf(
                                RecyclingMethod(
                                    method = if (isAr) "الفرز للوقود البديل (RDF)" else "Sorting for Alternative Fuel (RDF)",
                                    difficulty = if (isAr) "متوسط" else "Medium",
                                    steps = if (isAr) {
                                        listOf("فرز الغلاف في النفايات غير العضوية الجافة", "يتم جمعه وإرساله لمصانع الأسمنت", "حرق الغلاف كطاقة بديلة عالية الكفاءة بمستشعرات كربون")
                                    } else {
                                        listOf("Sort wrapper into dry non-organic bin", "Collected and routed to Egyptian cement factories", "Incinerated safely as highly efficient alternative fuel (RDF)")
                                    },
                                    result = if (isAr) "طاقة بديلة نظيفة لمصانع الأسمنت والحديد المصرية" else "Alternative thermal energy for cement and iron plants in Egypt",
                                    wowFact = if (isAr) "أغلفة الألومنيوم المحروقة تولد طاقة حرارية تعادل الفحم الحجري لكن بانبعاثات أقل!" else "Incinerated foil wrappers generate thermal energy comparable to coal but with fewer emissions!"
                                )
                            ),
                            creativeReuse = listOf(
                                CreativeReuse(
                                    idea = if (isAr) "صناعة لوحات ديكور من الموزاييك الفضي" else "Mosaic Silver Deco Canvas",
                                    difficulty = if (isAr) "متوسط" else "Medium",
                                    materialsNeeded = if (isAr) {
                                        listOf("كرتون مستهلك", "غراء لاصق شفاف", "ألوان")
                                    } else {
                                        listOf("Used cardboard sheet", "Clear adhesive glue", "Paint brushes")
                                    },
                                    timeMinutes = 30,
                                    steps = if (isAr) {
                                        listOf("قص الأغلفة الفضية إلى قطع ومربعات صغيرة جداً", "رسم لوحة فنية بسيطة على كرتونة قديمة كخلفية", "لصق القطع الفضية اللامعة بشكل هندسي لعمل برواز أو منظر خلاب")
                                    } else {
                                        listOf("Cut silver wrappers into tiny square fragments", "Draw a simple artistic outline on a cardboard sheet", "Glue the shiny silver fragments geometrically to create beautiful frames")
                                    },
                                    benefit = if (isAr) "تحويل غلاف غير قابل للتدوير لبرواز ريفي لامع وصديق للبيئة" else "Transforming non-recyclable foil into a shimmering rustic frame"
                                )
                            ),
                            funFact = if (isAr) {
                                "شوكولاتة كورونا هي أول شركة كاكاو وشوكولاتة تأسست في الشرق الأوسط في الإسماعيلية عام ١٩١٩!"
                            } else {
                                "Corona was the very first chocolate company established in the Middle East, in Ismailia, Egypt, back in 1919!"
                            },
                            pointsEarned = 20
                        )
                    }
                    "6221007011400" -> {
                        GeminiScanResult(
                            itemName = if (isAr) "علبة حليب جهينة (Tetra Pak)" else "Juhayna Milk Carton (Tetra Pak)",
                            itemEmoji = "🍼",
                            materialType = if (isAr) "ورق" else "Cardboard/Paper",
                            recyclable = true,
                            recyclabilityScore = 7,
                            environmentalImpact = if (isAr) {
                                "تتكون عبوات التترا باك من كرتون وبلاستيك وألومنيوم مدمج. تفكيكها يحتاج تقنيات خاصة متوفرة الآن في مصر للفرز الذكي."
                            } else {
                                "Tetra Pak cartons consist of paper, plastic, and aluminum. Delamination requires special pulp technologies now active in Egypt."
                            },
                            co2SavedGrams = 25.0,
                            waterSavedLiters = 1.5,
                            energySavedKwh = 0.08,
                            decompositionYears = if (isAr) "٥ سنوات" else "5 Years",
                            recyclingMethods = listOf(
                                RecyclingMethod(
                                    method = if (isAr) "فصل الألياف المائي بمصر" else "Hydro-Pulping Delamination",
                                    difficulty = if (isAr) "صعب" else "Difficult",
                                    steps = if (isAr) {
                                        listOf("شطف العلبة بالماء جيدا لمنع الروائح الكريهة", "فك الأطراف الأربعة لتصبح مسطحة تماماً وتوفير ٨٠٪ من المساحة", "فرز العلبة في صندوق الكرتون لإرسالها لمصنع الورق المتخصص")
                                    } else {
                                        listOf("Rinse carton thoroughly to avoid sour milk odor", "Unfold all 4 corners to flatten the carton completely", "Sort into cardboard bins for routing to specialized Egyptian paper mills")
                                    },
                                    result = if (isAr) "ألياف كرتون فاخر لصناعة الكراسات، ومخلف PolyAl لصناعة البلاط والمقاعد" else "Premium paper pulp for notebooks, and PolyAl compound for durable roof tiles and park benches",
                                    wowFact = if (isAr) "مصر افتتحت أول خط إعادة تدوير عبوات الكرتون التترا باك باستثمارات بيئية ضخمة!" else "Egypt recently launched its first dedicated Tetra Pak recycling line with massive green investments!"
                                )
                            ),
                            creativeReuse = listOf(
                                CreativeReuse(
                                    idea = if (isAr) "حامل أقلام ومكتب ذكي مقاوم للمياه" else "Waterproof Desk Pen Organizer",
                                    difficulty = if (isAr) "سهل" else "Easy",
                                    materialsNeeded = if (isAr) {
                                        listOf("مقص", "ألوان أكريليك أو ملصقات ملونة")
                                    } else {
                                        listOf("Scissors", "Acrylic colors or decorative stickers")
                                    },
                                    timeMinutes = 15,
                                    steps = if (isAr) {
                                        listOf("قص الجزء العلوي من علبة جهينة الفارغة ونظفها جيدا", "العلبة مبطنة بالألومنيوم والبلاستيك من الداخل فهي معزولة تماما وضد الماء", "تزيين الجزء الخارجي بألوان أو ملصقات بيئية واستخدامها للأقلام")
                                    } else {
                                        listOf("Cut top part of empty Juhayna carton and dry it", "The inside is lined with aluminum/polyethylene making it completely waterproof", "Decorate the exterior with green stickers and use it for pens/brushes")
                                    },
                                    benefit = if (isAr) "منظم مكتب متين للغاية، مقاوم للرطوبة والمياه بنسبة ١٠٠٪ وبدون أي تكلفة" else "Sturdy desk organizer, 100% waterproof and moisture resistant at zero cost"
                                )
                            ),
                            funFact = if (isAr) {
                                "جهينة هي كبرى شركات الألبان والعصائر بمصر، وتدعم بنشاط صفر هدر في سلاسل التوريد الخاصة بها!"
                            } else {
                                "Juhayna is the largest dairy firm in Egypt, actively supporting zero-waste campaigns across local supply chains!"
                            },
                            pointsEarned = 50
                        )
                    }
                    "6221014022307" -> {
                        GeminiScanResult(
                            itemName = if (isAr) "مرطبان كريم إيفا الزجاجي" else "Eva Cosmetics Glass Jar",
                            itemEmoji = "🧴",
                            materialType = if (isAr) "زجاج" else "Glass",
                            recyclable = true,
                            recyclabilityScore = 10,
                            environmentalImpact = if (isAr) {
                                "الزجاج مادة طبيعية لا تتحلل أبداً، لكن يمكن إعادة تدويرها بنسبة ١٠٠٪ وإلى ما لا نهاية دون فقدان الجودة!"
                            } else {
                                "Glass is a natural material that never decomposes, yet is 100% recyclable infinitely without losing any quality!"
                            },
                            co2SavedGrams = 60.0,
                            waterSavedLiters = 4.0,
                            energySavedKwh = 0.25,
                            decompositionYears = if (isAr) "مليون سنة" else "1 Million Years",
                            recyclingMethods = listOf(
                                RecyclingMethod(
                                    method = if (isAr) "صهر الزجاج وإعادة تشكيله" else "Glass Melting & Re-molding",
                                    difficulty = if (isAr) "صعب" else "Difficult",
                                    steps = if (isAr) {
                                        listOf("إزالة الغطاء البلاستيكي وغسل المرطبان جيداً", "فرزه في حاوية الزجاج الخضراء", "يتم تكسيره لكسر زجاج (Cullet) وصهره في أفران حرارية لتصنيع مرطبانات جديدة")
                                    } else {
                                        listOf("Remove plastic cap and wash jar clean", "Sort into the green Glass Bin", "Crushed into cullet and melted in furnace at high heat to mold new jars")
                                    },
                                    result = if (isAr) "زجاجات ومرطبانات زجاجية جديدة بجودة فائقة ونسبة كربون منخفضة" else "Brand new glass jars and bottles with top quality and reduced carbon footprint",
                                    wowFact = if (isAr) "صهر الزجاج المعاد تدويره يوفر ٣٠٪ من استهلاك الطاقة مقارنة بتصنيعه من الرمل الخام!" else "Melting recycled glass saves 30% energy compared to raw sand manufacturing!"
                                )
                            ),
                            creativeReuse = listOf(
                                CreativeReuse(
                                    idea = if (isAr) "شموع عطرية رومانسية مخصصة" else "Custom Scented Candle Holder",
                                    difficulty = if (isAr) "سهل" else "Easy",
                                    materialsNeeded = if (isAr) {
                                        listOf("بقايا شمع قديم دافئ", "خيط شمعة", "قطرة زيت عطري (ياسمين أو قرفة)")
                                    } else {
                                        listOf("Warm wax flakes", "Candle wick with metal base", "Scented oil drops")
                                    },
                                    timeMinutes = 20,
                                    steps = if (isAr) {
                                        listOf("تثبيت الخيط في قاع مرطبان كريم إيفا الزجاجي المتين", "إذابة بقايا الشمع وإضافة قطرات الزيت العطري الطبيعي", "صب الشمع الدافئ داخل المرطبان ببطء وتركه ليتماسك بالكامل")
                                    } else {
                                        listOf("Fix candle wick in the center of clean glass jar", "Melt leftover wax flakes and add drops of aromatic oil", "Pour warm wax into the jar slowly and let it solidify fully")
                                    },
                                    benefit = if (isAr) "شمعة معطرة مذهلة لديكور المنزل وغرف النوم تضاهي الماركات الفاخرة" else "Luxury candle holder with ambient scent to elevate your room at zero cost"
                                )
                            ),
                            funFact = if (isAr) {
                                "إيفا لمستحضرات التجميل هي شركة مصرية عريقة بدأت رحلتها منذ عام ١٩٧٢ وتصدر لجميع أنحاء العالم!"
                            } else {
                                "Eva Cosmetics is an iconic Egyptian beauty brand established in 1972, exporting premium eco-friendly packages worldwide!"
                            },
                            pointsEarned = 65
                        )
                    }
                    "193248102431" -> {
                        GeminiScanResult(
                            itemName = if (isAr) "صندوق كرتون أمازون مصر" else "Amazon Egypt Cardboard Box",
                            itemEmoji = "📦",
                            materialType = if (isAr) "ورق" else "Cardboard",
                            recyclable = true,
                            recyclabilityScore = 10,
                            environmentalImpact = if (isAr) {
                                "الكرتون المموج مادة ممتازة لإعادة التدوير. تركه في القمامة العادية يسبب تلف أليافه وتعفنه سريعا."
                            } else {
                                "Corrugated cardboard is a premium recyclable material. Landfilling it causes fiber rot and emits greenhouse gases."
                            },
                            co2SavedGrams = 45.0,
                            waterSavedLiters = 3.5,
                            energySavedKwh = 0.15,
                            decompositionYears = if (isAr) "٣ أشهر" else "3 Months",
                            recyclingMethods = listOf(
                                RecyclingMethod(
                                    method = if (isAr) "إعادة عجن الورق والكرتون" else "Cardboard Repulping",
                                    difficulty = if (isAr) "سهل" else "Easy",
                                    steps = if (isAr) {
                                        listOf("إزالة الشريط اللاصق البلاستيكي بالكامل من الصندوق", "تفكيك الصندوق وضغطه ليصبح مسطحاً تماماً", "فرزه في حاوية الكرتون الزرقاء لإرساله لخطوط الإنتاج")
                                    } else {
                                        listOf("Remove all plastic adhesive tape from box", "Flatten the box completely to reduce transport footprint", "Sort into blue paper/cardboard bin for delivery to repulping factories")
                                    },
                                    result = if (isAr) "كرتون مموج جديد عالي التحمل وورق تغليف صديق للبيئة" else "New high-durability corrugated boxes and ecological wrapping paper",
                                    wowFact = if (isAr) "إعادة تدوير طن كرتون واحد ينقذ حوالي ١٧ شجرة بالغة من القطع بالكامل!" else "Recycling one ton of cardboard saves approximately 17 mature trees from logging!"
                                )
                            ),
                            creativeReuse = listOf(
                                CreativeReuse(
                                    idea = if (isAr) "منظم أدراج وتقسيم ذكي للملابس" else "Smart Drawer Divider",
                                    difficulty = if (isAr) "سهل" else "Easy",
                                    materialsNeeded = if (isAr) {
                                        listOf("مقص", "مسطرة هندسية وقلم")
                                    } else {
                                        listOf("Scissors", "Ruler & pencil")
                                    },
                                    timeMinutes = 10,
                                    steps = if (isAr) {
                                        listOf("قص قطع كرتونية مستقيمة بطول الدرج وعرضه من علبة أمازون", "عمل شقوق متعامدة في منتصف القطع الكرتونية للتداخل", "تركيبها متعامدة داخل الدرج لتقسيمه إلى مربعات مخصصة للجوارب والإكسسوارات")
                                    } else {
                                        listOf("Cut long straight cardboard strips from Amazon box", "Cut intersecting slots in the middle of each strip", "Interlock them together to create customized compartments inside your drawers")
                                    },
                                    benefit = if (isAr) "ترتيب وتنسيق فوري للملابس والإكسسوارات داخل الأدراج بدون تكلفة" else "Instant, cost-free compartmental organization for clothes and small accessories"
                                )
                            ),
                            funFact = if (isAr) {
                                "ألياف الكرتون المموج قوية للغاية ويمكن إعادة تدويرها وإعادة تصنيعها حتى ٧ مرات بنجاح!"
                            } else {
                                "Corrugated cardboard fibers are incredibly resilient and can be repulped up to 7 times successfully!"
                            },
                            pointsEarned = 55
                        )
                    }
                    "6281007130234" -> {
                        GeminiScanResult(
                            itemName = if (isAr) "عبوة عصير المراعي (HDPE)" else "Al-Marai Juice Bottle (HDPE)",
                            itemEmoji = "🥫",
                            materialType = if (isAr) "بلاستيك" else "Plastic",
                            recyclable = true,
                            recyclabilityScore = 8,
                            environmentalImpact = if (isAr) {
                                "بلاستيك HDPE (البولي إيثيلين عالي الكثافة) آمن جداً ويحظى بطلب مرتفع لمصانع إعادة التدوير لمتانته وسهولة إعادة تشكيله."
                            } else {
                                "HDPE (High-Density Polyethylene) plastic is highly durable and strongly demanded for recycling due to its safety and robust reuse properties."
                            },
                            co2SavedGrams = 28.0,
                            waterSavedLiters = 1.8,
                            energySavedKwh = 0.10,
                            decompositionYears = if (isAr) "١٠٠ سنة" else "100 Years",
                            recyclingMethods = listOf(
                                RecyclingMethod(
                                    method = if (isAr) "تخرير وقذف بلاستيك HDPE" else "HDPE Extrusion and Pelletizing",
                                    difficulty = if (isAr) "صعب" else "Difficult",
                                    steps = if (isAr) {
                                        listOf("شطف العبوة جيداً بالماء لإزالة بقايا العصير الحمضي", "نزع الغطاء الخارجي وغسيل العبوة للتخلص من الملصقات", "فرز العبوة في حاوية البلاستيك لتسليمها لخط تخرير البلاستيك الكثيف")
                                    } else {
                                        listOf("Rinse bottle thoroughly to clean out acidic juice residue", "Remove plastic cap and wash external label off", "Sort into plastic bin to be routed to dense polymer extrusion lines")
                                    },
                                    result = if (isAr) "حبيبات HDPE معقمة تستخدم لصناعة أنابيب الصرف وخزانات المياه والمقاعد" else "Sterile HDPE plastic pellets used for eco-drainpipes, heavy-duty water tanks, and furniture",
                                    wowFact = if (isAr) "بلاستيك HDPE آمن وخالٍ من مادة BPA الضارة ويصمد لعقود دون أي تأثر بالحرارة أو المياه!" else "HDPE plastic is 100% BPA-free and is so resilient it withstands environmental corrosion for decades!"
                                )
                            ),
                            creativeReuse = listOf(
                                CreativeReuse(
                                    idea = if (isAr) "مغرفة تربة وطمي متينة للزرع" else "Heavy-Duty Garden Soil Scoop",
                                    difficulty = if (isAr) "سهل" else "Easy",
                                    materialsNeeded = if (isAr) {
                                        listOf("قلم ماركر", "مقص قوي أو كاتر")
                                    } else {
                                        listOf("Marker pen", "Utility knife or strong scissors")
                                    },
                                    timeMinutes = 10,
                                    steps = if (isAr) {
                                        listOf("رسم خط مائل يبدأ من أسفل مقبض عبوة المراعي وينزل باتجاه القاع", "قص الزجاجة على طول هذا الخط المرسوم مع الحفاظ على المقبض متصلا", "استخدام الجزء المقصوص المزود بالمقبض كمغرفة جاهزة وقوية لنقل التربة للزرع")
                                    } else {
                                        listOf("Draw a diagonal line on Al-Marai bottle starting below handle down to base", "Cut along the line with a utility knife, keeping the handle intact", "Use the handle part as a highly durable shovel for garden soil/compost")
                                    },
                                    benefit = if (isAr) "الحصول على جاروف يدوي متين جداً لخدمة النباتات والزرع مجاناً تماماً وبأقل مجهود" else "A robust, highly functional garden scoop with handle at zero cost"
                                )
                            ),
                            funFact = if (isAr) {
                                "عبوات HDPE هي الأكثر متانة، ويتم فرزها سريعاً بمستشعرات الأشعة تحت الحمراء في مراكز الفرز الرقمية!"
                            } else {
                                "HDPE containers are highly durable and are sorted instantly using infrared sensors in digital sorting centers!"
                            },
                            pointsEarned = 45
                        )
                    }
                    else -> {
                        // Dynamic Fallback for any other custom barcode entered!
                        val firstChar = if (barcode.isNotEmpty()) barcode.first() else '5'
                        val isCardboard = firstChar in listOf('0', '1', '2', '3')
                        val isMetal = firstChar in listOf('7', '8')
                        val isGlass = firstChar in listOf('9')
                        
                        if (isCardboard) {
                            GeminiScanResult(
                                itemName = if (isAr) "علبة كرتون لمنتج تغليف ($barcode)" else "Cardboard Product Packaging ($barcode)",
                                itemEmoji = "📦",
                                materialType = if (isAr) "ورق" else "Cardboard",
                                recyclable = true,
                                recyclabilityScore = 10,
                                environmentalImpact = if (isAr) "الورق والكرتون يسهل إعادة تدويره وحماية الغابات من الزوال." else "Paper and cardboard are highly recyclable, saving forests from deforestation.",
                                co2SavedGrams = 40.0,
                                waterSavedLiters = 3.0,
                                energySavedKwh = 0.12,
                                decompositionYears = if (isAr) "شهرين" else "2 Months",
                                recyclingMethods = listOf(
                                    RecyclingMethod(
                                        method = if (isAr) "الفرز وإعادة العجن" else "Sorting and Pulping",
                                        difficulty = if (isAr) "سهل" else "Easy",
                                        steps = listOf(if (isAr) "تفكيك الكرتونة وضغطها مسطحة" else "Flatten the packaging completely", if (isAr) "فرزها في صندوق الكرتون والأوراق" else "Deposit in blue paper/cardboard bin"),
                                        result = if (isAr) "كرتون معاد تصنيعه للطرود" else "Recycled kraft paper for courier packages",
                                        wowFact = if (isAr) "إعادة تدوير طن كرتون واحد يوفر ٢٥٠٠ لتر من المياه!" else "Recycling one ton of paper saves 2500 liters of water!"
                                    )
                                ),
                                creativeReuse = listOf(
                                    CreativeReuse(
                                        idea = if (isAr) "علبة تخزين وترتيب منزلي" else "Eco-Storage Tray",
                                        difficulty = if (isAr) "سهل" else "Easy",
                                        materialsNeeded = listOf(if (isAr) "مقص ولصق ملون" else "Scissors & colorful tape"),
                                        timeMinutes = 10,
                                        steps = listOf(if (isAr) "قص الجزء العلوي للعلبة الكرتونية" else "Cut top flaps of the cardboard container", if (isAr) "تغليف العبوة بالورق الملون لإكسابها مظهراً جذاباً" else "Wrap the outer body with recycled paper or fabric"),
                                        benefit = if (isAr) "علبة أنيقة لتخزين المفاتيح والإكسسوارات مجاناً" else "An elegant organizer tray for keys and gadgets"
                                    )
                                ),
                                funFact = if (isAr) "الورق المموج يحتوي على ألياف قوية للغاية تصمد لعدة دورات إعادة تدوير!" else "Corrugated paper fibers are highly durable, preserving strength through multiple cycles!",
                                pointsEarned = 35
                            )
                        } else if (isMetal) {
                            GeminiScanResult(
                                itemName = if (isAr) "عبوة صفيح أو ألومنيوم ($barcode)" else "Tin or Aluminum Can Packaging ($barcode)",
                                itemEmoji = "🥫",
                                materialType = if (isAr) "معدن" else "Metal",
                                recyclable = true,
                                recyclabilityScore = 9,
                                environmentalImpact = if (isAr) "المعادن تأخذ مئات السنين للتحلل وتوفر طاقة هائلة عند تدويرها." else "Metals take hundreds of years to decompose but save massive energy when recycled.",
                                co2SavedGrams = 55.0,
                                waterSavedLiters = 0.5,
                                energySavedKwh = 0.35,
                                decompositionYears = if (isAr) "٢٠٠ سنة" else "200 Years",
                                recyclingMethods = listOf(
                                    RecyclingMethod(
                                        method = if (isAr) "الفرز والصهر الحراري" else "Sorting & Smelting",
                                        difficulty = if (isAr) "صعب" else "Difficult",
                                        steps = listOf(if (isAr) "شطف العبوة جيداً بالماء" else "Rinse the container thoroughly", if (isAr) "فرزها في الحاوية الصفراء للمعادن" else "Deposit in the yellow metal bin"),
                                        result = if (isAr) "معدن خام فاخر للتصنيع" else "Raw high-grade metal ingots for manufacturing",
                                        wowFact = if (isAr) "إعادة تدوير علبة ألومنيوم واحدة يوفر ٩٥٪ من الطاقة المستخدمة لإنتاج علبة جديدة!" else "Recycling one aluminum can saves 95% of the energy to produce a raw one!"
                                    )
                                ),
                                creativeReuse = listOf(
                                    CreativeReuse(
                                        idea = if (isAr) "أصيص لزراعة الصبار المنزلي" else "Eco-Succulent Planter",
                                        difficulty = if (isAr) "سهل" else "Easy",
                                        materialsNeeded = listOf(if (isAr) "مسمار للثقب وطمي" else "Nail for drainage, soil"),
                                        timeMinutes = 15,
                                        steps = listOf(if (isAr) "عمل ثقوب صغيرة في قاع العبوة لتصريف المياه" else "Puncture drainage holes in the can base", if (isAr) "ملؤها بالتربة الطمية وغرس الصبار" else "Fill with soil and plant a cute succulent"),
                                        benefit = if (isAr) "أوعية زراعة معدنية ريفية أنيقة لشرفتك ومكتبك" else "Charming rustic planters to adorn windowsills or work desk"
                                    )
                                ),
                                funFact = if (isAr) "الألومنيوم يمكن صهره وإعادة تشكيله لعدد لا نهائي من المرات دون أن يفقد قوته!" else "Aluminum is infinitely recyclable without losing any structural properties!",
                                pointsEarned = 50
                            )
                        } else if (isGlass) {
                            GeminiScanResult(
                                itemName = if (isAr) "عبوة زجاجية لمنتج غذائي ($barcode)" else "Glass Jar Product Packaging ($barcode)",
                                itemEmoji = "🍶",
                                materialType = if (isAr) "زجاج" else "Glass",
                                recyclable = true,
                                recyclabilityScore = 10,
                                environmentalImpact = if (isAr) "الزجاج لا يتحلل لكنه طبيعي وآمن وصديق للبيئة تماماً عند إعادة فرزه." else "Glass never decomposes but is completely safe and eco-friendly when sorted.",
                                co2SavedGrams = 65.0,
                                waterSavedLiters = 4.2,
                                energySavedKwh = 0.28,
                                decompositionYears = if (isAr) "غير قابلة للتحلل" else "Never Decomposes",
                                recyclingMethods = listOf(
                                    RecyclingMethod(
                                        method = if (isAr) "الكسر والصهر والقولبة" else "Crushing, Melting & Molding",
                                        difficulty = if (isAr) "صعب" else "Difficult",
                                        steps = listOf(if (isAr) "غسل العبوة وإزالة الأغطية والملصقات" else "Wash glass clean and remove metal lid", if (isAr) "فرزها في حاوية الزجاج الخضراء" else "Deposit in the green glass bin"),
                                        result = if (isAr) "منتجات زجاجية جديدة بجودة فائقة" else "Premium new bottles and jars",
                                        wowFact = if (isAr) "الزجاج مادة فريدة تدور بنسبة ١٠٠٪ دون أي فاقد في الجودة أو النقاء!" else "Glass is 100% recyclable with zero quality loss or material decay!"
                                    )
                                ),
                                creativeReuse = listOf(
                                    CreativeReuse(
                                        idea = if (isAr) "مرطبان بهارات منظم وحافظ للأطعمة" else "Eco-Spice Storage Jar",
                                        difficulty = if (isAr) "سهل" else "Easy",
                                        materialsNeeded = listOf(if (isAr) "غسيل جيد وملصق باسم البهار" else "Clean jar, custom label marker"),
                                        timeMinutes = 5,
                                        steps = listOf(if (isAr) {
                                            "تنظيف وتجفيف المرطبان بالكامل"
                                        } else {
                                            "Sterilize and dry the glass jar completely"
                                        }, if (isAr) {
                                            "ملؤه بالبهارات (كمون، قرفة) ولصق الاسم عليه"
                                        } else {
                                            "Fill with spices and label beautifully"
                                        }),
                                        benefit = if (isAr) "توفير المال ومنع رطوبة الأطعمة والحفاظ على نكهتها الطازجة" else "Keeps spices clean, dry, and fresh with zero investment"
                                    )
                                ),
                                funFact = if (isAr) "الزجاج يمنع نفاذ الهواء تماماً فيحمي البهارات من التلف لفترات مضاعفة!" else "Glass is completely impermeable, extending spice shelf-life and freshness!",
                                pointsEarned = 45
                            )
                        } else {
                            // Default to Plastic Bottle packaging
                            GeminiScanResult(
                                itemName = if (isAr) "عبوة بلاستيكية لمنتج استهلاككي ($barcode)" else "Plastic Bottle Product Packaging ($barcode)",
                                itemEmoji = "🧴",
                                materialType = if (isAr) "بلاستيك" else "Plastic",
                                recyclable = true,
                                recyclabilityScore = 8,
                                environmentalImpact = if (isAr) "البلاستيك يلوث البحار والتربة ويأخذ مئات السنوات ليتحلل." else "Plastic litters marine environments and requires hundreds of years to break down.",
                                co2SavedGrams = 28.0,
                                waterSavedLiters = 2.0,
                                energySavedKwh = 0.10,
                                decompositionYears = if (isAr) "٤٥٠ سنة" else "450 Years",
                                recyclingMethods = listOf(
                                    RecyclingMethod(
                                        method = if (isAr) "الفرز والضغط للشحن" else "Sorting & Vertical Crushing",
                                        difficulty = if (isAr) "سهل" else "Easy",
                                        steps = listOf(if (isAr) "غسل العبوة جيدا بالماء" else "Rinse the empty plastic bottle", if (isAr) "فرزها في حاوية البلاستيك" else "Deposit in the orange plastic bin"),
                                        result = if (isAr) "بلاستيك PET/HDPE نقي معاد تدويره" else "Clean recycled polymer flakes for manufacturing",
                                        wowFact = if (isAr) "إعادة تدوير البلاستيك توفر طاقة تعادل ٨٠٪ مقارنة بتصنيعه من النفط الخام!" else "Recycling plastic saves up to 80% of the energy compared to raw petroleum extraction!"
                                    )
                                ),
                                creativeReuse = listOf(
                                    CreativeReuse(
                                        idea = if (isAr) "أصيص زراعة منزلي معلق" else "Inverted Hanging Planter",
                                        difficulty = if (isAr) "سهل" else "Easy",
                                        materialsNeeded = listOf(if (isAr) "مقص متين وحبل" else "Scissors & heavy twine"),
                                        timeMinutes = 15,
                                        steps = listOf(if (isAr) "قص قاع الزجاجة البلاستيكية الفارغة" else "Cut the lower base of the plastic bottle", if (isAr) "عمل ثقب جانبي وتمرير حبل للتعليق" else "Puncture side holes and slide twine through"),
                                        benefit = if (isAr) "أصيص معلق أنيق للحدائق المنزلية يثري ديكور شرفتك" else "Charming hanging green pot to elevate home gardens"
                                    )
                                ),
                                funFact = if (isAr) "البلاستيك مادة خفيفة الوزن للغاية لكنها قوية وممتازة للتطبيقات الإبداعية اليدوية!" else "Plastic is incredibly lightweight yet robust, making it excellent for DIY garden solutions!",
                                pointsEarned = 40
                            )
                        }
                    }
                }
                
                _activeScanResult.value = result
                // Save scan to history
                repository.addScanResult(result)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error scanning barcode: ${e.message}", e)
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun clearScanResult() {
        _activeScanResult.value = null
    }

    fun fetchAlternativeIdea() {
        val currentResult = _activeScanResult.value ?: return
        viewModelScope.launch {
            _isGeneratingAlternativeIdea.value = true
            try {
                val existingIdeas = currentResult.creativeReuse.map { it.idea }
                val lang = currentResult.creativeReuse.firstOrNull()?.let { 
                    if (it.idea.any { char -> char in '\u0600'..'\u06FF' }) "ar" else "en"
                } ?: "ar"
                
                val alternative = geminiService.getAlternativeCreativeIdea(
                    itemName = currentResult.itemName,
                    existingIdeas = existingIdeas,
                    lang = lang
                )
                
                if (alternative != null) {
                    val updatedList = currentResult.creativeReuse + alternative
                    _activeScanResult.value = currentResult.copy(creativeReuse = updatedList)
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to fetch alternative idea: ${e.message}", e)
            } finally {
                _isGeneratingAlternativeIdea.value = false
            }
        }
    }

    fun queryVoiceAssistant(query: String) {
        viewModelScope.launch {
            _isVoiceAssistantLoading.value = true
            _voiceAssistantResponse.value = null
            try {
                val advice = geminiService.getVoiceAssistantAdvice(query, _currentLanguage.value)
                _voiceAssistantResponse.value = advice
            } catch (e: Exception) {
                _voiceAssistantResponse.value = if (_currentLanguage.value == "ar") {
                    "حدث خطأ أثناء الاتصال بمساعد دَوِّر الصوتي. يرجى المحاولة مرة أخرى! 🌿"
                } else {
                    "An error occurred connecting to Dawwer Voice Assistant. Please try again! 🌿"
                }
            } finally {
                _isVoiceAssistantLoading.value = false
            }
        }
    }

    fun clearVoiceAssistantResponse() {
        _voiceAssistantResponse.value = null
    }

    fun completeChallengeDirectly(challengeId: String) {
        viewModelScope.launch {
            repository.completeChallengeDirectly(challengeId)
        }
    }

    fun clearScanHistory() {
        viewModelScope.launch {
            repository.clearAllHistory()
            // Reset impact scores to defaults
            val profile = userProfile.value ?: InitialData.defaultProfile
            repository.saveUserProfile(profile.copy(
                points = 0,
                level = 1,
                co2SavedGrams = 0.0,
                waterSavedLiters = 0.0,
                energySavedKwh = 0.0,
                treesPlanted = 0.0
            ))
        }
    }

    // Dynamic translation provider helper
    fun translate(key: String): String {
        val lang = _currentLanguage.value
        return translations[lang]?.get(key) ?: translations["ar"]?.get(key) ?: key
    }

    companion object {
        // Simple Localization Dictionaries
        private val translations = mapOf(
            "ar" to mapOf(
                "app_title" to "دَوِّر",
                "home" to "الرئيسية",
                "scanner" to "الماسح",
                "challenges" to "التحديات",
                "rewards" to "المكافآت",
                "profile" to "لوحتي",
                "points" to "نقطة",
                "level" to "المستوى",
                "welcome" to "مساء الخير،",
                "total_scans" to "مسح إجمالي",
                "completed_challenges" to "تحدي أُنجز",
                "streak_days" to "يوم متتالي",
                "impact" to "أثرك البيئي",
                "carbon_saved" to "كيلو CO2 وُفِّر",
                "water_saved" to "لتر مياه محفوظة",
                "energy_saved" to "كيلوواط طاقة موفرة",
                "trees_planted" to "معادل زراعة أشجار",
                "recent_activity" to "النشاط الأخير",
                "empty_history" to "لم تقم بمسح أي مخلفات بعد! ابدأ بمسح زجاجة أو كرتونة الآن! 🧴",
                "quick_actions" to "إجراءات سريعة",
                "scan_now" to "ابدأ مسح النفايات",
                "active_challenges" to "تحدياتك الحالية",
                "leaderboard" to "متصدرو هذا الشهر",
                "badges" to "الأوسمة والإنجازات",
                "details" to "التفاصيل",
                "scanning" to "جاري فحص المادة بالذكاء الاصطناعي...",
                "settings_title" to "الملف الشخصي والإعدادات",
                "name" to "الاسم",
                "email" to "البريد الإلكتروني",
                "phone" to "رقم الهاتف",
                "country" to "الدولة والمنطقة",
                "edit_profile" to "تعديل الملف",
                "general_settings" to "الإعدادات العامة للبرنامج:",
                "theme_and_colors" to "المظهر والألوان",
                "language" to "لغة التطبيق والمنطقة (Language)",
                "privacy_security" to "الخصوصية والأمان",
                "help_center" to "مركز المساعدة والأسئلة الشائعة",
                "about_app" to "حول تطبيق دَوِّر",
                "danger_zone" to "منطقة الخطر (إدارة البيانات)",
                "danger_zone_desc" to "مسح وحذف سجل النفايات وإعادة تهيئة العدادات بالكامل من الصفر لحالة المصنع.",
                "reset_db" to "إعادة تهيئة قاعدة البيانات محلياً",
                "logout" to "تسجيل الخروج من الحساب",
                "save" to "حفظ",
                "cancel" to "إلغاء",
                "dark" to "داكن 🌙",
                "light" to "مضيء ☀️",
                "auto" to "تلقائي 💻",
                "privacy_desc1" to "• كافة عمليات المسح والبيانات تحفظ محلياً على جهازك لخصوصية كاملة.",
                "privacy_desc2" to "• استخدام مفاتيح الذكاء الاصطناعي يتم بصيغة مشفرة وآمنة تماماً.",
                "faq_q1" to "كيف أقوم بربح النقاط وهدايا التدوير؟",
                "faq_a1" to "كلما قمت بمسح مخلف ضوئي بالذكاء الاصطناعي لتصنيفه، أو قمت بزيارة أحد حاويات جامعة القاهرة الذكية وتأكيد إيداعك، يتم رصد نقاط لرفع ترتيبك وفتح البطولات والأوسمة المغلقة!",
                "faq_q2" to "هل يعمل التطبيق بدون إنترنت؟",
                "faq_a2" to "نعم، يتميز تطبيق دَوِّر بنظام فرز وتصنيف ذكي محلي بالكامل يدعم تصنيف المواد الأساسية بكفاءة عالية بدون الحاجة لأي اتصال بالشبكة!",
                "about_desc1" to "الإصدار: ١.٠.٠ (بيتا)",
                "about_desc2" to "تم التطوير بكل فخر لدعم البيئة والاستدامة الخضراء في العالم العربي بالكامل.",
                "campus_bins_title" to "شبكة حاويات جامعة القاهرة الذكية (IoT Feed):",
                "nfc_guide" to "دليل اتصال NFC",
                "iot_active" to "🟢 متاح للاستلام",
                "iot_full" to "⚠️ ممتلئ تماماً",
                "fill_level" to "مؤشر نسبة الامتلاء الحالية:",
                "total_recycled" to "📊 الإجمالي المفرز: ",
                "power_source" to "⚡ مصدر الطاقة: شمسية (Solar)",
                "simulate_deposit" to "محاكاة إيداع مخلف وتأكيد الفرز 📥",
                "nfc_dialog_title" to "تقنية الاتصال الذكي (IoT) 📶",
                "nfc_dialog_desc" to "تطبيق دَوِّر يدعم بروتوكولات NFC وBluetooth BLE للربط الفوري مع صناديق الفرز الذكية بجامعة القاهرة. ما عليك سوى تقريب هاتفك من شريحة الاستشعار بجسم الحاوية لتفتح البوابة تلقائياً ويتم تسجيل فرزك وحساب وزن مكافأتك الفعلي بالجرام للتأكد من فرزها الواقعي والفعلي!",
                "understand" to "فهمت 👍",
                "iot_connect" to "اتصال IoT: ",
                "choose_deposit_material" to "أنت الآن أمام الحاوية الذكية. اختر نوع المادة التي ترغب في إيداعها حالياً بفتحة الفرز:",
                "connecting_nfc" to "جاري الاتصال الآمن بالحاوية الذكية عبر NFC...",
                "hold_phone_near" to "يرجى تقريب الهاتف من ملصق البلوتوث الخاص بالصندوق.",
                "lid_opened" to "🔓 تم فتح البوابة تلقائياً!",
                "weighing_sorting" to "جاري وزن المادة وفرزها ضوئياً داخل الصندوق الذكي...",
                "sensors_active" to "مستشعرات الضغط والوزن نشطة حالياً (IoT Sensors active).",
                "deposit_success_title" to "🎉 تم الإيداع والفرز بنجاح!",
                "deposit_success_desc" to "تم تحديث مستشعرات الحاوية وتعديل مؤشر الامتلاء والوزن.",
                "reward_text" to "المكافأة: +XP ونقاط إضافية لتوفير CO2 والمياه!",
                "process_done" to "تمت العملية 👍",
                "current_title_prefix" to "اللقب الحالي: ",
                "connecting_campus" to "جاري الاتصال بشبكة حاويات الجامعة الرقمية...",
                "plastic" to "بلاستيك 🧴",
                "plastic_sub" to "بلاستيك PET (عبوات مياه/عصير)",
                "aluminum" to "ألومنيوم 🥤",
                "aluminum_sub" to "عبوات معدنية ومشروبات غازية",
                "cardboard" to "ورق كرتون 📄",
                "cardboard_sub" to "كتب قديمة، كراسات، ورق جاف",
                "electronics" to "إلكترونيات 💻",
                "electronics_sub" to "أسلاك تالفة، هواتف قديمة، بطاريات",
                "challenges_rewards_title" to "التحديات والمكافآت البيئية",
                "daily_tab" to "يومية ⚡",
                "weekly_tab" to "أسبوعية 📅",
                "event_tab" to "الرمضان الأخضر 🌙",
                "rewards_tab" to "المكافآت 💎",
                "badges_tab" to "الأوسمة 🏅",
                "unlocked_badges_title" to "الأوسمة والبطولات المكتسبة:",
                "challenges_available_daily" to "تحديات اليوم المتاحة لتوفير الموارد:",
                "challenges_available_weekly" to "تحديات هذا الأسبوع التنافسية:",
                "challenges_available_event" to "الحدث النشط: رمضان الأخضر الموفر 🌙",
                "challenges_empty" to "لقد أنجزت كافة تحديات هذا القسم لليوم! واصل مسح النفايات لزيادة XP الخاص بك! 🎉",
                "simulate_completion" to "محاكاة الإنجاز",
                "completed_badge" to "مكتملة بنجاح ✅",
                "leaderboard_title" to "🏆 المتصدرون في حماية البيئة لشهر يونيو",
                "user_rank_label" to "ترتيبك الحالي في منطقتك:",
                "user_rank_value" to "ترتيب ٤٧ (أفضل ١٥٪)",
                "work_hard_up" to "اعمل أكتر وارتقي! 🌱",
                "saved_carbon_title" to "🌡️ انبعاثات الكربون الموفرة:",
                "saved_carbon_sub" to "تجنب انبعاث الغازات الدفيئة المسببة للاحتباس الحراري.",
                "saved_water_title" to "🌊 كمية المياه المحفوظة:",
                "saved_water_sub" to "منع إهدار المياه العذبة أثناء عمليات التصنيع الأولية.",
                "saved_energy_title" to "⚡ الطاقة والكهرباء الموفرة:",
                "saved_energy_sub" to "ما يعادل تشغيل مصابيح منزلك لعدة أشهر كاملة.",
                "saved_trees_title" to "🌳 معادل زراعة الأشجار الفتية:",
                "saved_trees_sub" to "الأثر الكربوني الممتص كأنك زرعت غابة صغيرة جديدة!",
                "ai_report_title" to "التقرير البيئي الأسبوعي للذكاء الاصطناعي",
                "ai_report_empty" to "أهلاً بك في دَوِّر! تشير بياناتك الأولى أنك جاهز للبدء. هدفنا المقترح للأسبوع القادم هو إجراء أول مسح لزجاجة بلاستيك أو عبوة ألومنيوم للتخلص منها في أقرب مركز بالدقي لتوفير أول جرامات من الكربون! 🌱",
                "ai_report_few" to "بداية رائعة! قمت بفرز ومسح %d مخلفات هذا الأسبوع. واصل جمع الكرتون الجاف لتنقذ أشجار الغابات العذراء، وننصحك بزيارة نقطة تجميع الدقي غداً لربح نقاط إضافية! 🛡️",
                "ai_report_many" to "أداء استثنائي بحق! أنت الآن في صدارة الـ ١٥٪ من حماة البيئة في منطقتك. لقد وفرت ما يعادل طاقة تشغيل تلفازك لـ ٥٠ ساعة متواصلة بفعل فرز المعادن والبلاستيك. استمر في تحدي الرمضان الأخضر وحافظ على شعلة Streak نشطة! 🔥",
                
                // Extra Keys
                "you" to "أنت",
                "level_name_1" to "بذرة 🌱",
                "level_name_2" to "شتلة 🌿",
                "level_name_3" to "شجيرة 🌳",
                "level_name_4" to "غابة صغيرة 🌲",
                "level_name_5" to "حارس البيئة 🛡️",
                "level_name_6" to "بطل الأرض 🌍",
                "level_name_fallback" to "حامي الأرض 🌍",
                "challenge_c1_title" to "امسح ٣ مخلفات اليوم",
                "challenge_c1_desc" to "استخدم الماسح الضوئي الذكي لتحديد ٣ مخلفات مختلفة وإعادة تدويرها.",
                "challenge_c1_time" to "٨ ساعات متبقية",
                "challenge_c2_title" to "إيداع بالحاوية الذكية",
                "challenge_c2_desc" to "قم بزيارة أحد الصناديق الذكية للحرم الجامعي وسجل إيداعاً لتأكيد فرز المخلفات.",
                "challenge_c2_time" to "١٢ ساعة متبقية",
                "challenge_c3_title" to "أسبوع خالي من البلاستيك",
                "challenge_c3_desc" to "تجنب تماماً استخدام الأكياس والزجاجات البلاستيكية ذات الاستخدام الواحد لمده أسبوع.",
                "challenge_c3_time" to "٣ أيام متبقية",
                "challenge_c4_title" to "جمع ١ كيلو كرتون وورق",
                "challenge_c4_desc" to "قم بتجميع أوراق الكتب القديمة والكرتون وتسليمها لأقرب مركز تدوير.",
                "challenge_c4_time" to "٤ أيام متبقية",
                "challenge_c5_title" to "الرمضان الأخضر 🌙",
                "challenge_c5_desc" to "تحدي خاص بتقليل مخلفات وجبات الإفطار والسحور وإعادة تدوير علب الأغذية.",
                "challenge_c5_time" to "١٥ يوم متبقي",
                "badge_first_scan_name" to "أول خطوة",
                "badge_first_scan_desc" to "قمت بمسح أول مخلف لك بنجاح وبدأت رحلتك البيئية.",
                "badge_plastic_hero_name" to "عدو البلاستيك",
                "badge_plastic_hero_desc" to "قمت بإعادة تدوير ١٠ مخلفات بلاستيكية لحماية البحار والمحيطات.",
                "badge_streak_7_name" to "أسبوع ذهبي",
                "badge_streak_7_desc" to "حافظت على نشاطك اليومي لـ ٧ أيام متتالية دون انقطاع.",
                "badge_map_explorer_name" to "بطل حاويات الحرم",
                "badge_map_explorer_desc" to "قمت بفرز وإيداع مخلفاتك بنجاح في حاويات فرز جامعة القاهرة الذكية.",
                "badge_eco_legend_name" to "أسطورة البيئة",
                "badge_eco_legend_desc" to "وصلت للمستوى ٥ (حارس البيئة) في الحفاظ على الموارد.",
                "badge_social_hero_name" to "السفير الأخضر",
                "badge_social_hero_desc" to "قمت بمشاركة إنجازاتك ودعوة أصدقائك للمشاركة في دَوِّر.",
                "loc_loc1_name" to "صندوق كلية الهندسة الذكي (IoT)",
                "loc_loc1_address" to "مبنى كلية الهندسة (المبنى الرئيسي)، جامعة القاهرة",
                "loc_loc1_desc" to "محطة فرز ذكية متكاملة تعمل بالطاقة الشمسية. تحتوي على حساسات امتلاء فائقة الدقة متصلة بشبكة الجامعة (IoT) لطلب تفريغ تلقائي للورق والبلاستيك والمعادن.",
                "loc_loc2_name" to "صندوق المكتبة المركزية لفرز البلاستيك",
                "loc_loc2_address" to "فناء المكتبة المركزية الجديدة، جامعة القاهرة",
                "loc_loc2_desc" to "نقطة تجميع ذكية مزودة بآلية كبس تلقائية لزجاجات البلاستيك (PET) لتقليل حجم المخلفات بنسبة ٨٠٪ وزيادة كفاءة النقل البيئي.",
                "loc_loc3_name" to "حاوية فرز الورق والكتب - كلية الآداب",
                "loc_loc3_address" to "بجوار مبنى كلية الآداب، جامعة القاهرة",
                "loc_loc3_desc" to "مخصصة لجمع الكتب المستعملة، الأوراق والمذكرات الدراسية. يتم فرزها لإعادة تصنيع كراسات جديدة للطلاب أو التبرع بالكتب بحالة جيدة.",
                "loc_loc4_name" to "مجمع تدوير الإلكترونيات - المدينة الجامعية",
                "loc_loc4_address" to "المدينة الجامعية للطلاب، بين السرايات، الجيزة",
                "loc_loc4_desc" to "حاوية ذكية مخصصة لتجميع الأسلاك التالفة، البطاريات، والأجهزة المحمولة القديمة بأمان لمنع تسرب السموم الإلكترونية وتلويث التربة.",
                "loc_loc5_name" to "صندوق فرز الزجاج والكرتون - كلية العلوم",
                "loc_loc5_address" to "بجوار مبنى قسم الكيمياء، كلية العلوم، جامعة القاهرة",
                "loc_loc5_desc" to "مخصصة لجمع الكرتون المقوى وعبوات الزجاج الفارغة للفرز المعملي الآمن وإعادة استخدامها في التصنيع المحلي الصديق للبيئة.",
                "scan_result_title" to "نتيجة الفحص الذكي",
                "sustainability_index" to "مؤشر الاستدامة:",
                "immediate_environmental_impact" to "الأثر البيئي الفوري (في حال التدوير):",
                "decomposition_time" to "زمن التحلل",
                "co2_saving" to "توفير CO2",
                "water_saving_label" to "مياه محفوظة",
                "suggested_recycling_methods" to "طرق التدوير والفرز المقترحة:",
                "creative_reuse_diy_ideas" to "أفكار إعادة الاستخدام الإبداعي (DIY):",
                "congrats_earned_reward" to "🎉 تهانينا! لقد ربحت مكافأتك البيئية",
                "points_registered_success" to "تم تسجيل نقاطك بنجاح لرفع مستواك وفتح أوسمة التحديات الجديدة.",
                "instant_deposit_bin" to "إيداع فوري بالحاوية 📥",
                "share_impact" to "شارك الأثر",
                "scientific_steps" to "الخطوات العلمية:",
                "target_product" to "المنتج المستهدف:",
                "amazing_info" to "معلومة مدهشة:",
                "minutes_unit" to "دقيقة",
                "additional_materials_needed" to "المواد المطلوبة:",
                "scan_waste_ai" to "امسح مخلفك بالذكاء الاصطناعي",
                "camera_viewfinder_instruction" to "وجه الكاميرا نحو زجاجة، علبة، أو كرتونة",
                "choose_sample_for_scan" to "اختر عينة تجريبية للمسح السريع:",
                "from_gallery" to "من الألبوم",
                "auto_flash" to "وميض تلقائي",
                "instant_smart_deposit" to "إيداع فوري بالحاوية الذكية 📶",
                "lid_opened_text" to "🔓 تم فتح البوابة تلقائياً!",
                "ble_hatch_opened_desc" to "تم فتح غطاء الحاوية بنجاح عبر البلوتوث وتفريغ المادة بأمان.",
                "bin_sensors_updated_text" to "تم تحديث مستشعرات الحاوية وإضافة النقاط.",
                "points_added_success_text" to "نقطة لرفع ترتيبك وتوفير CO2 والمياه بنجاح!",
                "connecting_to_bin" to "جاري الاتصال بـ",
                "securing_ble_connection_desc" to "توصيل آمن BLE • تفريغ الشحنة ووزن المادة...",
                "choose_closest_bin_deposit" to "اختر الحاوية الذكية بجامعة القاهرة القريبة منك حالياً لإيداع المادة وبدء فرزها التلقائي:",
                "confirm_connection_deposit" to "تأكيد الاتصال والإيداع 📥",
                "close_back_home" to "إغلاق والعودة للرئيسية 👍",
                "item_deposited_reward_logged" to "🎉 تم إلقاء المادة وتسجيل مكافأتك!",
                "sample_plastic" to "زجاجة بلاستيك",
                "sample_cardboard" to "كرتون جاف",
                "sample_aluminum" to "علبة ألومنيوم",
                "sample_food" to "بقايا طعام",
                "material_plastic" to "بلاستيك PET",
                "material_cardboard" to "كرتون جاف",
                "material_food" to "بقايا طعام",
                "material_metal" to "ألومنيوم",
                "auth_subtitle" to "منصة التدوير البيئي الفاخرة",
                "auth_subtitle_signup" to "انضم لحماة البيئة واستمتع بالمكافآت",
                "auth_login_title" to "تسجيل الدخول",
                "auth_signup_title" to "إنشاء حساب بيئي جديد",
                "auth_full_name" to "الاسم الكامل",
                "auth_email" to "البريد الإلكتروني",
                "auth_password" to "كلمة المرور",
                "auth_error_fields" to "برجاء ملء كافة الحقول بشكل صحيح!",
                "auth_error_email" to "البريد الإلكتروني الذي أدخلته غير صالح!",
                "auth_btn_login" to "دخول آمن",
                "auth_btn_signup" to "تسجيل حسابي الجديد",
                "auth_google" to "جوجل",
                "auth_facebook" to "فيسبوك",
                "auth_no_account" to "ليس لديك حساب؟ ",
                "auth_register_now" to "اشترك الآن مجاناً",
                "auth_has_account" to "لديك حساب بالفعل؟ ",
                "auth_login_here" to "سجل دخولك هنا",
                "reminders_alerts" to "التذكيرات والتنبيهات 🔔",
                "daily_reminder" to "تذكير التدوير اليومي",
                "daily_reminder_desc" to "ذكرني يومياً بفرز وتدوير النفايات المنزلية والجامعية وحماية الكوكب 🌱",
                "reminder_time" to "وقت التذكير اليومي",
                "peak_hour_reminder" to "تذكير ساعات الذروة الذكي 🎙️",
                "peak_hour_reminder_desc" to "استخدم مساعد الذكاء الاصطناعي الصوتي لتنبيهك صوتياً وتوجيهك لإعادة التدوير خلال ساعات الذروة (12:00 م و 6:00 م) ⏰",
                "levelup_alerts" to "تنبيهات الترقيات والإنجازات",
                "levelup_alerts_desc" to "أرسل لي تنبيهاً فورياً عند الحصول على نقاط أو الوصول لمستوى جديد 🏆",
                "test_notification" to "إرسال إشعار تجريبي الآن 🔔",
                "notification_history" to "سجل التنبيهات الأخيرة المستلمة",
                "clear_history" to "مسح السجل 🗑️",
                "no_history" to "لا يوجد تنبيهات مسجلة بعد."
            ),
            "en" to mapOf(
                "auth_subtitle" to "Luxury Eco-Recycling Platform",
                "auth_subtitle_signup" to "Join eco-protectors and earn rewards",
                "auth_login_title" to "Sign In",
                "auth_signup_title" to "Create New Eco Account",
                "auth_full_name" to "Full Name",
                "auth_email" to "Email Address",
                "auth_password" to "Password",
                "auth_error_fields" to "Please fill in all fields correctly!",
                "auth_error_email" to "The email address you entered is invalid!",
                "auth_btn_login" to "Secure Login",
                "auth_btn_signup" to "Register My New Account",
                "auth_google" to "Google",
                "auth_facebook" to "Facebook",
                "auth_no_account" to "Don't have an account? ",
                "auth_register_now" to "Subscribe Now Free",
                "auth_has_account" to "Already have an account? ",
                "auth_login_here" to "Log in here",
                "app_title" to "Dawwer",
                "home" to "Home",
                "scanner" to "Scanner",
                "challenges" to "Challenges",
                "rewards" to "Rewards",
                "profile" to "Dashboard",
                "points" to "Points",
                "level" to "Level",
                "welcome" to "Welcome back,",
                "total_scans" to "Total Scans",
                "completed_challenges" to "Completed",
                "streak_days" to "Streak Days",
                "impact" to "Environmental Impact",
                "carbon_saved" to "Kg CO2 Saved",
                "water_saved" to "Liters Water Saved",
                "energy_saved" to "kWh Energy Saved",
                "trees_planted" to "Trees Equivalent",
                "recent_activity" to "Recent Scans",
                "empty_history" to "No scans registered yet. Try scanning a bottle or cardboard! 🧴",
                "quick_actions" to "Quick Actions",
                "scan_now" to "Scan Waste Item",
                "active_challenges" to "Active Challenges",
                "leaderboard" to "Leaderboard",
                "badges" to "Badges & Achievements",
                "details" to "Details",
                "scanning" to "AI is analyzing material composition...",
                "settings_title" to "Profile & Settings",
                "name" to "Name",
                "email" to "Email",
                "phone" to "Phone Number",
                "country" to "Country / Region",
                "edit_profile" to "Edit Profile",
                "general_settings" to "General Settings:",
                "theme_and_colors" to "Appearance & Colors",
                "language" to "App Language & Region",
                "privacy_security" to "Privacy & Security",
                "help_center" to "Help Center & FAQ",
                "about_app" to "About Dawwer App",
                "danger_zone" to "Danger Zone (Data Management)",
                "danger_zone_desc" to "Delete all waste history and reset all metrics back to factory defaults.",
                "reset_db" to "Reset Local Database",
                "logout" to "Log Out from Account",
                "save" to "Save",
                "cancel" to "Cancel",
                "dark" to "Dark 🌙",
                "light" to "Light ☀️",
                "auto" to "Auto 💻",
                "privacy_desc1" to "• All scans and personal data are stored locally on your device for absolute privacy.",
                "privacy_desc2" to "• Artificial Intelligence integration keys are encrypted and fully secured.",
                "faq_q1" to "How do I earn points and recycling rewards?",
                "faq_a1" to "Whenever you scan waste using AI or deposit items into Cairo University campus smart bins, you earn XP points to increase your level, rank up, and unlock achievements!",
                "faq_q2" to "Does the app work offline?",
                "faq_a2" to "Yes! Dawwer features a fully offline material classifier fallback system that runs directly on your device without any network connection required!",
                "about_desc1" to "Version: 1.0.0 (Beta)",
                "about_desc2" to "Proudly developed to support green environmental sustainability and eco-friendliness in Cairo University and beyond.",
                "campus_bins_title" to "Cairo University Smart Bins Network (IoT Feed):",
                "nfc_guide" to "NFC Connection Guide",
                "iot_active" to "🟢 Available for Dropoff",
                "iot_full" to "⚠️ Full - Maintenance",
                "fill_level" to "Current Fill Index:",
                "total_recycled" to "📊 Total Collected: ",
                "power_source" to "⚡ Power Source: Solar Powered",
                "simulate_deposit" to "Simulate Smart Deposit & Sort 📥",
                "nfc_dialog_title" to "IoT Connection Technology 📶",
                "nfc_dialog_desc" to "Dawwer supports NFC and Bluetooth BLE protocols for instant connectivity with smart recycling bins. Just bring your device close to the bin sensor to open the hatch automatically and verify your real sorting weight for genuine rewards!",
                "understand" to "Got it 👍",
                "iot_connect" to "IoT Link: ",
                "choose_deposit_material" to "You are in front of the smart bin. Select the material type you want to deposit into the automatic sorting slot:",
                "connecting_nfc" to "Establishing secure connection to the smart bin via NFC...",
                "hold_phone_near" to "Please keep your device close to the Bluetooth BLE node on the bin.",
                "lid_opened" to "🔓 Hatch opened automatically!",
                "weighing_sorting" to "Weighing and optical sorting in progress inside the smart bin...",
                "sensors_active" to "IoT telemetry and weight sensors active.",
                "deposit_success_title" to "🎉 Deposit & Sorting Successful!",
                "deposit_success_desc" to "Bin sensors updated. Telemetry database adjusted and fill levels computed.",
                "reward_text" to "Reward: +XP and Carbon Credits awarded!",
                "process_done" to "Finished 👍",
                "current_title_prefix" to "Current Title: ",
                "connecting_campus" to "Connecting to campus IoT bin grid...",
                "plastic" to "Plastic 🧴",
                "plastic_sub" to "PET Plastics (Bottles / Cups)",
                "aluminum" to "Aluminum 🥤",
                "aluminum_sub" to "Metal cans and soda containers",
                "cardboard" to "Paper / Cardboard 📄",
                "cardboard_sub" to "Old books, paper, and boxes",
                "electronics" to "E-Waste 💻",
                "electronics_sub" to "Damaged wires, old phones, batteries",
                "challenges_rewards_title" to "Environmental Challenges & Rewards",
                "daily_tab" to "Daily ⚡",
                "weekly_tab" to "Weekly 📅",
                "event_tab" to "Green Ramadan 🌙",
                "rewards_tab" to "Rewards 💎",
                "badges_tab" to "Badges 🏅",
                "unlocked_badges_title" to "Unlocked Badges & Tournaments:",
                "challenges_available_daily" to "Available daily resource challenges:",
                "challenges_available_weekly" to "This week's competitive challenges:",
                "challenges_available_event" to "Active Event: Green Ramadan Eco 🌙",
                "challenges_empty" to "You have completed all challenges in this section for today! Keep scanning waste to earn more XP! 🎉",
                "simulate_completion" to "Simulate Done",
                "completed_badge" to "Completed Successfully ✅",
                "leaderboard_title" to "🏆 June Eco Conservation Leaderboard",
                "user_rank_label" to "Your current local rank:",
                "user_rank_value" to "Rank 47 (Top 15%)",
                "work_hard_up" to "Keep going to rise! 🌱",
                "saved_carbon_title" to "🌡️ Saved CO2 Emissions:",
                "saved_carbon_sub" to "Preventing greenhouse gases that cause global warming.",
                "saved_water_title" to "🌊 Saved Water:",
                "saved_water_sub" to "Preventing fresh water waste during early manufacturing processes.",
                "saved_energy_title" to "⚡ Saved Energy & Electricity:",
                "saved_energy_sub" to "Equivalent to powering your home lamps for several full months.",
                "saved_trees_title" to "🌳 Young Trees Planting Equivalent:",
                "saved_trees_sub" to "The carbon absorbed is as if you planted a whole new mini-forest!",
                "ai_report_title" to "AI Environmental Weekly Report",
                "ai_report_empty" to "Welcome to Dawwer! Your initial metrics show you are ready to start. Our suggested goal for next week is scanning your first plastic bottle or aluminum can to save your first carbon grams! 🌱",
                "ai_report_few" to "Great start! You sorted and scanned %d waste items this week. Keep collecting dry cardboard to save wild forest trees, and we advise visiting the Dokki collection hub tomorrow to earn extra points! 🛡️",
                "ai_report_many" to "Exceptional performance! You are in the top 15% of eco defenders in your area. You saved energy equivalent to running a TV for 50 continuous hours. Continue the Green Ramadan challenge and keep your Streak flame hot! 🔥",
                
                // Extra Keys
                "you" to "You",
                "level_name_1" to "Seedling 🌱",
                "level_name_2" to "Sprout 🌿",
                "level_name_3" to "Sapling 🌳",
                "level_name_4" to "Mini Forest 🌲",
                "level_name_5" to "Eco Guardian 🛡️",
                "level_name_6" to "Earth Hero 🌍",
                "level_name_fallback" to "Earth Protector 🌍",
                "challenge_c1_title" to "Scan 3 waste items today",
                "challenge_c1_desc" to "Use the smart camera to identify and sort 3 different waste items.",
                "challenge_c1_time" to "8 hours left",
                "challenge_c2_title" to "Smart bin dropoff",
                "challenge_c2_desc" to "Visit a smart bin on campus and register a secure deposit via BLE.",
                "challenge_c2_time" to "12 hours left",
                "challenge_c3_title" to "Plastic-free week",
                "challenge_c3_desc" to "Avoid single-use plastic bags and water bottles for 7 continuous days.",
                "challenge_c3_time" to "3 days left",
                "challenge_c4_title" to "Collect 1 kg paper/cardboard",
                "challenge_c4_desc" to "Collect old books and paper notes and deposit them for recycling.",
                "challenge_c4_time" to "4 days left",
                "challenge_c5_title" to "Green Ramadan 🌙",
                "challenge_c5_desc" to "Eco special: reduce food and packaging waste during Iftar and Suhoor.",
                "challenge_c5_time" to "15 days left",
                "badge_first_scan_name" to "First Step",
                "badge_first_scan_desc" to "Successfully scanned your first waste item and started your eco journey.",
                "badge_plastic_hero_name" to "Plastic Foe",
                "badge_plastic_hero_desc" to "Recycled 10 plastic items to protect oceans and marine life.",
                "badge_streak_7_name" to "Golden Week",
                "badge_streak_7_desc" to "Maintained daily active streak for 7 consecutive days.",
                "badge_map_explorer_name" to "Campus Champion",
                "badge_map_explorer_desc" to "Successfully deposited waste into Cairo University campus smart bins.",
                "badge_eco_legend_name" to "Eco Legend",
                "badge_eco_legend_desc" to "Reached Level 5 (Eco Guardian) in resource conservation.",
                "badge_social_hero_name" to "Green Envoy",
                "badge_social_hero_desc" to "Shared your eco achievements and invited friends to join Dawwer.",
                "loc_loc1_name" to "Engineering Faculty Smart Bin (IoT)",
                "loc_loc1_address" to "Engineering Faculty Main Building, Cairo University",
                "loc_loc1_desc" to "Solar-powered smart sorting station. Integrated with high-precision fullness telemetry to alert campus facility management.",
                "loc_loc2_name" to "Central Library Plastic Collection Node",
                "loc_loc2_address" to "New Central Library Courtyard, Cairo University",
                "loc_loc2_desc" to "Smart recovery unit with automated mechanical crushing for PET bottles to save 80% shipping volume.",
                "loc_loc3_name" to "Faculty of Arts Paper & Book Bin",
                "loc_loc3_address" to "Next to Faculty of Arts, Cairo University",
                "loc_loc3_desc" to "Designated bin for old books, exam drafts, and notebooks to recycle into new student notebooks.",
                "loc_loc4_name" to "Hostel E-Waste Center",
                "loc_loc4_address" to "University Student Hostels, Between the Streets, Giza",
                "loc_loc4_desc" to "A secure terminal to recycle charger wires, dead batteries, and old phones to prevent soil heavy metal pollution.",
                "loc_loc5_name" to "Faculty of Science Glass & Box Bin",
                "loc_loc5_address" to "Beside Chemistry Dept, Faculty of Science, Cairo University",
                "loc_loc5_desc" to "Designated for clean glass jars and cardboard packages to supply safe recycling local laboratories.",
                "scan_result_title" to "AI Scan Result",
                "sustainability_index" to "Sustainability index:",
                "immediate_environmental_impact" to "Immediate Environmental Impact (if recycled):",
                "decomposition_time" to "Decomposition time",
                "co2_saving" to "CO2 Saving",
                "water_saving_label" to "Saved Water",
                "suggested_recycling_methods" to "Suggested Recycling Methods:",
                "creative_reuse_diy_ideas" to "Creative Reuse & DIY Ideas:",
                "congrats_earned_reward" to "🎉 Congrats! You earned an eco reward",
                "points_registered_success" to "Your points were securely logged to raise your level and unlock new challenges.",
                "instant_deposit_bin" to "Instant Deposit 📥",
                "share_impact" to "Share Impact",
                "scientific_steps" to "Scientific Steps:",
                "target_product" to "Target Product:",
                "amazing_info" to "Amazing Fact:",
                "minutes_unit" to "min",
                "additional_materials_needed" to "Required Materials:",
                "scan_waste_ai" to "Scan Waste with AI",
                "camera_viewfinder_instruction" to "Point camera at bottles, cans, or boxes",
                "choose_sample_for_scan" to "Choose sample for quick scan:",
                "from_gallery" to "From Gallery",
                "auto_flash" to "Auto Flash",
                "instant_smart_deposit" to "Instant Smart Deposit 📶",
                "lid_opened_text" to "🔓 Lid opened automatically!",
                "ble_hatch_opened_desc" to "The bin lid opened successfully via BLE. Drop off the item safely.",
                "bin_sensors_updated_text" to "Telemetry sensors updated and rewards logged.",
                "points_added_success_text" to "XP points added to level up and protect resource index!",
                "connecting_to_bin" to "Connecting to",
                "securing_ble_connection_desc" to "Establishing BLE connection • Weighing material...",
                "choose_closest_bin_deposit" to "Select the nearest Cairo University smart IoT bin to deposit your scanned item:",
                "confirm_connection_deposit" to "Confirm Connection & Dropoff 📥",
                "close_back_home" to "Close & Return Home 👍",
                "item_deposited_reward_logged" to "🎉 Item deposited and reward logged!",
                "sample_plastic" to "Plastic Bottle",
                "sample_cardboard" to "Dry Cardboard",
                "sample_aluminum" to "Aluminum Can",
                "sample_food" to "Food Waste",
                "material_plastic" to "PET Plastic",
                "material_cardboard" to "Dry Cardboard",
                "material_food" to "Food Waste",
                "material_metal" to "Aluminum",
                "reminders_alerts" to "Reminders & Alerts 🔔",
                "daily_reminder" to "Daily Recycling Reminder",
                "daily_reminder_desc" to "Remind me daily to sort and recycle household and university waste 🌱",
                "reminder_time" to "Daily Reminder Time",
                "peak_hour_reminder" to "Smart Peak-Hour Reminder 🎙️",
                "peak_hour_reminder_desc" to "Use AI Voice Assistant to receive interactive recycling advice during high-waste peak hours (12:00 PM & 6:00 PM) ⏰",
                "levelup_alerts" to "Upgrade & Achievement Alerts",
                "levelup_alerts_desc" to "Send me an instant notification when I earn points or reach a new level 🏆",
                "test_notification" to "Send Test Notification Now 🔔",
                "notification_history" to "Recent Notifications History",
                "clear_history" to "Clear History 🗑️",
                "no_history" to "No recorded notifications yet."
            ),
            "es" to mapOf(
                "app_title" to "Dawwer",
                "home" to "Inicio",
                "scanner" to "Escáner",
                "challenges" to "Desafíos",
                "rewards" to "Premios",
                "profile" to "Perfil",
                "points" to "Puntos",
                "level" to "Nivel",
                "welcome" to "Bienvenido,",
                "total_scans" to "Escaneos totales",
                "completed_challenges" to "Logrado",
                "streak_days" to "Días seguidos",
                "impact" to "Impacto Ambiental",
                "carbon_saved" to "Kg CO2 Salvado",
                "water_saved" to "Litros Agua Salvada",
                "energy_saved" to "kWh Energía Salvada",
                "trees_planted" to "Árboles Equivalentes",
                "recent_activity" to "Escaneos Recientes",
                "empty_history" to "Sin escaneos todavía. ¡Prueba a escanear una botella! 🧴",
                "quick_actions" to "Acciones Rápidas",
                "scan_now" to "Escanear Residuo",
                "active_challenges" to "Desafíos Activos",
                "leaderboard" to "Clasificación",
                "badges" to "Insignias y Logros",
                "details" to "Detalles",
                "scanning" to "La IA está analizando los materiales...",
                "settings_title" to "Perfil y Ajustes",
                "name" to "Nombre",
                "email" to "Correo",
                "phone" to "Teléfono",
                "country" to "País / Región",
                "edit_profile" to "Editar Perfil",
                "general_settings" to "Ajustes Generales:",
                "theme_and_colors" to "Apariencia y Colores",
                "language" to "Idioma de la App",
                "privacy_security" to "Privacidad y Seguridad",
                "help_center" to "Ayuda y Preguntas",
                "about_app" to "Acerca de Dawwer",
                "danger_zone" to "Zona de Peligro",
                "danger_zone_desc" to "Borrar todo el historial de residuos y restablecer las métricas.",
                "reset_db" to "Restablecer Base de Datos",
                "logout" to "Cerrar Sesión",
                "save" to "Guardar",
                "cancel" to "Cancelar",
                "dark" to "Oscuro 🌙",
                "light" to "Claro ☀️",
                "auto" to "Auto 💻",
                "privacy_desc1" to "• Todos los escaneos se guardan localmente para total privacidad.",
                "privacy_desc2" to "• Las claves de Inteligencia Artificial están cifradas de forma segura.",
                "faq_q1" to "¿Cómo gano puntos y recompensas?",
                "faq_a1" to "¡Al escanear residuos con IA o depositarlos en contenedores inteligentes universitarios, ganas puntos XP!",
                "faq_q2" to "¿Funciona sin conexión?",
                "faq_a2" to "Sí, Dawwer tiene un clasificador inteligente local que funciona completamente sin red.",
                "about_desc1" to "Versión: 1.0.0 (Beta)",
                "about_desc2" to "Desarrollado con orgullo para apoyar la sostenibilidad ambiental y el reciclaje.",
                "campus_bins_title" to "Red de Contenedores de la Universidad de El Cairo (IoT):",
                "nfc_guide" to "Guía de Conexión NFC",
                "iot_active" to "🟢 Disponible para depósito",
                "iot_full" to "⚠️ Contenedor Lleno",
                "fill_level" to "Nivel de llenado actual:",
                "total_recycled" to "📊 Total Recolectado: ",
                "power_source" to "⚡ Energía: Solar",
                "simulate_deposit" to "Simular Depósito Inteligente 📥",
                "nfc_dialog_title" to "Tecnología de Conexión IoT 📶",
                "nfc_dialog_desc" to "Dawwer es compatible con NFC y Bluetooth para una conexión instantánea con los contenedores inteligentes universitarios.",
                "understand" to "Entendido 👍",
                "iot_connect" to "Conexión IoT: ",
                "choose_deposit_material" to "Selecciona el material que deseas depositar en la ranura:",
                "connecting_nfc" to "Estableciendo conexión segura con el contenedor vía NFC...",
                "hold_phone_near" to "Por favor, mantén tu dispositivo cerca del nodo Bluetooth del contenedor.",
                "lid_opened" to "🔓 ¡Tapa abierta automáticamente!",
                "weighing_sorting" to "Pesaje y clasificación óptica en curso...",
                "sensors_active" to "Sensores de telemetría y peso IoT activos.",
                "deposit_success_title" to "🎉 ¡Depósito Exitoso!",
                "deposit_success_desc" to "Sensores actualizados y base de datos recalculada.",
                "reward_text" to "¡Recompensa: Puntos XP y Créditos de Carbono ganados!",
                "process_done" to "Listo 👍",
                "current_title_prefix" to "Título Actual: ",
                "connecting_campus" to "Conectando con la red IoT...",
                "plastic" to "Plástico 🧴",
                "plastic_sub" to "Plásticos PET (Botellas)",
                "aluminum" to "Aluminio 🥤",
                "aluminum_sub" to "Latas de metal y refrescos",
                "cardboard" to "Papel / Cartón 📄",
                "cardboard_sub" to "Libros antiguos, folletos y cajas",
                "electronics" to "Residuos Electrónicos 💻",
                "electronics_sub" to "Cables dañados, móviles antiguos, baterías",
                "challenges_rewards_title" to "Desafíos y Recompensas Ecológicas",
                "daily_tab" to "Diario ⚡",
                "weekly_tab" to "Semanal 📅",
                "event_tab" to "Ramadán Verde 🌙",
                "rewards_tab" to "Premios 💎",
                "badges_tab" to "Insignias 🏅",
                "unlocked_badges_title" to "Insignias Desbloqueadas:",
                "challenges_available_daily" to "Desafíos diarios de recursos disponibles:",
                "challenges_available_weekly" to "Desafíos semanales competitivos:",
                "challenges_available_event" to "Evento Activo: Ramadán Ecológico Verde 🌙",
                "challenges_empty" to "¡Has completado todos los desafíos de esta sección por hoy! ¡Sigue escaneando residuos para ganar más XP! 🎉",
                "simulate_completion" to "Simular Hecho",
                "completed_badge" to "Completado con Éxito ✅",
                "leaderboard_title" to "🏆 Tabla de Líderes Ecológicos de Junio",
                "user_rank_label" to "Tu posición local actual:",
                "user_rank_value" to "Posición 47 (Top 15%)",
                "work_hard_up" to "¡Sigue adelante para subir! 🌱",
                "saved_carbon_title" to "🌡️ Emisiones de CO2 Evitadas:",
                "saved_carbon_sub" to "Prevención de gases de efecto invernadero.",
                "saved_water_title" to "🌊 Agua Ahorrada:",
                "saved_water_sub" to "Prevención del desperdicio de agua potable en la producción primaria.",
                "saved_energy_title" to "⚡ Energía Ahorrada:",
                "saved_energy_sub" to "Equivalente a alimentar las luces de tu hogar por meses enteros.",
                "saved_trees_title" to "🌳 Árboles Plantados Equivalentes:",
                "saved_trees_sub" to "El carbono absorbido equivale a plantar un pequeño y nuevo minibosque.",
                "ai_report_title" to "Informe Semanal Ecológico de IA",
                "ai_report_empty" to "¡Bienvenido a Dawwer! Tus métricas iniciales muestran que estás listo para comenzar. ¡Prueba a escanear tu primer residuo! 🌱",
                "ai_report_few" to "¡Buen comienzo! Clasificaste %d residuos esta semana. Sigue recolectando cartón seco para salvar los árboles y visita el punto Dokki mañana para ganar puntos. 🛡️",
                "ai_report_many" to "¡Rendimiento excepcional! Estás en el top 15% de defensores ecológicos. Ahorraste energía equivalente a ver televisión por 50 horas seguidas. ¡Sigue con el desafío del Ramadán Verde! 🔥",
                
                // Extra Keys
                "you" to "Tú",
                "level_name_1" to "Brote 🌱",
                "level_name_2" to "Plántula 🌿",
                "level_name_3" to "Arbolito 🌳",
                "level_name_4" to "Minibosque 🌲",
                "level_name_5" to "Guardián Eco 🛡️",
                "level_name_6" to "Héroe de la Tierra 🌍",
                "level_name_fallback" to "Protector de la Tierra 🌍",
                "challenge_c1_title" to "Escanea 3 residuos hoy",
                "challenge_c1_desc" to "Usa la cámara inteligente para identificar y clasificar 3 residuos diferentes.",
                "challenge_c1_time" to "8 horas restantes",
                "challenge_c2_title" to "Depósito en contenedor",
                "challenge_c2_desc" to "Visita un contenedor inteligente en el campus y deposita un residuo mediante BLE.",
                "challenge_c2_time" to "12 horas restantes",
                "challenge_c3_title" to "Semana libre de plásticos",
                "challenge_c3_desc" to "Evita botellas y bolsas de plástico de un solo uso durante 7 días continuos.",
                "challenge_c3_time" to "3 días restantes",
                "challenge_c4_title" to "Recolecta 1 kg de papel/cartón",
                "challenge_c4_desc" to "Reúne libros viejos y papel para entregarlos en un punto de reciclaje.",
                "challenge_c4_time" to "4 días restantes",
                "challenge_c5_title" to "Ramadán Verde 🌙",
                "challenge_c5_desc" to "Especial ecológico: reduce residuos de alimentos y envases en Iftar y Suhoor.",
                "challenge_c5_time" to "15 días restantes",
                "badge_first_scan_name" to "Primer Paso",
                "badge_first_scan_desc" to "Escaneaste tu primer residuo con éxito y comenzaste tu viaje ecológico.",
                "badge_plastic_hero_name" to "Enemigo del Plástico",
                "badge_plastic_hero_desc" to "Reciclaste 10 artículos plásticos para salvar océanos y vida marina.",
                "badge_streak_7_name" to "Semana Dorada",
                "badge_streak_7_desc" to "Mantuviste tu racha diaria activa durante 7 días consecutivos.",
                "badge_map_explorer_name" to "Campeón del Campus",
                "badge_map_explorer_desc" to "Depositaste tus residuos con éxito en contenedores inteligentes de la Universidad.",
                "badge_eco_legend_name" to "Leyenda Eco",
                "badge_eco_legend_desc" to "Alcanzaste el Nivel 5 (Guardián Eco) en conservación de recursos.",
                "badge_social_hero_name" to "Embajador Verde",
                "badge_social_hero_desc" to "Compartiste tus logros ecológicos e invitaste a tus amigos a unirse a Dawwer.",
                "loc_loc1_name" to "Contenedor Inteligente de Ingeniería (IoT)",
                "loc_loc1_address" to "Edificio Principal de Ingeniería, Universidad de El Cairo",
                "loc_loc1_desc" to "Estación inteligente solar con telemetría de nivel para optimizar la recolección.",
                "loc_loc2_name" to "Punto de Plásticos de la Biblioteca Central",
                "loc_loc2_address" to "Patio de la Biblioteca Central, Universidad de El Cairo",
                "loc_loc2_desc" to "Unidad con compactador de botellas PET que ahorra el 80% del volumen de transporte.",
                "loc_loc3_name" to "Contenedor de Papel de la Facultad de Letras",
                "loc_loc3_address" to "Bajo la Facultad de Letras, Universidad de El Cairo",
                "loc_loc3_desc" to "Para recolectar apuntes y libros viejos y transformarlos en cuadernos estudiantiles.",
                "loc_loc4_name" to "Centro de Residuos Electrónicos",
                "loc_loc4_address" to "Residencias Universitarias, Entre las Calles, Giza",
                "loc_loc4_desc" to "Para reciclar cargadores, baterías y móviles viejos, evitando verter metales pesados en el suelo.",
                "loc_loc5_name" to "Punto de Vidrio de la Facultad de Ciencias",
                "loc_loc5_address" to "Junto a Química, Facultad de Ciencias, Universidad de El Cairo",
                "loc_loc5_desc" to "Destinado a envases de vidrio limpios para abastecer laboratorios locales sostenibles.",
                "scan_result_title" to "Resultado de Escaneo",
                "sustainability_index" to "Índice de sostenibilidad:",
                "immediate_environmental_impact" to "Impacto ambiental inmediato (si se recicla):",
                "decomposition_time" to "Tiempo de descomposición",
                "co2_saving" to "Ahorro de CO2",
                "water_saving_label" to "Agua Ahorrada",
                "suggested_recycling_methods" to "Métodos de reciclaje sugeridos:",
                "creative_reuse_diy_ideas" to "Ideas creativas de reutilización (DIY):",
                "congrats_earned_reward" to "🎉 ¡Felicidades! Ganaste una recompensa eco",
                "points_registered_success" to "Tus puntos se guardaron con éxito para subir de nivel y abrir desafíos.",
                "instant_deposit_bin" to "Depósito Instantáneo 📥",
                "share_impact" to "Compartir Impacto",
                "scientific_steps" to "Pasos Científicos:",
                "target_product" to "Producto Objetivo:",
                "amazing_info" to "Hecho Increíble:",
                "minutes_unit" to "min",
                "additional_materials_needed" to "Materiales Requeridos:",
                "scan_waste_ai" to "Escanear con Inteligencia Artificial",
                "camera_viewfinder_instruction" to "Apunta la cámara a botellas, latas o cajas",
                "choose_sample_for_scan" to "Elige una muestra para escaneo rápido:",
                "from_gallery" to "De la Galería",
                "auto_flash" to "Flash Auto",
                "instant_smart_deposit" to "Depósito Inteligente Instantáneo 📶",
                "lid_opened_text" to "🔓 ¡Tapa abierta automáticamente!",
                "ble_hatch_opened_desc" to "La tapa se abrió mediante Bluetooth. Introduce el residuo de forma segura.",
                "bin_sensors_updated_text" to "Sensores actualizados y recompensa registrada.",
                "points_added_success_text" to "¡Puntos XP agregados para proteger el medio ambiente!",
                "connecting_to_bin" to "Conectando a",
                "securing_ble_connection_desc" to "Estableciendo conexión Bluetooth • Pesando material...",
                "choose_closest_bin_deposit" to "Selecciona el contenedor inteligente más cercano para depositar tu residuo:",
                "confirm_connection_deposit" to "Confirmar Conexión y Depósito 📥",
                "close_back_home" to "Cerrar y Volver a Inicio 👍",
                "item_deposited_reward_logged" to "🎉 ¡Residuo depositado y recompensa registrada!",
                "sample_plastic" to "Botella de Plástico",
                "sample_cardboard" to "Cartón Seco",
                "sample_aluminum" to "Lata de Aluminio",
                "sample_food" to "Residuos de Alimentos",
                "material_plastic" to "Plástico PET",
                "material_cardboard" to "Cartón Seco",
                "material_food" to "Restos de Comida",
                "material_metal" to "Aluminio"
            ),
            "fr" to mapOf(
                "app_title" to "Dawwer",
                "home" to "Accueil",
                "scanner" to "Scanner",
                "challenges" to "Défis",
                "rewards" to "Récompenses",
                "profile" to "Profil",
                "points" to "Points",
                "level" to "Niveau",
                "welcome" to "Bienvenue,",
                "total_scans" to "Scans totaux",
                "completed_challenges" to "Réalisé",
                "streak_days" to "Jours de suite",
                "impact" to "Impact Environnemental",
                "carbon_saved" to "Kg CO2 Évité",
                "water_saved" to "Litres Eau Évitée",
                "energy_saved" to "kWh Énergie Évitée",
                "trees_planted" to "Arbres Équivalents",
                "recent_activity" to "Scans Récents",
                "empty_history" to "Aucun scan enregistré. Essayez de scanner une bouteille ! 🧴",
                "quick_actions" to "Actions Rapides",
                "scan_now" to "Scanner Déchet",
                "active_challenges" to "Défis Actifs",
                "leaderboard" to "Classement",
                "badges" to "Badges & Succès",
                "details" to "Détails",
                "scanning" to "L'IA analyse la composition...",
                "settings_title" to "Profil & Paramètres",
                "name" to "Nom",
                "email" to "Email",
                "phone" to "Téléphone",
                "country" to "Pays / Région",
                "edit_profile" to "Modifier Profil",
                "general_settings" to "Paramètres Généraux:",
                "theme_and_colors" to "Apparence & Couleurs",
                "language" to "Langue de l'App",
                "privacy_security" to "Confidentialité & Sécurité",
                "help_center" to "Centre d'Aide",
                "about_app" to "À propos de Dawwer",
                "danger_zone" to "Zone de Danger",
                "danger_zone_desc" to "Supprimer tout l'historique et réinitialiser les compteurs.",
                "reset_db" to "Réinitialiser la Base locale",
                "logout" to "Se déconnecter",
                "save" to "Sauvegarder",
                "cancel" to "Annuler",
                "dark" to "Sombre 🌙",
                "light" to "Clair ☀️",
                "auto" to "Auto 💻",
                "privacy_desc1" to "• Tous les scans sont stockés localement sur votre appareil.",
                "privacy_desc2" to "• Les clés d'Intelligence Artificielle sont chiffrées de manière sûre.",
                "faq_q1" to "Comment gagner des points ?",
                "faq_a1" to "Gagnez des points XP en scannant des déchets ou en les déposant dans les conteneurs intelligents de l'université !",
                "faq_q2" to "L'application fonctionne-t-elle hors ligne ?",
                "faq_a2" to "Oui, Dawwer dispose d'un classificateur intelligent local qui fonctionne entièrement sans réseau.",
                "about_desc1" to "Version : 1.0.0 (Beta)",
                "about_desc2" to "Développé fièrement pour encourager la durabilité environnementale et le recyclage vert.",
                "campus_bins_title" to "Réseau de Conteneurs de l'Université du Caire (IoT) :",
                "nfc_guide" to "Guide de Connexion NFC",
                "iot_active" to "🟢 Disponible",
                "iot_full" to "⚠️ Conteneur Plein",
                "fill_level" to "Niveau de remplissage actuel :",
                "total_recycled" to "📊 Total Collecté : ",
                "power_source" to "⚡ Énergie : Solaire",
                "simulate_deposit" to "Simuler un Dépôt Intelligent 📥",
                "nfc_dialog_title" to "Technologie de Connexion IoT 📶",
                "nfc_dialog_desc" to "Dawwer prend en charge NFC et Bluetooth pour une connexion instantanée avec les conteneurs intelligents du campus.",
                "understand" to "Compris 👍",
                "iot_connect" to "Liaison IoT : ",
                "choose_deposit_material" to "Sélectionnez le type de matériau à déposer dans la fente :",
                "connecting_nfc" to "Connexion sécurisée au conteneur intelligent via NFC...",
                "hold_phone_near" to "Veuillez maintenir votre appareil près du nœud Bluetooth du conteneur.",
                "lid_opened" to "🔓 Trappe ouverte automatiquement !",
                "weighing_sorting" to "Pesée et tri optique en cours dans le conteneur...",
                "sensors_active" to "Capteurs de télémétrie et poids IoT actifs.",
                "deposit_success_title" to "🎉 Dépôt Réussi !",
                "deposit_success_desc" to "Capteurs mis à jour et base de données synchronisée.",
                "reward_text" to "Récompense : Points XP et Crédits Carbone obtenus !",
                "process_done" to "Terminé 👍",
                "current_title_prefix" to "Titre Actuel : ",
                "connecting_campus" to "Connexion au réseau IoT...",
                "plastic" to "Plastique 🧴",
                "plastic_sub" to "Plastiques PET (Bouteilles)",
                "aluminum" to "Aluminium 🥤",
                "aluminum_sub" to "Canettes métalliques et soda",
                "cardboard" to "Papier / Carton 📄",
                "cardboard_sub" to "Vieux livres, papiers et cartons",
                "electronics" to "Déchets Électroniques 💻",
                "electronics_sub" to "Fils endommagés, vieux téléphones, batteries",
                "challenges_rewards_title" to "Défis & Récompenses Écologiques",
                "daily_tab" to "Quotidien ⚡",
                "weekly_tab" to "Hebdomadaire 📅",
                "event_tab" to "Ramadan Vert 🌙",
                "rewards_tab" to "Récompenses 💎",
                "badges_tab" to "Badges 🏅",
                "unlocked_badges_title" to "Badges Déverrouillés :",
                "challenges_available_daily" to "Défis quotidiens disponibles pour économiser :",
                "challenges_available_weekly" to "Défis compétitifs de la semaine :",
                "challenges_available_event" to "Événement Actif: Ramadan Vert Éco 🌙",
                "challenges_empty" to "Vous avez terminé tous les défis de cette section pour aujourd'hui ! Continuez à scanner pour gagner des XP ! 🎉",
                "simulate_completion" to "Simuler Fait",
                "completed_badge" to "Réussi avec Succès ✅",
                "leaderboard_title" to "🏆 Classement Éco-Citoyen de Juin",
                "user_rank_label" to "Votre classement local actuel :",
                "user_rank_value" to "Classement 47 (Top 15%)",
                "work_hard_up" to "Continuez pour progresser ! 🌱",
                "saved_carbon_title" to "🌡️ Émissions de CO2 Évitées :",
                "saved_carbon_sub" to "Éviter les gaz à effet de serre responsables du réchauffement.",
                "saved_water_title" to "🌊 Eau Économisée :",
                "saved_water_sub" to "Prévenir le gaspillage d'eau douce dans les processus de production primaires.",
                "saved_energy_title" to "⚡ Énergie Économisée :",
                "saved_energy_sub" to "Équivaut à alimenter vos lampes à la maison pendant des mois complets.",
                "saved_trees_title" to "🌳 Jeunes Arbres Équivalents :",
                "saved_trees_sub" to "Le carbone absorbé équivaut à planter une toute nouvelle petite forêt.",
                "ai_report_title" to "Rapport Hebdomadaire Environnemental IA",
                "ai_report_empty" to "Bienvenue sur Dawwer ! Vos métriques initiales indiquent que vous êtes prêt. Essayez de scanner votre premier déchet ! 🌱",
                "ai_report_few" to "Bon début ! Vous avez trié %d déchets cette semaine. Continuez à recycler du carton sec pour sauver des arbres et visitez Dokki demain pour gagner des points. 🛡️",
                "ai_report_many" to "Performance exceptionnelle ! Vous êtes dans le top 15% des écologistes de votre région. Énergie économisée équivalente à un téléviseur allumé 50 heures de suite. Maintenez votre flamme de Streak active ! 🔥",
                
                // Extra Keys
                "you" to "Vous",
                "level_name_1" to "Jeune Pousse 🌱",
                "level_name_2" to "Plantule 🌿",
                "level_name_3" to "Arbrisseau 🌳",
                "level_name_4" to "Mini-Forêt 🌲",
                "level_name_5" to "Gardien Éco 🛡️",
                "level_name_6" to "Héros de la Terre 🌍",
                "level_name_fallback" to "Protecteur de la Terre 🌍",
                "challenge_c1_title" to "Scannez 3 déchets aujourd'hui",
                "challenge_c1_desc" to "Utilisez la caméra intelligente pour identifier et classer 3 déchets différents.",
                "challenge_c1_time" to "8 heures restantes",
                "challenge_c2_title" to "Dépôt en conteneur",
                "challenge_c2_desc" to "Visitez un bac intelligent sur le campus et effectuez un dépôt sécurisé via BLE.",
                "challenge_c2_time" to "12 heures restantes",
                "challenge_c3_title" to "Semaine sans plastique",
                "challenge_c3_desc" to "Évitez bouteilles et sacs plastiques à usage unique pendant 7 jours d'affilée.",
                "challenge_c3_time" to "3 jours restants",
                "challenge_c4_title" to "Collectez 1 kg de papier/carton",
                "challenge_c4_desc" to "Rassemblez de vieux livres ou papiers pour les confier au recyclage.",
                "challenge_c4_time" to "4 jours restants",
                "challenge_c5_title" to "Ramadan Vert 🌙",
                "challenge_c5_desc" to "Spécial écologie: réduisez le gaspillage alimentaire et d'emballage durant Iftar et Suhoor.",
                "challenge_c5_time" to "15 jours restants",
                "badge_first_scan_name" to "Premier Pas",
                "badge_first_scan_desc" to "Vous avez scanné votre premier déchet avec succès et commencé votre voyage vert.",
                "badge_plastic_hero_name" to "Ennemi du Plastique",
                "badge_plastic_hero_desc" to "Vous avez recyclé 10 plastiques pour protéger les océans et la vie marine.",
                "badge_streak_7_name" to "Semaine Dorée",
                "badge_streak_7_desc" to "Vous avez maintenu votre série d'activité quotidienne pendant 7 jours consécutifs.",
                "badge_map_explorer_name" to "Champion du Campus",
                "badge_map_explorer_desc" to "Vous avez trié vos déchets avec succès dans les conteneurs intelligents du campus.",
                "badge_eco_legend_name" to "Légende Éco",
                "badge_eco_legend_desc" to "Vous avez atteint le niveau 5 (Gardien Éco) en préservation des ressources.",
                "badge_social_hero_name" to "Ambassadeur Vert",
                "badge_social_hero_desc" to "Vous avez partagé vos exploits écologiques et invité vos amis à rejoindre Dawwer.",
                "loc_loc1_name" to "Bac intelligent d'Ingénierie (IoT)",
                "loc_loc1_address" to "Bâtiment principal de génie, Université du Caire",
                "loc_loc1_desc" to "Bac solaire connecté à télémétrie de niveau pour optimiser la collecte.",
                "loc_loc2_name" to "Recyclage de la Bibliothèque Centrale",
                "loc_loc2_address" to "Cour de la Nouvelle Bibliothèque Centrale, Université du Caire",
                "loc_loc2_desc" to "Unité de récupération intelligente avec écrasement mécanique automatique des bouteilles PET.",
                "loc_loc3_name" to "Bac de Papier de la Faculté des Lettres",
                "loc_loc3_address" to "À côté de la Faculté des Lettres, Université du Caire",
                "loc_loc3_desc" to "Bac de collecte des livres et notes usagés pour étudiants.",
                "loc_loc4_name" to "Centre de Déchets Électroniques",
                "loc_loc4_address" to "Résidences Universitaires, Entre les Rues, Gizeh",
                "loc_loc4_desc" to "Bac sécurisé pour recycler chargeurs, téléphones usagés et piles.",
                "loc_loc5_name" to "Bac de Verre de la Faculté des Sciences",
                "loc_loc5_address" to "À côté de la Chimie, Faculté des Sciences, Université du Caire",
                "loc_loc5_desc" to "Pour flacons en verre et emballages carton propres.",
                "scan_result_title" to "Résultat du Scan",
                "sustainability_index" to "Indice de durabilité:",
                "immediate_environmental_impact" to "Impact environnemental immédiat (si recyclé) :",
                "decomposition_time" to "Temps de décomposition",
                "co2_saving" to "Économie de CO2",
                "water_saving_label" to "Eau Économisée",
                "suggested_recycling_methods" to "Méthodes de recyclage suggérées :",
                "creative_reuse_diy_ideas" to "Idées de réutilisation créatives (DIY) :",
                "congrats_earned_reward" to "🎉 Bravo! Vous avez gagné un éco-cadeau",
                "points_registered_success" to "Vos points ont été enregistrés pour augmenter votre niveau et débloquer des défis.",
                "instant_deposit_bin" to "Dépôt Instantané 📥",
                "share_impact" to "Partager l'Impact",
                "scientific_steps" to "Étapes Scientifiques :",
                "target_product" to "Produit Cible :",
                "amazing_info" to "Fait Surprenant :",
                "minutes_unit" to "min",
                "additional_materials_needed" to "Matériaux Requis :",
                "scan_waste_ai" to "Scanner avec l'IA",
                "camera_viewfinder_instruction" to "Pointez la caméra vers bouteilles, canettes ou boîtes",
                "choose_sample_for_scan" to "Choisissez un échantillon pour scan rapide :",
                "from_gallery" to "De la Galerie",
                "auto_flash" to "Flash Auto",
                "instant_smart_deposit" to "Dépôt Intelligent Instantané 📶",
                "lid_opened_text" to "🔓 Trappe ouverte automatiquement !",
                "ble_hatch_opened_desc" to "Le couvercle s'est ouvert par Bluetooth. Insérez le déchet de façon sécurisée.",
                "bin_sensors_updated_text" to "Capteurs mis à jour et récompense enregistrée.",
                "points_added_success_text" to "Points XP ajoutés pour protéger l'environnement !",
                "connecting_to_bin" to "Connexion à",
                "securing_ble_connection_desc" to "Établissement connexion Bluetooth • Pesée du déchet...",
                "choose_closest_bin_deposit" to "Sélectionnez le conteneur intelligent le plus proche pour déposer votre déchet :",
                "confirm_connection_deposit" to "Confirmer Connexion et Dépôt 📥",
                "close_back_home" to "Fermer et Retourner à l'Accueil 👍",
                "item_deposited_reward_logged" to "🎉 Déchet déposé et récompense enregistrée !",
                "sample_plastic" to "Bouteille en Plastique",
                "sample_cardboard" to "Carton Sec",
                "sample_aluminum" to "Canette Aluminium",
                "sample_food" to "Déchets Alimentaires",
                "material_plastic" to "Plastique PET",
                "material_cardboard" to "Carton Sec",
                "material_food" to "Restes Alimentaires",
                "material_metal" to "Aluminium"
            ),
            "tr" to mapOf(
                "app_title" to "Dawwer",
                "home" to "Ana Sayfa",
                "scanner" to "Tarayıcı",
                "challenges" to "Görevler",
                "rewards" to "Ödüller",
                "profile" to "Profil",
                "points" to "Puan",
                "level" to "Seviye",
                "welcome" to "Hoş geldiniz,",
                "total_scans" to "Toplam Tarama",
                "completed_challenges" to "Tamamlanan",
                "streak_days" to "Günlük Seri",
                "impact" to "Çevresel Etki",
                "carbon_saved" to "Kg CO2 Tasarrufu",
                "water_saved" to "Litre Su Korundu",
                "energy_saved" to "kWh Enerji Tasarrufu",
                "trees_planted" to "Ağaç Eşdeğeri",
                "recent_activity" to "Son Taramalar",
                "empty_history" to "Henüz tarama yapılmadı. Bir şişe tarayın! 🧴",
                "quick_actions" to "Hızlı İşlemler",
                "scan_now" to "Atık Tara",
                "active_challenges" to "Aktif Görevler",
                "leaderboard" to "Sıralama",
                "badges" to "Rozetler",
                "details" to "Detaylar",
                "scanning" to "YZ atığı analiz ediyor...",
                "settings_title" to "Profil ve Ayarlar",
                "name" to "İsim",
                "email" to "E-posta",
                "phone" to "Telefon",
                "country" to "Ülke",
                "edit_profile" to "Profili Düzenle",
                "general_settings" to "Genel Ayarlar:",
                "theme_and_colors" to "Görünüm ve Renkler",
                "language" to "Uygulama Dili",
                "privacy_security" to "Gizlilik ve Güvenlik",
                "help_center" to "Yardım Merkezi ve SSS",
                "about_app" to "Dawwer Hakkında",
                "danger_zone" to "Tehlike Bölgesi",
                "danger_zone_desc" to "Tüm geçmişi sil ve tüm ölçümleri sıfırla.",
                "reset_db" to "Yerel Veritabanını Sıfırla",
                "logout" to "Oturumu Kapat",
                "save" to "Kaydet",
                "cancel" to "İptal",
                "dark" to "Karanlık 🌙",
                "light" to "Açık ☀️",
                "auto" to "Otomatik 💻",
                "privacy_desc1" to "• Tüm taramalar yerel olarak cihazınızda saklanır.",
                "privacy_desc2" to "• Yapay Zeka anahtarları şifrelenir ve güvenlidir.",
                "faq_q1" to "Nasıl puan ve geri dönüşüm ödülleri kazanırım?",
                "faq_a1" to "Atıkları Yapay Zeka ile taradığınızda veya depozito yaptığınızda seviye atlamak için puan kazanırsınız!",
                "faq_q2" to "Uygulama internetsiz çalışır mı?",
                "faq_a2" to "Evet, Dawwer tamamen internet dışı çalışan yerel bir sınıflandırma sistemine sahiptir!",
                "about_desc1" to "Sürüm: 1.0.0 (Beta)",
                "about_desc2" to "Sürdürülebilirlik ve yeşil çevre dostu desteklemek amacıyla gururla geliştirilmiştir.",
                "campus_bins_title" to "Kahire Üniversitesi Akıllı Kutular Ağı (IoT):",
                "nfc_guide" to "NFC Bağlantı Kılavuzu",
                "iot_active" to "🟢 Kullanılabilir",
                "iot_full" to "⚠️ Dolu",
                "fill_level" to "Güncel Doluluk Oranı:",
                "total_recycled" to "📊 Toplam Toplanan: ",
                "power_source" to "⚡ Güç Kaynağı: Güneş Enerjili",
                "simulate_deposit" to "Akıllı Depozitoyu Simüle Et 📥",
                "nfc_dialog_title" to "IoT Bağlantı Teknolojisi 📶",
                "nfc_dialog_desc" to "Dawwer, akıllı geri dönüşüm kutularıyla anında bağlantı sağlamak için NFC ve Bluetooth BLE protokollerini destekler.",
                "understand" to "Anladım 👍",
                "iot_connect" to "IoT Bağlantısı: ",
                "choose_deposit_material" to "Akıllı kutunun önündesiniz. Depolamak istediğiniz malzeme türünü seçin:",
                "connecting_nfc" to "NFC üzerinden akıllı kutuya güvenli bağlantı kuruluyor...",
                "hold_phone_near" to "Lütfen cihazınızı kutunun üzerindeki Bluetooth BLE düğümüne yakın tutun.",
                "lid_opened" to "🔓 Kapak otomatik olarak açıldı!",
                "weighing_sorting" to "Akıllı kutuda tartım ve optik sınıflandırma sürüyor...",
                "sensors_active" to "IoT ağırlık sensörleri aktif.",
                "deposit_success_title" to "🎉 Depozito Başarılı!",
                "deposit_success_desc" to "Sensörler güncellendi ve telemetri veritabanı ayarlandı.",
                "reward_text" to "Ödül: XP Puanı kazanıldı!",
                "process_done" to "Bitti 👍",
                "current_title_prefix" to "Mevcut Unvan: ",
                "connecting_campus" to "Kutu ağına bağlanılıyor...",
                "plastic" to "Plastik 🧴",
                "plastic_sub" to "PET Plastikler (Şişeler / Bardaklar)",
                "aluminum" to "Alüminyum 🥤",
                "aluminum_sub" to "Metal kutular ve kola kutuları",
                "cardboard" to "Kağıt / Karton 📄",
                "cardboard_sub" to "Eski kitaplar, kağıtlar ve kutular",
                "electronics" to "Elektronik Atık 💻",
                "electronics_sub" to "Hasarlı kablolar, eski telefonlar, piller",
                "challenges_rewards_title" to "Çevre Koruma Görevleri & Ödülleri",
                "daily_tab" to "Günlük ⚡",
                "weekly_tab" to "Haftalık 📅",
                "event_tab" to "Yeşil Ramazan 🌙",
                "rewards_tab" to "Ödüller 💎",
                "badges_tab" to "Rozetler 🏅",
                "unlocked_badges_title" to "Kazanılan Rozetler & Turnuvalar:",
                "challenges_available_daily" to "Günlük çevre tasarrufu görevleri:",
                "challenges_available_weekly" to "Bu haftanın rekabetçi görevleri:",
                "challenges_available_event" to "Aktif Çevre Etkinliği: Yeşil Ramazan 🌙",
                "challenges_empty" to "Bugün bu bölümdeki tüm görevleri tamamladınız! XP kazanmaya devam etmek için atık tarayın! 🎉",
                "simulate_completion" to "Tamamlandı Simüle Et",
                "completed_badge" to "Başarıyla Tamamlandı ✅",
                "leaderboard_title" to "🏆 Haziran Çevre Koruma Sıralaması",
                "user_rank_label" to "Mevcut yerel sıralamanız:",
                "user_rank_value" to "Sıra 47 (En iyi %15)",
                "work_hard_up" to "Yükselmek için devam edin! 🌱",
                "saved_carbon_title" to "🌡️ Önlenen CO2 Salınımı:",
                "saved_carbon_sub" to "Küresel ısınmaya neden olan sera gazlarının önlenmesi.",
                "saved_water_title" to "🌊 Kurtarılan Su:",
                "saved_water_sub" to "Birincil üretim süreçlerinde temiz su israfının önlenmesi.",
                "saved_energy_title" to "⚡ Tasarruf Edilen Enerji:",
                "saved_energy_sub" to "Evinizdeki lambaları aylarca çalıştırmaya eşdeğer tasarruf.",
                "saved_trees_title" to "🌳 Eşdeğer Genç Ağaç Sayısı:",
                "saved_trees_sub" to "Emilen karbon, yeni bir mini orman dikmişsiniz gibi etki yapar.",
                "ai_report_title" to "Yapay Zeka Haftalık Çevre Raporu",
                "ai_report_empty" to "Dawwer'a hoş geldiniz! İlk ölçümleriniz başlamaya hazır olduğunuzu gösteriyor. İlk atığınızı tarayarak başlayın! 🌱",
                "ai_report_few" to "Harika bir başlangıç! Bu hafta %d atık sınıflandırdınız. Orman ağaçlarını kurtarmak için kuru karton toplamaya devam edin ve ekstra puan için yarın Dokki noktasına uğrayın. 🛡️",
                "ai_report_many" to "Olağanüstü performans! Bölgenizdeki çevre savunucularının ilk %15'indesiniz. Televizyonu 50 saat çalıştırmaya yetecek enerji tasarrufu sağladınız. Yeşil Ramazan mücadelesine devam edin! 🔥",
                
                // Extra Keys
                "you" to "Siz",
                "level_name_1" to "Tohum 🌱",
                "level_name_2" to "Filiz 🌿",
                "level_name_3" to "Fidan 🌳",
                "level_name_4" to "Mini Orman 🌲",
                "level_name_5" to "Doğa Muhafızı 🛡️",
                "level_name_6" to "Dünya Kahramаны 🌍",
                "level_name_fallback" to "Dünya Koruyucusu 🌍",
                "challenge_c1_title" to "Bugün 3 atık tara",
                "challenge_c1_desc" to "3 farklı atığı akıllı kamera ile tarayıp sınıflandırın.",
                "challenge_c1_time" to "8 saat kaldı",
                "challenge_c2_title" to "Akıllı kutu depozitosu",
                "challenge_c2_desc" to "Kampüsteki bir akıllı kutuyu ziyaret edin ve BLE üzerinden depozito yapın.",
                "challenge_c2_time" to "12 saat kaldı",
                "challenge_c3_title" to "Plastiksiz bir hafta",
                "challenge_c3_desc" to "7 gün boyunca tek kullanımlık plastik şişe ve poşetlerden kaçının.",
                "challenge_c3_time" to "3 gün kaldı",
                "challenge_c4_title" to "1 kg kağıt/karton topla",
                "challenge_c4_desc" to "Eski kitap ve kağıtları toplayıp geri dönüşüm noktasına teslim edin.",
                "challenge_c4_time" to "4 gün kaldı",
                "challenge_c5_title" to "Yeşil Ramazan 🌙",
                "challenge_c5_desc" to "Çevre özel: İftar ve Sahurda yemek ve ambalaj atıklarını azaltın.",
                "challenge_c5_time" to "15 gün kaldı",
                "badge_first_scan_name" to "İlk Adım",
                "badge_first_scan_desc" to "İlk atığınızı başarıyla taradınız ve ekolojik yolculuğunuza başladınız.",
                "badge_plastic_hero_name" to "Plastik Düşmanı",
                "badge_plastic_hero_desc" to "Okyanusları ve deniz canlılarını korumak için 10 plastik ürünü geri dönüştürdünüz.",
                "badge_streak_7_name" to "Altın Hafta",
                "badge_streak_7_desc" to "Günlük aktif serinizi 7 gün boyunca kesintisiz korudunuz.",
                "badge_map_explorer_name" to "Kampüs Şampiyonu",
                "badge_map_explorer_desc" to "Atıklarınızı Kahire Üniversitesi akıllı kutularına başarıyla bıraktınız.",
                "badge_eco_legend_name" to "Doğa Efsanesi",
                "badge_eco_legend_desc" to "Kaynak korumada Seviye 5'e (Doğa Muhafızı) ulaştınız.",
                "badge_social_hero_name" to "Yeşil Elçi",
                "badge_social_hero_desc" to "Çevre başarılarınızı paylaştınız ve arkadaşlarınızı Dawwer'a davet ettiniz.",
                "loc_loc1_name" to "Mühendislik Fakültesi Akıllı Kutusu (IoT)",
                "loc_loc1_address" to "Mühendislik Fakültesi Ana Binası, Kahire Üniversitesi",
                "loc_loc1_desc" to "Güneş enerjili akıllı istasyon. Doluluk oranı bildirimi içerir.",
                "loc_loc2_name" to "Merkez Kütüphane Plastik Toplama Noktası",
                "loc_loc2_address" to "Yeni Merkez Kütüphane Avlusu, Kahire Üniversitesi",
                "loc_loc2_desc" to "PET şişeleri ezen akıllı geri dönüşüm birimi.",
                "loc_loc3_name" to "Edebiyat Fakültesi Kağıt Kutusu",
                "loc_loc3_address" to "Edebiyat Fakültesi Binası Yanı, Kahire Üniversitesi",
                "loc_loc3_desc" to "Eski kitap ve defterleri toplayıp geri dönüştürür.",
                "loc_loc4_name" to "Elektronik Atık Merkezi",
                "loc_loc4_address" to "Öğrenci Yurtları, Sokaklar Arası, Gize",
                "loc_loc4_desc" to "Eski telefon, şarj aleti ve pilleri güvenle geri dönüştürür.",
                "loc_loc5_name" to "Fen Fakültesi Cam Kutusu",
                "loc_loc5_address" to "Kimya Bölümü Yanı, Fen Fakültesi, Kahire Üniversitesi",
                "loc_loc5_desc" to "Temiz cam kapları ve kutuları geri dönüştürür.",
                "scan_result_title" to "Tarama Sonucu",
                "sustainability_index" to "Sürdürülebilirlik endeksi:",
                "immediate_environmental_impact" to "Anında Çevresel Etki (geri dönüştürülürse):",
                "decomposition_time" to "Ayrışma süresi",
                "co2_saving" to "CO2 Tasarrufu",
                "water_saving_label" to "Kurtarılan Su",
                "suggested_recycling_methods" to "Önerilen Geri Dönüşüm Yöntemleri:",
                "creative_reuse_diy_ideas" to "Yaratıcı Yeniden Kullanım (DIY) Fikirleri:",
                "congrats_earned_reward" to "🎉 Tebrikler! Çevre ödülü kazandınız",
                "points_registered_success" to "Puanlarınız başarıyla kaydedildi, seviyeniz yükseltildi ve yeni görevler açıldı.",
                "instant_deposit_bin" to "Anında Depozito 📥",
                "share_impact" to "Etkiyi Paylaş",
                "scientific_steps" to "Bilimsel Adımlar:",
                "target_product" to "Hedef Ürün:",
                "amazing_info" to "Şaşırtıcı Bilgi:",
                "minutes_unit" to "dk",
                "additional_materials_needed" to "Gerekli Malzemeler:",
                "scan_waste_ai" to "YZ ile Atık Tara",
                "camera_viewfinder_instruction" to "Kamerayı şişe, kutu veya ambalajlara doğrultun",
                "choose_sample_for_scan" to "Hızlı tarama için bir örnek seçin:",
                "from_gallery" to "Galeriden Seç",
                "auto_flash" to "Oto Flaş",
                "instant_smart_deposit" to "Anında Akıllı Depozito 📶",
                "lid_opened_text" to "🔓 Kapak otomatik olarak açıldı!",
                "ble_hatch_opened_desc" to "Kapak Bluetooth ile açıldı. Atığı güvenle yerleştirin.",
                "bin_sensors_updated_text" to "Sensörler güncellendi ve ödül kaydedildi.",
                "points_added_success_text" to "Çevreyi koruduğunuz için XP puanları eklendi!",
                "connecting_to_bin" to "Bağlanıyor:",
                "securing_ble_connection_desc" to "Bluetooth bağlantısı kuruluyor • Malzeme tartılıyor...",
                "choose_closest_bin_deposit" to "Atığınızı depolamak için en yakın akıllı kutuyu seçin:",
                "confirm_connection_deposit" to "Bağlantıyı ve Depozitoyu Onayla 📥",
                "close_back_home" to "Kapat ve Ana Sayfaya Dön 👍",
                "item_deposited_reward_logged" to "🎉 Atık bırakıldı ve ödül kaydedildi!",
                "sample_plastic" to "Plastik Şişe",
                "sample_cardboard" to "Kuru Karton",
                "sample_aluminum" to "Alüminyum Kutu",
                "sample_food" to "Yiyecek Atığı",
                "material_plastic" to "PET Plastik",
                "material_cardboard" to "Kuru Karton",
                "material_food" to "Yemek Atığı",
                "material_metal" to "Alüminyum"
            )
        )
    }
}
