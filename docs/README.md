# Dokumentasi Teknis — Al-Kaukaba Android

Folder ini berisi dokumentasi teknis per-fitur: keputusan desain, alasan di balik
implementasi yang tidak jelas hanya dari membaca kode, dan referensi ke file-file
kunci. Ini melengkapi `CLAUDE.md` di root repo (instruksi operasional cara
build/jalankan project), bukan menggantikannya.

## Konvensi struktur folder

- Satu file Markdown per fitur, di `docs/features/<nama-fitur>.md`.
- Nama file pakai kebab-case sesuai nama fitur di UI, mis. `waktu-sholat.md`
  untuk fitur "Waktu Sholat".
- Update dokumen begitu keputusan desain berubah — dokumen yang basi lebih
  berbahaya daripada tidak ada dokumen.

## Template isi dokumen fitur

> Catatan penting sebelum pakai template ini: repo ini **single Gradle module**
> (cuma `:app`), tanpa DI framework (Hilt/Koin/Dagger), tanpa split
> `data/domain/presentation`, dan navigasi antar-layar pakai `Intent` biasa
> (bukan Navigation Component/DeepLink). Jangan isi section di bawah dengan
> hal yang tidak ada di repo ini (mis. `implementation project(':features:x')`)
> — kalau suatu section memang tidak relevan untuk fitur yang didokumentasikan,
> tulis singkat "tidak relevan" beserta alasannya, jangan dikosongkan diam-diam.

### 1. Ringkasan (Overview)
- **Nama fitur**
- **Deskripsi singkat** — 1-2 kalimat, apa fungsi fitur ini dan masalah apa
  yang diselesaikannya.

### 2. Entry point & prasyarat
- Dari layar/tombol mana fitur ini dipicu (activity + elemen UI-nya).
- Prasyarat: permission, API key/config khusus, dsb yang dibutuhkan fitur ini
  supaya jalan (kalau ada).

### 3. Titik masuk logika & navigasi
- Kelas/fungsi kunci yang jadi tempat "colok" kalau developer lain mau
  extend/pakai ulang logika fitur ini (mis. object berisi daftar preset,
  method public di ViewModel/Repository).
- Navigasi antar-layar di dalam fitur ini (Activity mana ke Activity mana,
  lewat `Intent` apa, extra apa saja yang dibawa).

### 4. Struktur & alur data
- Daftar file nyata yang terlibat beserta perannya masing-masing (bukan
  struktur folder generik `data/domain/presentation` — tulis file yang benar
  ada di repo).
- Alur data singkat, mis. `View -> ViewModel -> Repository -> Retrofit`.

### 5. Dependencies & tech stack khusus
- Library pihak ketiga yang krusial/spesifik dipakai fitur ini saja (bukan
  yang sudah dipakai app-wide, seperti Retrofit/Gson). Kalau tidak ada, tulis
  "tidak ada tambahan khusus di luar stack umum app".

### 6. Testing
- Cakupan test otomatis untuk fitur ini saat ini (jujur — kalau belum ada,
  tulis itu sebagai gap, jangan dilewati).
- Cara manual verifikasi kalau belum ada test otomatis (mis. langkah build +
  install debug APK + skenario yang perlu dicoba di emulator).

### 7. Known issues & TODOs
- Technical debt atau bagian yang sengaja belum selesai/placeholder.
- Bug yang diketahui tapi belum jadi prioritas.

---

**Tips level-kode:** pakai KDoc pada kelas/fungsi public yang jadi titik masuk
fitur (bagian "3. Titik masuk logika" di atas) — nanti bisa di-generate jadi
HTML/Markdown pakai Dokka kalau suatu saat dibutuhkan.

## Dokumen lain

- [roadmap-clean-code.md](roadmap-clean-code.md) — bukan per-fitur, tapi
  catatan keputusan/rencana lintas-fitur soal kualitas kode (hygiene fixes,
  kapan domain layer/DI framework baru relevan, dsb).

## Status dokumentasi

| Fitur | Dokumen | Status |
|---|---|---|
| Waktu Sholat (metode perhitungan) | [features/waktu-sholat.md](features/waktu-sholat.md) | Terdokumentasi |
| Arah Kiblat | — | Belum |
| Bulan Hijriyah / Awal Bulan | — | Belum |
| Kalender | — | Belum |
| Autentikasi (login/register) | — | Belum |
