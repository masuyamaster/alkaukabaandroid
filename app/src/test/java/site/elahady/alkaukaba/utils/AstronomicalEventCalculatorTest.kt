package site.elahady.alkaukaba.utils

import site.elahady.alkaukaba.model.AstronomiKategori
import site.elahady.alkaukaba.model.AstronomicalEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * `AstronomicalEventCalculator.calculate()` murni logic (Observer + Astronomy Engine, tanpa
 * Android/API/DB) — GOLDEN/REFERENCE TEST seperti [EphemerisCalculatorTest]. Rujukan tanggal
 * dari riset web yang tercatat di task Notion "Modifikasi Hari Besar jadi Event Besar"
 * (Sept-Okt 2026, dalam WIB); waktu tepatnya dicek dengan toleransi hari, bukan menit,
 * karena sumber riset hanya memberi tanggal.
 */
class AstronomicalEventCalculatorTest {

    private val wib = TimeZone.getTimeZone("Asia/Jakarta")

    private fun millis(year: Int, month: Int, day: Int): Long {
        val cal = Calendar.getInstance(wib)
        cal.clear()
        cal.set(year, month - 1, day, 0, 0, 0)
        return cal.timeInMillis
    }

    private fun localDate(event: AstronomicalEvent): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        sdf.timeZone = wib
        return sdf.format(Date(event.epochMillis))
    }

    private fun calculate(lat: Double, lng: Double, from: Long, to: Long) =
        AstronomicalEventCalculator.calculate(lat, lng, 0.0, from, to, wib)

    private fun List<AstronomicalEvent>.datesOf(kategori: AstronomiKategori) =
        filter { it.kategori == kategori }.map { localDate(it) }

    // --- Hari Tanpa Bayangan ---------------------------------------------------------

    @Test
    fun `hari tanpa bayangan Pontianak jatuh di ekuinoks 23 September 2026`() {
        val events = calculate(-0.0267, 109.3425, millis(2026, 9, 1), millis(2026, 11, 1))
        assertEquals(listOf("2026-09-23"), events.datesOf(AstronomiKategori.HARI_TANPA_BAYANGAN))
    }

    @Test
    fun `hari tanpa bayangan Jakarta jatuh sekitar 9 Oktober 2026`() {
        val events = calculate(-6.2088, 106.8456, millis(2026, 9, 1), millis(2026, 11, 1))
        val dates = events.datesOf(AstronomiKategori.HARI_TANPA_BAYANGAN)
        assertEquals(1, dates.size)
        assertDayNear("2026-10-09", dates.first(), toleranceDays = 1)
    }

    @Test
    fun `hari tanpa bayangan Surabaya jatuh sekitar 12 Oktober 2026`() {
        val events = calculate(-7.2575, 112.7521, millis(2026, 9, 1), millis(2026, 11, 1))
        val dates = events.datesOf(AstronomiKategori.HARI_TANPA_BAYANGAN)
        assertEquals(1, dates.size)
        assertDayNear("2026-10-12", dates.first(), toleranceDays = 1)
    }

    @Test
    fun `hari tanpa bayangan terjadi dua kali setahun di lintang tropis`() {
        val events = calculate(-6.2088, 106.8456, millis(2026, 1, 1), millis(2027, 1, 1))
        assertEquals(2, events.datesOf(AstronomiKategori.HARI_TANPA_BAYANGAN).size)
    }

    @Test
    fun `hari tanpa bayangan tidak ada di lintang di luar Garis Balik`() {
        // Tokyo (35,7 LU) dan Melbourne (37,8 LS): Matahari tidak pernah mencapai zenit.
        val tokyo = calculate(35.68, 139.69, millis(2026, 1, 1), millis(2027, 1, 1))
        val melbourne = calculate(-37.81, 144.96, millis(2026, 1, 1), millis(2027, 1, 1))
        assertTrue(tokyo.datesOf(AstronomiKategori.HARI_TANPA_BAYANGAN).isEmpty())
        assertTrue(melbourne.datesOf(AstronomiKategori.HARI_TANPA_BAYANGAN).isEmpty())
    }

    @Test
    fun `hari tanpa bayangan di tepi jendela tetap terdeteksi`() {
        // Jendela dimulai persis di hari kejadian Pontianak; jendela terakhir berakhir sehari setelahnya.
        val startOnDay = calculate(-0.0267, 109.3425, millis(2026, 9, 23), millis(2026, 9, 30))
        val endAfterDay = calculate(-0.0267, 109.3425, millis(2026, 9, 16), millis(2026, 9, 24))
        assertEquals(listOf("2026-09-23"), startOnDay.datesOf(AstronomiKategori.HARI_TANPA_BAYANGAN))
        assertEquals(listOf("2026-09-23"), endAfterDay.datesOf(AstronomiKategori.HARI_TANPA_BAYANGAN))
    }

    // --- Ekuinoks / Purnama Panen / Oposisi / Meteor ----------------------------------

    @Test
    fun `ekuinoks September 2026 jatuh 23 September`() {
        val events = calculate(-6.2, 106.8, millis(2026, 9, 1), millis(2026, 11, 1))
        assertEquals(listOf("2026-09-23"), events.datesOf(AstronomiKategori.EKUINOKS_SOLSTIS))
    }

    @Test
    fun `empat titik balik matahari tahun 2026 muncul lengkap dan berurutan`() {
        val events = calculate(-6.2, 106.8, millis(2026, 1, 1), millis(2027, 1, 1))
            .filter { it.kategori == AstronomiKategori.EKUINOKS_SOLSTIS }
        assertEquals(
            listOf("Ekuinoks Maret", "Solstis Juni", "Ekuinoks September", "Solstis Desember"),
            events.map { it.judul }
        )
    }

    @Test
    fun `purnama panen 2026 jatuh 26 September`() {
        val events = calculate(-6.2, 106.8, millis(2026, 9, 1), millis(2026, 11, 1))
        assertEquals(listOf("2026-09-26"), events.datesOf(AstronomiKategori.PURNAMA))
    }

    @Test
    fun `oposisi Neptunus 2026 jatuh 26 September`() {
        val events = calculate(-6.2, 106.8, millis(2026, 9, 1), millis(2026, 11, 1))
        val neptunus = events.filter { it.judul == "Oposisi Neptunus" }
        assertEquals(listOf("2026-09-26"), neptunus.map { localDate(it) })
    }

    @Test
    fun `hujan meteor Sextantid 2026 dekat 1 Oktober sesuai kalender IMO 2026`() {
        val events = calculate(-6.2, 106.8, millis(2026, 9, 1), millis(2026, 11, 1))
        val sextantid = events.filter { it.judul.contains("Sextantid") }
        assertEquals(1, sextantid.size)
        assertDayNear("2026-10-01", localDate(sextantid.first()), toleranceDays = 1)
    }

    @Test
    fun `hujan meteor awal tahun dan akhir tahun terdeteksi - Quadrantid dan Geminid 2026`() {
        val events = calculate(-6.2, 106.8, millis(2026, 1, 1), millis(2027, 1, 1))
        val quadrantid = events.single { it.judul.contains("Quadrantid") }
        val geminid = events.single { it.judul.contains("Geminid") }
        assertDayNear("2026-01-03", localDate(quadrantid), toleranceDays = 1)
        assertDayNear("2026-12-14", localDate(geminid), toleranceDays = 1)
    }

    // --- Invariant umum --------------------------------------------------------------

    @Test
    fun `jam pada keterangan memakai singkatan zona Indonesia, bukan GMT offset`() {
        val ekuinoks = calculate(-6.2, 106.8, millis(2026, 9, 1), millis(2026, 11, 1))
            .single { it.kategori == AstronomiKategori.EKUINOKS_SOLSTIS }
        assertTrue(ekuinoks.keterangan, ekuinoks.keterangan.contains("07:05 WIB"))

        val makassar = AstronomicalEventCalculator.calculate(
            -6.2, 106.8, 0.0, millis(2026, 9, 1), millis(2026, 11, 1), TimeZone.getTimeZone("Asia/Makassar")
        ).single { it.kategori == AstronomiKategori.EKUINOKS_SOLSTIS }
        assertTrue(makassar.keterangan, makassar.keterangan.contains("08:05 WITA"))
    }

    @Test
    fun `hasil urut menurut waktu dan semua berada dalam jendela`() {
        val from = millis(2026, 9, 1)
        val to = millis(2027, 3, 1)
        val events = calculate(-6.2088, 106.8456, from, to)

        assertTrue(events.isNotEmpty())
        assertEquals(events.sortedBy { it.epochMillis }, events)
        assertTrue(events.all { it.epochMillis >= from && it.epochMillis < to })
    }

    @Test
    fun `jendela kosong tidak menghasilkan event`() {
        val at = millis(2026, 9, 10)
        assertTrue(calculate(-6.2, 106.8, at, at).isEmpty())
    }

    @Test
    fun `gerhana Bulan total 3 Maret 2026 tercatat sebagai event`() {
        val events = calculate(-6.2, 106.8, millis(2026, 2, 1), millis(2026, 4, 1))
        val gerhanaBulan = events.filter { it.judul.startsWith("Gerhana Bulan") }
        assertEquals(1, gerhanaBulan.size)
        assertEquals("Gerhana Bulan Total", gerhanaBulan.first().judul)
        assertDayNear("2026-03-03", localDate(gerhanaBulan.first()), toleranceDays = 1)
    }

    private fun assertDayNear(expected: String, actual: String, toleranceDays: Int) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        sdf.timeZone = wib
        val diffDays = kotlin.math.abs(sdf.parse(expected)!!.time - sdf.parse(actual)!!.time) / 86_400_000L
        assertTrue("Diharapkan $expected ±$toleranceDays hari, dapat $actual", diffDays <= toleranceDays)
    }
}
