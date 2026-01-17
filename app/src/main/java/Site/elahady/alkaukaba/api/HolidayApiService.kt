package Site.elahady.alkaukaba.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import com.google.gson.annotations.SerializedName


data class HolidayItem(
    val tanggal: String,
    val tanggalHijriah: String,
    val keterangan: String,
    val is_cuti: Boolean = true
)


interface HolidayApi {
    @GET("api")
    suspend fun getNationalHolidays(): List<HolidayItem>
}

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