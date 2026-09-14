package site.elahady.alkaukaba.repo

import site.elahady.alkaukaba.api.DailyQuoteRetrofitClient
import site.elahady.alkaukaba.model.DailyQuote
import site.elahady.alkaukaba.utils.Resource
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Backend sudah cache per-hari (lihat DailyQuoteController::today di alkaukabaweb), jadi cukup
 * cache in-memory sederhana di sini, di-key per tanggal - pola sama dengan [DoaRepository]. Kalau
 * app di-kill lalu dibuka lagi, cache hilang dan fetch ulang, tapi itu bukan masalah karena
 * backend juga masih balas hasil yang sama untuk hari itu. */
object DailyQuoteRepository {

    private val api = DailyQuoteRetrofitClient.instance
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private var cachedDate: String? = null
    private var cachedQuote: DailyQuote? = null

    suspend fun getToday(): Resource<DailyQuote> {
        val today = dateFormat.format(Date())

        if (cachedDate == today) {
            cachedQuote?.let { return Resource.Success(it) }
        }

        return try {
            val response = api.getToday()
            if (response.isSuccessful && response.body() != null) {
                val quote = response.body()!!.data
                cachedDate = today
                cachedQuote = quote
                Resource.Success(quote)
            } else {
                Resource.Error(response.message())
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Gagal memuat kutipan harian")
        }
    }
}
