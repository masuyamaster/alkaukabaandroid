package site.elahady.alkaukaba.model

/** Satu ruas (surah, rentang nomor ayat) di dalam satu Juz - satu Juz bisa terdiri dari
 * beberapa ruas karena batas Juz sering jatuh di tengah surah (mis. Juz 1 = Al-Fatihah
 * ayat 1-7 + Al-Baqarah ayat 1-141, dua ruas). */
data class JuzSegment(val surahNumber: Int, val startAyat: Int, val endAyat: Int)

/** Tabel batas 30 Juz - equran.id (sumber data utama surah/ayat di app ini) tidak punya
 * endpoint per-Juz sama sekali, jadi batasnya di-hardcode di sini. Diverifikasi langsung dari
 * endpoint resmi `GET v1/juz/{juz}/quran-simple` di api.alquran.cloud (bukan dihafal/ditebak),
 * dengan mengelompokkan tiap ayat dalam response berdasarkan nomor surah & ayat berurutan.
 * Data ini tetap dan tidak pernah berubah (pembagian Juz sudah baku sejak lama), jadi aman
 * di-hardcode daripada fetch API tiap buka layar Juz. */
object JuzBoundaries {

    val segments: Map<Int, List<JuzSegment>> = mapOf(
        1 to listOf(JuzSegment(1, 1, 7), JuzSegment(2, 1, 141)),
        2 to listOf(JuzSegment(2, 142, 252)),
        3 to listOf(JuzSegment(2, 253, 286), JuzSegment(3, 1, 92)),
        4 to listOf(JuzSegment(3, 93, 200), JuzSegment(4, 1, 23)),
        5 to listOf(JuzSegment(4, 24, 147)),
        6 to listOf(JuzSegment(4, 148, 176), JuzSegment(5, 1, 81)),
        7 to listOf(JuzSegment(5, 82, 120), JuzSegment(6, 1, 110)),
        8 to listOf(JuzSegment(6, 111, 165), JuzSegment(7, 1, 87)),
        9 to listOf(JuzSegment(7, 88, 206), JuzSegment(8, 1, 40)),
        10 to listOf(JuzSegment(8, 41, 75), JuzSegment(9, 1, 92)),
        11 to listOf(JuzSegment(9, 93, 129), JuzSegment(10, 1, 109), JuzSegment(11, 1, 5)),
        12 to listOf(JuzSegment(11, 6, 123), JuzSegment(12, 1, 52)),
        13 to listOf(JuzSegment(12, 53, 111), JuzSegment(13, 1, 43), JuzSegment(14, 1, 52)),
        14 to listOf(JuzSegment(15, 1, 99), JuzSegment(16, 1, 128)),
        15 to listOf(JuzSegment(17, 1, 111), JuzSegment(18, 1, 74)),
        16 to listOf(JuzSegment(18, 75, 110), JuzSegment(19, 1, 98), JuzSegment(20, 1, 135)),
        17 to listOf(JuzSegment(21, 1, 112), JuzSegment(22, 1, 78)),
        18 to listOf(JuzSegment(23, 1, 118), JuzSegment(24, 1, 64), JuzSegment(25, 1, 20)),
        19 to listOf(JuzSegment(25, 21, 77), JuzSegment(26, 1, 227), JuzSegment(27, 1, 55)),
        20 to listOf(JuzSegment(27, 56, 93), JuzSegment(28, 1, 88), JuzSegment(29, 1, 45)),
        21 to listOf(
            JuzSegment(29, 46, 69), JuzSegment(30, 1, 60), JuzSegment(31, 1, 34),
            JuzSegment(32, 1, 30), JuzSegment(33, 1, 30)
        ),
        22 to listOf(JuzSegment(33, 31, 73), JuzSegment(34, 1, 54), JuzSegment(35, 1, 45), JuzSegment(36, 1, 27)),
        23 to listOf(JuzSegment(36, 28, 83), JuzSegment(37, 1, 182), JuzSegment(38, 1, 88), JuzSegment(39, 1, 31)),
        24 to listOf(JuzSegment(39, 32, 75), JuzSegment(40, 1, 85), JuzSegment(41, 1, 46)),
        25 to listOf(
            JuzSegment(41, 47, 54), JuzSegment(42, 1, 53), JuzSegment(43, 1, 89),
            JuzSegment(44, 1, 59), JuzSegment(45, 1, 37)
        ),
        26 to listOf(
            JuzSegment(46, 1, 35), JuzSegment(47, 1, 38), JuzSegment(48, 1, 29),
            JuzSegment(49, 1, 18), JuzSegment(50, 1, 45), JuzSegment(51, 1, 30)
        ),
        27 to listOf(
            JuzSegment(51, 31, 60), JuzSegment(52, 1, 49), JuzSegment(53, 1, 62), JuzSegment(54, 1, 55),
            JuzSegment(55, 1, 78), JuzSegment(56, 1, 96), JuzSegment(57, 1, 29)
        ),
        28 to listOf(
            JuzSegment(58, 1, 22), JuzSegment(59, 1, 24), JuzSegment(60, 1, 13), JuzSegment(61, 1, 14),
            JuzSegment(62, 1, 11), JuzSegment(63, 1, 11), JuzSegment(64, 1, 18), JuzSegment(65, 1, 12),
            JuzSegment(66, 1, 12)
        ),
        29 to listOf(
            JuzSegment(67, 1, 30), JuzSegment(68, 1, 52), JuzSegment(69, 1, 52), JuzSegment(70, 1, 44),
            JuzSegment(71, 1, 28), JuzSegment(72, 1, 28), JuzSegment(73, 1, 20), JuzSegment(74, 1, 56),
            JuzSegment(75, 1, 40), JuzSegment(76, 1, 31), JuzSegment(77, 1, 50)
        ),
        30 to listOf(
            JuzSegment(78, 1, 40), JuzSegment(79, 1, 46), JuzSegment(80, 1, 42), JuzSegment(81, 1, 29),
            JuzSegment(82, 1, 19), JuzSegment(83, 1, 36), JuzSegment(84, 1, 25), JuzSegment(85, 1, 22),
            JuzSegment(86, 1, 17), JuzSegment(87, 1, 19), JuzSegment(88, 1, 26), JuzSegment(89, 1, 30),
            JuzSegment(90, 1, 20), JuzSegment(91, 1, 15), JuzSegment(92, 1, 21), JuzSegment(93, 1, 11),
            JuzSegment(94, 1, 8), JuzSegment(95, 1, 8), JuzSegment(96, 1, 19), JuzSegment(97, 1, 5),
            JuzSegment(98, 1, 8), JuzSegment(99, 1, 8), JuzSegment(100, 1, 11), JuzSegment(101, 1, 11),
            JuzSegment(102, 1, 8), JuzSegment(103, 1, 3), JuzSegment(104, 1, 9), JuzSegment(105, 1, 5),
            JuzSegment(106, 1, 4), JuzSegment(107, 1, 7), JuzSegment(108, 1, 3), JuzSegment(109, 1, 6),
            JuzSegment(110, 1, 3), JuzSegment(111, 1, 5), JuzSegment(112, 1, 4), JuzSegment(113, 1, 5),
            JuzSegment(114, 1, 6)
        ),
    )

    /** Ringkasan satu Juz untuk kartu di daftar Juz (layar awal Al-Qur'an) - nama surah diambil
     * lewat callback [namaLatinOf] (bukan hardcode di sini) karena nama Latin surah sumbernya
     * tetap dari cache [site.elahady.alkaukaba.repo.QuranRepository], satu sumber kebenaran
     * yang sama dipakai di seluruh layar lain. */
    fun buildSummary(nomorJuz: Int, namaLatinOf: (Int) -> String, namaArabOf: (Int) -> String): JuzSummary? {
        val juzSegments = segments[nomorJuz] ?: return null
        val jumlahAyat = juzSegments.sumOf { it.endAyat - it.startAyat + 1 }
        return JuzSummary(
            nomor = nomorJuz,
            startSurahNamaLatin = namaLatinOf(juzSegments.first().surahNumber),
            endSurahNamaLatin = namaLatinOf(juzSegments.last().surahNumber),
            startSurahNamaArab = namaArabOf(juzSegments.first().surahNumber),
            endSurahNamaArab = namaArabOf(juzSegments.last().surahNumber),
            jumlahAyat = jumlahAyat
        )
    }
}

data class JuzSummary(
    val nomor: Int,
    val startSurahNamaLatin: String,
    val endSurahNamaLatin: String,
    val startSurahNamaArab: String,
    val endSurahNamaArab: String,
    val jumlahAyat: Int
)

/** Satu ayat di dalam tampilan Juz, dibungkus dengan info surah asalnya - beda dari [Ayat]
 * polos yang dipakai di layar per-surah, karena satu layar Juz bisa menampilkan ayat dari
 * beberapa surah berurutan. [isFirstOfSurah] dipakai UI untuk menyisipkan header nama surah
 * setiap kali surah-nya berganti. */
data class JuzAyat(
    val ayat: Ayat,
    val surahNomor: Int,
    val surahNamaLatin: String,
    val isFirstOfSurah: Boolean
)

data class JuzDetail(val nomor: Int, val ayatList: List<JuzAyat>)
