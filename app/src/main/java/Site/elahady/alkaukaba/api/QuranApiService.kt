package site.elahady.alkaukaba.api

import site.elahady.alkaukaba.model.AyatSearchResponse
import site.elahady.alkaukaba.model.QuranApiResponse
import site.elahady.alkaukaba.model.Surah
import site.elahady.alkaukaba.model.SurahDetail
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

interface EquranApi {
    @GET("api/v2/surat")
    suspend fun getSurahList(): Response<QuranApiResponse<List<Surah>>>

    @GET("api/v2/surat/{nomor}")
    suspend fun getSurahDetail(@Path("nomor") nomor: Int): Response<QuranApiResponse<SurahDetail>>
}

object QuranRetrofitClient {
    private const val BASE_URL = "https://equran.id/"

    val instance: EquranApi by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(EquranApi::class.java)
    }
}

/** equran.id tidak punya endpoint search ayat, jadi khusus pencarian ayat pakai API publik
 * terpisah ini (alquran.cloud) yang menyediakan full-text search terjemahan Indonesia lintas
 * seluruh Al-Qur'an. */
interface AlQuranCloudApi {
    @GET("v1/search/{keyword}/all/id.indonesian")
    suspend fun searchAyat(@Path("keyword") keyword: String): Response<AyatSearchResponse>
}

object AlQuranCloudRetrofitClient {
    private const val BASE_URL = "https://api.alquran.cloud/"

    val instance: AlQuranCloudApi by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(AlQuranCloudApi::class.java)
    }
}
