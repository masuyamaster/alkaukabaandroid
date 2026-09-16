package site.elahady.alkaukaba.model

// Satu titik markaz representatif (barat -> timur Indonesia) dipakai hisab nasional.
data class MarkazNasional(
    val nama: String,
    val provinsi: String,
    val latitude: Double,
    val longitude: Double
)

// Hasil hisab (HilalResult) untuk satu markaz nasional, dibungkus bareng identitas markaznya.
data class MarkazHisabResult(
    val markaz: MarkazNasional,
    val hasil: HilalResult
)
