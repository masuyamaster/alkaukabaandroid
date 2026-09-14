package site.elahady.alkaukaba.repo

import site.elahady.alkaukaba.api.DoaRetrofitClient
import site.elahady.alkaukaba.model.DoaCategory
import site.elahady.alkaukaba.model.DoaCategoryDetail
import site.elahady.alkaukaba.utils.Resource

/** Konten Hisnul Muslim/Al-Mathurat dari backend sendiri tidak pernah berubah dalam satu sesi
 * pemakaian, jadi di-cache in-memory di sini - pola sama persis dengan [QuranRepository].
 * Belum ada local DB (Room), jadi cache hilang begitu proses app dimatikan (tidak ada
 * dukungan offline penuh - butuh koneksi internet tiap kali app baru dibuka). */
object DoaRepository {

    private val api = DoaRetrofitClient.instance

    private var categoryListCache: List<DoaCategory>? = null
    private val categoryDetailCache = mutableMapOf<String, DoaCategoryDetail>()

    suspend fun getCategories(): Resource<List<DoaCategory>> {
        categoryListCache?.let { return Resource.Success(it) }

        return try {
            val response = api.getCategories()
            if (response.isSuccessful && response.body() != null) {
                val list = response.body()!!.data
                categoryListCache = list
                Resource.Success(list)
            } else {
                Resource.Error(response.message())
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Gagal memuat kategori doa & dzikir")
        }
    }

    suspend fun getCategoryItems(slug: String): Resource<DoaCategoryDetail> {
        categoryDetailCache[slug]?.let { return Resource.Success(it) }

        return try {
            val response = api.getCategoryItems(slug)
            if (response.isSuccessful && response.body() != null) {
                val detail = response.body()!!.data
                categoryDetailCache[slug] = detail
                Resource.Success(detail)
            } else {
                Resource.Error(response.message())
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Gagal memuat doa & dzikir")
        }
    }
}
