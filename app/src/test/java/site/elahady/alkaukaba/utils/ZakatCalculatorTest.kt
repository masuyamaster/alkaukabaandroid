package site.elahady.alkaukaba.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure logic test untuk [ZakatCalculator] (tanpa Android/API/DB) - mengecek formula fitrah,
 * mal, dan profesi sesuai keputusan desain di docs/features/zakat.md §6 (2.5 kg beras/jiwa,
 * nisab 85 gram emas, tarif 2.5%, harta bersih tidak boleh negatif).
 */
class ZakatCalculatorTest {

    @Test
    fun `fitrah - total beras dan uang mengikuti 2,5 kg per jiwa`() {
        val hasil = ZakatCalculator.hitungFitrah(jumlahJiwa = 4, hargaBerasPerKg = 13000.0)
        assertEquals(10.0, hasil.totalBerasKg, 1e-9)
        assertEquals(130000.0, hasil.totalUang, 1e-9)
        assertEquals(4, hasil.jumlahJiwa)
    }

    @Test
    fun `fitrah - satu jiwa`() {
        val hasil = ZakatCalculator.hitungFitrah(jumlahJiwa = 1, hargaBerasPerKg = 12000.0)
        assertEquals(2.5, hasil.totalBerasKg, 1e-9)
        assertEquals(30000.0, hasil.totalUang, 1e-9)
    }

    @Test
    fun `mal - harta bersih di atas nisab wajib zakat 2,5 persen`() {
        // nisab = 85 * 500.000 = 42.500.000; harta bersih 50 juta > nisab
        val hasil = ZakatCalculator.hitungMal(totalHarta = 50_000_000.0, totalHutang = 0.0, hargaEmasPerGram = 500_000.0)
        assertEquals(42_500_000.0, hasil.nisabRupiah, 1e-6)
        assertEquals(50_000_000.0, hasil.hartaBersih, 1e-6)
        assertTrue(hasil.wajibZakat)
        assertEquals(1_250_000.0, hasil.jumlahZakat, 1e-6)
    }

    @Test
    fun `mal - harta bersih di bawah nisab tidak wajib zakat`() {
        val hasil = ZakatCalculator.hitungMal(totalHarta = 10_000_000.0, totalHutang = 0.0, hargaEmasPerGram = 500_000.0)
        assertFalse(hasil.wajibZakat)
        assertEquals(0.0, hasil.jumlahZakat, 1e-9)
    }

    @Test
    fun `mal - hutang mengurangi harta bersih sebelum dibandingkan ke nisab`() {
        // nisab = 42.500.000; harta 50 juta - hutang 10 juta = 40 juta, di bawah nisab
        val hasil = ZakatCalculator.hitungMal(totalHarta = 50_000_000.0, totalHutang = 10_000_000.0, hargaEmasPerGram = 500_000.0)
        assertEquals(40_000_000.0, hasil.hartaBersih, 1e-6)
        assertFalse(hasil.wajibZakat)
    }

    @Test
    fun `mal - hutang lebih besar dari harta tidak menghasilkan harta bersih negatif`() {
        val hasil = ZakatCalculator.hitungMal(totalHarta = 5_000_000.0, totalHutang = 20_000_000.0, hargaEmasPerGram = 500_000.0)
        assertEquals(0.0, hasil.hartaBersih, 1e-9)
        assertFalse(hasil.wajibZakat)
        assertEquals(0.0, hasil.jumlahZakat, 1e-9)
    }

    @Test
    fun `mal - tepat di nisab tetap wajib zakat (pakai lebih-besar-sama-dengan)`() {
        val hasil = ZakatCalculator.hitungMal(totalHarta = 42_500_000.0, totalHutang = 0.0, hargaEmasPerGram = 500_000.0)
        assertTrue(hasil.wajibZakat)
        assertEquals(1_062_500.0, hasil.jumlahZakat, 1e-6)
    }

    @Test
    fun `profesi - penghasilan setahun di atas nisab wajib zakat 2,5 persen`() {
        val hasil = ZakatCalculator.hitungProfesi(totalPenghasilanSetahun = 120_000_000.0, hargaEmasPerGram = 1_000_000.0)
        assertEquals(85_000_000.0, hasil.nisabRupiah, 1e-6)
        assertTrue(hasil.wajibZakat)
        assertEquals(3_000_000.0, hasil.jumlahZakat, 1e-6)
    }

    @Test
    fun `profesi - penghasilan setahun di bawah nisab tidak wajib zakat`() {
        val hasil = ZakatCalculator.hitungProfesi(totalPenghasilanSetahun = 50_000_000.0, hargaEmasPerGram = 1_000_000.0)
        assertFalse(hasil.wajibZakat)
        assertEquals(0.0, hasil.jumlahZakat, 1e-9)
    }
}
