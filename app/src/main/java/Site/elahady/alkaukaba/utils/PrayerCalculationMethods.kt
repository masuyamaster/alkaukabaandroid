package site.elahady.alkaukaba.utils

data class PrayerCalculationMethod(
    val id: Int,
    val displayName: String,
    val subtitle: String
)

object PrayerCalculationMethods {
    const val CUSTOM_ID = 99

    /** Kriteria hisab manual milik Al-Hasib (belum ada engine perhitungannya di app ini). */
    const val EPHEMERIS_ID = 1000

    val PRESETS = listOf(
        PrayerCalculationMethod(EPHEMERIS_ID, "Ephemeris (Al Hasib - Alkaukaba Team)", "Fajr 20°, Isya 18°"),
        PrayerCalculationMethod(3, "Muslim World League (Aladhan API)", "Fajr 18°, Isya 17°"),
        PrayerCalculationMethod(2, "Islamic Society of North America - ISNA (Aladhan API)", "Fajr 15°, Isya 15°"),
        PrayerCalculationMethod(4, "Umm al-Qura, Makkah (Aladhan API)", "Fajr 18,5°, Isya 90 menit setelah Maghrib"),
        PrayerCalculationMethod(5, "Egyptian General Authority (Aladhan API)", "Fajr 19,5°, Isya 17,5°"),
        PrayerCalculationMethod(1, "University of Islamic Sciences, Karachi (Aladhan API)", "Fajr 18°, Isya 18°"),
        PrayerCalculationMethod(CUSTOM_ID, "Custom (Aladhan API)", "Atur sudut Fajr & Isya sendiri")
    )

    fun findById(id: Int): PrayerCalculationMethod =
        PRESETS.firstOrNull { it.id == id } ?: PRESETS.first()
}
