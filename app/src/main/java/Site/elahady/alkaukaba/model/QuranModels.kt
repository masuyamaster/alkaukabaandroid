package site.elahady.alkaukaba.model

data class QuranApiResponse<T>(
    val code: Int,
    val message: String,
    val data: T
)

data class Surah(
    val nomor: Int,
    val nama: String,
    val namaLatin: String,
    val jumlahAyat: Int,
    val tempatTurun: String,
    val arti: String,
    val deskripsi: String,
    val audioFull: Map<String, String>
)

data class SurahDetail(
    val nomor: Int,
    val nama: String,
    val namaLatin: String,
    val jumlahAyat: Int,
    val tempatTurun: String,
    val arti: String,
    val deskripsi: String,
    val audioFull: Map<String, String>,
    val ayat: List<Ayat>,
    val suratSelanjutnya: SurahRingkas?,
    val suratSebelumnya: SurahRingkas?
)

data class SurahRingkas(
    val nomor: Int,
    val nama: String,
    val namaLatin: String,
    val jumlahAyat: Int
)

data class Ayat(
    val nomorAyat: Int,
    val teksArab: String,
    val teksLatin: String,
    val teksIndonesia: String,
    val audio: Map<String, String>
)
