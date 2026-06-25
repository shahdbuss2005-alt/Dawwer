package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val name: String,
    val level: Int = 1,
    val points: Int = 0,
    val streak: Int = 0,
    val lastActive: Long = System.currentTimeMillis(),
    val co2SavedGrams: Double = 0.0,
    val waterSavedLiters: Double = 0.0,
    val energySavedKwh: Double = 0.0,
    val treesPlanted: Double = 0.0,
    val email: String = "",
    val phone: String = "+20 10 1234 5678",
    val country: String = "مصر",
    val language: String = "ar", // ar, en, es, fr, tr, it, pt
    val themeMode: String = "dark" // dark, light, auto
) {
    companion object {
        fun empty() = UserProfile(name = "")
    }
}

@Entity(tableName = "scan_history")
data class ScanHistoryItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val itemName: String,
    val itemEmoji: String,
    val materialType: String, // بلاستيك، ورق، زجاج، معدن، عضوي، إلكتروني
    val recyclable: Boolean,
    val recyclabilityScore: Int,
    val environmentalImpact: String,
    val co2SavedGrams: Double,
    val waterSavedLiters: Double,
    val energySavedKwh: Double,
    val decompositionYears: String,
    val recyclingMethodsJson: String, // Serialized list
    val creativeReuseJson: String, // Serialized list
    val funFact: String,
    val pointsEarned: Int
)

@Entity(tableName = "challenges")
data class Challenge(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val type: String, // daily, weekly, event
    val progress: Int,
    val target: Int,
    val completed: Boolean,
    val rewardPoints: Int,
    val timeLeftLabel: String
)

@Entity(tableName = "badges")
data class BadgeItem(
    @PrimaryKey val id: String,
    val name: String,
    val emoji: String,
    val description: String,
    val unlocked: Boolean
)



// Data transfer / parsed helper objects for Gemini Scan Results
data class GeminiScanResult(
    val itemName: String,
    val itemEmoji: String,
    val materialType: String,
    val recyclable: Boolean,
    val recyclabilityScore: Int,
    val environmentalImpact: String,
    val co2SavedGrams: Double,
    val waterSavedLiters: Double,
    val energySavedKwh: Double,
    val decompositionYears: String,
    val recyclingMethods: List<RecyclingMethod>,
    val creativeReuse: List<CreativeReuse>,
    val funFact: String,
    val pointsEarned: Int,
    val badgeUnlocked: String? = null
)

data class RecyclingMethod(
    val method: String,
    val difficulty: String,
    val steps: List<String>,
    val result: String,
    val wowFact: String
)

data class CreativeReuse(
    val idea: String,
    val difficulty: String,
    val materialsNeeded: List<String>,
    val timeMinutes: Int,
    val steps: List<String> = emptyList(),
    val benefit: String = ""
)

// Room Type Converters
class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        if (value == null) return "[]"
        val array = JSONArray()
        for (item in value) {
            array.put(item)
        }
        return array.toString()
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrEmpty()) return emptyList()
        val list = mutableListOf<String>()
        val array = JSONArray(value)
        for (i in 0 until array.length()) {
            list.add(array.getString(i))
        }
        return list
    }
}

object InitialData {
    val defaultProfile = UserProfile(
        id = 1,
        name = "المستخدم الجديد",
        level = 1,
        points = 0,
        streak = 0,
        co2SavedGrams = 0.0,
        waterSavedLiters = 0.0,
        energySavedKwh = 0.0
    )

    val defaultChallenges = listOf(
        Challenge("c1", "امسح ٣ مخلفات اليوم", "استخدم الماسح الضوئي الذكي لتحديد ٣ مخلفات مختلفة وإعادة تدويرها.", "daily", 0, 3, false, 150, "٨ ساعات متقبة"),
        Challenge("c2", "إيداع بالحاوية الذكية", "قم بزيارة أحد الصناديق الذكية للحرم الجامعي وسجل إيداعاً لتأكيد فرز المخلفات.", "daily", 0, 1, false, 100, "١٢ ساعة متبقية"),
        Challenge("c3", "أسبوع خالي من البلاستيك", "تجنب تماماً استخدام الأكياس والزجاجات البلاستيكية ذات الاستخدام الواحد لمده أسبوع.", "weekly", 0, 7, false, 500, "٣ أيام متبقية"),
        Challenge("c4", "جمع ١ كيلو كرتون وورق", "قم بتجميع أوراق الكتب القديمة والكرتون وتسليمها لأقرب مركز تدوير.", "weekly", 0, 1, false, 300, "٤ أيام متبقية"),
        Challenge("c5", "الرمضان الأخضر 🌙", "تحدي خاص بتقليل مخلفات وجبات الإفطار والسحور وإعادة تدوير علب الأغذية.", "event", 0, 20, false, 1000, "١٥ يوم متبقي")
    )

    val defaultBadges = listOf(
        BadgeItem("first_scan", "أول خطوة", "👶", "قمت بمسح أول مخلف لك بنجاح وبدأت رحلتك البيئية.", false),
        BadgeItem("plastic_hero", "عدو البلاستيك", "🦸", "قمت بإعادة تدوير ١٠ مخلفات بلاستيكية لحماية البحار والمحيطات.", false),
        BadgeItem("streak_7", "أسبوع ذهبي", "🔥", "حافظت على نشاطك اليومي لـ ٧ أيام متتالية دون انقطاع.", false),
        BadgeItem("map_explorer", "بطل حاويات الحرم", "📥", "قمت بفرز وإيداع مخلفاتك بنجاح في حاويات فرز جامعة القاهرة الذكية.", false),
        BadgeItem("eco_legend", "أسطورة البيئة", "🛡️", "وصلت للمستوى ٥ (حارس البيئة) في الحفاظ على الموارد.", false),
        BadgeItem("social_hero", "السفير الأخضر", "📢", "قمت بمشاركة إنجازاتك ودعوة أصدقائك للمشاركة في دَوِّر.", false)
    )

}
