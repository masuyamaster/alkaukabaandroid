package site.elahady.alkaukaba.utils

import java.util.Calendar

/**
 * Pasaran (siklus pancawara 5 harian) Kalender Jawa: Legi, Pahing, Pon, Wage, Kliwon,
 * berulang terus menerus tanpa terpengaruh bulan/tahun - jadi cukup dihitung dari selisih
 * hari (Julian Day Number) ke tanggal acuan yang pasarannya sudah pasti diketahui.
 *
 * Acuan: Jumat 17 Agustus 1945 (Proklamasi Kemerdekaan) tercatat sebagai "Jumat Legi",
 * fakta yang lazim dikutip dalam sejarah Indonesia - dipakai di sini sebagai titik nol
 * pancawara supaya independen dari perhitungan Hijriyah/hisab lain di app ini.
 */
object JavaneseCalendarUtil {

    private val PASARAN_NAMES = arrayOf("Legi", "Pahing", "Pon", "Wage", "Kliwon")

    // JDN 17 Agustus 1945 (Gregorian) = 2.431.685, jatuh pada pasaran Legi.
    private const val PASARAN_ANCHOR_JDN = 2_431_685L

    private fun gregorianToJdn(year: Int, month: Int, day: Int): Long {
        val a = (14 - month) / 12
        val y = year + 4800 - a
        val m = month + 12 * a - 3
        return day + (153L * m + 2) / 5 + 365L * y + y / 4 - y / 100 + y / 400 - 32045
    }

    /** Nama pasaran (Legi/Pahing/Pon/Wage/Kliwon) untuk tanggal Masehi [date]. */
    fun pasaranFor(date: Calendar): String {
        val jdn = gregorianToJdn(
            date.get(Calendar.YEAR),
            date.get(Calendar.MONTH) + 1,
            date.get(Calendar.DAY_OF_MONTH)
        )
        val index = (((jdn - PASARAN_ANCHOR_JDN) % 5 + 5) % 5).toInt()
        return PASARAN_NAMES[index]
    }
}
