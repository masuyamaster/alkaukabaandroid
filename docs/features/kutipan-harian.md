# Ayat/Hadits Harian (Kutipan Harian)

### 1. Ringkasan (Overview)

- **Nama fitur**: Ayat/Artikel Islami Harian (Daily Quote).
- **Deskripsi singkat**: Menampilkan satu ayat Al-Qur'an atau hadits Arbain
  Nawawi di halaman utama, di bawah kartu Kalender, berganti tiap hari
  berdasarkan hari-dalam-tahun (semua user melihat kutipan yang sama di hari
  yang sama). Item backlog Notion prioritas Low.

### 2. Entry point & prasyarat

- Kartu `cardDailyQuote` di `activity_main.xml`, langsung di bawah
  `layoutCalendarContainer` (kartu Kalender) — dimuat otomatis saat
  `MainActivity` dibuka, tidak ada tombol/interaksi user untuk memicunya.
- Prasyarat: koneksi internet (data diambil dari backend sendiri,
  `alkaukabaweb`, endpoint `/api/daily-quote` — lihat §4). Tidak ada API
  key/config khusus di sisi Android.

### 3. Titik masuk logika & navigasi

- Tidak ada navigasi ke layar lain — kartu statis di halaman utama.
- `DailyQuoteRepository` (object singleton di `repo/DailyQuoteRepository.kt`)
  adalah titik masuk kalau developer lain mau reuse data ini di layar lain —
  `getToday()`, `suspend fun` mengembalikan `Resource<DailyQuote>`, di-cache
  in-memory per tanggal (pola sama dengan `DoaRepository`).
- Dipanggil dari `MainActivity.setupDailyQuote()`, dipanggil sekali di
  `onCreate()` (bukan lewat `MainViewModel`/lokasi, karena kontennya tidak
  bergantung lokasi user).

### 4. Struktur & alur data

**Android:**
- `model/DailyQuoteModels.kt` — `DailyQuoteResponse` (wrapper `data`),
  `DailyQuote` (type/arabic/latin (nullable)/translation/source).
- `api/DailyQuoteApiService.kt` — interface `DailyQuoteApi` (Retrofit, `GET
  api/daily-quote`) + `object DailyQuoteRetrofitClient` (base url
  `https://alkaukaba.com/`, sama dengan `AuthClient`/`DoaRetrofitClient` —
  untuk testing lokal ganti ke `http://127.0.0.1:8000/` + `adb reverse
  tcp:8000 tcp:8000` + `php artisan serve` di `alkaukabaweb`, lihat CLAUDE.md
  bagian "Konfigurasi API backend").
- `repo/DailyQuoteRepository.kt` — bungkus response Retrofit jadi
  `Resource<DailyQuote>`, cache in-memory di-key per tanggal (format
  `yyyy-MM-dd`) supaya tidak fetch ulang tiap kali `onCreate` dipanggil
  (mis. rotasi layar) dalam hari yang sama.
- `MainActivity.setupDailyQuote()` — kalau sukses, isi
  `tvDailyQuoteArabic`/`tvDailyQuoteLatin` (disembunyikan kalau null, mis.
  untuk hadits)/`tvDailyQuoteTranslation`/`tvDailyQuoteSource`; kalau gagal
  (`Resource.Error`, mis. offline), sembunyikan `cardDailyQuote` sepenuhnya
  — kartu prioritas rendah ini tidak boleh mengganggu pengalaman beranda
  dengan pesan error.

Alur data: `MainActivity -> DailyQuoteRepository -> Retrofit (alkaukaba.com)`.

**Backend (`alkaukabaweb`, repo Laravel terpisah):**
- Migrasi `daily_quote_refs` (type enum `ayat`/`hadits`, `ref` string —
  `"surah:ayah"` untuk ayat mis. `"2:286"`, nomor Arbain 1-42 untuk hadits,
  `order` unik).
- Model `DailyQuoteRef` (fillable saja, tanpa relasi).
- `DailyQuoteController@today` → `GET /api/daily-quote` — pilih ref index
  `(hari-dalam-tahun - 1) % jumlah ref`, lalu resolve isi lengkap secara
  **live** dari API eksternal (bukan disimpan penuh di DB sendiri):
  - `type=ayat` → `GET https://equran.id/api/v2/surat/{surah}`, ambil ayat
    sesuai nomor.
  - `type=hadits` → `GET https://api.myquran.com/v2/hadits/arbain/{ref}`.
  - Hasil di-cache `Cache::remember` per tanggal (`now()->endOfDay()`) supaya
    tidak hit API eksternal berulang di hari yang sama.
  - Kalau API eksternal gagal/response tidak valid, fallback ke konstanta
    statis `DailyQuoteController::FALLBACK` (QS. Al-Insyirah: 5) — endpoint
    tidak pernah balas error ke client.
- `DailyQuoteRefSeeder` mengisi 72 ref (30 ayat pendek/inspiratif dikurasi
  manual + 42 hadits Arbain Nawawi lengkap), diselang-seling.

### 5. Dependencies & tech stack khusus

- **Android**: tidak ada tambahan khusus di luar stack umum app (Retrofit +
  Gson, Coroutines, ViewBinding). Tidak ada Room/local DB — cache hanya
  in-memory per proses (hilang saat app di-kill, fetch ulang saat dibuka
  lagi; bukan masalah karena backend tetap balas hasil yang sama untuk hari
  itu berkat cache server-side).
- **Backend**: dua API eksternal publik tanpa API key —
  [equran.id](https://equran.id/apidev/v2) (ayat, terjemahan Indonesia) dan
  [api.myquran.com](https://api.myquran.com/) (hadits Arbain Nawawi). Kalau
  salah satu berhenti gratis/berubah kontrak, dampaknya terbatas ke jenis
  konten itu saja untuk hari-hari yang ref-nya mengarah ke sana — fallback
  statis tetap menjaga endpoint tidak error total.

### 6. Testing

- Belum ada unit test otomatis untuk `DailyQuoteRepository` maupun
  `DailyQuoteController` — gap yang perlu diisi kalau fitur ini dikembangkan
  lebih lanjut (lihat `strategi-unit-test.md`).
- Verifikasi manual yang sudah dilakukan (2026-09-14): backend — migrate +
  seed ke SQLite lokal, `php artisan serve`, `curl` ke `/api/daily-quote`
  berhasil balikin ayat QS. Al-Isra':23 lengkap (arabic/latin/translation/
  source) sesuai rotasi hari-dalam-tahun. Android — `compileDebugKotlin` &
  `installDebug` sukses ke emulator, `MainActivity` terbuka normal tanpa
  crash dan seluruh kartu lain (Sholat, Fase Bulan, menu, Kalender) tetap
  render benar. **Belum** sempat diverifikasi visual langsung (screenshot
  kartu `cardDailyQuote` ter-scroll) di sesi ini — swipe-scroll manual lewat
  `adb` dua kali salah kena tombol menu lain (nyasar ke layar Al-Qur'an),
  sesuai aturan di CLAUDE.md soal batasi percobaan tap/swipe berulang,
  verifikasi visual dihentikan dan diganti cek lewat `uiautomator dump` +
  review kode/binding (ID XML `cardDailyQuote` dkk. berhasil di-resolve
  ViewBinding tanpa error kompilasi, jadi struktur layout valid) — **perlu
  scroll manual sekali oleh user/tester** untuk konfirmasi visual akhir.

### 7. Known issues & TODOs

- **Belum diverifikasi visual di emulator** (lihat §6) — perlu dicek manual
  sekali bahwa kartu tampil rapi di bawah kalender, termasuk kasus hadits
  (tanpa `latin`, TextView `tvDailyQuoteLatin` harus tetap `GONE`).
- Endpoint bergantung dua API eksternal pihak ketiga tanpa SLA — kalau mau
  lebih tahan lama, pertimbangkan simpan hasil resolve ke tabel sendiri
  (bukan cuma `ref`) sebagai cache permanen, bukan fetch live tiap cache
  harian habis.
- 30 ref ayat saat ini kurasi manual satu putaran (tidak ada mekanisme untuk
  menambah tanpa migrate DB produksi) — nambah/edit ref berarti insert
  langsung ke tabel `daily_quote_refs` di server (belum ada UI admin).
- Tidak ada mode offline untuk hari yang belum pernah dibuka sebelumnya
  (beda dari fitur lain yang datanya di-cache in-memory sepanjang proses) —
  kalau app dibuka pertama kali tanpa internet, kartu disembunyikan
  (`View.GONE`), bukan tampil dari cache lama.
