package site.elahady.alkaukaba.repo.masjidterdekat

import android.location.Location
import site.elahady.alkaukaba.api.OverpassElement
import site.elahady.alkaukaba.api.OverpassRetrofitClient
import site.elahady.alkaukaba.model.NearbyMosque
import site.elahady.alkaukaba.utils.Resource

/** Sumber data masjid terdekat: Overpass API (OpenStreetMap), bukan Google Places - dipilih
 * karena gratis total tanpa API key/billing GCP (lihat diskusi awal fitur ini di Notion, yang
 * secara eksplisit minta cek biaya API dulu sebelum implementasi). Trade-off: kelengkapan data
 * masjid tergantung kontribusi mapper OSM lokal, bukan data komersial Google. */
object NearbyMosqueRepository {

    private val apis = OverpassRetrofitClient.instances

    suspend fun findNearby(lat: Double, lon: Double, radiusMeters: Int): Resource<List<NearbyMosque>> {
        // node = titik tunggal, way = bangunan yang dipetakan sebagai poligon (perlu `out center;`
        // supaya keduanya sama-sama balik satu koordinat pusat).
        val overpassQl = """
            [out:json][timeout:25];
            (
              node["amenity"="place_of_worship"]["religion"="muslim"](around:$radiusMeters,$lat,$lon);
              way["amenity"="place_of_worship"]["religion"="muslim"](around:$radiusMeters,$lat,$lon);
            );
            out center;
        """.trimIndent()

        var lastError: String? = null

        // Coba tiap endpoint Overpass satu-satu (lihat OverpassRetrofitClient) - instance utama
        // pernah balas 406 untuk semua request, jadi jangan gagal total kalau satu instance error.
        for (api in apis) {
            try {
                val response = api.query(overpassQl)
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    val results = body.elements
                        .mapNotNull { it.toNearbyMosque(lat, lon) }
                        .sortedBy { it.distanceMeters }
                    return Resource.Success(results)
                } else {
                    lastError = "Gagal memuat data masjid terdekat (${response.code()})"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                lastError = e.message ?: "Gagal memuat data masjid terdekat. Periksa koneksi internet."
            }
        }

        return Resource.Error(lastError ?: "Gagal memuat data masjid terdekat.")
    }

    private fun OverpassElement.toNearbyMosque(originLat: Double, originLon: Double): NearbyMosque? {
        val lat = resolvedLat ?: return null
        val lon = resolvedLon ?: return null

        val distanceResult = FloatArray(1)
        Location.distanceBetween(originLat, originLon, lat, lon, distanceResult)

        val tagMap = tags ?: emptyMap()
        val addressParts = listOfNotNull(
            tagMap["addr:street"],
            tagMap["addr:suburb"] ?: tagMap["addr:city"]
        )

        return NearbyMosque(
            id = id,
            name = tagMap["name"] ?: "Masjid",
            address = addressParts.takeIf { it.isNotEmpty() }?.joinToString(", "),
            latitude = lat,
            longitude = lon,
            distanceMeters = distanceResult[0]
        )
    }
}
