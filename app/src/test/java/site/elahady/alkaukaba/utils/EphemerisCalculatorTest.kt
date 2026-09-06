package site.elahady.alkaukaba.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import site.elahady.alkaukaba.model.HilalInput
import site.elahady.alkaukaba.utils.addurrulaniq.AdDurrulAniqCalculator
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.abs

/**
 * `EphemerisCalculator.calculate()` murni logic (Observer + Astronomy Engine, tanpa
 * Android/API/DB) dan mencari ijtima' dari `System.currentTimeMillis()` -- sama seperti
 * `AdDurrulAniqCalculatorTest`, jadi jalurnya GOLDEN/REFERENCE TEST, bukan mock: hasil
 * dibanding ke rentang fisis yang masuk akal dan/atau invariant algoritma, bukan dipalsukan.
 *
 * Format waktu string (`ijtimaTime`/`ghurubTime`) di-parse balik pakai pola yang sama
 * dengan `formatLocalTime()` di kedua calculator ("dd MMMM yyyy, HH:mm:ss", locale id-ID)
 * supaya bisa dibandingkan sebagai angka (epoch millis), bukan string.
 */
class EphemerisCalculatorTest {

    private val waktuFormat = SimpleDateFormat("dd MMMM yyyy, HH:mm:ss", Locale("id", "ID"))

    // --- Kewajaran fisis dasar (pola sama dgn AdDurrulAniqCalculatorTest) ---

    @Test
    fun `hitung hilal untuk Sampang menghasilkan angka-angka dalam rentang fisis yang masuk akal`() {
        val input = HilalInput(latitude = -7.2, longitude = 113.25, heightMeters = 5.0)
        val result = EphemerisCalculator.calculate(input)

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
    fun `hitung hilal untuk Jakarta juga tidak error dan angkanya masuk akal`() {
        val input = HilalInput(latitude = -6.2088, longitude = 106.8456, heightMeters = 50.0)
        val result = EphemerisCalculator.calculate(input)

        println("Jakarta -> tinggi=${result.tinggiHilal}, elongasi=${result.elongasi}, status=${result.statusBadge}")
        assertTrue(result.tinggiHilal in -90.0..90.0)
        assertTrue(result.elongasi in 0.0..180.0)
    }

    @Test
    fun `hitung hilal untuk lokasi lintang tinggi mendekati kutub melempar exception saat matahari tidak terbenam`() {
        // Di lintang ekstrem dekat kutub ada musim matahari tidak pernah terbenam/terbit
        // (polar day/night) -- dikonfirmasi INI PERILAKU YANG DIINGINKAN, bukan bug:
        // EphemerisCalculator.calculate() sengaja throw IllegalStateException (baris 55/58),
        // bukan menangani secara diam-diam.
        //
        // CATATAN: test ini bergantung musim saat dijalankan (lintang ekstrem + waktu
        // SEKARANG, sama seperti test kewajaran fisis lainnya di file ini). Di sekitar
        // ekuinoks (~20-24 Maret / ~20-24 September) matahari baru mulai terbit/terbenam
        // lagi di kutub, jadi hasilnya bisa beda pada tanggal-tanggal itu.
        val input = HilalInput(latitude = -89.9, longitude = 0.0, heightMeters = 0.0)

        assertThrows(IllegalStateException::class.java) {
            EphemerisCalculator.calculate(input)
        }
    }

    // --- Invariant yang HARUS selalu benar dari urutan algoritma ---

    @Test
    fun `ijtima' hasil hitung selalu di masa depan relatif waktu sekarang`() {
        val sebelum = System.currentTimeMillis()
        val input = HilalInput(latitude = -6.2088, longitude = 106.8456, heightMeters = 50.0)
        val result = EphemerisCalculator.calculate(input)

        val ijtimaMillis = waktuFormat.parse(result.ijtimaTime)!!.time
        assertTrue(
            "Ijtima' ($ijtimaMillis) harus setelah waktu test mulai dijalankan ($sebelum)",
            ijtimaMillis > sebelum
        )
    }

    @Test
    fun `ghurub yang dipakai selalu setelah waktu ijtima'`() {
        // Invariant yang sengaja dijaga kode (EphemerisCalculator.kt baris 56-59): geser ke
        // ghurub keesokan hari kalau ijtima' terjadi setelah ghurub hari itu.
        val input = HilalInput(latitude = -6.2088, longitude = 106.8456, heightMeters = 50.0)
        val result = EphemerisCalculator.calculate(input)

        val ijtimaMillis = waktuFormat.parse(result.ijtimaTime)!!.time
        val ghurubMillis = waktuFormat.parse(result.ghurubTime)!!.time
        assertTrue("Ghurub ($ghurubMillis) harus >= ijtima' ($ijtimaMillis)", ghurubMillis >= ijtimaMillis)
    }

    @Test
    fun `statusBadge selalu konsisten dengan angka tinggiHilal dan elongasi`() {
        val input = HilalInput(latitude = -6.2088, longitude = 106.8456, heightMeters = 50.0)
        val result = EphemerisCalculator.calculate(input)

        val seharusnyaMemenuhi = result.tinggiHilal >= 3.0 && result.elongasi >= 6.4
        assertEquals(seharusnyaMemenuhi, result.hilalMemenuhiKriteria)
        assertEquals(
            if (seharusnyaMemenuhi) "Hilal Mungkin Terlihat" else "Belum Memenuhi Kriteria — Istikmal",
            result.statusBadge
        )
    }

    // --- Struktur output (dipakai UI accordion + export PDF) ---

    @Test
    fun `breakdownSections berisi 5 seksi sesuai urutan buku Ephemeris Hisab Rukyat`() {
        val input = HilalInput(latitude = -6.2088, longitude = 106.8456, heightMeters = 50.0)
        val result = EphemerisCalculator.calculate(input)

        val labelUrut = result.breakdownSections.map { it.prayerLabel }
        assertEquals(
            listOf(
                "Markaz",
                "Ijtima' (Konjungsi)",
                "Data Matahari saat Ghurub",
                "Data Bulan saat Ghurub",
                "Kesimpulan Kriteria (Neo-MABIMS)"
            ),
            labelUrut
        )
    }

    @Test
    fun `calculationLog memuat label semua seksi dan tidak kosong`() {
        val input = HilalInput(latitude = -6.2088, longitude = 106.8456, heightMeters = 50.0)
        val result = EphemerisCalculator.calculate(input)

        assertTrue(result.calculationLog.isNotBlank())
        result.breakdownSections.forEach { section ->
            assertTrue(
                "calculationLog harus memuat label '${section.prayerLabel}'",
                result.calculationLog.uppercase(Locale.getDefault())
                    .contains(section.prayerLabel.uppercase(Locale.getDefault()))
            )
        }
    }

    // --- Validasi silang dengan mesin hisab lain ---

    @Test
    fun `ijtima' dan ghurub dari EphemerisCalculator berdekatan dengan hasil AdDurrulAniqCalculator untuk input sama`() {
        // TOLERANSI BERBASIS RISET (masih ditandai provisional -- lihat memo riset):
        // - EphemerisCalculator (Astronomy Engine) diklaim akurat ±1 arcminute vs VSOP87/
        //   NOVAS/JPL Horizons (github.com/cosinekitty/astronomy). Kecepatan relatif
        //   Bulan-Matahari ~0.5 arcmin/menit -> 1 arcmin error posisi kira-kira setara
        //   ~2 menit error waktu ijtima'.
        // - AdDurrulAniqCalculator sendiri divalidasi vs contoh kitab dgn toleransi
        //   deklinasi/tinggi ±0.01-0.05 derajat tapi azimuth ±0.5-1.0 derajat
        //   (AdDurrulAniqHilalCalculatorTest) -- lebih kasar dari Astronomy Engine.
        // - Literatur falak (studi banding hisab kontemporer vs klasik) umumnya
        //   menganggap selisih ijtima' orde menit-detik sebagai wajar, bukan orde jam.
        // Angka di bawah: buffer ~3-5x dari estimasi teori + 1 sampel empiris (2.2/0.9
        // menit) -- longgar cukup utk variasi tanggal, ketat cukup utk nangkep bug nyata
        // (mis. salah hari/unit). Masih perlu poin riset #4 (banyak tanggal) buat finalisasi.
        val toleransiIjtimaMenit = 10.0
        val toleransiGhurubMenit = 5.0

        val input = HilalInput(latitude = -6.2088, longitude = 106.8456, heightMeters = 50.0)
        val ephemeris = EphemerisCalculator.calculate(input)
        val durrulAniq = AdDurrulAniqCalculator.calculate(input)

        val selisihIjtimaMenit = abs(
            waktuFormat.parse(ephemeris.ijtimaTime)!!.time - waktuFormat.parse(durrulAniq.ijtimaTime)!!.time
        ) / 60000.0
        val selisihGhurubMenit = abs(
            waktuFormat.parse(ephemeris.ghurubTime)!!.time - waktuFormat.parse(durrulAniq.ghurubTime)!!.time
        ) / 60000.0

        println("Selisih ijtima'=$selisihIjtimaMenit menit, selisih ghurub=$selisihGhurubMenit menit")

        assertTrue(
            "Selisih ijtima' $selisihIjtimaMenit menit, melebihi toleransi sementara $toleransiIjtimaMenit menit",
            selisihIjtimaMenit <= toleransiIjtimaMenit
        )
        assertTrue(
            "Selisih ghurub $selisihGhurubMenit menit, melebihi toleransi sementara $toleransiGhurubMenit menit",
            selisihGhurubMenit <= toleransiGhurubMenit
        )
    }
}
