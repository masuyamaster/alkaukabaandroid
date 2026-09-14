# Kumpulan Doa & Dzikir (Al-Mathurat/Hisnul Muslim)

### 1. Ringkasan (Overview)

- **Nama fitur**: Kumpulan Doa & Dzikir (Al-Mathurat/Hisnul Muslim).
- **Deskripsi singkat**: Menampilkan koleksi doa & dzikir harian (Dzikir Pagi,
  Dzikir Petang, Dzikir Setelah Shalat, Doa Harian, Doa Pilihan) lengkap
  dengan teks Arab, transliterasi Latin, terjemahan Indonesia, catatan jumlah
  bacaan, fawaid (keutamaan), dan sumber hadits. Item backlog Notion dengan
  fokus utama Dzikir Pagi & Petang, diperkirakan meningkatkan retensi
  pengguna harian.

### 2. Entry point & prasyarat

- Tombol "Doa & Dzikir" di grid menu beranda `MainActivity` (`bt_doa_dzikir`
  di `activity_main.xml`, handler `binding.btDoaDzikir` di `MainActivity.kt`)
  membuka `DaftarKategoriDoaActivity`.
- Prasyarat: koneksi internet (data diambil dari backend sendiri,
  `alkaukabawebserver`, endpoint `/api/doa-categories*` — lihat §4). Tidak
  ada API key/config khusus.

### 3. Titik masuk logika & navigasi

- `DaftarKategoriDoaActivity` (5 kategori) → tap kategori →
  `DetailKategoriDoaActivity` (extra `EXTRA_SLUG: String`, `EXTRA_NAMA:
  String` untuk judul toolbar).
- `DoaRepository` (object singleton di `repo/DoaRepository.kt`) adalah titik
  masuk kalau developer lain mau reuse data doa/dzikir di layar lain —
  `getCategories()` dan `getCategoryItems(slug)`, keduanya `suspend fun`
  mengembalikan `Resource<T>` dan sudah di-cache in-memory (pola sama persis
  dengan `QuranRepository`).

### 4. Struktur & alur data

**Android:**
- `model/DoaModels.kt` — `DoaApiResponse<T>` (wrapper `data`), `DoaCategory`
  (id/slug/name/order/items_count), `DoaCategoryDetail` (category +
  `List<DoaItem>`), `DoaItem` (title/arabic/latin/translation/notes/
  fawaid/source/order).
- `api/DoaApiService.kt` — interface `DoaApi` (Retrofit, `GET
  api/doa-categories` & `GET api/doa-categories/{slug}`) + `object
  DoaRetrofitClient` (base url `https://alkaukaba.com/`, sama dengan
  `AuthClient` — untuk testing lokal ganti ke `http://127.0.0.1:8000/` +
  `adb reverse tcp:8000 tcp:8000` + `php artisan serve` di
  `alkaukabawebserver`, lihat CLAUDE.md bagian "Konfigurasi API backend").
- `repo/DoaRepository.kt` — bungkus response Retrofit jadi `Resource<T>`,
  cache in-memory (`categoryListCache`, `categoryDetailCache`).
- `viewmodel/doa/DaftarKategoriDoaViewModel.kt` &
  `DetailKategoriDoaViewModel.kt` — `ViewModel` biasa tanpa Factory, expose
  `LiveData<Resource<T>>`.
- `ui/doa/DaftarKategoriDoaActivity.kt` — RecyclerView + `SwipeRefreshLayout`
  daftar 5 kategori, adapter `adapter/DoaCategoryAdapter.kt`
  (`item_doa_kategori.xml`).
- `ui/doa/DetailKategoriDoaActivity.kt` — RecyclerView daftar item doa/dzikir
  dalam satu kategori, adapter `adapter/DoaItemAdapter.kt`
  (`item_doa.xml`) — field opsional (`notes`/`fawaid`/`source`) disembunyikan
  (`View.GONE`) kalau null/kosong dari API, bukan ditampilkan string kosong.

Alur data: `Activity -> ViewModel -> DoaRepository -> Retrofit (alkaukaba.com)`.

**Backend (`alkaukabawebserver`, repo Laravel terpisah):**
- Migrasi `doa_categories` (slug, name, order) & `doa_items` (doa_category_id
  FK, title, arabic, latin, translation, notes, fawaid, source, order).
- Model `DoaCategory` (`hasMany` items, di-order) & `DoaItem` (`belongsTo`
  category).
- `DoaController@categories` → `GET /api/doa-categories` (list + `items_count`
  via `withCount`), `DoaController@items` → `GET
  /api/doa-categories/{slug}` (detail kategori + seluruh item-nya).
- `DoaSeeder` mengisi data dari fixture JSON lokal di
  `database/seeders/data/doa-dzikir/*.json` (bukan fetch API tiap seed, biar
  reproducible & tidak bergantung API pihak ketiga yang bisa down/rate-limit)
  — lihat §5 untuk sumber datanya.

### 5. Dependencies & tech stack khusus

- **Android**: tidak ada tambahan khusus di luar stack umum app (Retrofit +
  Gson, Coroutines, ViewBinding). Tidak ada Room/local DB — cache hanya
  in-memory per proses `DoaRepository`, hilang saat app di-kill (tidak ada
  dukungan offline penuh, beda dari rencana awal "bundle lokal" yang sempat
  dipertimbangkan — diputuskan pakai backend sendiri karena kontennya mau
  bisa dikelola/di-update tanpa rilis APK baru).
- **Sumber data konten**: fixture JSON di
  `alkaukabawebserver/database/seeders/data/doa-dzikir/` diambil dari API
  publik MIT-licensed
  [fitrahive/dua-dhikr](https://github.com/fitrahive/dua-dhikr) (konten
  Hisnul Muslim berbahasa Indonesia, default `Accept-Language: id`). 97 item
  di 5 kategori: Dzikir Pagi (19), Dzikir Petang (19), Dzikir Setelah Shalat
  (13), Doa Harian (38), Doa Pilihan (8) — bukan seluruh ~250 entri buku
  Hisnul Muslim asli, tapi kategori paling umum dipakai (termasuk fokus utama
  Dzikir Pagi & Petang dari catatan Notion).

### 6. Testing

- Belum ada unit test otomatis untuk `DoaRepository`/ViewModel maupun
  `DoaController` — gap yang perlu diisi kalau fitur ini dikembangkan lebih
  lanjut (lihat `strategi-unit-test.md`).
- Verifikasi manual yang sudah dilakukan (2026-09-14): backend — migrate +
  seed ke SQLite lokal, `php artisan serve`, `curl` ke
  `/api/doa-categories` dan `/api/doa-categories/morning-dhikr` berhasil
  balikin data lengkap. Android — build + install APK debug ke emulator
  dengan `DoaRetrofitClient` sementara dialihkan ke `127.0.0.1:8000` (+ `adb
  reverse`), buka menu "Doa & Dzikir" dari beranda, 5 kategori termuat
  dengan jumlah item yang benar, buka "Dzikir Pagi", item Ayat al-Kursi &
  Al-Ikhlas tampil lengkap (Arab RTL + Latin italic + terjemahan + pill
  "Dibaca 1x" + kartu fawaid + sumber hadits). Setelah verifikasi,
  `DoaRetrofitClient.BASE_URL` dikembalikan ke `https://alkaukaba.com/`.

### 7. Known issues & TODOs

- **Backend belum di-deploy ke VPS produksi** — endpoint `/api/doa-categories*`
  baru ada di working tree lokal `alkaukabawebserver` (migrasi + seeder +
  route sudah jalan & diverifikasi lokal), belum di-push & migrate di
  `alkaukaba.com`. Sampai itu dilakukan, fitur ini akan gagal fetch
  (`tvEmptyState` tampil) di build produksi/release APK.
- Belum ada mode offline — beda dari Al-Qur'an yang juga streaming API,
  tapi konten doa/dzikir ini kecil (97 item, hemat data) sehingga kandidat
  kuat untuk local DB (Room) kalau mau didukung offline penuh nanti.
- Belum ada fitur counter/tasbih interaktif (tap untuk menghitung bacaan
  sesuai `notes`, mis. "Dibaca 1x"/"33x") — saat ini `notes` cuma
  ditampilkan sebagai teks, bukan counter fungsional.
- Baru 5 kategori (subset Hisnul Muslim yang paling umum dipakai), bukan
  seluruh koleksi asli — nambah kategori baru berarti nambah data di
  `DoaSeeder::CATEGORIES` + fixture JSON baru, model/controller/Android
  tidak perlu berubah.
- Field `order` di `DoaItem`/`DoaCategory` diisi otomatis dari urutan API
  sumber (fitrahive/dua-dhikr) saat seeding, bukan hasil kurasi manual — belum
  ada UI admin untuk reorder.
