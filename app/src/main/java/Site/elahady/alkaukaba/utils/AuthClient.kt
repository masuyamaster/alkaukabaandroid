package site.elahady.alkaukaba.utils

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import site.elahady.alkaukaba.api.AuthApiService

object AuthClient {
    // API produksi (alkaukabaweb sudah live di VPS, lihat CLAUDE.md di repo itu).
    // Untuk balik ke testing lokal: "http://127.0.0.1:8000/" + `adb reverse tcp:8000 tcp:8000`
    // + jalankan `php artisan serve` di alkaukabaweb (butuh cleartext exception,
    // sudah ada di network_security_config.xml untuk 127.0.0.1/10.0.2.2).
    private const val BASE_URL = "https://alkaukaba.com/"

    val instance: AuthApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(AuthApiService::class.java)
    }
}