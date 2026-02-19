package Site.elahady.alkaukaba.utils

import java.util.Calendar
import kotlin.math.*

object PrayerTextCalculator {

    fun generatePrayerDetails(lat: Double, lng: Double, timeZone: Double = 7.0): String {
        val sb = StringBuilder()
        val cal = Calendar.getInstance()
        val dayOfYear = cal.get(Calendar.DAY_OF_YEAR)

        // 1. Variabel Astronomi Dasar (Approximation)
        // B = 360/365 * (d - 81)
        val b = (360.0 / 365.0) * (dayOfYear - 81)
        val bRad = Math.toRadians(b)

        // Equation of Time (EoT) - Menit
        val eot = 9.87 * sin(2 * bRad) - 7.53 * cos(bRad) - 1.5 * sin(bRad)

        // Deklinasi Matahari (Declination) - Derajat
        val declination = 23.45 * sin(Math.toRadians(360.0 / 365.0 * (dayOfYear - 81)))

        sb.append("DATA ASTRONOMI\n")
        sb.append("Latitude (φ): $lat\n")
        sb.append("Longitude (λ): $lng\n")
        sb.append("Hari ke (d): $dayOfYear\n")
        sb.append("Deklinasi Matahari (δ): ${String.format("%.4f", declination)}°\n")
        sb.append("Equation of Time (EoT): ${String.format("%.4f", eot)} menit\n\n")

        sb.append("=== RUMUS PERHITUNGAN ===\n")
        sb.append("Rumus Dasar (Waktu Sudut):\n")
        sb.append("T = Z - Lng/15 - EoT/60 + 1/15 * cos⁻¹(-tan(φ) * tan(δ) + sin(α) / (cos(φ) * cos(δ)))\n\n")

        // 2. Hitung Per Waktu

        // --- DZUHUR ---
        // Rumus: 12 + TimeZone - Long/15 - EoT/60
        val dzuhurBase = 12 + timeZone - (lng / 15.0) - (eot / 60.0)
        sb.append("1. DZUHUR (Transit Matahari)\n")
        sb.append("   = 12 + $timeZone - ($lng/15) - ($eot/60)\n")
        sb.append("   = ${convertDecToTime(dzuhurBase)} WIB\n\n")

        // --- ASHAR ---
        // Sudut Ashar (Shadow Ratio = 1 + tan(Lat - Declination))
        val altAshar = atan(1.0 / (1.0 + tan(Math.toRadians(abs(lat - declination)))))
        val altAsharDeg = Math.toDegrees(altAshar)
        val asharTime = calculateHourAngle(lat, declination, altAsharDeg, dzuhurBase, isAfternoon = true)
        sb.append("2. ASHAR\n")
        sb.append("   Sudut Elevasi Matahari: ${String.format("%.2f", altAsharDeg)}°\n")
        sb.append("   Hasil: ${convertDecToTime(asharTime)} WIB\n\n")

        // --- MAGHRIB ---
        // Sudut matahari terbenam (biasanya -0.833 derajat untuk refraksi)
        val maghribAngle = -0.833
        val maghribTime = calculateHourAngle(lat, declination, maghribAngle, dzuhurBase, isAfternoon = true)
        sb.append("3. MAGHRIB\n")
        sb.append("   Sudut Terbenam (Sunset): $maghribAngle°\n")
        sb.append("   Hasil: ${convertDecToTime(maghribTime)} WIB\n\n")

        // --- ISYA ---
        // Kemenag menggunakan sudut -18 derajat
        val isyaAngle = -18.0
        val isyaTime = calculateHourAngle(lat, declination, isyaAngle, dzuhurBase, isAfternoon = true)
        sb.append("4. ISYA\n")
        sb.append("   Sudut Matahari (Depression): $isyaAngle°\n")
        sb.append("   Hasil: ${convertDecToTime(isyaTime)} WIB\n\n")

        // --- SUBUH ---
        // Kemenag menggunakan sudut -20 derajat
        val subuhAngle = -20.0
        val subuhTime = calculateHourAngle(lat, declination, subuhAngle, dzuhurBase, isAfternoon = false)
        sb.append("5. SUBUH\n")
        sb.append("   Sudut Fajar (Dawn): $subuhAngle°\n")
        sb.append("   Hasil: ${convertDecToTime(subuhTime)} WIB\n\n")

        // --- IMSAK ---
        // Biasanya 10 menit sebelum Subuh
        val imsakDec = subuhTime - (10.0 / 60.0)
        sb.append("6. IMSAK\n")
        sb.append("   = Waktu Subuh - 10 Menit ikhtiyat\n")
        sb.append("   Hasil: ${convertDecToTime(imsakDec)} WIB\n")

        return sb.toString()
    }

    private fun calculateHourAngle(lat: Double, dec: Double, altitude: Double, transit: Double, isAfternoon: Boolean): Double {
        val latRad = Math.toRadians(lat)
        val decRad = Math.toRadians(dec)
        val altRad = Math.toRadians(altitude)

        val cosH = (sin(altRad) - sin(latRad) * sin(decRad)) / (cos(latRad) * cos(decRad))

        // Safety check range -1 to 1
        if (cosH < -1 || cosH > 1) return 0.0

        val hourAngleRad = acos(cosH)
        val hourAngleDeg = Math.toDegrees(hourAngleRad)
        val hourDiff = hourAngleDeg / 15.0

        return if (isAfternoon) transit + hourDiff else transit - hourDiff
    }

    private fun convertDecToTime(decimalTime: Double): String {
        val hours = floor(decimalTime).toInt()
        val minutes = round((decimalTime - hours) * 60).toInt()

        val hStr = if (hours < 10) "0$hours" else "$hours"
        val mStr = if (minutes < 10) "0$minutes" else "$minutes"

        // Handle overflow 60 menit
        return if (minutes == 60) {
            val hPlus = hours + 1
            val hPlusStr = if (hPlus < 10) "0$hPlus" else "$hPlus"
            "$hPlusStr:00"
        } else {
            "$hStr:$mStr"
        }
    }
}