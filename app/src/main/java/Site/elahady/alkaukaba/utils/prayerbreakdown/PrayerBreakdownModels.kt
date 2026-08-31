package site.elahady.alkaukaba.utils.prayerbreakdown

data class PrayerBreakdownRow(val label: String, val value: String)

data class PrayerBreakdownSection(
    val prayerLabel: String,
    val resultTime: String,
    val rows: List<PrayerBreakdownRow>
)

/**
 * Titik ekstensi: metode perhitungan waktu sholat manapun BOLEH (tidak wajib) punya
 * breakdown detail seperti ini. Hari ini cuma Ephemeris yang mengimplementasikan
 * (lihat [EphemerisPrayerCalculator]) - metode dari Aladhan API tidak, karena
 * angkanya memang murni dari response API, bukan hasil hitung lokal.
 *
 * Untuk menambah breakdown ke metode lain nanti: implement interface ini, lalu
 * daftarkan di [PrayerCalculationBreakdownRegistry]. Tidak perlu ubah
 * ViewModel/Activity sama sekali.
 */
interface PrayerCalculationBreakdownProvider {
    fun breakdown(lat: Double, lng: Double, timeZoneHour: Double): List<PrayerBreakdownSection>
}
