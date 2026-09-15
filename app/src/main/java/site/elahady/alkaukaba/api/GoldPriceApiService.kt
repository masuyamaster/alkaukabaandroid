package site.elahady.alkaukaba.api

import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

data class GoldPriceResponse(
    val success: Boolean,
    val data: List<GoldPriceEntry>
)

data class GoldPriceEntry(
    val materialType: String,
    val weight: Double,
    val sellPrice: Long,
    val currency: String
)

interface GoldPriceApi {
    @GET("api/prices/anekalogam")
    suspend fun getAntamPrices(@Query("weight") weight: Int = 1): Response<GoldPriceResponse>
}

/** Sumber harga emas Antam: logam-mulia-api (scraper anekalogam.co.id/Logam Mulia via Cloudflare
 * Worker), gratis total tanpa API key - dipilih untuk hitung nisab zakat mal & profesi (85 gram
 * emas) secara otomatis. Lihat docs/features/zakat.md untuk alasan pemilihan & fallback kalau
 * API ini mati. */
object GoldPriceRetrofitClient {
    private const val BASE_URL = "https://logam-mulia-api.iamutaki.workers.dev/"

    val instance: GoldPriceApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GoldPriceApi::class.java)
    }
}
