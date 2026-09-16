package site.elahady.alkaukaba.utils

import site.elahady.alkaukaba.model.HilalInput
import site.elahady.alkaukaba.model.HilalResult
import io.github.cosinekitty.astronomy.Aberration
import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.Direction
import io.github.cosinekitty.astronomy.Equatorial
import io.github.cosinekitty.astronomy.EquatorEpoch
import io.github.cosinekitty.astronomy.Observer
import io.github.cosinekitty.astronomy.Refraction
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.Topocentric
import io.github.cosinekitty.astronomy.elongation
import io.github.cosinekitty.astronomy.equator
import io.github.cosinekitty.astronomy.horizon
import io.github.cosinekitty.astronomy.illumination
import io.github.cosinekitty.astronomy.nextMoonQuarter
import io.github.cosinekitty.astronomy.searchMoonQuarter
import io.github.cosinekitty.astronomy.searchRiseSet
import site.elahady.alkaukaba.utils.prayerbreakdown.PrayerBreakdownRow
import site.elahady.alkaukaba.utils.prayerbreakdown.PrayerBreakdownSection
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor

/**
 * Mesin hisab awal bulan real, berbasis "Astronomy Engine" (utils/Astronomy.kt,
 * io.github.cosinekitty.astronomy, MIT license) yang sudah di-vendor di repo.
 * Menggantikan versi lama yang 100% data simulasi/hardcode.
 */
object EphemerisCalculator {

    private const val KRITERIA_TINGGI_MIN = 3.0 // derajat, Neo-MABIMS
    private const val KRITERIA_ELONGASI_MIN = 6.4 // derajat, Neo-MABIMS

    fun calculate(input: HilalInput): HilalResult =
        calculate(input, findIjtima(input.monthOffset))

    /**
     * Sama seperti [calculate], tapi menerima [ijtima] yang sudah dihitung sebelumnya lewat
     * [findIjtima] alih-alih menghitung ulang dari nol -- dipakai `WorldVisibilityCalculator` yang
     * memanggil fungsi ini ratusan kali per grid dengan monthOffset yang sama: ijtima' adalah
     * peristiwa global (tidak tergantung observer), jadi menghitungnya sekali lalu dipakai ulang
     * jauh lebih murah daripada mencari ulang rantai new-moon di tiap titik grid.
     */
    fun calculate(input: HilalInput, ijtima: Time): HilalResult {
        val observer = Observer(input.latitude, input.longitude, input.heightMeters)

        // 1. Ghurub markaz: ghurub di tanggal yang sama dengan ijtima'; kalau
        // ijtima' terjadi setelah ghurub hari itu, geser ke ghurub keesokan hari.
        val ijtimaLocalMidnight = localMidnightOf(ijtima)
        var ghurub = searchRiseSet(Body.Sun, observer, Direction.Set, ijtimaLocalMidnight, 1.5)
            ?: throw IllegalStateException("Tidak bisa menghitung waktu ghurub untuk lokasi ini")
        if (ijtima > ghurub) {
            ghurub = searchRiseSet(Body.Sun, observer, Direction.Set, ghurub.addDays(0.5), 1.5)
                ?: throw IllegalStateException("Tidak bisa menghitung waktu ghurub keesokan hari")
        }

        // 2. Data Matahari saat ghurub
        val sunEq = equator(Body.Sun, ghurub, observer, EquatorEpoch.OfDate, Aberration.Corrected)
        val sunHor = horizon(ghurub, observer, sunEq.ra, sunEq.dec, Refraction.Normal)

        // 3. Data Bulan saat ghurub
        val moonEq = equator(Body.Moon, ghurub, observer, EquatorEpoch.OfDate, Aberration.Corrected)
        val moonHor = horizon(ghurub, observer, moonEq.ra, moonEq.dec, Refraction.Normal)
        val elong = elongation(Body.Moon, ghurub)
        val illum = illumination(Body.Moon, ghurub)

        // 4. Mukuts: lama hilal di atas ufuk setelah ghurub (moonset - ghurub)
        val moonset = searchRiseSet(Body.Moon, observer, Direction.Set, ghurub, 1.0)
        val mukutsMenit = moonset?.let { (it.ut - ghurub.ut) * 24.0 * 60.0 } ?: 0.0

        // 5. Kesimpulan kriteria Neo-MABIMS (tinggi >= 3 derajat DAN elongasi >= 6.4 derajat)
        val tinggiHilal = moonHor.altitude
        val elongasi = elong.elongation
        val memenuhiKriteria = tinggiHilal >= KRITERIA_TINGGI_MIN && elongasi >= KRITERIA_ELONGASI_MIN
        val statusBadge = if (memenuhiKriteria) "Hilal Mungkin Terlihat" else "Belum Memenuhi Kriteria — Istikmal"

        val bulanHijriyahLabel = HijriDateUtil.nextMonthLabel(calendarFromTime(ghurub))
        val tanggalGhurubLabel = formatLocalDate(ghurub)

        val sections = buildBreakdownSections(
            input = input,
            ijtima = ijtima,
            ghurub = ghurub,
            sunEq = sunEq,
            sunHor = sunHor,
            moonEq = moonEq,
            moonHor = moonHor,
            elongasiDeg = elongasi,
            illumFraction = illum.phaseFraction,
            mukutsMenit = mukutsMenit,
            tinggiHilal = tinggiHilal,
            statusBadge = statusBadge
        )

        return HilalResult(
            bulanHijriyahLabel = bulanHijriyahLabel,
            tanggalGhurubLabel = tanggalGhurubLabel,
            statusBadge = statusBadge,
            hilalMemenuhiKriteria = memenuhiKriteria,
            tinggiHilal = tinggiHilal,
            elongasi = elongasi,
            mukutsMenit = mukutsMenit,
            azimuthHilal = moonHor.azimuth,
            azimuthMatahari = sunHor.azimuth,
            tinggiMatahari = sunHor.altitude,
            illumFraction = illum.phaseFraction,
            ijtimaTime = formatLocalTime(ijtima),
            ghurubTime = formatLocalTime(ghurub),
            breakdownSections = sections,
            calculationLog = flattenToLog(sections)
        )
    }

    /** Breakdown 5 seksi, mirror urutan buku Ephemeris Hisab Rukyat: Markaz -> Ijtima' -> Data Matahari saat ghurub -> Data Bulan saat ghurub -> Kesimpulan. */
    private fun buildBreakdownSections(
        input: HilalInput,
        ijtima: Time,
        ghurub: Time,
        sunEq: Equatorial,
        sunHor: Topocentric,
        moonEq: Equatorial,
        moonHor: Topocentric,
        elongasiDeg: Double,
        illumFraction: Double,
        mukutsMenit: Double,
        tinggiHilal: Double,
        statusBadge: String
    ): List<PrayerBreakdownSection> = listOf(
        PrayerBreakdownSection(
            prayerLabel = "Markaz",
            resultTime = "",
            rows = listOf(
                PrayerBreakdownRow("Lintang", decimalToDMS(input.latitude)),
                PrayerBreakdownRow("Bujur", decimalToDMS(input.longitude)),
                PrayerBreakdownRow("Ketinggian", "${"%.1f".format(input.heightMeters)} m"),
                PrayerBreakdownRow("Metode", "Astronomy Engine")
            )
        ),
        PrayerBreakdownSection(
            prayerLabel = "Ijtima' (Konjungsi)",
            resultTime = formatLocalTime(ijtima),
            rows = emptyList()
        ),
        PrayerBreakdownSection(
            prayerLabel = "Data Matahari saat Ghurub",
            resultTime = formatLocalTime(ghurub),
            rows = listOf(
                PrayerBreakdownRow("Deklinasi", decimalToDMS(sunEq.dec)),
                PrayerBreakdownRow("Asensiorekta", "${"%.4f".format(sunEq.ra)} jam"),
                PrayerBreakdownRow("Azimuth", decimalToDMS(sunHor.azimuth)),
                PrayerBreakdownRow("Tinggi", decimalToDMS(sunHor.altitude))
            )
        ),
        PrayerBreakdownSection(
            prayerLabel = "Data Bulan saat Ghurub",
            resultTime = decimalToDMS(tinggiHilal),
            rows = listOf(
                PrayerBreakdownRow("Deklinasi", decimalToDMS(moonEq.dec)),
                PrayerBreakdownRow("Asensiorekta", "${"%.4f".format(moonEq.ra)} jam"),
                PrayerBreakdownRow("Azimuth", decimalToDMS(moonHor.azimuth)),
                PrayerBreakdownRow("Tinggi Mar'i (topo)", decimalToDMS(moonHor.altitude)),
                PrayerBreakdownRow("Elongasi", decimalToDMS(elongasiDeg)),
                PrayerBreakdownRow("Fraksi Iluminasi", "${"%.2f".format(illumFraction * 100.0)}%"),
                PrayerBreakdownRow("Lama Hilal di Ufuk (Mukuts)", "${"%.1f".format(mukutsMenit)} menit")
            )
        ),
        PrayerBreakdownSection(
            prayerLabel = "Kesimpulan Kriteria (Neo-MABIMS)",
            resultTime = statusBadge,
            rows = listOf(
                PrayerBreakdownRow("Kriteria", "Tinggi >= 3°, Elongasi >= 6.4°"),
                PrayerBreakdownRow("Tinggi Hilal", decimalToDMS(tinggiHilal)),
                PrayerBreakdownRow("Elongasi", decimalToDMS(elongasiDeg))
            )
        )
    )

    private fun flattenToLog(sections: List<PrayerBreakdownSection>): String {
        val sb = StringBuilder()
        sb.append("PERHITUNGAN AWAL BULAN HIJRIYAH\n")
        sb.append("SISTEM EPHEMERIS HISAB RUKYAT (Astronomy Engine)\n\n")
        sections.forEach { section ->
            sb.append(section.prayerLabel.uppercase(Locale.getDefault()))
            if (section.resultTime.isNotEmpty()) sb.append(" : ${section.resultTime}")
            sb.append("\n")
            section.rows.forEach { row ->
                sb.append("   ${row.label} : ${row.value}\n")
            }
            sb.append("\n")
        }
        return sb.toString()
    }

    /**
     * Ijtima' ke-[monthOffset] relatif ke ijtima' terdekat ke depan dari sekarang (offset 0 =
     * perilaku lama). Dipisah dari [calculate] supaya bisa dihitung sekali lalu dipakai ulang lewat
     * overload [calculate] yang menerima `ijtima` -- lihat dokumentasi overload itu.
     */
    fun findIjtima(monthOffset: Int): Time =
        findIjtimaAtOffset(Time.fromMillisecondsSince1970(System.currentTimeMillis()), monthOffset)

    /**
     * Ijtima' ke-[monthOffset] relatif ke ijtima' terdekat ke depan dari [now] (offset 0 = perilaku
     * lama). Jalan lewat rantai new-moon sungguhan (bukan estimasi +-29.53 hari/bulan) supaya presisi
     * terjaga walau offset besar -- margin awal `32 hari x (|offset|+2)` selalu lebih dari cukup
     * synodic month (~29.53 hari) untuk menjamin ada >= |offset| ijtima' sebelum baseline.
     */
    private fun findIjtimaAtOffset(now: Time, monthOffset: Int): Time {
        val marginSynodicMonths = abs(monthOffset) + 2
        var mq = searchMoonQuarter(now.addDays(-32.0 * marginSynodicMonths))
        while (mq.quarter != 0) mq = nextMoonQuarter(mq)

        val chain = mutableListOf(mq.time)
        while (chain.last() < now) {
            mq = nextMoonQuarter(mq)
            while (mq.quarter != 0) mq = nextMoonQuarter(mq)
            chain.add(mq.time)
        }

        val baselineIndex = chain.lastIndex
        var targetIndex = baselineIndex + monthOffset
        while (targetIndex > chain.lastIndex) {
            mq = nextMoonQuarter(mq)
            while (mq.quarter != 0) mq = nextMoonQuarter(mq)
            chain.add(mq.time)
        }
        require(targetIndex >= 0) { "monthOffset $monthOffset terlalu jauh ke belakang" }
        return chain[targetIndex]
    }

    private fun localMidnightOf(time: Time): Time {
        val cal = Calendar.getInstance()
        cal.timeInMillis = time.toMillisecondsSince1970()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return Time.fromMillisecondsSince1970(cal.timeInMillis)
    }

    private fun calendarFromTime(time: Time): Calendar {
        val cal = Calendar.getInstance()
        cal.timeInMillis = time.toMillisecondsSince1970()
        return cal
    }

    private fun formatLocalTime(time: Time): String {
        val sdf = SimpleDateFormat("dd MMMM yyyy, HH:mm:ss", Locale("id", "ID"))
        return sdf.format(Date(time.toMillisecondsSince1970()))
    }

    private fun formatLocalDate(time: Time): String {
        val sdf = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
        return sdf.format(Date(time.toMillisecondsSince1970()))
    }

    private fun decimalToDMS(value: Double): String {
        val sign = if (value < 0) "-" else ""
        val abs = abs(value)
        val d = floor(abs).toInt()
        val m = floor((abs - d) * 60).toInt()
        val s = ((abs - d) * 60 - m) * 60
        return String.format(Locale.US, "%s%d° %d' %.2f\"", sign, d, m, s)
    }
}
