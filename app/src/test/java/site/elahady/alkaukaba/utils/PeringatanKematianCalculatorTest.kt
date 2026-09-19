package site.elahady.alkaukaba.utils

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.GregorianCalendar

/**
 * Pure logic test untuk [PeringatanKematianCalculator] (tanpa Android/API/DB) - mengecek
 * hitungan hari ke-7/40/100/1000 untuk dua konvensi (hari wafat = hari ke-1 vs ke-0), lintas
 * bulan/tahun/kabisat, dan selisih hari kalender.
 */
class PeringatanKematianCalculatorTest {

    private fun tanggal(y: Int, m: Int, d: Int, jam: Int = 0, menit: Int = 0) =
        GregorianCalendar(y, m - 1, d, jam, menit)

    private fun Calendar.label() =
        "${get(Calendar.YEAR)}-${get(Calendar.MONTH) + 1}-${get(Calendar.DAY_OF_MONTH)}"

    private fun hitungLabel(wafat: Calendar, hariWafatDihitungKe1: Boolean = true) =
        PeringatanKematianCalculator.hitung(wafat, hariWafatDihitungKe1).associate { it.hariKe to it.tanggal.label() }

    @Test
    fun `daftar peringatan 7, 40, 100, 1000 hari berurutan`() {
        val hasil = PeringatanKematianCalculator.hitung(tanggal(2026, 1, 1))
        assertEquals(listOf(7, 40, 100, 1000), hasil.map { it.hariKe })
    }

    @Test
    fun `hari wafat dihitung hari ke-1 - 7 hari jatuh pada wafat tambah 6 hari`() {
        // Senin 5 Januari 2026 -> hari ke-7 = Ahad 11 Januari 2026
        val hasil = hitungLabel(tanggal(2026, 1, 5))
        assertEquals("2026-1-11", hasil[7])
        // hari ke-40 = wafat + 39 hari = 13 Februari 2026
        assertEquals("2026-2-13", hasil[40])
        // hari ke-100 = wafat + 99 hari = 14 April 2026
        assertEquals("2026-4-14", hasil[100])
        // hari ke-1000 = wafat + 999 hari = 30 September 2028 (2028 kabisat)
        assertEquals("2028-9-30", hasil[1000])
    }

    @Test
    fun `hari wafat dihitung hari ke-0 - 7 hari jatuh pada wafat tambah 7 hari`() {
        val hasil = hitungLabel(tanggal(2026, 1, 5), hariWafatDihitungKe1 = false)
        assertEquals("2026-1-12", hasil[7])
        assertEquals("2026-2-14", hasil[40])
        assertEquals("2026-4-15", hasil[100])
        assertEquals("2028-10-1", hasil[1000])
    }

    @Test
    fun `melewati akhir tahun dan tahun kabisat`() {
        // wafat 28 Februari 2028 (kabisat), hari ke-7 = wafat + 6 = 5 Maret 2028 (ada 29 Feb)
        assertEquals("2028-3-5", hitungLabel(tanggal(2028, 2, 28))[7])
        // wafat 30 Desember 2026, hari ke-7 = 5 Januari 2027
        assertEquals("2027-1-5", hitungLabel(tanggal(2026, 12, 30))[7])
    }

    @Test
    fun `jam pada tanggal wafat diabaikan dan tanggal input tidak berubah`() {
        val wafat = tanggal(2026, 1, 5, jam = 23, menit = 59)
        val hasil = PeringatanKematianCalculator.hitung(wafat)
        assertEquals("2026-1-11", hasil[0].tanggal.label())
        assertEquals(0, hasil[0].tanggal.get(Calendar.HOUR_OF_DAY))
        assertEquals("2026-1-5", wafat.label())
        assertEquals(23, wafat.get(Calendar.HOUR_OF_DAY))
    }

    @Test
    fun `selisih hari - masa depan, hari ini, dan masa lalu`() {
        val hariIni = tanggal(2026, 9, 19, jam = 15)
        assertEquals(3L, PeringatanKematianCalculator.selisihHari(hariIni, tanggal(2026, 9, 22)))
        assertEquals(0L, PeringatanKematianCalculator.selisihHari(hariIni, tanggal(2026, 9, 19, jam = 1)))
        assertEquals(-5L, PeringatanKematianCalculator.selisihHari(hariIni, tanggal(2026, 9, 14)))
    }

    @Test
    fun `selisih hari lintas tahun kabisat`() {
        assertEquals(366L, PeringatanKematianCalculator.selisihHari(tanggal(2028, 1, 1), tanggal(2029, 1, 1)))
    }
}
