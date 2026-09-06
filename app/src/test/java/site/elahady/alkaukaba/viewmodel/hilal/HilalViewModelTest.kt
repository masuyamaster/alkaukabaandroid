package site.elahady.alkaukaba.viewmodel.hilal

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import site.elahady.alkaukaba.model.HilalInput
import site.elahady.alkaukaba.model.HilalResult
import site.elahady.alkaukaba.utils.EphemerisCalculator
import site.elahady.alkaukaba.utils.SessionManager
import site.elahady.alkaukaba.utils.addurrulaniq.AdDurrulAniqCalculator

/**
 * Beda dengan EphemerisCalculatorTest (golden test, tanpa mock): `HilalViewModel` ada
 * DEPENDENCY EKSTERNAL (EphemerisCalculator, AdDurrulAniqCalculator) -- jadi ini jalur
 * MOCK. Yang diuji di sini BUKAN "apakah kalkulasinya benar" (itu sudah dicover
 * EphemerisCalculatorTest/AdDurrulAniqCalculatorTest), tapi "apakah ViewModel manggil
 * calculator yang tepat & meneruskan hasilnya dengan benar" -- murni soal WIRING.
 *
 * TEKNIK: `EphemerisCalculator`/`AdDurrulAniqCalculator` itu Kotlin `object` (singleton),
 * bukan interface yang di-inject ke constructor (beda dari `PrayerRepository` yang
 * menerima `AladhanApi` lewat constructor). Object begini tidak bisa di-mock lewat
 * `mockk<T>()` biasa -- perlu `mockkObject(EphemerisCalculator)` lalu
 * `every { EphemerisCalculator.calculate(any()) } returns fakeResult` ("static mocking").
 * Wajib `unmockkAll()` di `@After`, karena object itu singleton JVM-wide -- kalau tidak
 * di-unmock, mock-nya "bocor" ke test lain yang jalan setelahnya.
 *
 * CATATAN: `calculateHilal()` di `HilalViewModel` itu SINKRON (bukan `suspend`, tidak
 * pakai `viewModelScope.launch`) -- jadi TIDAK butuh `runTest`/`advanceUntilIdle()` sama
 * sekali, beda dari `PrayerTimesViewModel.fetchPrayerTimes()`. Bukti bahwa tidak semua
 * ViewModel butuh perkakas yang sama -- lihat dulu kodenya, baru pilih alatnya.
 *
 * CATATAN LAIN (2026-09-06): `HilalViewModel.generatePdf()` yang sebelumnya ada di sini
 * SUDAH DIHAPUS dari kode -- direfactor jadi alur baru lewat `LaporanHisabActivity` +
 * `HilalPdfService.exportViewAsPdf()` (screenshot View jadi PDF, bukan lagi
 * `calculationLog` polos). Refactor itu masih berjalan/belum di-commit saat file test ini
 * ditulis, jadi test untuk alur baru itu SENGAJA belum ditulis di sini -- jadi area
 * terpisah nanti setelah refactor-nya stabil.
 */
class HilalViewModelTest {

    // WAJIB karena test ini baca LiveData.value (calculationResult)
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        mockkObject(EphemerisCalculator)
        mockkObject(AdDurrulAniqCalculator)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    /** Data mock "asal boleh", karena yang diuji di sini WIRING, bukan keakuratan hisab. */
    private fun fakeHilalResult(penanda: String) = HilalResult(
        bulanHijriyahLabel = penanda,
        tanggalGhurubLabel = "01 Januari 2026",
        statusBadge = "Hilal Mungkin Terlihat",
        hilalMemenuhiKriteria = true,
        tinggiHilal = 5.0,
        elongasi = 8.0,
        mukutsMenit = 20.0,
        azimuthHilal = 260.0,
        azimuthMatahari = 258.0,
        tinggiMatahari = -1.0,
        illumFraction = 0.02,
        ijtimaTime = "01 Januari 2026, 10:00:00",
        ghurubTime = "01 Januari 2026, 17:30:00",
        breakdownSections = emptyList(),
        calculationLog = "log-$penanda"
    )

    @Test
    fun `calculateHilal dengan method default memanggil EphemerisCalculator dan mengisi calculationResult`() {
        val fakeResult = fakeHilalResult("dari EphemerisCalculator")
        every { EphemerisCalculator.calculate(any()) } returns fakeResult

        val viewModel = HilalViewModel()
        viewModel.calculateHilal(lat = -6.2088, lng = 106.8456, heightMeters = 50.0)

        assertEquals(fakeResult, viewModel.calculationResult.value)
        verify(exactly = 1) { EphemerisCalculator.calculate(any()) }
        verify(exactly = 0) { AdDurrulAniqCalculator.calculate(any()) }
    }

    @Test
    fun `calculateHilal dengan method Durrul Aniq memanggil AdDurrulAniqCalculator, bukan EphemerisCalculator`() {
        val fakeResult = fakeHilalResult("dari AdDurrulAniqCalculator")
        every { AdDurrulAniqCalculator.calculate(any()) } returns fakeResult

        val viewModel = HilalViewModel()
        viewModel.calculateHilal(
            lat = -7.2,
            lng = 113.25,
            heightMeters = 5.0,
            method = SessionManager.HISAB_AWAL_BULAN_DURRUL_ANIQ
        )

        assertEquals(fakeResult, viewModel.calculationResult.value)
        verify(exactly = 1) { AdDurrulAniqCalculator.calculate(any()) }
        verify(exactly = 0) { EphemerisCalculator.calculate(any()) }
    }

    @Test
    fun `calculateHilal membentuk HilalInput dari lat, lng, heightMeters dengan benar`() {
        every { EphemerisCalculator.calculate(any()) } returns fakeHilalResult("x")

        val viewModel = HilalViewModel()
        viewModel.calculateHilal(lat = -6.2088, lng = 106.8456, heightMeters = 50.0)

        // HilalInput data class -> equals berbasis isi, bukan referensi
        verify { EphemerisCalculator.calculate(HilalInput(latitude = -6.2088, longitude = 106.8456, heightMeters = 50.0)) }
    }
}
