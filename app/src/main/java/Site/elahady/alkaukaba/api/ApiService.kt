package Site.elahady.alkaukaba.api

import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

// 1. Data Models (Sesuai Response JSON Aladhan)
data class PrayerResponse(
    val code: Int,
    val status: String,
    val data: PrayerData
)

data class PrayerData(
    val timings: Timings,
    val date: DateInfo,
    val meta: Meta
)

data class Timings(
    val Fajr: String,
    val Dhuhr: String,
    val Asr: String,
    val Maghrib: String,
    val Isha: String
)

data class DateInfo(
    val readable: String,
    val hijri: Hijri
)

data class Hijri(
    val day: String,
    val month: HijriMonth,
    val year: String,
    val holidays: List<String>
)

data class HijriMonth(
    val en: String,
    val ar: String
)

data class Meta(
    val timezone: String
)

// 2. Interface API
interface AladhanApi {
    // Endpoint ini membutuhkan tanggal (dd-MM-yyyy) atau timestamp
    // Contoh URL nanti: https://api.aladhan.com/v1/timings/17-01-2026?latitude=...
    @GET("v1/timings/{date}")
    fun getTimings(
        @Path("date") date: String, // Format: dd-MM-yyyy
        @Query("latitude") lat: Double,
        @Query("longitude") lng: Double,
        @Query("method") method: Int = 20 // 20 adalah Kemenag RI
    ): Call<PrayerResponse>
}

// 3. Singleton Retrofit Client
object RetrofitClient {
    // Gunakan HTTPS agar aman
    private const val BASE_URL = "https://api.aladhan.com/"

    val instance: AladhanApi by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(AladhanApi::class.java)
    }
}