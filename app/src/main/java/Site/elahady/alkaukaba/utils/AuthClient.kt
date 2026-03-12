package site.elahady.alkaukaba.utils

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import site.elahady.alkaukaba.api.AuthApiService

object AuthClient {
    // Gunakan URL hosting Anda yang sudah HTTPS
    private const val BASE_URL = "https://elahady.site/alkaukabaauth/"

    val instance: AuthApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(AuthApiService::class.java)
    }
}