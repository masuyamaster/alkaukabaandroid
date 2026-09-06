package site.elahady.alkaukaba.utils.addurrulaniq

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Validasi AdDurrulAniqIjtimaCalculator terhadap 4 contoh manual di kitab
 * Ad-Durrul Aniq (rangkuman Notion, per 2026-09-05).
 */
class AdDurrulAniqIjtimaCalculatorTest {

    @Test
    fun `ijtima akhir Syaban 1434H`() {
        // Ekspektasi kitab: Senin Pon, 8 Juli 2013 M, 14:15:10.9 WIB (TZ+7)
        val r = AdDurrulAniqIjtimaCalculator.calculate(1434, 8)
        println("Sya'ban 1434H -> AM=${r.alamatMuaddalah}, ET=${r.jamEt}, DeltaT=${r.deltaTDetik}, UT=${r.jamUt}, tanggal=${r.gregorianDay}-${r.gregorianMonth}-${r.gregorianYear}, hari=${r.hariMingguan} ${r.hariPasaran}")
        assertEquals(2013, r.gregorianYear)
        assertEquals(7, r.gregorianMonth)
        assertEquals(8, r.gregorianDay)
        assertEquals("Senin", r.hariMingguan)
        assertEquals("Pon", r.hariPasaran)
        val wib = r.jamUt + 7.0
        assertEquals(14.2530, wib, 0.02) // 14:15:10.9 = 14.2530 jam desimal
    }

    @Test
    fun `ijtima akhir Ramadhan 1434H`() {
        // Ekspektasi kitab: Rabu Pon, 7 Agustus 2013 M, 04:50:42.1 WIB (TZ+7).
        // Kitab sendiri mencatat "K=6(UT)/7(WD)" -- tanggal UT (6) berbeda dari
        // tanggal waktu daerah (7) karena jam ET/UT sore hari (~21:5x) bergeser
        // ke tanggal berikutnya setelah +7 jam WIB. Jadi cek tanggal WIB via
        // waktuDaerah(), bukan gregorianDay (yang basis UT) secara langsung.
        val r = AdDurrulAniqIjtimaCalculator.calculate(1434, 9)
        val wib = r.waktuDaerah(7.0)
        println("Ramadhan 1434H -> AM=${r.alamatMuaddalah}, ET=${r.jamEt}, DeltaT=${r.deltaTDetik}, UT=${r.jamUt}, tanggal UT=${r.gregorianDay}-${r.gregorianMonth}-${r.gregorianYear}, dayShift=${wib.dayShift}, jamWib=${wib.jamLokal}, hari WIB=${wib.hariMingguan} ${wib.hariPasaran}")
        assertEquals(2013, r.gregorianYear)
        assertEquals(8, r.gregorianMonth)
        assertEquals(6, r.gregorianDay) // tanggal UT, sesuai "K=6(UT)" di kitab
        assertEquals(1, wib.dayShift) // WIB jatuh di tanggal berikutnya
        assertEquals("Rabu", wib.hariMingguan)
        assertEquals("Pon", wib.hariPasaran)
        assertEquals(4.8450, wib.jamLokal, 0.02) // 04:50:42.1 = 4.8450 jam desimal
    }

    @Test
    fun `ijtima akhir Shafar 1434H`() {
        // Ekspektasi kitab (halaman 32-35): Jum'at Kliwon, 11 Januari 2013 M,
        // hisab hilal lokasi VANCOUVER CANADA (TZ-8) -- BUKAN Kuwait.
        val r = AdDurrulAniqIjtimaCalculator.calculate(1434, 2)
        val vancouver = r.waktuDaerah(-8.0)
        println("Shafar 1434H -> AM=${r.alamatMuaddalah}, ET=${r.jamEt}, DeltaT=${r.deltaTDetik}, UT=${r.jamUt}, tanggal UT=${r.gregorianDay}-${r.gregorianMonth}-${r.gregorianYear}, dayShift=${vancouver.dayShift}, jamVancouver=${vancouver.jamLokal}, hari=${vancouver.hariMingguan} ${vancouver.hariPasaran}")
        assertEquals(2013, r.gregorianYear)
        assertEquals(1, r.gregorianMonth)
        assertEquals(11, r.gregorianDay)
        assertEquals(0, vancouver.dayShift)
        assertEquals("Jum'at", vancouver.hariMingguan)
        assertEquals("Kliwon", vancouver.hariPasaran)
        assertEquals(11.7571, vancouver.jamLokal, 0.02) // 11:45:25.5 = 11.7571 jam desimal
    }

    @Test
    fun `ijtima akhir Shafar -52H`() {
        // Ekspektasi kitab: Jum'at Legi, 10 April 571 M, 09:54:40.4 (Makkah TZ+3)
        val r = AdDurrulAniqIjtimaCalculator.calculate(-52, 2)
        println("Shafar -52H -> AM=${r.alamatMuaddalah}, ET=${r.jamEt}, DeltaT=${r.deltaTDetik}, UT=${r.jamUt}, tanggal=${r.gregorianDay}-${r.gregorianMonth}-${r.gregorianYear}, hari=${r.hariMingguan} ${r.hariPasaran}")
        assertEquals(571, r.gregorianYear)
        assertEquals(4, r.gregorianMonth)
        assertEquals(10, r.gregorianDay)
        assertEquals("Jum'at", r.hariMingguan)
        assertEquals("Legi", r.hariPasaran)
    }
}
