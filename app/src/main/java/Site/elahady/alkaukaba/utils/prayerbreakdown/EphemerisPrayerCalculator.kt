package site.elahady.alkaukaba.utils.prayerbreakdown

import io.github.cosinekitty.astronomy.Aberration
import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.EquatorEpoch
import io.github.cosinekitty.astronomy.Observer
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.equator
import io.github.cosinekitty.astronomy.searchHourAngle
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
 * Breakdown perhitungan waktu sholat untuk metode Ephemeris (Al Hasib - Alkaukaba Team),
 * mengikuti prosedur hisab klasik "Perhitungan Waktu Sholat" (M. Khoirul Anam) - lihat
 * docs/features/rumus-hisab-ephemeris.md untuk rumus & contoh perhitungan manual lengkap
 * yang jadi rujukan implementasi ini.
 *
 * Deklinasi matahari (δ) dan Equation of Time (e) TIDAK lagi didekati lewat rumus
 * sinusoidal (seperti versi sebelumnya) - keduanya diturunkan dari posisi matahari
 * riil hasil "Astronomy Engine" (utils/Astronomy.kt, sudah dipakai juga oleh
 * EphemerisCalculator untuk Awal Bulan), lewat waktu transit/istiwa' yang dicari
 * dengan searchHourAngle(Sun, 0°). Rumus gabungan per waktu sholat (Kwd, h° per
 * waktu, sudut waktu matahari t, ikhtiyat) tetap persis mengikuti buku rujukan di atas.
 */
object EphemerisPrayerCalculator : PrayerCalculationBreakdownProvider {

    private const val IKHTIYAT_HOURS = 2.0 / 60.0

    override fun breakdown(lat: Double, lng: Double, timeZoneHour: Double): List<PrayerBreakdownSection> {
        val observer = Observer(lat, lng, 0.0)
        val startOfDay = localMidnight()

        // Transit matahari (istiwa') hari ini - sumber δ & e yang akurat, menggantikan
        // pendekatan sinusoidal versi sebelumnya.
        val transit = searchHourAngle(Body.Sun, observer, 0.0, startOfDay, +1).time
        val declination = equator(Body.Sun, transit, observer, EquatorEpoch.OfDate, Aberration.Corrected).dec

        val zoneMeridian = timeZoneHour * 15.0
        val kwdHours = (zoneMeridian - lng) / 15.0
        val transitZoneHours = utDecimalHours(transit) + timeZoneHour
        val meanNoonUtHours = 12.0 - lng / 15.0
        val eMinutes = (meanNoonUtHours - utDecimalHours(transit)) * 60.0

        val dataMatahari = listOf(
            PrayerBreakdownRow("Lintang (φ)", degreesToDms(lat)),
            PrayerBreakdownRow("Bujur (λ)", degreesToDms(lng)),
            PrayerBreakdownRow("Deklinasi Matahari (δ)", degreesToDms(declination)),
            PrayerBreakdownRow("Equation of Time (e)", "%+.2f menit".format(eMinutes)),
            PrayerBreakdownRow("Koreksi Waktu Daerah (Kwd)", hoursToClock(kwdHours))
        )

        fun section(
            label: String,
            altitudeDeg: Double?,
            altitudeLabel: String,
            beforeTransit: Boolean,
            ikhtiyatHours: Double,
            rumus: String,
            extraNote: String? = null
        ): PrayerBreakdownSection {
            val t = altitudeDeg?.let { hourAngleHours(lat, declination, it) }
            val time = when {
                altitudeDeg == null -> transitZoneHours + ikhtiyatHours
                t == null -> null
                beforeTransit -> transitZoneHours - t + ikhtiyatHours
                else -> transitZoneHours + t + ikhtiyatHours
            }
            val rows = dataMatahari.toMutableList()
            if (altitudeDeg != null) rows += PrayerBreakdownRow(altitudeLabel, degreesToDms(altitudeDeg))
            if (t != null) rows += PrayerBreakdownRow("Sudut Waktu Matahari (t)", hoursToClock(t))
            rows += PrayerBreakdownRow("Ikhtiyat (i)", "%+.0f detik".format(ikhtiyatHours * 3600))
            rows += PrayerBreakdownRow("Rumus", rumus)
            extraNote?.let { rows += PrayerBreakdownRow("Catatan", it) }

            return PrayerBreakdownSection(
                prayerLabel = label,
                resultTime = time?.let { convertDecToTime(it) } ?: "-",
                rows = rows
            )
        }

        return listOf(
            section(
                "Imsak", -22.0, "Tinggi Matahari (h°)", beforeTransit = true, ikhtiyatHours = 0.0,
                rumus = "12 - e - t + Kwd",
                extraNote = "Margin kehati-hatian sebelum Subuh sudah ada di sudut -22° (Subuh -20° dikurangi 2°), tidak ditambah ikhtiyat lagi"
            ),
            section(
                "Subuh", -20.0, "Tinggi Matahari (h°)", beforeTransit = true, ikhtiyatHours = IKHTIYAT_HOURS,
                rumus = "12 - e - t + Kwd + i"
            ),
            section(
                "Terbit/Syuruq", 1.0, "Tinggi Matahari (h°, dari ufuk Timur)", beforeTransit = true, ikhtiyatHours = -IKHTIYAT_HOURS,
                rumus = "12 - e - t + Kwd - i",
                extraNote = "Ikhtiyat dikurangkan (bukan ditambah) supaya waktu Terbit tidak dinyatakan lebih lambat dari kejadian sebenarnya"
            ),
            section(
                "Dhuha", 4.5, "Tinggi Matahari (h°, dari ufuk Timur)", beforeTransit = true, ikhtiyatHours = IKHTIYAT_HOURS,
                rumus = "12 - e - t + Kwd + i"
            ),
            section(
                "Dzuhur", null, "", beforeTransit = false, ikhtiyatHours = IKHTIYAT_HOURS,
                rumus = "12 - e + Kwd + i"
            ),
            section(
                "Ashar", asharAltitudeDeg(lat, declination), "Tinggi Matahari (h°)", beforeTransit = false, ikhtiyatHours = IKHTIYAT_HOURS,
                rumus = "12 - e + t + Kwd + i",
                extraNote = "h° dari Cotan h° = tan|φ - δ| + 1 (panjang bayangan = panjang benda + bayangan saat istiwa')"
            ),
            section(
                "Maghrib", -1.0, "Tinggi Matahari (h°, dari ufuk Barat)", beforeTransit = false, ikhtiyatHours = IKHTIYAT_HOURS,
                rumus = "12 - e + t + Kwd + i"
            ),
            section(
                "Isya", -18.0, "Tinggi Matahari (h°)", beforeTransit = false, ikhtiyatHours = IKHTIYAT_HOURS,
                rumus = "12 - e + t + Kwd + i"
            )
        )
    }

    /** Cotan h° = tan|φ - δ| + 1 → h° = arctan(1 / (tan|φ - δ| + 1)), lihat rumus Ashar di dokumen rujukan. */
    private fun asharAltitudeDeg(lat: Double, dec: Double): Double =
        Math.toDegrees(atan(1.0 / (1.0 + tan(Math.toRadians(abs(lat - dec))))))

    /** Cos t = -tan φ tan δ + sin h / cos φ / cos δ, dikembalikan sebagai jam (t°/15). Null kalau matahari tidak pernah mencapai ketinggian ini di lintang ini. */
    private fun hourAngleHours(lat: Double, dec: Double, altitudeDeg: Double): Double? {
        val latRad = Math.toRadians(lat)
        val decRad = Math.toRadians(dec)
        val altRad = Math.toRadians(altitudeDeg)

        val cosT = (sin(altRad) - sin(latRad) * sin(decRad)) / (cos(latRad) * cos(decRad))
        if (cosT < -1.0 || cosT > 1.0) return null

        return Math.toDegrees(acos(cosT)) / 15.0
    }

    private fun localMidnight(): Time {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return Time.fromMillisecondsSince1970(cal.timeInMillis)
    }

    private fun utDecimalHours(time: Time): Double {
        val cal = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
        cal.timeInMillis = time.toMillisecondsSince1970()
        return cal.get(Calendar.HOUR_OF_DAY) + cal.get(Calendar.MINUTE) / 60.0 + cal.get(Calendar.SECOND) / 3600.0
    }

    private fun degreesToDms(value: Double): String {
        val sign = if (value < 0) "-" else ""
        val absVal = abs(value)
        val d = floor(absVal).toInt()
        val m = floor((absVal - d) * 60).toInt()
        val s = ((absVal - d) * 60 - m) * 60
        return "%s%d° %d' %.0f\"".format(sign, d, m, s)
    }

    private fun hoursToClock(value: Double): String {
        val sign = if (value < 0) "-" else ""
        val absVal = abs(value)
        val h = floor(absVal).toInt()
        val m = floor((absVal - h) * 60).toInt()
        val s = round(((absVal - h) * 60 - m) * 60).toInt()
        return "%s%02d:%02d:%02d".format(sign, h, m, s)
    }

    private fun convertDecToTime(decimalTimeRaw: Double): String {
        var decimalTime = decimalTimeRaw % 24.0
        if (decimalTime < 0) decimalTime += 24.0

        val hours = floor(decimalTime).toInt()
        val minutes = round((decimalTime - hours) * 60).toInt()

        return if (minutes == 60) {
            "%02d:00".format((hours + 1) % 24)
        } else {
            "%02d:%02d".format(hours, minutes)
        }
    }
}
