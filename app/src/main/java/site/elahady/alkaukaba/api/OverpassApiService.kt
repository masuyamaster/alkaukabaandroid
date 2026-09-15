package site.elahady.alkaukaba.api

import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

data class OverpassResponse(
    val elements: List<OverpassElement>
)

data class OverpassCenter(
    val lat: Double,
    val lon: Double
)

/** Satu hasil node/way dari Overpass API (OpenStreetMap). Node punya lat/lon langsung, way
 * (mis. bangunan masjid yang dipetakan sebagai poligon) cuma punya `center` - makanya query
 * di [site.elahady.alkaukaba.repo.masjidterdekat.NearbyMosqueRepository] selalu pakai
 * `out center;` supaya keduanya konsisten. */
data class OverpassElement(
    val type: String,
    val id: Long,
    val lat: Double? = null,
    val lon: Double? = null,
    val center: OverpassCenter? = null,
    val tags: Map<String, String>? = null
) {
    val resolvedLat: Double? get() = lat ?: center?.lat
    val resolvedLon: Double? get() = lon ?: center?.lon
}

interface OverpassApi {
    @GET("api/interpreter")
    suspend fun query(@Query("data") overpassQl: String): Response<OverpassResponse>
}

/** Overpass API punya beberapa instance publik independen (masing-masing sinkron dari data OSM
 * yang sama, bukan pihak ketiga terpisah). Instance utama (`overpass-api.de`) sempat balas 406
 * ke semua request (dicoba juga langsung lewat curl, di luar app - bukan bug di app ini) saat
 * fitur ini dibangun (2026-09), sementara mirror `overpass.kumi.systems` normal - jadi
 * [site.elahady.alkaukaba.repo.masjidterdekat.NearbyMosqueRepository] coba urutan endpoint ini
 * satu-satu sampai ada yang berhasil, bukan cuma andalkan satu instance. */
object OverpassRetrofitClient {
    private val BASE_URLS = listOf(
        "https://overpass-api.de/",
        "https://overpass.kumi.systems/"
    )

    // Query Overpass (apalagi radius besar/way search) bisa makan beberapa detik - default OkHttp
    // (10s) sempat kepotong "timeout" duluan sebelum server sempat balas, jadi diperpanjang.
    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    val instances: List<OverpassApi> by lazy {
        BASE_URLS.map { baseUrl ->
            Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(httpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(OverpassApi::class.java)
        }
    }
}
