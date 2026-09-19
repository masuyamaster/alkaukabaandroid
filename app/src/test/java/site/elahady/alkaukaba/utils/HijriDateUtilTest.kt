package site.elahady.alkaukaba.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import site.elahady.alkaukaba.utils.HijriDateUtil.DateParts
import java.time.LocalDate
import java.time.chrono.HijrahDate
import kotlin.math.abs

/**
 * Golden/invariant test (tanpa mock) untuk konversi Masehi <-> Hijriyah tabular di [HijriDateUtil]
 * (dipakai menu Konversi Hijriyah - Masehi, lihat docs/features/konversi-hijriyah-masehi.md).
 * Rujukan independen: `java.time` (LocalDate untuk urutan hari Gregorian, HijrahChronology Umm al-Qura
 * untuk batas selisih tabular).
 */
class HijriDateUtilTest {

    private fun DateParts.toLocalDate(): LocalDate = LocalDate.of(year, month, day)

    @Test
    fun `epoch - 1 Muharram 1 H jatuh di hari Jumat (16 Juli 622 Julian = 19 Juli 622 Gregorian proleptik)`() {
        assertEquals(DateParts(1, 1, 1), HijriDateUtil.gregorianToHijri(622, 7, 19))
        assertEquals(DateParts(622, 7, 19), HijriDateUtil.hijriToGregorian(1, 1, 1))
        assertEquals("Jumat", HijriDateUtil.weekdayName(622, 7, 19))
    }

    @Test
    fun `nama hari - rujukan sejarah dan awal milenium`() {
        assertEquals("Jumat", HijriDateUtil.weekdayName(1945, 8, 17)) // Proklamasi Kemerdekaan RI
        assertEquals("Sabtu", HijriDateUtil.weekdayName(2000, 1, 1))
        assertEquals("Ahad", HijriDateUtil.weekdayName(2026, 9, 20))
    }

    @Test
    fun `siklus 30 tahun Hijriyah tepat 10631 hari dengan 11 tahun kabisat`() {
        val leapYears = (1..30).filter { HijriDateUtil.isHijriLeapYear(it) }
        assertEquals(listOf(2, 5, 7, 10, 13, 16, 18, 21, 24, 26, 29), leapYears)

        val days = (1..30).sumOf { year -> (1..12).sumOf { month -> HijriDateUtil.hijriMonthLength(year, month) } }
        assertEquals(10631, days)
    }

    @Test
    fun `panjang bulan Hijriyah - ganjil 30 genap 29, Dzulhijjah 30 hanya di tahun kabisat`() {
        assertEquals(30, HijriDateUtil.hijriMonthLength(1447, 1))
        assertEquals(29, HijriDateUtil.hijriMonthLength(1447, 2))
        assertEquals(30, HijriDateUtil.hijriMonthLength(1447, 12)) // 1447 % 30 = 7 -> tahun kabisat
        assertEquals(29, HijriDateUtil.hijriMonthLength(1448, 12)) // 1448 % 30 = 8 -> bukan kabisat
    }

    @Test
    fun `panjang bulan Masehi - kabisat Gregorian`() {
        assertEquals(29, HijriDateUtil.gregorianMonthLength(2024, 2))
        assertEquals(28, HijriDateUtil.gregorianMonthLength(2100, 2))
        assertEquals(29, HijriDateUtil.gregorianMonthLength(2000, 2))
        assertEquals(28, HijriDateUtil.gregorianMonthLength(1900, 2))
        assertEquals(30, HijriDateUtil.gregorianMonthLength(2026, 9))
        assertEquals(31, HijriDateUtil.gregorianMonthLength(2026, 12))
    }

    @Test
    fun `round trip Masehi ke Hijriyah ke Masehi untuk setiap hari 1600 sampai 2250`() {
        var date = LocalDate.of(1600, 1, 1)
        val end = LocalDate.of(2250, 12, 31)
        while (!date.isAfter(end)) {
            val hijri = HijriDateUtil.gregorianToHijri(date.year, date.monthValue, date.dayOfMonth)
            assertTrue("bulan Hijriyah di luar 1..12 untuk $date", hijri.month in 1..12)
            assertTrue(
                "tanggal Hijriyah $hijri melebihi panjang bulan untuk $date",
                hijri.day in 1..HijriDateUtil.hijriMonthLength(hijri.year, hijri.month)
            )
            val back = HijriDateUtil.hijriToGregorian(hijri.year, hijri.month, hijri.day)
            assertEquals(date, back.toLocalDate())
            date = date.plusDays(1)
        }
    }

    @Test
    fun `hijriToGregorian maju tepat 1 hari untuk tiap hari Hijriyah berurutan (1000-1700 H)`() {
        var previous: LocalDate? = null
        for (year in 1000..1700) {
            for (month in 1..12) {
                for (day in 1..HijriDateUtil.hijriMonthLength(year, month)) {
                    val current = HijriDateUtil.hijriToGregorian(year, month, day).toLocalDate()
                    if (previous != null) {
                        assertEquals("loncatan hari di $day/$month/$year H", previous.plusDays(1), current)
                    }
                    previous = current
                }
            }
        }
    }

    @Test
    fun `hari terakhir tiap bulan Hijriyah berganti ke tanggal 1 bulan berikutnya`() {
        for ((year, month) in listOf(1447 to 12, 1448 to 1, 1448 to 4, 1449 to 12)) {
            val last = HijriDateUtil.hijriMonthLength(year, month)
            val lastGregorian = HijriDateUtil.hijriToGregorian(year, month, last).toLocalDate()
            val next = lastGregorian.plusDays(1)
            val nextHijri = HijriDateUtil.gregorianToHijri(next.year, next.monthValue, next.dayOfMonth)
            val expected = if (month == 12) DateParts(year + 1, 1, 1) else DateParts(year, month + 1, 1)
            assertEquals(expected, nextHijri)
        }
    }

    @Test
    fun `selisih tabular dengan Umm al-Qura maksimal 2 hari (1400-1500 H, awal tiap bulan)`() {
        var maxDiff = 0L
        for (year in 1400..1500) {
            for (month in 1..12) {
                val tabular = HijriDateUtil.hijriToGregorian(year, month, 1).toLocalDate()
                val ummAlQura = LocalDate.from(HijrahDate.of(year, month, 1))
                maxDiff = maxOf(maxDiff, abs(tabular.toEpochDay() - ummAlQura.toEpochDay()))
            }
        }
        assertTrue("selisih maksimum $maxDiff hari > 2", maxDiff <= 2)
    }

    @Test
    fun `konsisten dengan fullDateLabel yang sudah ada (jalur Calendar)`() {
        val cal = java.util.Calendar.getInstance().apply { clear(); set(2026, java.util.Calendar.SEPTEMBER, 19) }
        val viaCalendar = HijriDateUtil.fullDateLabel(cal)
        val viaParts = HijriDateUtil.hijriLabel(HijriDateUtil.gregorianToHijri(2026, 9, 19))
        assertEquals(viaCalendar, viaParts)
    }

    @Test
    fun `label Masehi dan Hijriyah`() {
        assertEquals("Sabtu, 1 Januari 2000", HijriDateUtil.gregorianLabel(DateParts(2000, 1, 1)))
        assertEquals("17 Rabiul Awal 1447 H", HijriDateUtil.hijriLabel(DateParts(1447, 3, 17)))
        assertFalse(HijriDateUtil.gregorianMonthNames.isEmpty())
    }
}
