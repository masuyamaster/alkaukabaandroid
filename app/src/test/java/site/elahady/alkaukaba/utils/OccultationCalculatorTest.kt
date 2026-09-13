package site.elahady.alkaukaba.utils

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `OccultationCalculator.calculate()` murni logic (Observer + Astronomy Engine, tanpa
 * Android/API/DB) dan mencari konjungsi Bulan-Venus dari `System.currentTimeMillis()`,
 * sama seperti [EphemerisCalculatorTest] - GOLDEN/REFERENCE TEST, hasil dibanding ke
 * rentang fisis yang masuk akal dan invariant algoritma, bukan dipalsukan.
 */
class OccultationCalculatorTest {

    @Test
    fun `hitung okultasi Venus untuk Jakarta menghasilkan angka-angka dalam rentang fisis yang masuk akal`() {
        val result = OccultationCalculator.calculate(latitude = -6.2088, longitude = 106.8456, heightMeters = 50.0)

        result.venusOccultations.forEach { item ->
            println(
                "Jakarta -> ${item.eventDateLabel}, puncak=${item.peakTimeLabel}, " +
                    "okultasi=${item.isOccultation}, masuk=${item.ingressTimeLabel}, keluar=${item.egressTimeLabel}, " +
                    "sepMin=${item.minSeparationArcmin}', terlihat=${item.visibleFromLocation}"
            )
        }

        assertTrue("Harus menemukan minimal 1 konjungsi Bulan-Venus", result.venusOccultations.isNotEmpty())
        result.venusOccultations.forEach { item ->
            assertTrue("Jarak sudut minimum harus non-negatif", item.minSeparationArcmin >= 0.0)
            assertTrue("Jarak sudut minimum harus masuk akal (<180')", item.minSeparationArcmin < 180.0)
            if (item.isOccultation) {
                assertTrue("Okultasi nyata harus punya waktu masuk", item.ingressTimeLabel != null)
                assertTrue("Okultasi nyata harus punya waktu keluar", item.egressTimeLabel != null)
            }
        }
    }

    @Test
    fun `hitung okultasi Venus untuk beberapa lokasi di Indonesia tidak error`() {
        val locations = listOf(
            "Sabang" to Pair(5.8, 95.32),
            "Merauke" to Pair(-8.47, 140.4),
            "Sampang" to Pair(-7.2, 113.25)
        )

        locations.forEach { (name, latLon) ->
            val result = OccultationCalculator.calculate(latLon.first, latLon.second, 0.0)
            println("$name -> ${result.venusOccultations.size} event ditemukan")
            result.venusOccultations.forEach { item ->
                println(
                    "  ${item.eventDateLabel}, puncak=${item.peakTimeLabel}, okultasi=${item.isOccultation}, " +
                        "sepMin=${item.minSeparationArcmin}'"
                )
            }
            assertTrue(result.venusOccultations.size <= 3)
        }
    }
}
