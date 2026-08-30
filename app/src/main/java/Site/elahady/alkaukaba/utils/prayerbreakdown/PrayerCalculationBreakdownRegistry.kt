package site.elahady.alkaukaba.utils.prayerbreakdown

import site.elahady.alkaukaba.utils.PrayerCalculationMethods

/**
 * Daftar metode waktu sholat yang punya breakdown detail perhitungan. Cuma
 * Ephemeris hari ini. Untuk menambah metode lain nanti, cukup tambah satu
 * entry di map ini (dan buat provider-nya) - ViewModel/Activity tidak perlu
 * diubah karena mereka cuma bertanya "ada breakdown untuk method ini atau tidak".
 */
object PrayerCalculationBreakdownRegistry {
    private val providers: Map<Int, PrayerCalculationBreakdownProvider> = mapOf(
        PrayerCalculationMethods.EPHEMERIS_ID to EphemerisPrayerCalculator
    )

    fun providerFor(methodId: Int): PrayerCalculationBreakdownProvider? = providers[methodId]
}
