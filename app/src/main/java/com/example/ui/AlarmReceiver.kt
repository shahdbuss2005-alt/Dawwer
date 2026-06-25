package com.example.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.MainActivity

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        NotificationHelper.createNotificationChannel(context)
        val isPeakHour = intent?.getBooleanExtra("is_peak_hour", false) ?: false

        if (isPeakHour) {
            val prefs = context.getSharedPreferences("eco_reminders_prefs", Context.MODE_PRIVATE)
            val currentLang = prefs.getString("current_language", "ar") ?: "ar"
            val isAr = currentLang == "ar"

            val title = if (isAr) "⏰ ساعة الذروة للتدوير الذكي!" else "⏰ Smart Peak Recycling Hour!"
            val message = if (isAr) {
                "ساعة ذروة تدوير العبوات الآن! انقر للتحدث مع مساعد الذكاء الاصطناعي للحصول على استراتيجية تدوير ذكية 🎙️♻️"
            } else {
                "Peak packaging recycling hour! Tap to consult your AI voice assistant for a smart strategy 🎙️♻️"
            }

            val triggerIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("trigger_voice_assistant", true)
            }

            NotificationHelper.showNotification(
                context = context,
                title = title,
                message = message,
                notificationId = 1003,
                customIntent = triggerIntent
            )
        } else {
            NotificationHelper.showNotification(
                context = context,
                title = "فرصتك اليوم لإنقاذ الكوكب 🌱",
                message = "حان الوقت لإعادة تدوير مخلفاتك اليومية! افتح تطبيق دَوِّر وامسح المواد الذكية لربح نقاط XP فورية ♻️",
                notificationId = 1002
            )
        }
    }
}
