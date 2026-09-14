package site.elahady.alkaukaba.api

import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Hari libur nasional Indonesia dari Nager.Date (date.nager.at) — gratis, tanpa API
 * key. `localName` sudah dalam Bahasa Indonesia. Menggantikan `api-harilibur.vercel.app`
 * yang sudah tidak aktif (server membalas "Payment required / DEPLOYMENT_DISABLED").
 *
 * Cakupan terbatas ke hari libur nasional yang tetap/global (Tahun Baru Masehi, Hari
 * Buruh, HUT RI, Natal, dll) — TIDAK termasuk cuti bersama maupun hari besar Islam
 * (Idul Fitri/Idul Adha/dll sudah tercakup lewat Aladhan via [PrayersApiService]).
 */
data class NagerHolidayItem(
    val date: String,
    val localName: String,
    val name: String
)

interface NationalHolidayApi {
    @GET("api/v3/PublicHolidays/{year}/{countryCode}")
    suspend fun getPublicHolidays(
        @Path("year") year: Int,
        @Path("countryCode") countryCode: String = "ID"
    ): Response<List<NagerHolidayItem>>
}

object NationalHolidayRetrofitClient {
    private const val BASE_URL = "https://date.nager.at/"

    val instance: NationalHolidayApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NationalHolidayApi::class.java)
    }
}
