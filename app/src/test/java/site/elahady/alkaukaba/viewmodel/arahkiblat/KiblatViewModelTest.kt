package site.elahady.alkaukaba.viewmodel.arahkiblat

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import site.elahady.alkaukaba.repo.arahkiblat.KiblatRepository

/**
 * SKELETON — daftar skenario dulu, isi diputuskan bareng sebelum ditulis assertion-nya.
 * Semua nama fungsi di bawah masih `TODO()`.
 *
 * Beda dengan `HilalViewModelTest` (dependency berupa Kotlin `object`, pakai
 * `mockkObject`): `KiblatViewModel` menerima `KiblatRepository` lewat CONSTRUCTOR --
 * pola yang sama dengan `PrayerRepository` yang didiskusikan di awal. Jadi di sini
 * dipakai teknik 1 dari docs/strategi-unit-test.md: `mockk<KiblatRepository>()` +
 * `coEvery { ... } returns ...` (bukan `mockkObject`).
 *
 * TEKNIK BARU yang belum pernah dipakai di project ini: `fetchQiblaAngle()` pakai
 * `viewModelScope.launch` (coroutine beneran) -- jadi ini test ViewModel PERTAMA yang
 * butuh `kotlinx-coroutines-test` (`runTest` + dispatcher test). Tanpa
 * `Dispatchers.setMain(testDispatcher)`, `viewModelScope.launch` akan coba pakai
 * `Dispatchers.Main` Android asli yang tidak ada di unit test JVM biasa -> crash
 * `IllegalStateException: Module with the Main dispatcher had failed to initialize`.
 *
 * Skenario ini juga yang pertama menguji PATH ERROR (try/catch) di ViewModel --
 * `KiblatRepository.getQiblaAngle()` juga suspend & bisa melempar Exception (dibungkus
 * `withContext(Dispatchers.IO)` di KiblatRepository, lihat repo-nya).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class KiblatViewModelTest {

    // WAJIB karena test ini baca LiveData.value (qiblaAngle, error)
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: KiblatRepository
    private lateinit var viewModel: KiblatViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        viewModel = KiblatViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `fetchQiblaAngle sukses mengisi qiblaAngle, bukan error`() {
        // coEvery { repository.getQiblaAngle(any(), any()) } returns 294.0
        // -- ingat: viewModelScope.launch itu asynchronous, WAJIB
        // testDispatcher.scheduler.advanceUntilIdle() sebelum assert .value, kalau
        // tidak LiveData masih null saat dicek.
        TODO()
    }

    @Test
    fun `fetchQiblaAngle gagal melempar exception mengisi error, qiblaAngle tetap kosong`() {
        // coEvery { repository.getQiblaAngle(any(), any()) } throws RuntimeException(...)
        // -- verify pesan error yang di-set persis "Gagal mengambil arah kiblat"
        // (hardcoded di KiblatViewModel, bukan e.message -- perlu didiskusikan apakah ini
        // sudah cukup informatif buat user, atau perlu dicatat sebagai catatan terpisah)
        TODO()
    }

    @Test
    fun `fetchQiblaAngle meneruskan lat dan lon yang benar ke repository`() {
        // coVerify { repository.getQiblaAngle(lat, lon) } dengan angka spesifik
        TODO()
    }
}
