package site.elahady.alkaukaba.utils

/**
 * Aladhan API mengembalikan nama hari besar Hijriah dalam Bahasa Inggris (field
 * `hijri.holidays`), termasuk peringatan haul ulama tarekat ("Urs of ...") yang
 * jumlah variasinya sangat banyak. [exactTranslations] menangani hari besar Islam
 * umum secara presisi; [titleReplacements] menangani pola "Urs of Shaykh X" dkk
 * secara umum tanpa perlu hardcode tiap nama ulama (nama diri dibiarkan apa adanya).
 * Nama yang tidak dikenali sama sekali dikembalikan apa adanya (fallback aman).
 */
object HijriHolidayTranslator {

    private val exactTranslations = mapOf(
        "islamic new year" to "Tahun Baru Islam (1 Muharram)",
        "al-hijra" to "Tahun Baru Islam (1 Muharram)",
        "ashura" to "Hari Asyura (10 Muharram)",
        "day of ashura" to "Hari Asyura (10 Muharram)",
        "mawlid al-nabi" to "Maulid Nabi Muhammad SAW",
        "the prophet's birthday" to "Maulid Nabi Muhammad SAW",
        "prophet's birthday" to "Maulid Nabi Muhammad SAW",
        "isra and mi'raj" to "Isra Mi'raj",
        "isra and miraj" to "Isra Mi'raj",
        "al-isra wal mi'raj" to "Isra Mi'raj",
        "nisf sha'ban" to "Nisfu Sya'ban",
        "laylat al-qadr" to "Lailatul Qadar",
        "1st day of ramadan" to "Awal Bulan Ramadhan",
        "start of ramadan" to "Awal Bulan Ramadhan",
        "eid-ul-fitr" to "Idul Fitri",
        "eid al-fitr" to "Idul Fitri",
        "eid ul fitr" to "Idul Fitri",
        "eid-ul-adha" to "Idul Adha",
        "eid al-adha" to "Idul Adha",
        "eid ul adha" to "Idul Adha",
        "arafat day" to "Hari Arafah",
        "day of arafah" to "Hari Arafah"
    )

    private val titleReplacements = listOf(
        "Urs of" to "Haul",
        "Mawlānā" to "Maulana",
        "Mawlana" to "Maulana",
        "Shaykh" to "Syaikh",
        "Sultan al-Awliya" to "Sultanul Auliya"
    )

    fun translate(rawName: String): String {
        val trimmed = rawName.trim()
        exactTranslations[trimmed.lowercase()]?.let { return it }

        var result = trimmed
        for ((from, to) in titleReplacements) {
            result = result.replace(from, to, ignoreCase = true)
        }
        return result
    }

    /** Terjemahkan daftar nama hari besar yang sudah digabung dengan ", ". */
    fun translateJoined(joinedNames: String): String {
        if (joinedNames.isBlank()) return joinedNames
        return joinedNames.split(", ").joinToString(", ") { translate(it) }
    }
}
