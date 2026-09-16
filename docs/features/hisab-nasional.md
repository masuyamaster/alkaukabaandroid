# Hisab Awal Bulan (Hijriyah) Nasional

## 1. Ringkasan

**Fitur**: hisab awal bulan Hijriyah di beberapa titik markaz representatif
yang membentang dari barat ke timur Indonesia (Sabang s.d. Jayapura),
ditampilkan sebagai daftar per-markaz + satu kesimpulan level nasional
(berapa dari titik yang dihitung memenuhi kriteria Neo-MABIMS).

Ini pelengkap [`bulan-hijriyah.md`](bulan-hijriyah.md) ("Awal Bulan"), bukan
pengganti — "Awal Bulan" menghitung untuk **satu markaz** (lokasi user,
GPS/manual), fitur ini menghitung **banyak markaz sekaligus** untuk memberi
gambaran apakah kriteria imkan rukyat cenderung terpenuhi merata di seluruh
Indonesia atau hanya di sebagian wilayah — mendekati logika yang jadi dasar
Sidang Isbat Kemenag (yang mempertimbangkan hasil rukyat/hisab dari banyak
titik pemantauan, bukan cuma satu tempat).

Dibuat 2026-09-16 sebagai isi awal untuk ticket Notion "Hisab Awal Bulan
(Hijriyah) Nasional" — user belum punya spesifikasi rinci saat ticket dibuat,
jadi cakupan/desain menu & layar ini adalah interpretasi pertama yang masuk
akal, bukan hasil requirement gathering. Lihat section 7 untuk hal-hal yang
kemungkinan perlu didiskusikan ulang dengan user.

## 2. Entry point & navigasi

- Layar: `HisabNasionalActivity` (layout `activity_hisab_nasional.xml`,
  judul UI "Hisab Awal Bulan Nasional").
- Terdaftar di `AndroidManifest.xml` (`exported="false"`, sama seperti
  activity fitur lain).
- Dibuka dari grid "Semua Menu" (`SemuaMenuActivity`), item "Hisab Awal
  Bulan Nasional" — sengaja diletakkan tepat setelah "Okultasi" sesuai
  permintaan user saat ticket dibuat, warna baru `bg_cyan_light`/`icon_cyan`
  (ditambahkan khusus fitur ini karena semua 11 pasangan warna kartu menu
  yang ada sudah terpakai masing-masing tepat sekali). Icon reuse
  `ic_menu_document` (belum ada icon dedicated "peta/globe" di project).
- **Juga ditambahkan ke quick-access home** (`activity_main.xml`/
  `MainActivity.kt`, `bt_hisab_nasional`) — awalnya cuma ditaruh di "Semua
  Menu", tapi user eksplisit minta muncul juga di home tepat setelah
  Okultasi. Grid home dikunci tetap 2 baris x 4 kolom (bukan direnggangkan
  jadi 3 baris atau diubah ke 3 kolom) sesuai instruksi user — supaya
  8 slot tetap pas, **"Kalkulator" (`bt_kalkulator`) dihapus dari home**
  atas permintaan user (tetap ada di "Semua Menu", cuma tidak lagi jadi
  shortcut cepat di home). Urutan home sekarang: Kiblat, Waktu Sholat,
  Awal Bulan, Gerhana / Okultasi, **Hisab Nasional**, Al-Qur'an, Doa &
  Dzikir.
- Tidak butuh permission lokasi sama sekali — semua titik markaz sudah
  fixed (lihat section 4), beda dari "Awal Bulan"/Okultasi yang butuh
  GPS/lokasi user.

## 3. Alur

1. `onCreate` -> langsung panggil `HisabNasionalViewModel.calculateNasional()`
   (default `monthOffset=0`, tanpa selector bulan/tahun seperti "Awal Bulan"
   — lihat known limitation di section 7) tanpa perlu input apa pun dari
   user.
2. Hisab dijalankan sekali per markaz (8x pemanggilan
   `EphemerisCalculator.calculate()`) di `Dispatchers.Default` supaya UI
   tidak nge-freeze, hasil dikirim sebagai satu list lewat LiveData.
3. Kartu navy di atas menampilkan label bulan Hijriyah yang dicek + kalimat
   kesimpulan ("X dari Y titik markaz memenuhi kriteria...") + badge status.
4. Di bawahnya, `RecyclerView` menampilkan kartu per-markaz (nama+provinsi,
   ghurub, tinggi hilal, elongasi, badge Memenuhi/Belum Memenuhi).
5. `btnBack` toolbar -> `finish()`. Tidak ada tombol hitung ulang/refresh —
   tidak relevan karena tidak ada input lokasi yang bisa diubah user.

## 4. Struktur & alur data

| File | Peran |
|---|---|
| `ui/hisabnasional/HisabNasionalActivity.kt` + `activity_hisab_nasional.xml` | UI: kartu kesimpulan nasional + daftar per-markaz |
| `viewmodel/hisabnasional/HisabNasionalViewModel.kt` | `LiveData<List<MarkazHisabResult>>`, jembatan ke `HisabNasionalCalculator` |
| `utils/HisabNasionalCalculator.kt` | Daftar 8 `MarkazNasional` tetap (Sabang, Jakarta, Yogyakarta, Surabaya, Mataram, Makassar, Ambon, Jayapura — lat/lng hardcode ibu kota/kota besar tiap titik, `heightMeters=0.0` semua) + `calculate(monthOffset)` yang memanggil `EphemerisCalculator.calculate()` (mesin yang sama dgn "Awal Bulan", lihat [`bulan-hijriyah.md` section 5](bulan-hijriyah.md#5-mesin-hisab-ephemeriscalculator)) untuk tiap titik |
| `model/HisabNasionalModels.kt` | `MarkazNasional` (nama, provinsi, lat, lng) dan `MarkazHisabResult` (markaz + `HilalResult`) |
| `adapter/MarkazHisabAdapter.kt` + `item_markaz_hisab.xml` | Render satu kartu per markaz di `RecyclerView` |

Alur data: `onCreate` -> `calculateNasional()` -> loop 8 markaz ->
`EphemerisCalculator.calculate()` per markaz -> `List<MarkazHisabResult>` ->
LiveData -> Activity render kartu kesimpulan (hitung
`count { hilalMemenuhiKriteria }`) + `MarkazHisabAdapter.setData()`.

## 5. Pemilihan titik markaz & kriteria

8 titik dipilih murni supaya bentang bujur Indonesia (~95°BT Sabang s.d.
~141°BT Jayapura) terwakili, **bukan** daftar titik rukyat resmi Kemenag
(yang jumlahnya ratusan dan tersebar per kabupaten/kota). Kriteria kelulusan
tiap titik pakai Neo-MABIMS yang sama dengan "Awal Bulan" (tinggi hilal ≥3°,
elongasi ≥6.4°, dari `HilalResult.hilalMemenuhiKriteria` — tidak ada logika
kriteria baru yang ditulis khusus di sini).

Kesimpulan nasional di layar cuma penjumlahan sederhana
(`count/total` markaz yang lulus) dengan 3 label:
"Kriteria Terpenuhi Secara Nasional" (semua lulus), "Kriteria Terpenuhi
Sebagian Wilayah" (sebagian), "Kriteria Belum Terpenuhi di Semua Markaz"
(tidak ada yang lulus) — **bukan** simulasi proses Sidang Isbat sungguhan
(yang mempertimbangkan laporan rukyat lapangan, bukan cuma hisab).

## 6. Testing

Belum ada test otomatis (`HisabNasionalCalculator` murni komposisi berulang
dari `EphemerisCalculator` yang sudah ada, tidak ada rumus baru yang perlu
divalidasi terpisah). Verifikasi manual (emulator, 2026-09-16): install APK
debug, home -> Semua Menu -> "Hisab Awal Bulan Nasional" -> kartu kesimpulan
& 8 kartu markaz terisi tanpa crash (dicek via `uiautomator dump`, bukan
screenshot — lihat `CLAUDE.md` soal batasi screenshot), hasil contoh saat
verifikasi: "Menjelang Jumadil Awal 1448 H", 8/8 markaz "Memenuhi", badge
"Kriteria Terpenuhi Secara Nasional".

## 7. Known limitations

- [ ] Home quick-access sekarang cuma menyisakan 8 fitur (Kalkulator
      dikeluarkan supaya "Hisab Nasional" muat tanpa nambah baris) — kalau
      nanti ada fitur lain yang mau ditambahkan ke home juga, slotnya sudah
      penuh lagi, perlu keputusan serupa (ganti salah satu, atau baru boleh
      nambah baris/kolom).

- [ ] **Scope/desain layar ini adalah interpretasi pertama, belum divalidasi
      user** — ticket Notion dibuat kosong ("saya belum punya ide untuk
      isinya"), jadi kemungkinan besar perlu direvisi setelah user lihat
      hasilnya (mis. mungkin maunya bukan daftar 8 kota tapi sesuatu yang
      lain sama sekali).
- [ ] Jam ghurub ditampilkan mengikuti zona waktu **perangkat**
      (`EphemerisCalculator.formatLocalTime` pakai `Locale`/`TimeZone`
      default device), BUKAN zona waktu lokal tiap markaz (WIB/WITA/WIT) —
      sudah diberi disclaimer di UI, tapi kalau nanti mau akurat per-zona,
      `formatLocalTime` perlu parameter `TimeZone` eksplisit per markaz
      (perubahan ke `EphemerisCalculator` yang dipakai bareng "Awal Bulan",
      jadi perlu hati-hati tidak mengubah perilaku fitur itu).
- [ ] Tidak ada selector bulan/tahun seperti "Awal Bulan" (section 3 di
      `bulan-hijriyah.md`) — selalu bulan Hijriyah terdekat ke depan
      (`monthOffset=0`). `HisabNasionalViewModel.calculateNasional()` sudah
      menerima parameter `monthOffset` supaya gampang disambung ke UI
      selector kalau nanti dibutuhkan.
- [ ] 8 titik markaz hardcode di kode (`HisabNasionalCalculator.markazList`)
      — kalau user mau titik yang berbeda/lebih banyak/mengikuti daftar
      resmi Kemenag, tinggal ubah list ini, tidak ada dependensi lain yang
      perlu diubah.
- [ ] Elevasi (`heightMeters`) semua markaz disamakan `0.0` — penyederhanaan,
      belum pakai elevasi asli tiap kota.
