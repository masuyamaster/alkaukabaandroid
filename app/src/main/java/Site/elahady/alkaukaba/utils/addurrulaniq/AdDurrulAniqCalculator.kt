package site.elahady.alkaukaba.utils.addurrulaniq

import site.elahady.alkaukaba.model.HilalInput
import site.elahady.alkaukaba.model.HilalResult
import site.elahady.alkaukaba.utils.prayerbreakdown.PrayerBreakdownRow
import site.elahady.alkaukaba.utils.prayerbreakdown.PrayerBreakdownSection
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.floor

/**
 * Metode kedua hisab awal bulan: "Ad-Durrul Aniq fi Ma'rifatil Hilal wal
 * Kusufain bit-Tadqiq" (Ahmad Ghozali Muhammad Fathulloh) -- ALTERNATIF dari
 * `EphemerisCalculator` (Astronomy Engine), bukan pengganti. User memilih
 * salah satu di Konfigurasi.
 *
 * Menyatukan [AdDurrulAniqIjtimaCalculator] (Ijtima', pakai tabel kitab
 * Alamat/Hishshatul-Ardh/Khashshah/Markaz yang tervalidasi 4/4 contoh) dan
 * [AdDurrulAniqHilalCalculator] (Ghurub+Hilal, pakai rumus Meeus yang
 * tervalidasi persis menggantikan tabel gerak Matahari-Bulan) jadi satu
 * [HilalResult] yang sama bentuknya dengan output EphemerisCalculator,
 * supaya UI (AwalBulanActivity, HilalViewModel) bisa pakai keduanya
 * bergantian tanpa perubahan.
 */
object AdDurrulAniqCalculator {

    private const val KRITERIA_TINGGI_MIN = 3.0
    private const val KRITERIA_ELONGASI_MIN = 6.4

    fun calculate(input: HilalInput): HilalResult {
        val phi = input.latitude
        val lambda = input.longitude
        val tt = input.heightMeters

        val ijtima = AdDurrulAniqIjtimaCalculator.findNearestFuture(System.currentTimeMillis())
        val ijtimaMillis = with(AdDurrulAniqIjtimaCalculator) { ijtima.toUtcMillis() }

        val ghurub = cariGhurubSetelah(ijtimaMillis, phi, lambda, tt)

        val dalilGhurub = AdDurrulAniqHilalCalculator.dalilDariJulianDay(ghurub.julianDay)
        val matahari = AdDurrulAniqHilalCalculator.hitungMatahari(dalilGhurub, phi, tt)
        val gmst = AdDurrulAniqHilalCalculator.gmstDerajat(ghurub.julianDay)
        val bulan = AdDurrulAniqHilalCalculator.hitungBulan(dalilGhurub, matahari, phi, lambda, gmst, tt)

        val tinggiHilal = bulan.tinggiTopocentric
        val elongasi = bulan.elongasiTopocentric
        val memenuhiKriteria = tinggiHilal >= KRITERIA_TINGGI_MIN && elongasi >= KRITERIA_ELONGASI_MIN
        val statusBadge = if (memenuhiKriteria) "Hilal Mungkin Terlihat" else "Belum Memenuhi Kriteria — Istikmal"

        val ghurubDate = Date(ghurub.utcMillis)
        val bulanHijriyahLabel = site.elahady.alkaukaba.utils.HijriDateUtil.nextMonthLabel(
            Calendar.getInstance().apply { time = ghurubDate }
        )

        val sections = buildBreakdownSections(
            input = input,
            ijtimaMillis = ijtimaMillis,
            ghurubMillis = ghurub.utcMillis,
            matahari = matahari,
            bulan = bulan,
            statusBadge = statusBadge
        )

        return HilalResult(
            bulanHijriyahLabel = bulanHijriyahLabel,
            tanggalGhurubLabel = formatLocalDate(ghurubDate),
            statusBadge = statusBadge,
            hilalMemenuhiKriteria = memenuhiKriteria,
            tinggiHilal = tinggiHilal,
            elongasi = elongasi,
            mukutsMenit = bulan.mukutsMenit,
            azimuthHilal = bulan.azimuth,
            azimuthMatahari = matahari.azimuth,
            tinggiMatahari = matahari.tinggi,
            illumFraction = bulan.illuminationFraction,
            ijtimaTime = formatLocalDateTime(Date(ijtimaMillis)),
            ghurubTime = formatLocalDateTime(ghurubDate),
            breakdownSections = sections,
            calculationLog = flattenToLog(sections)
        )
    }

    private data class GhurubMoment(val utcMillis: Long, val julianDay: Double)

    /**
     * Cari ghurub pertama SETELAH [afterMillis] (ijtima'): mulai dari hari
     * kalender (UT) yang sama dengan ijtima', hitung ghurub hari itu; kalau
     * ternyata masih sebelum ijtima' (ijtima' terjadi setelah ghurub hari
     * itu), maju ke ghurub hari berikutnya -- sama seperti pola
     * EphemerisCalculator. Setiap hari dihitung 2 pass (dalil dievaluasi
     * ulang di JD ghurub hasil pass 1) supaya presisi, karena dm/e Matahari
     * brubah sangat lambat dalam hitungan jam.
     */
    private fun cariGhurubSetelah(afterMillis: Long, phi: Double, lambda: Double, tt: Double): GhurubMoment {
        val cal = GregorianCalendar(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = afterMillis
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        var guard = 0
        while (guard < 3) {
            val y = cal.get(Calendar.YEAR)
            val m = cal.get(Calendar.MONTH) + 1
            val d = cal.get(Calendar.DAY_OF_MONTH)

            var jdRef = AdDurrulAniqHilalCalculator.julianDay(y, m, d, 12.0)
            var utGhurub = 12.0
            repeat(2) {
                val dalil = AdDurrulAniqHilalCalculator.dalilDariJulianDay(jdRef)
                val matahari = AdDurrulAniqHilalCalculator.hitungMatahari(dalil, phi, tt)
                utGhurub = matahari.ghurubLmtJam - lambda / 15.0
                jdRef = AdDurrulAniqHilalCalculator.julianDay(y, m, d, utGhurub)
            }
            val ghurubMillis = utcMillisFrom(y, m, d, utGhurub)
            if (ghurubMillis > afterMillis) {
                return GhurubMoment(ghurubMillis, jdRef)
            }
            cal.add(Calendar.DAY_OF_MONTH, 1)
            guard++
        }
        error("Tidak bisa menemukan ghurub dalam 3 hari setelah ijtima' -- periksa lokasi/lintang ekstrem")
    }

    private fun utcMillisFrom(year: Int, month: Int, day: Int, utJamDesimal: Double): Long {
        val cal = GregorianCalendar(TimeZone.getTimeZone("UTC"))
        cal.clear()
        cal.set(year, month - 1, day)
        cal.timeInMillis += (utJamDesimal * 3600.0 * 1000.0).toLong()
        return cal.timeInMillis
    }

    private fun buildBreakdownSections(
        input: HilalInput,
        ijtimaMillis: Long,
        ghurubMillis: Long,
        matahari: AdDurrulAniqHilalCalculator.MatahariHasil,
        bulan: AdDurrulAniqHilalCalculator.BulanHasil,
        statusBadge: String
    ): List<PrayerBreakdownSection> = listOf(
        PrayerBreakdownSection(
            prayerLabel = "Markaz",
            resultTime = "",
            rows = listOf(
                PrayerBreakdownRow("Lintang", decimalToDMS(input.latitude)),
                PrayerBreakdownRow("Bujur", decimalToDMS(input.longitude)),
                PrayerBreakdownRow("Ketinggian", "${"%.1f".format(input.heightMeters)} m"),
                PrayerBreakdownRow("Metode", "Ad-Durrul Aniq (Ahmad Ghozali Muhammad Fathulloh)")
            )
        ),
        PrayerBreakdownSection(
            prayerLabel = "Ijtima' (Konjungsi)",
            resultTime = formatLocalDateTime(Date(ijtimaMillis)),
            rows = emptyList()
        ),
        PrayerBreakdownSection(
            prayerLabel = "Data Matahari saat Ghurub",
            resultTime = formatLocalDateTime(Date(ghurubMillis)),
            rows = listOf(
                PrayerBreakdownRow("Deklinasi", decimalToDMS(matahari.deklinasi)),
                PrayerBreakdownRow("Asensiorekta", "${"%.4f".format(matahari.asensiorekta / 15.0)} jam"),
                PrayerBreakdownRow("Azimuth", decimalToDMS(matahari.azimuth)),
                PrayerBreakdownRow("Tinggi", decimalToDMS(matahari.tinggi))
            )
        ),
        PrayerBreakdownSection(
            prayerLabel = "Data Bulan saat Ghurub",
            resultTime = decimalToDMS(bulan.tinggiTopocentric),
            rows = listOf(
                PrayerBreakdownRow("Deklinasi", decimalToDMS(bulan.deklinasi)),
                PrayerBreakdownRow("Asensiorekta", "${"%.4f".format(bulan.asensiorekta / 15.0)} jam"),
                PrayerBreakdownRow("Azimuth", decimalToDMS(bulan.azimuth)),
                PrayerBreakdownRow("Tinggi Mar'i (topo)", decimalToDMS(bulan.tinggiTopocentric)),
                PrayerBreakdownRow("Elongasi", decimalToDMS(bulan.elongasiTopocentric)),
                PrayerBreakdownRow("Fraksi Iluminasi", "${"%.2f".format(bulan.illuminationFraction * 100.0)}%"),
                PrayerBreakdownRow("Lama Hilal di Ufuk (Mukuts)", "${"%.1f".format(bulan.mukutsMenit)} menit"),
                PrayerBreakdownRow("Jarak Bumi-Bulan", "${"%.0f".format(bulan.jarakKm)} km")
            )
        ),
        PrayerBreakdownSection(
            prayerLabel = "Kesimpulan Kriteria (Neo-MABIMS)",
            resultTime = statusBadge,
            rows = listOf(
                PrayerBreakdownRow("Kriteria", "Tinggi >= 3°, Elongasi >= 6.4°"),
                PrayerBreakdownRow("Tinggi Hilal", decimalToDMS(bulan.tinggiTopocentric)),
                PrayerBreakdownRow("Elongasi", decimalToDMS(bulan.elongasiTopocentric))
            )
        )
    )

    private fun flattenToLog(sections: List<PrayerBreakdownSection>): String {
        val sb = StringBuilder()
        sb.append("PERHITUNGAN AWAL BULAN HIJRIYAH\n")
        sb.append("METODE AD-DURRUL ANIQ (Ahmad Ghozali Muhammad Fathulloh)\n\n")
        sections.forEach { section ->
            sb.append(section.prayerLabel.uppercase(Locale.getDefault()))
            if (section.resultTime.isNotEmpty()) sb.append(" : ${section.resultTime}")
            sb.append("\n")
            section.rows.forEach { row -> sb.append("   ${row.label} : ${row.value}\n") }
            sb.append("\n")
        }
        return sb.toString()
    }

    private fun formatLocalDateTime(date: Date): String {
        val sdf = SimpleDateFormat("dd MMMM yyyy, HH:mm:ss", Locale("id", "ID"))
        return sdf.format(date)
    }

    private fun formatLocalDate(date: Date): String {
        val sdf = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
        return sdf.format(date)
    }

    private fun decimalToDMS(value: Double): String {
        val sign = if (value < 0) "-" else ""
        val abs = abs(value)
        val d = floor(abs).toInt()
        val minVal = floor((abs - d) * 60).toInt()
        val s = ((abs - d) * 60 - minVal) * 60
        return String.format(Locale.US, "%s%d° %d' %.2f\"", sign, d, minVal, s)
    }
}
