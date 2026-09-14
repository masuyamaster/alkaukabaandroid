package site.elahady.alkaukaba.repo

import site.elahady.alkaukaba.api.AlQuranCloudRetrofitClient
import site.elahady.alkaukaba.api.QuranRetrofitClient
import site.elahady.alkaukaba.model.AyatSearchMatch
import site.elahady.alkaukaba.model.JuzAyat
import site.elahady.alkaukaba.model.JuzBoundaries
import site.elahady.alkaukaba.model.JuzDetail
import site.elahady.alkaukaba.model.Surah
import site.elahady.alkaukaba.model.SurahDetail
import site.elahady.alkaukaba.utils.Resource

/** Batas jumlah hasil pencarian ayat yang ditampilkan - query umum ("Allah", dsb) bisa balikin
 * ribuan match dari alquran.cloud, tidak realistis dirender semua di satu RecyclerView. */
private const val MAX_AYAT_SEARCH_RESULTS = 30

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
    private val searchApi = AlQuranCloudRetrofitClient.instance

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

    /** Nama Latin surah dari cache daftar surah (equran.id) - dipakai untuk menampilkan nama
     * surah yang benar di hasil pencarian ayat, karena penulisan nama surah di alquran.cloud
     * (mis. "Al-Faatiha") beda dengan equran.id (mis. "Al-Fatihah"). Null kalau daftar surah
     * belum pernah dimuat (mestinya sudah, karena layar daftar surah memuatnya duluan). */
    fun getCachedSurahName(nomor: Int): String? = surahListCache?.find { it.nomor == nomor }?.namaLatin

    /** Susun ayat satu Juz dari data per-surah equran.id yang sudah ada (lewat [getSurahDetail],
     * jadi ikut ter-cache) - disaring pakai [JuzBoundaries], bukan dari API/endpoint Juz
     * terpisah (equran.id tidak punya). Kalau Juz merentang beberapa surah, tiap surah di-fetch
     * satu per satu lalu digabung sesuai urutan ruasnya. */
    suspend fun getJuzDetail(nomorJuz: Int): Resource<JuzDetail> {
        val segments = JuzBoundaries.segments[nomorJuz]
            ?: return Resource.Error("Data juz tidak ditemukan")

        val ayatList = mutableListOf<JuzAyat>()
        for (segment in segments) {
            val result = getSurahDetail(segment.surahNumber)
            val detail = (result as? Resource.Success)?.data
                ?: return Resource.Error(result.message ?: "Gagal memuat data juz")

            detail.ayat
                .filter { it.nomorAyat in segment.startAyat..segment.endAyat }
                .forEachIndexed { index, ayat ->
                    ayatList.add(JuzAyat(ayat, segment.surahNumber, detail.namaLatin, isFirstOfSurah = index == 0))
                }
        }

        return Resource.Success(JuzDetail(nomorJuz, ayatList))
    }

    suspend fun searchAyat(keyword: String): Resource<List<AyatSearchMatch>> {
        return try {
            val response = searchApi.searchAyat(keyword)
            when {
                response.code() == 404 -> Resource.Success(emptyList())
                response.isSuccessful && response.body() != null ->
                    Resource.Success(response.body()!!.data.matches.take(MAX_AYAT_SEARCH_RESULTS))
                else -> Resource.Error(response.message())
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Gagal mencari ayat")
        }
    }
}
