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
    val ayat: List<Ayat>
)

data class Ayat(
    val nomorAyat: Int,
    val teksArab: String,
    val teksLatin: String,
    val teksIndonesia: String,
    val audio: Map<String, String>
)

/** Respons pencarian ayat dari api.alquran.cloud - equran.id tidak punya endpoint search,
 * jadi ayat dicari lewat API terpisah ini (hanya untuk cari nomor surah+ayat & cuplikan
 * terjemahan; teks Arab/Latin final tetap ditampilkan dari data equran.id begitu user
 * membuka detail surahnya, supaya konsisten dengan satu sumber data utama). */
data class AyatSearchResponse(
    val code: Int,
    val status: String,
    val data: AyatSearchData
)

data class AyatSearchData(
    val count: Int,
    val matches: List<AyatSearchMatch>
)

data class AyatSearchMatch(
    val text: String,
    val numberInSurah: Int,
    val surah: AyatSearchSurahRef
)

data class AyatSearchSurahRef(
    val number: Int
)
