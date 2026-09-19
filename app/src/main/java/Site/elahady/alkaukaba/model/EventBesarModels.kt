package site.elahady.alkaukaba.model

/**
 * Jenis "Event Besar" — satu daftar yang menyatukan hari besar (Islam & nasional) dengan
 * fenomena astronomi, dibedakan lewat jenis ini (dipakai sebagai filter di CalendarActivity).
 */
enum class EventJenis(val label: String) {
    HARI_BESAR("Hari Besar"),
    ASTRONOMI("Astronomi")
}

/** Kelompok fenomena astronomi — dipakai buat label kecil di kartu event. */
enum class AstronomiKategori(val label: String) {
    HARI_TANPA_BAYANGAN("Hari Tanpa Bayangan"),
    EKUINOKS_SOLSTIS("Ekuinoks / Solstis"),
    PURNAMA("Purnama"),
    OPOSISI_PLANET("Oposisi Planet"),
    HUJAN_METEOR("Hujan Meteor"),
    GERHANA("Gerhana")
}

/**
 * Satu fenomena astronomi hasil hisab [site.elahady.alkaukaba.utils.AstronomicalEventCalculator].
 *
 * [epochMillis] adalah waktu puncak (UTC epoch); tanggal/jam lokal diturunkan dari sini oleh
 * pemanggil supaya calculator tetap bebas dari urusan tampilan.
 */
data class AstronomicalEvent(
    val epochMillis: Long,
    val kategori: AstronomiKategori,
    val judul: String,
    val keterangan: String
)
