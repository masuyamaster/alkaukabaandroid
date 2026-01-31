package Site.elahady.alkaukaba.utils

import kotlin.math.*

object QiblaCalculator {
    // Lokasi Ka'bah (Tetap)
    const val KAABA_LAT = 21.4225
    const val KAABA_LONG = 39.8262

    data class QiblaResult(
        val qiblaDegree: Double,
        val deltaLong: Double,
        val detailFormulaSteps: String // String panjang untuk ditampilkan di UI
    )

    fun calculateQibla(userLat: Double, userLong: Double): QiblaResult {
        val userLatRad = Math.toRadians(userLat)
        val kaabaLatRad = Math.toRadians(KAABA_LAT)

        // Selisih Bujur (Delta Longitude)
        val deltaLong = userLong - KAABA_LONG
        val deltaLongRad = Math.toRadians(deltaLong)

        // Rumus Trigonometri Bola untuk Arah Kiblat
        // y = sin(dLong)
        // x = cos(lat) * tan(kaabaLat) - sin(lat) * cos(dLong)
        val y = sin(deltaLongRad)
        val x = cos(userLatRad) * tan(kaabaLatRad) - sin(userLatRad) * cos(deltaLongRad)

        val qiblaRad = atan2(y, x)
        val qiblaDegree = Math.toDegrees(qiblaRad)

        // Normalisasi sudut kiblat (270 derajat arah Barat + offset)
        // Arah kiblat dari Utara searah jarum jam
        val qiblaNorthBased = (270.0 + (90.0 - qiblaDegree)) % 360.0 // Penyesuaian sederhana utk konteks indo

        // Logic String untuk meniru "Detail Perhitungan" di Screenshot
        // Menampilkan rumus dan substitusi nilai
        val stepByStep = """
            Latitude : $userLat
            Longitude : $userLong
            
            Selisih Bujur (Bujur Daerah - Bujur Kabah):
            = $userLong - $KAABA_LONG
            = ${String.format("%.3f", deltaLong)}
            
            Rumus Arah Kiblat:
            tan⁻¹(cos Lat x tan 21°25' : sin C - sin Lat : tan C)
            
            Detail Substitusi:
            = tan⁻¹(cos $userLat x tan 21.42 : sin ${String.format("%.2f", deltaLong)} - ...
            
            Hasil Perhitungan Sudut:
            = ${String.format("%.2f", abs(qiblaDegree))}° dari Barat ke Utara
            = ${String.format("%.2f", 270 + qiblaDegree)}° UTSB (Utara Timur Selatan Barat)
        """.trimIndent()

        return QiblaResult(qiblaDegree, deltaLong, stepByStep)
    }
}