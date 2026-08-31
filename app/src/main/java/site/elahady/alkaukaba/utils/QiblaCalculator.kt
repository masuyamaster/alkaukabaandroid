package site.elahady.alkaukaba.utils

import kotlin.math.*

/**
 * Breakdown perhitungan manual Arah Kiblat (Al Hasib - Alkaukaba Team), berdasarkan
 * rumus segitiga bola "tan⁻¹(cos φ x tan φ_Kabah : sin C - sin φ : tan C)".
 *
 * Ini BUKAN sumber sudut kompas yang ditampilkan di KiblatActivity - itu tetap dari
 * Aladhan API lewat KiblatRepository/KiblatViewModel. Kelas ini murni buat breakdown
 * "angka ini didapat dari mana" di dialog Detail Perhitungan, sama seperti
 * EphemerisPrayerCalculator untuk Waktu Sholat.
 */
object QiblaCalculator {
    // Lokasi Ka'bah: 21°25' LU, 39°50' BT
    const val KAABA_LAT = 21.4225
    const val KAABA_LONG = 39.8262

    data class QiblaBreakdownRow(val label: String, val value: String)

    data class QiblaBreakdownResult(
        val rows: List<QiblaBreakdownRow>,
        val utsbDegree: Double
    )

    fun calculateBreakdown(userLat: Double, userLong: Double): QiblaBreakdownResult {
        // Selisih Bujur (C) = Bujur Daerah - Bujur Ka'bah
        val c = userLong - KAABA_LONG

        val userLatRad = Math.toRadians(userLat)
        val kaabaLatRad = Math.toRadians(KAABA_LAT)
        val cRad = Math.toRadians(c)

        // Rumus: tan⁻¹(cos φ x tan φ_Kabah : sin C - sin φ : tan C)
        val thetaBaratUtara = Math.toDegrees(
            atan((cos(userLatRad) * tan(kaabaLatRad)) / sin(cRad) - sin(userLatRad) / tan(cRad))
        )
        val thetaUtaraBarat = 90 - thetaBaratUtara
        val utsb = (270 + thetaBaratUtara + 360) % 360

        val rows = listOf(
            QiblaBreakdownRow("Lintang Ka'bah (φ)", "${toDms(KAABA_LAT)} LU"),
            QiblaBreakdownRow("Bujur Ka'bah (λ)", "${toDms(KAABA_LONG)} BT"),
            QiblaBreakdownRow("Lintang lokasi (φ)", "${toDms(abs(userLat))} ${if (userLat < 0) "LS" else "LU"}"),
            QiblaBreakdownRow("Bujur lokasi (λ)", "${toDms(abs(userLong))} ${if (userLong < 0) "BB" else "BT"}"),
            QiblaBreakdownRow("Selisih Bujur (C)", toDms(abs(c))),
            QiblaBreakdownRow("Rumus", "tan⁻¹(cos φ × tan φ_Kabah ÷ sin C − sin φ ÷ tan C)"),
            QiblaBreakdownRow("Hasil (Barat ke Utara)", "${toDms(abs(thetaBaratUtara))} (B-U)"),
            QiblaBreakdownRow("Hasil (Utara ke Barat)", "${toDms(abs(thetaUtaraBarat))} (U-B)"),
            QiblaBreakdownRow("Hasil akhir (UTSB, dari Utara)", "${toDms(utsb)}")
        )

        return QiblaBreakdownResult(rows, utsb)
    }

    private fun toDms(decimalDegrees: Double): String {
        val degrees = floor(decimalDegrees).toInt()
        val minutesDecimal = (decimalDegrees - degrees) * 60
        val minutes = floor(minutesDecimal).toInt()
        val seconds = ((minutesDecimal - minutes) * 60).roundToInt()
        return "$degrees°$minutes'$seconds\""
    }
}
