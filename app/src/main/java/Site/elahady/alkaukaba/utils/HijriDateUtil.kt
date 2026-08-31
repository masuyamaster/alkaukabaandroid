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

    /** Label bulan Hijriyah berikutnya (yang sedang dicek awal bulannya) relatif terhadap [gregorianDate]. */
    fun nextMonthLabel(gregorianDate: Calendar): String {
        val jdn = gregorianToJdn(
            gregorianDate.get(Calendar.YEAR),
            gregorianDate.get(Calendar.MONTH) + 1,
            gregorianDate.get(Calendar.DAY_OF_MONTH)
        )
        val (_, month, year) = jdnToHijri(jdn)
        val nextMonth = if (month >= 12) 1 else month + 1
        val nextYear = if (month >= 12) year + 1 else year
        return "Menjelang ${MONTH_NAMES[nextMonth - 1]} $nextYear H"
    }
}
