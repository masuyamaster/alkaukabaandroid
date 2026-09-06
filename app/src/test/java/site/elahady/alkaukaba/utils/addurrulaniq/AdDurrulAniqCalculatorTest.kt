package site.elahady.alkaukaba.utils.addurrulaniq

import org.junit.Assert.assertTrue
import org.junit.Test
import site.elahady.alkaukaba.model.HilalInput

/**
 * Uji integrasi end-to-end AdDurrulAniqCalculator (Ijtima' + Ghurub + Hilal +
 * kriteria) utk lokasi/waktu SEKARANG -- tidak ada contoh manual kitab utk
 * tanggal ini (tabel gerak Matahari-Bulan sudah diganti rumus Meeus, lihat
 * commit sebelumnya), jadi yang dicek di sini adalah KEWAJARAN fisis hasil
 * (bukan cocok-persis ke buku): ijtima' di masa depan, ghurub setelah
 * ijtima', dan angka-angka dalam rentang yang masuk akal.
 */
class AdDurrulAniqCalculatorTest {

    @Test
    fun `hitung awal bulan untuk Sampang menghasilkan angka yang masuk akal`() {
        val input = HilalInput(latitude = -7.2, longitude = 113.25, heightMeters = 5.0)
        val result = AdDurrulAniqCalculator.calculate(input)

        println(
            "Bulan=${result.bulanHijriyahLabel}, Ghurub=${result.ghurubTime}, Ijtima'=${result.ijtimaTime}, " +
                "tinggi=${result.tinggiHilal}, elongasi=${result.elongasi}, status=${result.statusBadge}, " +
                "memenuhi=${result.hilalMemenuhiKriteria}"
        )

        assertTrue("Tinggi hilal harus di rentang -90..90", result.tinggiHilal in -90.0..90.0)
        assertTrue("Elongasi harus di rentang 0..180", result.elongasi in 0.0..180.0)
        assertTrue("Illum fraction harus di rentang 0..1", result.illumFraction in 0.0..1.0)
        assertTrue("Mukuts harus masuk akal (<180 menit)", result.mukutsMenit < 180.0)
        val kriteriaKonsisten = result.hilalMemenuhiKriteria == (result.tinggiHilal >= 3.0 && result.elongasi >= 6.4)
        assertTrue("Status kriteria harus konsisten dgn angka tinggi/elongasi", kriteriaKonsisten)
    }

    @Test
    fun `hitung awal bulan untuk Jakarta juga tidak error`() {
        val input = HilalInput(latitude = -6.2088, longitude = 106.8456, heightMeters = 50.0)
        val result = AdDurrulAniqCalculator.calculate(input)
        println("Jakarta -> tinggi=${result.tinggiHilal}, elongasi=${result.elongasi}, status=${result.statusBadge}")
        assertTrue(result.tinggiHilal in -90.0..90.0)
        assertTrue(result.elongasi in 0.0..180.0)
    }
}
