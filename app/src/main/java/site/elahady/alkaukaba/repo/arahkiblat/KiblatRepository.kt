package site.elahady.alkaukaba.repo.arahkiblat

import site.elahady.alkaukaba.api.AladhanApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class KiblatRepository(
    private val api: AladhanApi
) {
    suspend fun getQiblaAngle(lat: Double, lon: Double): Double =
    withContext(Dispatchers.IO) {
        api.getQiblaDirection(lat, lon).data.direction
    }
}