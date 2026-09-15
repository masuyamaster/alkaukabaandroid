# Kalkulator Zakat

### 1. Ringkasan (Overview)
- **Nama fitur**: Kalkulator Zakat
- **Deskripsi singkat**: Satu layar dengan 3 tab kalkulator zakat - fitrah, mal
  (harta), dan profesi - supaya user bisa menghitung kewajiban zakatnya tanpa
  perlu tabel/rumus manual. Diangkat dari ide fitur di Notion (board
  "Pengembangan Al-Kaukaba", kartu "Kalkulator Zakat", dicatat 2026-08-30).

### 2. Entry point & prasyarat
- Dipicu dari menu utama (`activity_main.xml`), ikon `bt_zakat` (hati emerald)
  di baris menu ke-3, kolom ke-3 (setelah Masjid Terdekat & Tasbih Digital).
  Wiring klik ada di `MainActivity.setupClickListeners()`.
- Prasyarat: koneksi internet untuk tab Mal & Profesi (fetch harga emas untuk
  hitung nisab) - tab Fitrah tidak butuh internet sama sekali (murni input
  manual). Tidak ada API key/config khusus yang perlu di-setup developer.

### 3. Titik masuk logika & navigasi
- `ZakatActivity` (`ui/zakat/ZakatActivity.kt`) - satu Activity, 3 tab
  (`TabLayout`) yang toggle visibility 3 blok `LinearLayout` statis
  (`layoutFitrah`/`layoutMal`/`layoutProfesi`), bukan ViewPager2/Fragment -
  semua konten statis & ringan, tidak perlu lazy-load per tab.
- `ZakatCalculator` (`utils/ZakatCalculator.kt`) - object berisi 3 fungsi
  murni (`hitungFitrah`, `hitungMal`, `hitungProfesi`), titik "colok" utama
  kalau mau reuse logika hitung zakat di tempat lain atau menambah unit test.
- `GoldPriceRepository.getHargaEmasPerGram()` (`repo/zakat/`) - satu-satunya
  titik akses harga emas, dipanggil dari `ZakatViewModel.muatHargaEmas()`.
- Tidak ada navigasi keluar dari `ZakatActivity` ke Activity lain - murni
  layar tunggal, kembali lewat tombol back toolbar.

### 4. Struktur & alur data
- `ui/zakat/ZakatActivity.kt` - UI, observer, validasi input, format Rupiah
  (`NumberFormat.getCurrencyInstance(Locale("id","ID"))`).
- `viewmodel/zakat/ZakatViewModel.kt` - `LiveData<Resource<Double>>` harga
  emas per gram, fetch sekali saat `init` + bisa retry manual.
- `repo/zakat/GoldPriceRepository.kt` - panggil `GoldPriceApi`, pilih entry
  gramasi 1 gram dengan `materialType` mengandung "LM Antam produksi tahun"
  (harga batangan standar, bukan varian Certicard 100gr yang dipotong per-gram
  - API balikin beberapa varian produk untuk gramasi yang sama).
- `api/GoldPriceApiService.kt` - interface Retrofit + client ke
  `logam-mulia-api` (lihat §5).
- `utils/ZakatCalculator.kt` - murni fungsi hitung, tidak ada dependency ke
  Android framework (gampang di-unit-test tanpa Robolectric/instrumented).
- Alur data tab Mal/Profesi: `ZakatActivity` (tap tombol Hitung) -> baca
  `hargaEmasPerGram` (di-cache di Activity dari observer ViewModel) ->
  `ZakatCalculator.hitungMal/hitungProfesi` -> render ke `TextView` hasil.
  Tab Fitrah tidak lewat ViewModel sama sekali (tidak butuh data async).

### 5. Dependencies & tech stack khusus
- **Sumber harga emas**: [logam-mulia-api](https://github.com/iamutaki/logam-mulia-api)
  (`https://logam-mulia-api.iamutaki.workers.dev/api/prices/anekalogam`) -
  scraper Antam/Aneka Logam via Cloudflare Worker, gratis total tanpa API
  key/billing (sama pertimbangannya dengan Overpass API di fitur Masjid
  Terdekat: dicek dulu supaya tidak butuh biaya). Trade-off: bukan API resmi
  Antam, jadi keandalan tergantung uptime scraper pihak ketiga - kalau API ini
  mati, tab Mal & Profesi akan tampil kartu error "Gagal memuat harga emas"
  dengan tombol "Coba Lagi", tab Fitrah tetap berfungsi normal.
- Tidak ada library tambahan di luar stack umum app (Retrofit/Gson yang sudah
  dipakai fitur lain, Material Components untuk `TabLayout`).

### 6. Formula & asumsi fiqih (keputusan desain, bukan derivable dari kode)
Keputusan berikut diambil lewat diskusi dengan user sebelum implementasi
(2026-09-15), dicatat di sini karena beda ormas/lembaga bisa beda metode:
- **Fitrah**: 2.5 kg beras per jiwa (konsensus mayoritas ormas Indonesia),
  dikonversi ke Rupiah pakai harga beras per kg yang diisi manual oleh user
  (tidak ada API harga beras, jadi tidak diotomatisasi).
- **Mal**: nisab = 85 gram emas x harga emas per gram hari ini. Zakat = 2.5%
  x (total harta - total hutang), hanya kalau harta bersih >= nisab. **Tidak
  ada pengecekan haul** (kepemilikan genap 1 tahun) - kalkulator asumsikan
  user sudah tahu hartanya memenuhi syarat haul.
- **Profesi**: metode "akumulasi tahunan ala zakat mal" (bukan metode
  per-panen ala zakat pertanian 5-10%) - total penghasilan setahun dizakati
  2.5% kalau totalnya mencapai nisab 85 gram emas. Ini salah satu dari
  beberapa metode yang dipakai lembaga zakat di Indonesia; dipilih karena
  paling sederhana dihitung tanpa perlu breakdown pengeluaran/kebutuhan pokok
  bulanan.

### 7. Testing
- `ZakatCalculatorTest` (`app/src/test/.../utils/ZakatCalculatorTest.kt`) -
  unit test murni (tanpa Android/API/DB) untuk `ZakatCalculator`: fitrah
  (2.5 kg/jiwa), mal (nisab 85gr, hutang mengurangi harta bersih, harta
  bersih tidak boleh negatif, batas tepat di nisab), profesi (akumulasi
  tahunan 2.5%). Tidak menguji `GoldPriceRepository`/`ZakatViewModel` (butuh
  network/LiveData, di luar scope unit test murni untuk sekarang).
- Verifikasi manual yang sudah dilakukan (2026-09-15, emulator Pixel 4 XL API
  36): build `installDebug` sukses, menu ikon "Kalkulator Zakat" muncul benar
  di baris 3 main menu, tab Fitrah diisi (4 jiwa, harga beras Rp13.000) ->
  hasil benar (10 kg, Rp130.000), tab Mal terbuka & berhasil fetch harga emas
  live dari API (nisab 85 gr terhitung benar). Tab Profesi & hasil hitung Mal
  setelah perbaikan filter harga emas **belum sempat diverifikasi visual**
  (sesi verifikasi UI terhenti karena tap otomatis mulai tidak akurat/tidak
  bisa diandalkan - lihat aturan di root `CLAUDE.md` soal membatasi percobaan
  tap berulang) - perlu dicoba manual oleh user sebelum dianggap selesai
  total.

### 8. Known issues & TODOs
- Harga beras (zakat fitrah) & tidak ada override manual untuk harga emas
  kalau API `logam-mulia-api` down - user harus tunggu/retry, tidak bisa input
  manual sebagai fallback. Kalau ke depan API ini sering down, pertimbangkan
  tambah opsi input manual harga emas.
- Zakat mal tidak cek syarat haul (lihat §6) - murni disclaimer di teks
  deskripsi tab, bukan validasi input.
- Tab Profesi belum diverifikasi visual di emulator (lihat §7).
