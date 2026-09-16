package site.elahady.alkaukaba.utils

import io.github.cosinekitty.astronomy.Aberration
import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.Direction
import io.github.cosinekitty.astronomy.EquatorEpoch
import io.github.cosinekitty.astronomy.Observer
import io.github.cosinekitty.astronomy.Refraction
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.elongation
import io.github.cosinekitty.astronomy.equator
import io.github.cosinekitty.astronomy.horizon
import io.github.cosinekitty.astronomy.searchMoonPhase
import io.github.cosinekitty.astronomy.searchRiseSet
import java.util.Calendar
import kotlin.math.roundToInt

/**
 * Kalender Hijriyah untuk widget Kalender di beranda, dibangun dari mesin hisab yang SAMA
 * dengan fitur Awal Bulan (ijtima' -> ghurub -> kriteria Neo-MABIMS: tinggi >= 3 derajat DAN
 * elongasi >= 6.4 derajat, lihat [EphemerisCalculator]), termasuk istikmal (genap 30 hari)
 * kalau kriteria belum terpenuhi. Ini memastikan tanggal 1 tiap bulan Hijriyah di beranda selalu
 * konsisten dengan hasil hitung di halaman Awal Bulan, bukan hasil API pihak ketiga.
 *
 * Nama bulan & tahun tetap dibaca dari [HijriDateUtil] (tabular), tapi dievaluasi di
 * pertengahan segmen (bukan di tanggal 1) supaya tidak salah "kebawa" ke bulan tabular
 * sebelah kalau tabular berbeda 1-2 hari dari batas hasil hisab hakiki.
 */
object HijriCalendarEngine {

    private const val KRITERIA_TINGGI_MIN = 3.0 // derajat, Neo-MABIMS
    private const val KRITERIA_ELONGASI_MIN = 6.4 // derajat, Neo-MABIMS
    private const val SEARCH_WINDOW_DAYS = 40.0 // > 1 bulan sinodis (~29.53 hari), margin pencarian ijtima'

    data class HijriDay(val day: Int, val monthName: String, val year: Int) {
        val label: String get() = "$monthName $year H"
    }

    /** Rentang 1 bulan Hijriyah dalam kalender Masehi: [startDate] = tanggal 1 H (Masehi),
     *  [dayCount] = jumlah hari (29/30, hasil istikmal). Dipakai fitur yang butuh iterasi
     *  per-hari dalam 1 bulan Hijriyah penuh, mis. Jadwal Imsakiyah. */
    data class HijriMonthRange(val startDate: Calendar, val dayCount: Int, val monthName: String, val year: Int)

    private data class Segment(val start: Time, val end: Time, val ijtima: Time)

    /** Hijri day/bulan/tahun untuk tiap tanggal (1..[daysInMonth]) dalam bulan Masehi yang diawali [monthStartDate]. */
    fun buildCalendar(observer: Observer, monthStartDate: Calendar, daysInMonth: Int): List<HijriDay> {
        val dayCal = monthStartDate.clone() as Calendar
        dayCal.set(Calendar.DAY_OF_MONTH, 1)

        var segment = findSegmentContaining(observer, timeAtLocalMidnight(dayCal))
        val result = ArrayList<HijriDay>(daysInMonth)

        for (dayOfMonth in 1..daysInMonth) {
            dayCal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            val dayTime = timeAtLocalMidnight(dayCal)
            while (dayTime >= segment.end) {
                segment = nextSegment(observer, segment)
            }
            val hijriDay = daysBetween(segment.start, dayTime) + 1
            val (monthName, year) = monthLabelOf(segment)
            result.add(HijriDay(hijriDay, monthName, year))
        }
        return result
    }

    /** Label "Nama Bulan Tahun H" Hijriyah yang berlaku pada [gregorianDate]. */
    fun hijriTitleFor(observer: Observer, gregorianDate: Calendar): String {
        val segment = findSegmentContaining(observer, timeAtLocalMidnight(gregorianDate))
        val (monthName, year) = monthLabelOf(segment)
        return "$monthName $year H"
    }

    /** Label lengkap "tanggal Nama Bulan Tahun H" Hijriyah, mis. "3 Rabiul Awal 1448 H", untuk [gregorianDate]. */
    fun fullDateLabelFor(observer: Observer, gregorianDate: Calendar): String {
        val dayTime = timeAtLocalMidnight(gregorianDate)
        val segment = findSegmentContaining(observer, dayTime)
        val hijriDay = daysBetween(segment.start, dayTime) + 1
        val (monthName, year) = monthLabelOf(segment)
        return "$hijriDay $monthName $year H"
    }

    /** Rentang Masehi (tanggal 1 + jumlah hari) bulan Hijriyah yang memuat [referenceDate],
     *  digeser [monthOffset] bulan (0 = bulan yang memuat [referenceDate], + = maju, - = mundur) —
     *  pola sama seperti `HilalInput.monthOffset` di fitur Awal Bulan. */
    fun monthRangeForOffset(observer: Observer, referenceDate: Calendar, monthOffset: Int): HijriMonthRange {
        var segment = findSegmentContaining(observer, timeAtLocalMidnight(referenceDate))
        if (monthOffset > 0) {
            repeat(monthOffset) { segment = nextSegment(observer, segment) }
        } else if (monthOffset < 0) {
            repeat(-monthOffset) { segment = previousSegment(observer, segment) }
        }
        val startCal = Calendar.getInstance()
        startCal.timeInMillis = segment.start.toMillisecondsSince1970()
        val dayCount = daysBetween(segment.start, segment.end)
        val (monthName, year) = monthLabelOf(segment)
        return HijriMonthRange(startCal, dayCount, monthName, year)
    }

    // Cari segmen (rentang 1 bulan Hijriyah) yang memuat [date], dengan mencari mundur dulu
    // sampai ketemu awal bulan <= date, lalu maju sekali untuk batas akhirnya.
    private fun findSegmentContaining(observer: Observer, date: Time): Segment {
        var ijtima = searchMoonPhase(0.0, date.addDays(2.0), -SEARCH_WINDOW_DAYS)
            ?: error("Tidak bisa menemukan ijtima' untuk tanggal ini")
        var start = monthStartAfterIjtima(observer, ijtima)
        while (start > date) {
            ijtima = searchMoonPhase(0.0, ijtima.addDays(-5.0), -SEARCH_WINDOW_DAYS)
                ?: error("Tidak bisa menemukan ijtima' sebelumnya")
            start = monthStartAfterIjtima(observer, ijtima)
        }

        val nextIjtima = searchMoonPhase(0.0, ijtima.addDays(20.0), SEARCH_WINDOW_DAYS)
            ?: error("Tidak bisa menemukan ijtima' berikutnya")
        val end = monthStartAfterIjtima(observer, nextIjtima)
        return Segment(start, end, nextIjtima)
    }

    // Lanjut ke segmen (bulan Hijriyah) berikutnya setelah [segment].
    private fun nextSegment(observer: Observer, segment: Segment): Segment {
        val nextIjtima = searchMoonPhase(0.0, segment.ijtima.addDays(20.0), SEARCH_WINDOW_DAYS)
            ?: error("Tidak bisa menemukan ijtima' berikutnya")
        val end = monthStartAfterIjtima(observer, nextIjtima)
        return Segment(segment.end, end, nextIjtima)
    }

    // Mundur ke segmen (bulan Hijriyah) sebelum [segment] — cukup cari ulang segmen yang
    // memuat 1 hari sebelum awal [segment], karena tiap bulan Hijriyah minimal 29 hari.
    private fun previousSegment(observer: Observer, segment: Segment): Segment {
        return findSegmentContaining(observer, segment.start.addDays(-1.0))
    }

    // Tanggal 1 bulan Hijriyah berikutnya, dihitung dari ghurub setelah [ijtima] + kriteria Neo-MABIMS.
    // Kalau kriteria belum terpenuhi -> istikmal (bulan berjalan digenapkan 30 hari, mundur 1 hari lagi).
    private fun monthStartAfterIjtima(observer: Observer, ijtima: Time): Time {
        val ghurub = ghurubForIjtima(observer, ijtima)
        val extraDay = if (hilalMemenuhiKriteria(observer, ghurub)) 1.0 else 2.0
        return localMidnightOf(ghurub).addDays(extraDay)
    }

    // Sama persis dengan logic ghurub markaz di EphemerisCalculator.calculate().
    private fun ghurubForIjtima(observer: Observer, ijtima: Time): Time {
        val ijtimaLocalMidnight = localMidnightOf(ijtima)
        var ghurub = searchRiseSet(Body.Sun, observer, Direction.Set, ijtimaLocalMidnight, 1.5)
            ?: error("Tidak bisa menghitung waktu ghurub untuk lokasi ini")
        if (ijtima > ghurub) {
            ghurub = searchRiseSet(Body.Sun, observer, Direction.Set, ghurub.addDays(0.5), 1.5)
                ?: error("Tidak bisa menghitung waktu ghurub keesokan hari")
        }
        return ghurub
    }

    private fun hilalMemenuhiKriteria(observer: Observer, ghurub: Time): Boolean {
        val moonEq = equator(Body.Moon, ghurub, observer, EquatorEpoch.OfDate, Aberration.Corrected)
        val moonHor = horizon(ghurub, observer, moonEq.ra, moonEq.dec, Refraction.Normal)
        val elong = elongation(Body.Moon, ghurub)
        return moonHor.altitude >= KRITERIA_TINGGI_MIN && elong.elongation >= KRITERIA_ELONGASI_MIN
    }

    // Nama bulan & tahun dievaluasi di pertengahan segmen (hari ke-15) supaya aman dari
    // pergeseran 1-2 hari antara batas tabular vs batas hasil hisab hakiki di [start]/[end].
    private fun monthLabelOf(segment: Segment): Pair<String, Int> {
        val midCal = Calendar.getInstance()
        midCal.timeInMillis = segment.start.toMillisecondsSince1970()
        midCal.add(Calendar.DAY_OF_MONTH, 15)
        return HijriDateUtil.monthYearAt(midCal)
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

    private fun timeAtLocalMidnight(cal: Calendar): Time {
        val midnight = cal.clone() as Calendar
        midnight.set(Calendar.HOUR_OF_DAY, 0)
        midnight.set(Calendar.MINUTE, 0)
        midnight.set(Calendar.SECOND, 0)
        midnight.set(Calendar.MILLISECOND, 0)
        return Time.fromMillisecondsSince1970(midnight.timeInMillis)
    }

    private fun daysBetween(start: Time, day: Time): Int = (day.ut - start.ut).roundToInt()
}
