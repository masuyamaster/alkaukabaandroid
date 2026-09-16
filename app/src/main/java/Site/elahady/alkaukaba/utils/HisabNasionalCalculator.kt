package site.elahady.alkaukaba.utils

import site.elahady.alkaukaba.model.HilalInput
import site.elahady.alkaukaba.model.MarkazHisabResult
import site.elahady.alkaukaba.model.MarkazNasional

// Hisab awal bulan Hijriyah lintas markaz representatif dari barat ke timur Indonesia
// (Sabang s.d. Jayapura) — bukan daftar lengkap titik rukyat resmi Kemenag (ratusan
// titik), hanya sampel yang cukup merentang bentang bujur Indonesia (~95-141 BT) supaya
// perbedaan waktu ghurub & posisi hilal antar wilayah kelihatan.
object HisabNasionalCalculator {

    val markazList = listOf(
        MarkazNasional("Sabang", "Aceh", 5.8933, 95.3238),
        MarkazNasional("Jakarta", "DKI Jakarta", -6.2088, 106.8456),
        MarkazNasional("Yogyakarta", "DI Yogyakarta", -7.8014, 110.3644),
        MarkazNasional("Surabaya", "Jawa Timur", -7.2575, 112.7521),
        MarkazNasional("Mataram", "Nusa Tenggara Barat", -8.5833, 116.1167),
        MarkazNasional("Makassar", "Sulawesi Selatan", -5.1477, 119.4327),
        MarkazNasional("Ambon", "Maluku", -3.6954, 128.1814),
        MarkazNasional("Jayapura", "Papua", -2.5337, 140.7181)
    )

    // heightMeters disamakan 0.0 (markaz permukaan laut) untuk semua titik — penyederhanaan
    // yang sama dipakai fitur Okultasi utk lokasi default.
    fun calculate(monthOffset: Int = 0): List<MarkazHisabResult> =
        markazList.map { markaz ->
            val hasil = EphemerisCalculator.calculate(
                HilalInput(markaz.latitude, markaz.longitude, 0.0, monthOffset)
            )
            MarkazHisabResult(markaz, hasil)
        }
}
