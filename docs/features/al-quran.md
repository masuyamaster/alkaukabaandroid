# Al-Qur'an Digital & Audio

### 1. Ringkasan (Overview)

- **Nama fitur**: Al-Qur'an Digital & Audio.
- **Deskripsi singkat**: Menampilkan daftar 114 surah beserta detail ayat
  (teks Arab, transliterasi Latin, terjemahan Indonesia) dan tombol putar
  audio murottal per ayat, plus pencarian surat & ayat. Layar detail surah
  punya 2 mode baca yang bisa ditoggle: **Terjemahan** (kartu per-ayat +
  transliterasi + terjemahan + tombol audio) dan **Mushaf** (teks Arab
  mengalir satu paragraf tanpa terjemahan, meniru halaman mushaf fisik).
  Item backlog Notion dengan prioritas tertinggi karena ini fitur paling
  dicari user aplikasi sholat Al-Kaukaba.

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
- Kotak pencarian di `DaftarSurahActivity` (`etSearch`) → filter surat lokal
  + `QuranRepository.searchAyat(keyword)` → tap hasil ayat → buka
  `DetailSurahActivity` dengan extra tambahan `EXTRA_HIGHLIGHT_AYAT: Int`,
  yang men-scroll RecyclerView ke ayat itu dan memberi highlight sementara
  (`AyatAdapter.setHighlightedAyat`).

### 4. Struktur & alur data

- `model/QuranModels.kt` — `QuranApiResponse<T>` (wrapper `code/message/data`
  dari equran.id), `Surah` (item daftar), `SurahDetail` (detail + `List<Ayat>`),
  `Ayat` (`teksArab`, `teksLatin`, `teksIndonesia`, `audio` per qori).
- `api/QuranApiService.kt` — interface `EquranApi` (Retrofit,
  `GET api/v2/surat` & `GET api/v2/surat/{nomor}`) + `object QuranRetrofitClient`
  (base url `https://equran.id/`), pola sama persis dengan `AladhanApi` di
  `PrayersApiService.kt` untuk Waktu Sholat. Di file yang sama juga ada
  `AlQuranCloudApi` + `AlQuranCloudRetrofitClient` (base url
  `https://api.alquran.cloud/`) khusus untuk **search ayat** — equran.id
  tidak punya endpoint pencarian sama sekali, jadi dipakai API publik lain
  yang punya full-text search terjemahan Indonesia lintas seluruh Qur'an
  (`GET v1/search/{keyword}/all/id.indonesian`).
- `repo/QuranRepository.kt` — bungkus response Retrofit jadi `Resource<T>`,
  cache in-memory (`surahListCache`, `surahDetailCache`) supaya navigasi
  bolak-balik tidak fetch API berulang. `searchAyat(keyword)` memanggil
  `AlQuranCloudApi`, membatasi hasil ke 30 teratas (`MAX_AYAT_SEARCH_RESULTS`)
  karena query umum bisa balikin ribuan match, dan memperlakukan HTTP 404
  (respons API saat tidak ada match sama sekali) sebagai `Resource.Success`
  list kosong, bukan error. `getCachedSurahName(nomor)` dipakai untuk
  menampilkan nama surah versi equran.id di hasil pencarian ayat — penulisan
  nama surah di alquran.cloud beda (mis. "Al-Faatiha" vs "Al-Fatihah" di
  equran.id), jadi nama selalu diambil dari cache daftar surah yang sama
  dengan yang ditampilkan di seluruh layar lain.
- `viewmodel/quran/DaftarSurahViewModel.kt` & `DetailSurahViewModel.kt` —
  `ViewModel` biasa tanpa Factory (tidak butuh constructor arg, sama seperti
  `GerhanaViewModel`), expose `LiveData<Resource<T>>`. `searchAyat(keyword)`
  di `DaftarSurahViewModel` debounce 400ms (`Job` di-cancel tiap kali dipanggil
  ulang) dan skip panggilan API sama sekali kalau keyword di bawah
  `MIN_AYAT_SEARCH_LENGTH` (3 karakter).
- `ui/quran/DaftarSurahActivity.kt` — RecyclerView + `SwipeRefreshLayout`
  untuk daftar normal, adapter `adapter/SurahAdapter.kt`; plus kotak
  pencarian (`etSearch`) yang saat berisi teks mengganti tampilan ke
  `rvSearchResults` (adapter `adapter/SearchResultAdapter.kt`, multi-view-type:
  header section, item surat, item ayat) — hasil surat difilter lokal dari
  `allSurah` yang sudah dimuat, hasil ayat dari `viewModel.searchAyat(...)`.
- `ui/quran/DetailSurahActivity.kt` — header info surah (card gradient navy,
  deskripsi bisa expand/collapse) + toggle mode baca (`tvModeTerjemahan`/
  `tvModeMushaf`, enum privat `ReadingMode`) yang switch visibility antara
  `rvAyat` (RecyclerView, adapter `adapter/AyatAdapter.kt`) dan `scrollMushaf`
  (`ScrollView` + satu `TextView tvMushaf`). Audio diputar via
  `android.media.MediaPlayer` biasa (bukan Service) langsung di Activity —
  `prepareAsync()` dari URL `ayat.audio[DEFAULT_QORI_KEY]`, `release()` setiap
  ganti ayat/keluar layar; hanya tersedia di mode Terjemahan (mode Mushaf
  murni baca, tidak ada tombol audio). Highlight ayat yang sedang diputar
  dikelola lewat `AyatAdapter.setPlayingAyat(nomorAyat)` (pakai
  `notifyItemChanged`, bukan `notifyDataSetChanged`, supaya scroll position
  tidak reset).
- `utils/MushafTextBuilder.kt` + `utils/AyahMarkerSpan.kt` — susun teks Arab
  satu surah jadi satu `SpannableStringBuilder` mengalir untuk mode Mushaf.
  Batas antar-ayat ditandai lingkaran kecil gold gambar manual
  (`AyahMarkerSpan`, sebuah `ReplacementSpan`) berisi nomor Arab-Indic, BUKAN
  karakter Unicode tanda kurung hias U+FD3E/FD3F (konvensi teks Mushaf
  digital seperti Tanzil) — karakter itu tidak punya glyph di font Arab
  bawaan Android yang dipakai app ini dan tampil sebagai kotak/tofu. Placeholder
  span sengaja cuma 1 karakter tanpa spasi tambahan di sekitarnya; beberapa
  spasi netral berurutan terbukti bikin algoritma bidi salah menempatkan
  marker (meloncat ke tengah kata ayat berikutnya, bukan di akhir ayat).

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
  diputar. Pencarian: keyword "yasin" -> muncul section "Surat" dengan surah
  36; keyword "rahmat" -> muncul section "Ayat" (30 hasil) dari beberapa
  surah, tap salah satu ("Al-Baqarah Ayat 64") -> berhasil buka
  `DetailSurahActivity`, auto-scroll & highlight persis ke ayat 64. Toggle
  mode Mushaf: dites di Al-Fatihah, urutan baca 7 ayat benar & marker
  lingkaran ada tepat di batas tiap ayat (sempat ada bug marker meloncat ke
  tengah kata sebelum fix spasi di `MushafTextBuilder`), toggle balik ke
  Terjemahan juga normal.

### 7. Known issues & TODOs

- **Data equran.id sengaja tidak lengkap dipetakan**: field `suratSelanjutnya`
  / `suratSebelumnya` dari response API TIDAK dimasukkan ke `SurahDetail` —
  API mengembalikan `false` (boolean) untuk surah pertama/terakhir alih-alih
  `null`/objek, yang bikin Gson gagal parse (crash "Gagal memuat surah" saat
  dites di surah 1). Karena field ini juga belum dipakai UI (belum ada
  tombol next/prev surah), solusinya menghapus field itu dari model daripada
  menulis custom Gson adapter untuk data yang tidak dipakai.
- Pencarian ayat bergantung pada API pihak ketiga terpisah (alquran.cloud)
  dari sumber data utama (equran.id) — cuplikan teks di hasil pencarian
  berasal dari edisi terjemahan alquran.cloud, bisa sedikit beda kata/tanda
  baca dari teks final yang tampil di `DetailSurahActivity` (yang selalu
  dari equran.id). Nomor surah+ayat tetap konsisten (penomoran ayat
  universal), jadi navigasinya tetap akurat.
- Belum ada picker qori — qori audio fixed ke `DEFAULT_QORI_KEY` ("05").
- Belum ada mode offline/download audio — tiap ayat streaming langsung dari
  CDN equran.id, dan cache repository hilang begitu proses app dimatikan.
- Belum ada bookmark/penanda "terakhir dibaca".
- Font Arab masih rendering default Android (Noto Sans/Naskh Arabic bawaan
  OS), belum ada font kaligrafi Utsmani khusus yang di-bundle.
- Highlight & auto-scroll dari hasil pencarian ayat (`EXTRA_HIGHLIGHT_AYAT`)
  cuma jalan di mode Terjemahan (`rvAyat`, RecyclerView) — mode Mushaf selalu
  mulai dari atas karena `ScrollView` + satu `TextView` panjang tidak punya
  API "scroll ke posisi ayat X" sesederhana `LinearLayoutManager`. Mode baca
  juga selalu reset ke Terjemahan tiap buka surah baru (tidak diingat lintas
  sesi).
