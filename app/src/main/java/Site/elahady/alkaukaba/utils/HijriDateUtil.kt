package site.elahady.alkaukaba.utils

import java.util.Calendar

/**
 * Konversi Masehi -> Hijriyah pakai algoritma tabular Kuwaiti (offline, tanpa
 * dependency/network). Akurasi kalender tabular seperti ini berkisar +-1..2
 * hari dibanding rukyat/hisab hakiki asli, jadi HANYA dipakai untuk label
 * tampilan ("bulan Hijriyah yang dicek") — bukan untuk perhitungan
 * ijtima'/ghurub/kriteria hilal, yang semuanya 100% dari Astronomy Engine
 * (lihat EphemerisCalculator).
 */
object HijriDateUtil {

    private val MONTH_NAMES = arrayOf(
        "Muharram", "Safar", "Rabiul Awal", "Rabiul Akhir",
        "Jumadil Awal", "Jumadil Akhir", "Rajab", "Sya'ban",
        "Ramadhan", "Syawal", "Dzulqa'dah", "Dzulhijjah"
    )

    /** 12 nama bulan Hijriyah urut (index 0 = Muharram) — dipakai selector bulan di Awal Bulan Hijriyah. */
    val monthNames: List<String> get() = MONTH_NAMES.toList()

    private fun gregorianToJdn(year: Int, month: Int, day: Int): Long {
        val a = (14 - month) / 12
        val y = year + 4800 - a
        val m = month + 12 * a - 3
        return day + (153L * m + 2) / 5 + 365L * y + y / 4 - y / 100 + y / 400 - 32045
    }

    private fun jdnToHijri(jdn: Long): Triple<Int, Int, Int> {
        val jd = jdn - 1948440 + 10632
        val n = (jd - 1) / 10631
        val jd2 = jd - 10631 * n + 354
        val j = ((10985 - jd2) / 5316) * ((50 * jd2) / 17719) + (jd2 / 5670) * ((43 * jd2) / 15238)
        val jd3 = jd2 - ((30 - j) / 15) * ((17719 * j) / 50) - (j / 16) * ((15238 * j) / 43) + 29
        val month = ((24 * jd3) / 709).toInt()
        val day = (jd3 - (709 * month) / 24).toInt()
        val year = (30 * n + j - 30).toInt()
        return Triple(day, month.coerceIn(1, 12), year)
    }

    /** Tanggal sederhana (tahun/bulan/hari) tanpa zona waktu — hasil/masukan konversi Masehi <-> Hijriyah. */
    data class DateParts(val year: Int, val month: Int, val day: Int)

    private val GREGORIAN_MONTH_NAMES = arrayOf(
        "Januari", "Februari", "Maret", "April", "Mei", "Juni",
        "Juli", "Agustus", "September", "Oktober", "November", "Desember"
    )

    // Indeks 0 = Ahad (Minggu), sama urutan dengan Calendar.SUNDAY - 1.
    private val WEEKDAY_NAMES = arrayOf("Ahad", "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu")

    // JDN acuan 1 Muharram 1 H pada kalender Hijriyah sipil (tabular).
    private const val HIJRI_EPOCH_JDN = 1948440L

    /** 12 nama bulan Masehi (Bahasa Indonesia) urut, index 0 = Januari — dipakai konverter tanggal. */
    val gregorianMonthNames: List<String> get() = GREGORIAN_MONTH_NAMES.toList()

    /** Tahun kabisat Hijriyah (tabular): 11 dari tiap siklus 30 tahun — 2, 5, 7, 10, 13, 16, 18, 21, 24, 26, 29. */
    fun isHijriLeapYear(hijriYear: Int): Boolean = (11 * hijriYear + 14) % 30 < 11

    /** Jumlah hari bulan Hijriyah [month] (1-12) tahun [hijriYear]: bulan ganjil 30, genap 29, Dzulhijjah 30 di tahun kabisat. */
    fun hijriMonthLength(hijriYear: Int, month: Int): Int =
        if (month % 2 == 1 || (month == 12 && isHijriLeapYear(hijriYear))) 30 else 29

    /** Jumlah hari bulan Masehi [month] (1-12) tahun [year], memperhitungkan tahun kabisat Gregorian. */
    fun gregorianMonthLength(year: Int, month: Int): Int = when (month) {
        2 -> if ((year % 4 == 0 && year % 100 != 0) || year % 400 == 0) 29 else 28
        4, 6, 9, 11 -> 30
        else -> 31
    }

    private fun hijriToJdn(year: Int, month: Int, day: Int): Long =
        day + (59L * (month - 1) + 1) / 2 + 354L * (year - 1) + (3 + 11L * year) / 30 + HIJRI_EPOCH_JDN - 1

    private fun jdnToGregorian(jdn: Long): DateParts {
        val a = jdn + 32044
        val b = (4 * a + 3) / 146097
        val c = a - 146097 * b / 4
        val d = (4 * c + 3) / 1461
        val e = c - 1461 * d / 4
        val m = (5 * e + 2) / 153
        val day = (e - (153 * m + 2) / 5 + 1).toInt()
        val month = (m + 3 - 12 * (m / 10)).toInt()
        val year = (100 * b + d - 4800 + m / 10).toInt()
        return DateParts(year, month, day)
    }

    /** Konversi tanggal Masehi ([year], [month] 1-12, [day]) ke Hijriyah (tabular). */
    fun gregorianToHijri(year: Int, month: Int, day: Int): DateParts {
        val (hijriDay, hijriMonth, hijriYear) = jdnToHijri(gregorianToJdn(year, month, day))
        return DateParts(hijriYear, hijriMonth, hijriDay)
    }

    /** Konversi tanggal Hijriyah ([year], [month] 1-12, [day]) ke Masehi (tabular). Input tidak divalidasi —
     * pemanggil yang menjamin [day] <= [hijriMonthLength]. */
    fun hijriToGregorian(year: Int, month: Int, day: Int): DateParts =
        jdnToGregorian(hijriToJdn(year, month, day))

    /** Nama hari (Ahad..Sabtu) untuk tanggal Masehi [year]/[month]/[day]. */
    fun weekdayName(year: Int, month: Int, day: Int): String {
        val jdn = gregorianToJdn(year, month, day)
        return WEEKDAY_NAMES[((jdn + 1) % 7).toInt()]
    }

    /** Label Masehi lengkap, mis. "Jumat, 19 September 2026". */
    fun gregorianLabel(date: DateParts): String =
        "${weekdayName(date.year, date.month, date.day)}, ${date.day} ${GREGORIAN_MONTH_NAMES[date.month - 1]} ${date.year}"

    /** Label Hijriyah lengkap, mis. "6 Rabiul Akhir 1448 H". */
    fun hijriLabel(date: DateParts): String = "${date.day} ${MONTH_NAMES[date.month - 1]} ${date.year} H"

    /** Nama bulan & tahun Hijriyah (tabular) untuk [gregorianDate] - dipakai [HijriCalendarEngine]
     * untuk memberi label bulan/tahun pada segmen yang batas awalnya sudah dikoreksi hisab hakiki. */
    fun monthYearAt(gregorianDate: Calendar): Pair<String, Int> {
        val jdn = gregorianToJdn(
            gregorianDate.get(Calendar.YEAR),
            gregorianDate.get(Calendar.MONTH) + 1,
            gregorianDate.get(Calendar.DAY_OF_MONTH)
        )
        val (_, month, year) = jdnToHijri(jdn)
        return MONTH_NAMES[month - 1] to year
    }

    /** Tanggal Hijriyah lengkap untuk [gregorianDate], mis. "17 Rabiul Awal 1447 H". */
    fun fullDateLabel(gregorianDate: Calendar): String {
        val jdn = gregorianToJdn(
            gregorianDate.get(Calendar.YEAR),
            gregorianDate.get(Calendar.MONTH) + 1,
            gregorianDate.get(Calendar.DAY_OF_MONTH)
        )
        val (day, month, year) = jdnToHijri(jdn)
        return "$day ${MONTH_NAMES[month - 1]} $year H"
    }

    /** Tahun & bulan Hijriyah (tabular, 1-12) "bulan berikutnya" relatif ke [gregorianDate] — bagian
     * mentah dari [nextMonthLabel], dipakai untuk isi default selector bulan/tahun di Awal Bulan
     * Hijriyah (`AwalBulanActivity`). */
    fun nextMonthYearMonth(gregorianDate: Calendar): Pair<Int, Int> {
        val jdn = gregorianToJdn(
            gregorianDate.get(Calendar.YEAR),
            gregorianDate.get(Calendar.MONTH) + 1,
            gregorianDate.get(Calendar.DAY_OF_MONTH)
        )
        val (_, month, year) = jdnToHijri(jdn)
        val nextMonth = if (month >= 12) 1 else month + 1
        val nextYear = if (month >= 12) year + 1 else year
        return nextYear to nextMonth
    }

    /** Label bulan Hijriyah berikutnya (yang sedang dicek awal bulannya) relatif terhadap [gregorianDate]. */
    fun nextMonthLabel(gregorianDate: Calendar): String {
        val (year, month) = nextMonthYearMonth(gregorianDate)
        return "Menjelang ${MONTH_NAMES[month - 1]} $year H"
    }
}
