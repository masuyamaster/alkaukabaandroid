package site.elahady.alkaukaba.repo

import site.elahady.alkaukaba.api.QuranRetrofitClient
import site.elahady.alkaukaba.model.Surah
import site.elahady.alkaukaba.model.SurahDetail
import site.elahady.alkaukaba.utils.Resource

/** Qori default untuk audio murottal - key sesuai `audio`/`audioFull` map dari API equran.id
 * ("05" = Misyari Rasyid Al-Afasi). Belum ada UI pemilihan qori, jadi disimpan di satu
 * tempat supaya gampang diganti/dijadikan preference nanti. */
const val DEFAULT_QORI_KEY = "05"

/** Data Al-Qur'an (teks + audio) dari equran.id tidak pernah berubah, jadi di-cache in-memory
 * di sini - object singleton (bukan class), tidak butuh Context/session seperti
 * [PrayerRepository], supaya balik dari detail ke daftar surah tidak fetch API berulang.
 * Belum ada local DB (Room) di repo ini, jadi cache ini hilang begitu proses app dimatikan
 * (tidak ada dukungan offline penuh). */
object QuranRepository {

    private val api = QuranRetrofitClient.instance

    private var surahListCache: List<Surah>? = null
    private val surahDetailCache = mutableMapOf<Int, SurahDetail>()

    suspend fun getSurahList(): Resource<List<Surah>> {
        surahListCache?.let { return Resource.Success(it) }

        return try {
            val response = api.getSurahList()
            if (response.isSuccessful && response.body() != null) {
                val list = response.body()!!.data
                surahListCache = list
                Resource.Success(list)
            } else {
                Resource.Error(response.message())
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Gagal memuat daftar surah")
        }
    }

    suspend fun getSurahDetail(nomor: Int): Resource<SurahDetail> {
        surahDetailCache[nomor]?.let { return Resource.Success(it) }

        return try {
            val response = api.getSurahDetail(nomor)
            if (response.isSuccessful && response.body() != null) {
                val detail = response.body()!!.data
                surahDetailCache[nomor] = detail
                Resource.Success(detail)
            } else {
                Resource.Error(response.message())
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Gagal memuat surah")
        }
    }
}
