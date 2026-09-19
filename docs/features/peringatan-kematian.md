# Peringatan Hari Wafat (7, 40, 100, 1.000 hari)

### 1. Ringkasan (Overview)
- **Nama fitur**: Peringatan Hari Wafat
- **Deskripsi singkat**: Satu layar untuk menghitung tanggal peringatan (tahlilan)
  7, 40, 100, dan 1.000 hari wafatnya seseorang. User cukup memilih tanggal
  wafat, hasil muncul otomatis lengkap dengan nama hari, pasaran Jawa, dan
  status "N hari lagi / Hari ini / Sudah lewat N hari". Ditambahkan 2026-09-19.

### 2. Entry point & prasyarat
- Dipicu dari layar **Semua Menu** (`SemuaMenuActivity`), item paling belakang
  (setelah "Kalkulator Zakat"), label "Peringatan\nHari Wafat", ikon kalender
  slate (`ic_menu_peringatan_kematian`, warna `bg_slate_light`/`icon_slate`).
  Navigasi lewat `Intent` biasa (`item.activityClass`), tidak ada extra.
- Prasyarat: tidak ada. Fitur sepenuhnya offline, tanpa permission, tanpa
  API key.

### 3. Titik masuk logika & navigasi
- `PeringatanKematianCalculator` (`utils/PeringatanKematianCalculator.kt`) -
  object berisi fungsi murni, titik "colok" kalau mau reuse atau tambah test:
  - `hitung(tanggalWafat, hariWafatDihitungKe1 = true): List<PeringatanKematian>`
  - `selisihHari(dari, ke): Long` - selisih hari kalender (bukan per 24 jam),
    dipakai untuk status "N hari lagi".
  - `HARI_PERINGATAN` = 7, 40, 100, 1000.
- `PeringatanKematianActivity` (`ui/peringatankematian/`) - UI, DatePicker,
  render 4 baris hasil.
- Tidak ada navigasi keluar dari layar ini; kembali lewat tombol back toolbar.

### 4. Struktur & alur data
- `ui/peringatankematian/PeringatanKematianActivity.kt` - UI. Tap kotak
  "Tanggal Wafat" membuka `DatePickerDialog` (`maxDate` = hari ini, wafat tidak
  mungkin di masa depan). Setiap tanggal atau checkbox berubah,
  `tampilkanHasil()` render ulang 4 baris ke `containerPeringatan`.
- `utils/PeringatanKematianCalculator.kt` - logika tanggal (lihat §3).
- `utils/JavaneseCalendarUtil.kt` (sudah ada) - `pasaranFor()` untuk pasaran
  Legi/Pahing/Pon/Wage/Kliwon, dipakai ulang, tidak ditulis ulang.
- `res/layout/activity_peringatan_kematian.xml`,
  `res/layout/item_peringatan_kematian.xml` (satu baris hasil: "Hari ke-N",
  status, tanggal, divider), `res/drawable/ic_menu_peringatan_kematian.xml`.
- Alur: pilih tanggal -> `PeringatanKematianCalculator.hitung()` -> tiap
  tanggal diformat (`EEEE` + pasaran + `d MMMM yyyy`, locale `id-ID`) + status
  dari `selisihHari(hariIni, tanggal)` -> baris hasil.

### 5. Dependencies & tech stack khusus
Tidak ada tambahan khusus di luar stack umum app (`java.util.Calendar`,
`DatePickerDialog`, ViewBinding). Sengaja pakai `Calendar` (bukan `java.time`)
supaya konsisten dengan util tanggal lain di repo dan aman untuk minSdk 21
tanpa desugaring.

### 6. Keputusan desain & asumsi (bukan derivable dari kode)
- **Hari wafat = hari ke-1 (default)**. Konvensi yang paling lazim di
  Indonesia: wafat Senin -> hari ke-7 jatuh pada Minggu (wafat + 6 hari), ke-40
  = wafat + 39, dst. Ada daerah/tradisi yang tidak menghitung hari wafat
  (hari ke-7 = wafat + 7), jadi ada checkbox "Hari wafat dihitung sebagai hari
  ke-1" yang bisa dimatikan (`hariWafatDihitungKe1 = false`).
- **Tidak ada opsi "wafat setelah Maghrib"**. Secara hisab Islam pergantian
  hari terjadi saat Maghrib, sehingga wafat setelah Maghrib bisa dihitung
  sebagai hari berikutnya. Sengaja tidak dibuat karena butuh jam & lokasi, dan
  user bisa memilih tanggal +1 secara manual. Kalau ada permintaan, cukup
  tambah offset 1 hari sebelum `hitung()`.
- **Tidak menampilkan tanggal Hijriyah**. `HijriDateUtil` (tabular) bisa
  meleset 1-2 hari dari hisab hakiki di beranda, dan pergeseran itu akan
  membingungkan di fitur yang tanggalnya dipakai untuk acara. Hari + pasaran +
  Masehi sudah cukup untuk penjadwalan tahlilan.
- Nama hari memakai locale Indonesia bawaan Java ("Minggu", bukan "Ahad").

### 7. Testing
- `PeringatanKematianCalculatorTest`
  (`app/src/test/.../utils/PeringatanKematianCalculatorTest.kt`) - 7 test murni
  (tanpa Android): urutan 7/40/100/1000, dua konvensi hitung, lintas
  bulan/tahun/kabisat (2028), jam pada tanggal input diabaikan dan objek input
  tidak dimutasi, `selisihHari` (masa depan/hari ini/lalu/lintas kabisat).
  Semua lulus (2026-09-19). Tidak ada test UI/instrumented.
- Verifikasi manual (2026-09-19, emulator Pixel 6 API 34): build `installDebug`
  sukses, layar terbuka dengan default hari ini (Sabtu Wage, 19 September 2026)
  dan hasil hari ke-7 = Jumat Kliwon 25 September 2026, ke-40 = Rabu Pon
  28 Oktober 2026, ke-100 = Minggu Pon 27 Desember 2026, ke-1.000 = Kamis Pon
  14 Juni 2029 - cocok dengan hitungan manual. **Belum diverifikasi visual**:
  DatePicker, toggle checkbox, dan tampilan mode gelap (warna menu pastel
  memang tidak punya override di `values-night`, sama seperti menu lain).

### 8. Known issues & TODOs
- Belum ada opsi "wafat setelah Maghrib" (lihat §6).
- Belum ada tombol bagikan/salin hasil ke WhatsApp - kandidat kalau dibutuhkan.
