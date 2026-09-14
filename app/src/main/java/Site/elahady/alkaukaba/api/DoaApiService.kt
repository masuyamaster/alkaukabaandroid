package site.elahady.alkaukaba.api

import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import site.elahady.alkaukaba.model.DoaApiResponse
import site.elahady.alkaukaba.model.DoaCategory
import site.elahady.alkaukaba.model.DoaCategoryDetail

interface DoaApi {
    @GET("api/doa-categories")
    suspend fun getCategories(): Response<DoaApiResponse<List<DoaCategory>>>

    @GET("api/doa-categories/{slug}")
    suspend fun getCategoryItems(@Path("slug") slug: String): Response<DoaApiResponse<DoaCategoryDetail>>
}

object DoaRetrofitClient {
    // Backend sendiri (alkaukabawebserver, endpoint /api/doa-categories*), base URL sama
    // dengan AuthClient. Untuk testing lokal: ganti ke "http://127.0.0.1:8000/" +
    // `adb reverse tcp:8000 tcp:8000` + `php artisan serve` di alkaukabawebserver
    // (lihat CLAUDE.md bagian "Konfigurasi API backend").
    private const val BASE_URL = "https://alkaukaba.com/"

    val instance: DoaApi by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(DoaApi::class.java)
    }
}
