package site.elahady.alkaukaba.notifikasi

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import site.elahady.alkaukaba.api.TimingPrayers
import java.util.Calendar

/** Pasang alarm exact untuk 5 waktu sholat wajib berdasarkan [TimingPrayers] hari ini. */
object AdzanScheduler {

    const val EXTRA_PRAYER_NAME = "prayer_name"

    // requestCode PendingIntent per waktu sholat, supaya alarm lama otomatis ter-replace
    // (bukan menumpuk) tiap kali dijadwalkan ulang.
    private val PRAYER_REQUEST_CODES = mapOf(
        "Subuh" to 4101,
        "Dzuhur" to 4102,
        "Ashar" to 4103,
        "Maghrib" to 4104,
        "Isya" to 4105
    )

    fun scheduleFromTimings(context: Context, timings: TimingPrayers) {
        val prayers = linkedMapOf(
            "Subuh" to timings.subuh,
            "Dzuhur" to timings.dzuhur,
            "Ashar" to timings.ashar,
            "Maghrib" to timings.maghrib,
            "Isya" to timings.isya
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val now = Calendar.getInstance()

        prayers.forEach { (name, hhmm) ->
            val time = parseToCalendarToday(hhmm) ?: return@forEach
            // Waktu yang sudah lewat hari ini dilewati - refresh berikutnya (dini hari) akan
            // menjadwalkan lagi untuk besok.
            if (time.before(now)) return@forEach
            scheduleAlarm(context, alarmManager, name, time)
        }
    }

    private fun scheduleAlarm(context: Context, alarmManager: AlarmManager, prayerName: String, time: Calendar) {
        val requestCode = PRAYER_REQUEST_CODES[prayerName] ?: return
        val intent = Intent(context, AdzanAlarmReceiver::class.java).apply {
            putExtra(EXTRA_PRAYER_NAME, prayerName)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            // Izin "Alarm & pengingat" belum diberikan user - fallback ke alarm tidak-exact
            // supaya notifikasi tetap muncul (mundur beberapa menit itu wajar untuk kasus ini).
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time.timeInMillis, pendingIntent)
            return
        }
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time.timeInMillis, pendingIntent)
    }

    /** Parse "HH:mm" (format Aladhan API) jadi Calendar hari ini di timezone device. */
    private fun parseToCalendarToday(hhmm: String): Calendar? {
        val parts = hhmm.trim().split(":")
        if (parts.size < 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }
}
