package site.elahady.alkaukaba.model

import site.elahady.alkaukaba.utils.prayerbreakdown.PrayerBreakdownSection

data class HilalInput(
    val latitude: Double,
    val longitude: Double,
    val heightMeters: Double
)

data class HilalResult(
    val bulanHijriyahLabel: String, // Label bulan Hijriyah yang dicek, mis. "Menjelang Ramadhan 1447 H"
    val tanggalGhurubLabel: String, // Tanggal Masehi ghurub markaz yang dipakai, mis. "29 Februari 2026"
    val statusBadge: String, // "Hilal Mungkin Terlihat" / "Belum Memenuhi Kriteria — Istikmal"
    val hilalMemenuhiKriteria: Boolean,
    val tinggiHilal: Double, // Tinggi hilal mar'i (topocentric, derajat)
    val elongasi: Double, // Elongasi bulan-matahari (derajat)
    val mukutsMenit: Double, // Lama hilal di atas ufuk setelah ghurub (menit)
    val ijtimaTime: String,
    val ghurubTime: String,
    val breakdownSections: List<PrayerBreakdownSection>, // Rincian per-seksi, dirender sebagai accordion (reuse komponen dari fitur Waktu Sholat)
    val calculationLog: String // Sama isinya dengan breakdownSections, diratakan jadi teks polos untuk export PDF
)
