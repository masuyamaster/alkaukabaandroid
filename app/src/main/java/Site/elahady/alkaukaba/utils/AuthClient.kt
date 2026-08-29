package site.elahady.alkaukaba.utils

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import site.elahady.alkaukaba.api.AuthApiService

object AuthClient {
    // TESTING LOKAL (emulator): dipetakan via `adb reverse tcp:8000 tcp:8000`
    // sehingga 127.0.0.1 di emulator = localhost mesin host.
    // Jalankan `php artisan serve` di alkaukabaweb sebelum tes.
    // Sebelum rilis, ganti ke BASE_URL produksi: "https://alkaukaba.com/"
    private const val BASE_URL = "http://127.0.0.1:8000/"

    val instance: AuthApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(AuthApiService::class.java)
    }
}