package site.elahady.alkaukaba.utils.prayerbreakdown

import java.util.Calendar
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.tan

/**
 * Breakdown perhitungan waktu sholat untuk metode Ephemeris (Al Hasib - Alkaukaba Team).
 *
 * Rumus deklinasi & Equation of Time di sini pakai pendekatan sinusoidal sederhana
 * berbasis hari-ke-berapa-dalam-setahun, BUKAN tabel ephemeris presisi tinggi
 * (Buku Ephemeris / algoritma Jean Meeus). Ini didokumentasikan apa adanya di UI
 * ("Ephemeris Approximation") - kalau nanti ada sumber data matahari yang lebih
 * presisi, ganti bagian deklinasi & Equation of Time di sini saja, struktur
 * breakdown-nya tidak perlu berubah.
 */
object EphemerisPrayerCalculator : PrayerCalculationBreakdownProvider {

    override fun breakdown(lat: Double, lng: Double, timeZoneHour: Double): List<PrayerBreakdownSection> {
        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)

        val bRad = Math.toRadians((360.0 / 365.0) * (dayOfYear - 81))
        val eot = 9.87 * sin(2 * bRad) - 7.53 * cos(bRad) - 1.5 * sin(bRad)
        val declination = 23.45 * sin(bRad)

        val dataMatahari = listOf(
            PrayerBreakdownRow("Lintang (φ)", "$lat°"),
            PrayerBreakdownRow("Bujur (λ)", "$lng°"),
            PrayerBreakdownRow("Deklinasi Matahari (δ)", "${"%.4f".format(declination)}°"),
            PrayerBreakdownRow("Equation of Time (e)", "${"%.4f".format(eot)} menit")
        )

        val dzuhurBase = 12 + timeZoneHour - (lng / 15.0) - (eot / 60.0)

        val sections = mutableListOf<PrayerBreakdownSection>()

        sections += PrayerBreakdownSection(
            prayerLabel = "Dzuhur",
            resultTime = convertDecToTime(dzuhurBase),
            rows = dataMatahari + PrayerBreakdownRow("Rumus", "12 + TZ - (λ/15) - (e/60)")
        )

        val altAsharDeg = Math.toDegrees(atan(1.0 / (1.0 + tan(Math.toRadians(abs(lat - declination))))))
        val asharTime = calculateHourAngle(lat, declination, altAsharDeg, dzuhurBase, isAfternoon = true)
        sections += PrayerBreakdownSection(
            prayerLabel = "Ashar",
            resultTime = convertDecToTime(asharTime),
            rows = dataMatahari + PrayerBreakdownRow("Sudut elevasi matahari", "${"%.2f".format(altAsharDeg)}°")
        )

        val maghribAngle = -0.833
        val maghribTime = calculateHourAngle(lat, declination, maghribAngle, dzuhurBase, isAfternoon = true)
        sections += PrayerBreakdownSection(
            prayerLabel = "Maghrib",
            resultTime = convertDecToTime(maghribTime),
            rows = dataMatahari + PrayerBreakdownRow("Sudut terbenam (refraksi)", "$maghribAngle°")
        )

        val isyaAngle = -18.0
        val isyaTime = calculateHourAngle(lat, declination, isyaAngle, dzuhurBase, isAfternoon = true)
        sections += PrayerBreakdownSection(
            prayerLabel = "Isya",
            resultTime = convertDecToTime(isyaTime),
            rows = dataMatahari + PrayerBreakdownRow("Sudut depresi matahari", "$isyaAngle°")
        )

        val subuhAngle = -20.0
        val subuhTime = calculateHourAngle(lat, declination, subuhAngle, dzuhurBase, isAfternoon = false)
        sections += PrayerBreakdownSection(
            prayerLabel = "Subuh",
            resultTime = convertDecToTime(subuhTime),
            rows = dataMatahari + PrayerBreakdownRow("Sudut fajar", "$subuhAngle°")
        )

        val imsakDec = subuhTime - (10.0 / 60.0)
        sections += PrayerBreakdownSection(
            prayerLabel = "Imsak",
            resultTime = convertDecToTime(imsakDec),
            rows = dataMatahari + PrayerBreakdownRow("Rumus", "Waktu Subuh - 10 menit ikhtiyat")
        )

        return sections
    }

    private fun calculateHourAngle(lat: Double, dec: Double, altitude: Double, transit: Double, isAfternoon: Boolean): Double {
        val latRad = Math.toRadians(lat)
        val decRad = Math.toRadians(dec)
        val altRad = Math.toRadians(altitude)

        val cosH = (sin(altRad) - sin(latRad) * sin(decRad)) / (cos(latRad) * cos(decRad))
        if (cosH < -1 || cosH > 1) return 0.0

        val hourDiff = Math.toDegrees(acos(cosH)) / 15.0
        return if (isAfternoon) transit + hourDiff else transit - hourDiff
    }

    private fun convertDecToTime(decimalTime: Double): String {
        val hours = floor(decimalTime).toInt()
        val minutes = round((decimalTime - hours) * 60).toInt()

        return if (minutes == 60) {
            "%02d:00".format(hours + 1)
        } else {
            "%02d:%02d".format(hours, minutes)
        }
    }
}
