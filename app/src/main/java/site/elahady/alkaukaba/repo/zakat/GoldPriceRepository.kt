package site.elahady.alkaukaba.repo.zakat

import site.elahady.alkaukaba.api.GoldPriceRetrofitClient
import site.elahady.alkaukaba.utils.Resource

/** Ambil harga jual emas Antam per gram hari ini, dipakai sebagai acuan nisab (85 gram) untuk
 * zakat mal & zakat profesi. API balikin beberapa varian gramasi & produk untuk gramasi 1 gram
 * saja (Certicard 100gr dipotong per-gram, LM Antam produksi tahun berjalan, dst) - dipilih
 * entry "LM Antam produksi tahun ..." (harga batangan 1 gram standar, bukan varian Certicard)
 * sebagai acuan paling umum dipakai, fallback ke entry 1 gram pertama kalau tidak ketemu. */
object GoldPriceRepository {

    private val api = GoldPriceRetrofitClient.instance

    suspend fun getHargaEmasPerGram(): Resource<Double> {
        return try {
            val response = api.getAntamPrices(weight = 1)
            val entries = response.body()?.data
            if (response.isSuccessful && !entries.isNullOrEmpty()) {
                val oneGram = entries.filter { it.weight == 1.0 }
                val pick = oneGram.firstOrNull {
                    it.materialType.contains("LM Antam produksi tahun", ignoreCase = true)
                } ?: oneGram.firstOrNull()
                if (pick != null) {
                    Resource.Success(pick.sellPrice.toDouble())
                } else {
                    Resource.Error("Data harga emas tidak ditemukan pada respons API.")
                }
            } else {
                Resource.Error("Gagal memuat harga emas (${response.code()})")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Gagal memuat harga emas. Periksa koneksi internet.")
        }
    }
}
