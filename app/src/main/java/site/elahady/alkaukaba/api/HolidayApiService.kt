package site.elahady.alkaukaba.api

data class HolidayItem(
    val tanggal: String,
    val tanggalHijriah: String,
    val keterangan: String,
    val is_cuti: Boolean = true
)