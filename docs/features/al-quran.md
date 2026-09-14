# Al-Qur'an Digital & Audio

### 1. Ringkasan (Overview)

- **Nama fitur**: Al-Qur'an Digital & Audio.
- **Deskripsi singkat**: Menampilkan daftar 114 surah beserta detail ayat
  (teks Arab, transliterasi Latin, terjemahan Indonesia) dan tombol putar
  audio murottal per ayat. Item backlog Notion dengan prioritas tertinggi
  karena ini fitur paling dicari user aplikasi sholat Al-Kaukaba.

### 2. Entry point & prasyarat

- Tombol "Al-Qur'an" di grid menu beranda `MainActivity` (`bt_quran` di
  `activity_main.xml`, handler `binding.btQuran` di `MainActivity.kt`) membuka
  `DaftarSurahActivity`.
- Prasyarat: koneksi internet (data & audio diambil langsung dari API publik
  `equran.id`, tidak ada API key/config khusus yang perlu di-setup).

### 3. Titik masuk logika & navigasi

- `DaftarSurahActivity` → tap item surah → `DetailSurahActivity` (extra
  `EXTRA_NOMOR_SURAH: Int`, nomor surah 1-114).
- `QuranRepository` (object singleton di `repo/QuranRepository.kt`) adalah
  titik masuk kalau developer lain mau reuse data Qur'an di layar lain —
  `getSurahList()` dan `getSurahDetail(nomor)`, keduanya `suspend fun`
  mengembalikan `Resource<T>` dan sudah di-cache in-memory.
- `DEFAULT_QORI_KEY` (di file yang sama) adalah qori default untuk audio
  ("05" = Misyari Rasyid Al-Afasi) — satu-satunya tempat yang perlu diubah
  kalau nanti mau ganti qori default atau menambah picker qori.

### 4. Struktur & alur data

- `model/QuranModels.kt` — `QuranApiResponse<T>` (wrapper `code/message/data`
  dari equran.id), `Surah` (item daftar), `SurahDetail` (detail + `List<Ayat>`),
  `Ayat` (`teksArab`, `teksLatin`, `teksIndonesia`, `audio` per qori).
- `api/QuranApiService.kt` — interface `EquranApi` (Retrofit,
  `GET api/v2/surat` & `GET api/v2/surat/{nomor}`) + `object QuranRetrofitClient`
  (base url `https://equran.id/`), pola sama persis dengan `AladhanApi` di
  `PrayersApiService.kt` untuk Waktu Sholat.
- `repo/QuranRepository.kt` — bungkus response Retrofit jadi `Resource<T>`,
  cache in-memory (`surahListCache`, `surahDetailCache`) supaya navigasi
  bolak-balik tidak fetch API berulang.
- `viewmodel/quran/DaftarSurahViewModel.kt` & `DetailSurahViewModel.kt` —
  `ViewModel` biasa tanpa Factory (tidak butuh constructor arg, sama seperti
  `GerhanaViewModel`), expose `LiveData<Resource<T>>`.
- `ui/quran/DaftarSurahActivity.kt` — RecyclerView + `SwipeRefreshLayout`,
  adapter `adapter/SurahAdapter.kt`.
- `ui/quran/DetailSurahActivity.kt` — header info surah (card gradient navy,
  deskripsi bisa expand/collapse) + RecyclerView ayat, adapter
  `adapter/AyatAdapter.kt`. Audio diputar via `android.media.MediaPlayer`
  biasa (bukan Service) langsung di Activity — `prepareAsync()` dari URL
  `ayat.audio[DEFAULT_QORI_KEY]`, `release()` setiap ganti ayat/keluar layar.
  Highlight ayat yang sedang diputar dikelola lewat
  `AyatAdapter.setPlayingAyat(nomorAyat)` (pakai `notifyItemChanged`, bukan
  `notifyDataSetChanged`, supaya scroll position tidak reset).

Alur data: `Activity -> ViewModel -> QuranRepository -> Retrofit (equran.id)`.

### 5. Dependencies & tech stack khusus

- Tidak ada tambahan khusus di luar stack umum app (Retrofit + Gson,
  Coroutines, ViewBinding sudah dipakai fitur lain). Tidak menambah Room/DB
  library apapun — cache hanya in-memory per proses `QuranRepository`.

### 6. Testing

- Belum ada unit test otomatis untuk `QuranRepository`/ViewModel — gap yang
  perlu diisi kalau fitur ini dikembangkan lebih lanjut (lihat
  `strategi-unit-test.md` untuk pola mocking yang dipakai fitur lain).
- Verifikasi manual yang sudah dilakukan: build + install APK debug ke
  emulator, buka menu Al-Qur'an dari beranda, daftar 114 surah termuat dari
  API asli, buka Al-Fatihah, ayat+terjemahan+transliterasi tampil benar,
  tombol audio berpindah ikon play/pause & kartu ter-highlight saat ayat
  diputar.

### 7. Known issues & TODOs

- **Data equran.id sengaja tidak lengkap dipetakan**: field `suratSelanjutnya`
  / `suratSebelumnya` dari response API TIDAK dimasukkan ke `SurahDetail` —
  API mengembalikan `false` (boolean) untuk surah pertama/terakhir alih-alih
  `null`/objek, yang bikin Gson gagal parse (crash "Gagal memuat surah" saat
  dites di surah 1). Karena field ini juga belum dipakai UI (belum ada
  tombol next/prev surah), solusinya menghapus field itu dari model daripada
  menulis custom Gson adapter untuk data yang tidak dipakai.
- Belum ada search/filter surah di `DaftarSurahActivity` (114 item, murni
  scroll).
- Belum ada picker qori — qori audio fixed ke `DEFAULT_QORI_KEY` ("05").
- Belum ada mode offline/download audio — tiap ayat streaming langsung dari
  CDN equran.id, dan cache repository hilang begitu proses app dimatikan.
- Belum ada bookmark/penanda "terakhir dibaca".
- Font Arab masih rendering default Android (Noto Sans/Naskh Arabic bawaan
  OS), belum ada font kaligrafi Utsmani khusus yang di-bundle.
