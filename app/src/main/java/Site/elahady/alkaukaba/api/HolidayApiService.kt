package Site.elahady.alkaukaba.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

// Model Data untuk API Hari Libur Nasional
data class HolidayItem(
    val tanggal: String, // Format: YYYY-MM-DD
    val keterangan: String,
    val is_cuti: Boolean
)

interface HolidayApi {
    @GET("api") // Menggunakan endpoint default dari api-harilibur
    suspend fun getNationalHolidays(): List<HolidayItem>
}

// Client khusus untuk API Libur Nasional (karena Base URL beda dengan Aladhan)
object HolidayRetrofitClient {
    private const val BASE_URL = "https://api-harilibur.vercel.app/"

    val instance: HolidayApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(HolidayApi::class.java)
    }
}