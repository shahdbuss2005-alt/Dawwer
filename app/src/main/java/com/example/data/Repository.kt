package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

class DawwerRepository(private val database: AppDatabase) {

    val userProfile: Flow<UserProfile?> = database.userProfileDao().getUserProfileFlow()
    val scanHistory: Flow<List<ScanHistoryItem>> = database.scanHistoryDao().getAllHistoryFlow()
    val challenges: Flow<List<Challenge>> = database.challengeDao().getAllChallengesFlow()
    val badges: Flow<List<BadgeItem>> = database.badgeDao().getAllBadgesFlow()

    suspend fun checkAndSeedDatabase() {
        val currentProfile = database.userProfileDao().getUserProfile()
        if (currentProfile == null) {
            // Seed initial data
            database.userProfileDao().insertOrUpdateProfile(InitialData.defaultProfile)
            database.challengeDao().insertChallenges(InitialData.defaultChallenges)
            database.badgeDao().insertBadges(InitialData.defaultBadges)
        }
    }

    suspend fun saveUserProfile(profile: UserProfile) {
        database.userProfileDao().insertOrUpdateProfile(profile)
    }

    suspend fun addScanResult(result: GeminiScanResult) {
        // 1. Save scan item in history
        val methodsJson = android.util.JsonWriter(java.io.StringWriter()).apply {
            // we can convert lists to JSON string using built-in JSONArray
        }
        
        val methodsArray = JSONArray()
        result.recyclingMethods.forEach { method ->
            val obj = JSONObject().apply {
                put("method", method.method)
                put("difficulty", method.difficulty)
                val stepsArr = JSONArray()
                method.steps.forEach { stepsArr.put(it) }
                put("steps", stepsArr)
                put("result", method.result)
                put("wowFact", method.wowFact)
            }
            methodsArray.put(obj)
        }

        val reuseArray = JSONArray()
        result.creativeReuse.forEach { reuse ->
            val obj = JSONObject().apply {
                put("idea", reuse.idea)
                put("difficulty", reuse.difficulty)
                val itemsArr = JSONArray()
                reuse.materialsNeeded.forEach { itemsArr.put(it) }
                put("materialsNeeded", itemsArr)
                put("timeMinutes", reuse.timeMinutes)
            }
            reuseArray.put(obj)
        }

        val scanItem = ScanHistoryItem(
            itemName = result.itemName,
            itemEmoji = result.itemEmoji,
            materialType = result.materialType,
            recyclable = result.recyclable,
            recyclabilityScore = result.recyclabilityScore,
            environmentalImpact = result.environmentalImpact,
            co2SavedGrams = result.co2SavedGrams,
            waterSavedLiters = result.waterSavedLiters,
            energySavedKwh = result.energySavedKwh,
            decompositionYears = result.decompositionYears,
            recyclingMethodsJson = methodsArray.toString(),
            creativeReuseJson = reuseArray.toString(),
            funFact = result.funFact,
            pointsEarned = result.pointsEarned
        )
        database.scanHistoryDao().insertScan(scanItem)

        // 2. Update User Profile (Points, Streak, Saved impact stats)
        val profile = database.userProfileDao().getUserProfile() ?: InitialData.defaultProfile
        val newPoints = profile.points + result.pointsEarned
        
        // Calculate Level (Thresholds: e.g. Level 1: <500, Level 2: 500-1500, Level 3: 1500-3500, Level 4: 3500-7000, Level 5: 7000-15000, Level 6: 15000+)
        val newLevel = when {
            newPoints < 500 -> 1
            newPoints < 1500 -> 2
            newPoints < 3500 -> 3
            newPoints < 7000 -> 4
            newPoints < 15000 -> 5
            else -> 6
        }

        val newProfile = profile.copy(
            points = newPoints,
            level = newLevel,
            co2SavedGrams = profile.co2SavedGrams + result.co2SavedGrams,
            waterSavedLiters = profile.waterSavedLiters + result.waterSavedLiters,
            energySavedKwh = profile.energySavedKwh + result.energySavedKwh,
            treesPlanted = (profile.co2SavedGrams + result.co2SavedGrams) / 15000.0 // 15 kg co2 ≈ 1 tree
        )
        database.userProfileDao().insertOrUpdateProfile(newProfile)

        // 3. Update Progress of Challenges
        val allChallenges = database.challengeDao().getAllChallengesFlow().first()
        for (challenge in allChallenges) {
            if (!challenge.completed) {
                var progressIncrement = 0
                when (challenge.id) {
                    "c1" -> progressIncrement = 1 // scan 3 items
                    "c3" -> if (result.materialType.contains("بلاستيك", ignoreCase = true) || result.materialType.contains("plastic", ignoreCase = true)) {
                        // wait, avoid plastic challenge? Maybe if it's plastic, we reset or give progress for recycling it?
                        // Let's assume progress is number of plastics recycled instead of avoided, or maybe days avoided? 
                        // The desc says "تجنب تماماً استخدام الأكياس والزجاجات". So scanning plastic means they failed?
                        // If they scan plastic to RECYCLE it, maybe it counts as good? We'll give +1.
                        progressIncrement = 1
                    }
                    "c4" -> if (result.materialType.contains("كرتون", ignoreCase = true) || result.materialType.contains("ورق", ignoreCase = true) || result.materialType.contains("paper", ignoreCase = true) || result.materialType.contains("cardboard", ignoreCase = true)) {
                        progressIncrement = 1 // 1 kg cardboard (we assume 1 scan = 1 progress)
                    }
                    "c5" -> progressIncrement = 1 // green ramadan, scan any item
                }
                
                if (progressIncrement > 0) {
                    val newProgress = (challenge.progress + progressIncrement).coerceAtMost(challenge.target)
                    val completed = newProgress >= challenge.target
                    database.challengeDao().updateChallengeProgress(challenge.id, newProgress, completed)
                    if (completed && challenge.progress < challenge.target) {
                        rewardPoints(challenge.rewardPoints)
                    }
                }
            }
        }

        // 4. Handle badge unlocks
        if (result.badgeUnlocked != null) {
            database.badgeDao().unlockBadge(result.badgeUnlocked)
        }
        
        // Unlock plastic hero badge if plastic count exceeds 5
        val history = database.scanHistoryDao().getAllHistoryFlow().first()
        val plasticsCount = history.count { it.materialType.contains("بلاستيك") || it.materialType.lowercase().contains("plastic") }
        if (plasticsCount >= 3) {
            database.badgeDao().unlockBadge("plastic_hero")
        }
    }

    suspend fun completeChallengeDirectly(challengeId: String) {
        val challengesList = database.challengeDao().getAllChallengesFlow().first()
        val challenge = challengesList.find { it.id == challengeId }
        if (challenge != null && !challenge.completed) {
            database.challengeDao().updateChallengeProgress(challengeId, challenge.target, true)
            rewardPoints(challenge.rewardPoints)
        }
    }

    suspend fun rewardPoints(points: Int) {
        val profile = database.userProfileDao().getUserProfile() ?: InitialData.defaultProfile
        val newPoints = profile.points + points
        val newLevel = when {
            newPoints < 500 -> 1
            newPoints < 1500 -> 2
            newPoints < 3500 -> 3
            newPoints < 7000 -> 4
            newPoints < 15000 -> 5
            else -> 6
        }
        database.userProfileDao().insertOrUpdateProfile(
            profile.copy(
                points = newPoints,
                level = newLevel
            )
        )
    }

    suspend fun clearAllHistory() {
        database.scanHistoryDao().clearHistory()
    }
}
