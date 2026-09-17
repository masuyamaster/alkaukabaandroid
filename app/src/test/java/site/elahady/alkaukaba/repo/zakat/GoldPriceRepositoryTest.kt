package site.elahady.alkaukaba.repo.zakat

import org.junit.After
import org.junit.Test

/**
 * Mock test untuk [GoldPriceRepository] (Jalur 2 di docs/strategi-unit-test.md - dependency
 * eksternal lewat Retrofit). SKELETON - nama skenario disepakati dulu di sini sebelum diisi
 * Arrange-Act-Assert, ikuti alur "Alur menulis satu unit test" di strategi-unit-test.md.
 *
 * CATATAN TEKNIK MOCKING (perlu didiskusikan/divalidasi sebelum diisi):
 * `GoldPriceRepository.api` adalah `private val` yang diambil dari singleton `object`
 * [site.elahady.alkaukaba.api.GoldPriceRetrofitClient.instance] saat object
 * [GoldPriceRepository] pertama kali dipakai di JVM test yang sama (persis pola
 * `NearbyMosqueRepository` - dependency-nya bukan lewat constructor). Konsekuensinya:
 * `mockkObject(GoldPriceRetrofitClient)` + `every { GoldPriceRetrofitClient.instance } returns
 * mockApi` HARUS dipasang sebelum panggilan pertama ke `GoldPriceRepository` di proses test ini
 * (kalau tidak, `api` sudah kadung terikat ke instance asli). Belum ada contoh pola ini di repo
 * (repo/ semuanya masih di backlog Notion) - HilalViewModelTest cuma mock object yang
 * fungsi-nya dipanggil langsung, bukan yang dipakai buat inisialisasi `private val` lain.
 */
class GoldPriceRepositoryTest {

    @After
    fun tearDown() {
        // TODO: unmockkAll() setelah mockkObject dipasang (lihat strategi-unit-test.md Jalur 2)
    }

    @Test
    fun `sukses - pilih entry gramasi 1 gram dengan materialType 'LM Antam produksi tahun'`() {
        TODO("Arrange: mock GoldPriceApi.getAntamPrices() return beberapa entry 1 gram " +
            "(termasuk varian Certicard) + entry gramasi lain (misal weight=2) yang harus " +
            "diabaikan. Assert: Resource.Success dengan sellPrice dari entry 'LM Antam produksi " +
            "tahun', BUKAN entry Certicard walau sama-sama weight=1.")
    }

    @Test
    fun `fallback - tidak ada entry 'LM Antam produksi tahun', pakai entry 1 gram pertama`() {
        TODO("Arrange: mock response yang cuma punya entry Certicard untuk weight=1 (tanpa " +
            "'LM Antam produksi tahun' sama sekali). Assert: Resource.Success tetap balik " +
            "(fallback jalan), bukan Resource.Error.")
    }

    @Test
    fun `gagal - response tidak successful (misal HTTP 500)`() {
        TODO("Arrange: mock Response.error(...). Assert: Resource.Error dengan pesan yang " +
            "menyebut kode HTTP.")
    }

    @Test
    fun `gagal - body null atau data kosong`() {
        TODO("Arrange: mock response sukses tapi body/data null atau list kosong. Assert: " +
            "Resource.Error, bukan crash NPE.")
    }

    @Test
    fun `gagal - exception saat request (misal tidak ada koneksi internet)`() {
        TODO("Arrange: mock api melempar IOException. Assert: Resource.Error dengan pesan " +
            "fallback yang menyebut 'periksa koneksi internet' (lihat pesan di kode asli).")
    }
}
