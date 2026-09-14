package site.elahady.alkaukaba.api

import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import site.elahady.alkaukaba.model.DailyQuoteResponse

interface DailyQuoteApi {
    @GET("api/daily-quote")
    suspend fun getToday(): Response<DailyQuoteResponse>
}

object DailyQuoteRetrofitClient {
    // Backend alkaukabaweb (endpoint /api/daily-quote), base URL sama dengan
    // AuthClient/DoaRetrofitClient. Untuk testing lokal: ganti ke
    // "http://127.0.0.1:8000/" + `adb reverse tcp:8000 tcp:8000` + `php artisan
    // serve` di alkaukabaweb (lihat CLAUDE.md bagian "Konfigurasi API backend").
    private const val BASE_URL = "https://alkaukaba.com/"

    val instance: DailyQuoteApi by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(DailyQuoteApi::class.java)
    }
}
