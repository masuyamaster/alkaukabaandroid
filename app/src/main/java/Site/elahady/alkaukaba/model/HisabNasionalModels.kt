package site.elahady.alkaukaba.model

// Satu titik markaz (ibu kota provinsi) yang bisa dipakai hisab nasional.
// `id` dipakai sebagai key persist pilihan user (SessionManager) — pakai slug provinsi,
// BUKAN nama kota, supaya stabil biarpun label kota di UI diubah nanti.
data class MarkazNasional(
    val id: String,
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
