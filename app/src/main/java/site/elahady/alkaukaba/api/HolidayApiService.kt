package site.elahady.alkaukaba.api

import site.elahady.alkaukaba.model.EventJenis

data class HolidayItem(
    val tanggal: String,
    val tanggalHijriah: String,
    val keterangan: String,
    val is_cuti: Boolean = true,
    val jenis: EventJenis = EventJenis.HARI_BESAR,
    val catatan: String? = null
)
