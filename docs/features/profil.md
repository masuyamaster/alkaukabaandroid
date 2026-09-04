# Profil

## 1. Ringkasan (Overview)

**Nama fitur**: Profil (Akun)

**Deskripsi singkat**: Halaman pusat kelola akun — lihat/ubah nama pengguna,
bantuan & informasi (Hubungi Kami, Kebijakan Privasi), dan tindakan
keamanan akun (ubah kata sandi, keluar, hapus akun). Dibangun 2026-08-30
sekaligus dengan upgrade backend `alkaukabaweb` dari "tanpa
token/session sama sekali" ke Laravel Sanctum bearer token, karena tiga
aksi di sini (ubah nama, ubah password, hapus akun) butuh cara
mengidentifikasi user yang sedang login ke server — sebelumnya tidak ada
mekanisme itu sama sekali (lihat [features/autentikasi.md](autentikasi.md)
section 7 untuk histori bug-nya).

**Per 2026-09-04**: ditambah upload foto profil (ambil foto dari kamera,
pilih dari galeri, atau hapus foto) — sebelumnya avatar cuma ikon
placeholder statis (`ic_person`) yang tidak pernah bisa diganti sama
sekali. Foto yang sama juga tampil di ikon profil header `MainActivity`,
bukan cuma di halaman Profil.

## 2. Entry point & prasyarat

- Dipicu dari `btnProfile` (avatar bulat di header `MainActivity`, ujung
  kanan, di sebelah ikon Konfigurasi) → `ProfileActivity`.
- Prasyarat sama seperti fitur Autentikasi: backend `alkaukabaweb` harus
  hidup (lihat [features/autentikasi.md](autentikasi.md) section 2 untuk
  detail `BASE_URL`).
- **Prasyarat tambahan khusus fitur ini**: tiga aksi (Edit Profil, Ubah
  Kata Sandi, Hapus Akun) butuh `SessionManager.getAuthToken()` terisi.
  User yang login SEBELUM 2026-08-30 (sebelum `LoginActivity` menyimpan
  token) tidak akan punya token tersimpan sampai logout+login ulang sekali
  — `ProfileActivity.requireBearerToken()` menangani ini dengan toast
  "Sesi belum lengkap, silakan logout lalu login ulang dulu" alih-alih
  crash/diam-diam gagal.
- **Prasyarat khusus upload foto profil (2026-09-04)**:
  - Permission `CAMERA` (dideklarasikan di manifest, diminta runtime lewat
    `ActivityResultContracts.RequestPermission` hanya saat user pilih
    "Ambil Foto" — bukan diminta di awal/`onCreate`).
  - `FileProvider` (`${applicationId}.fileprovider`, konfigurasi di
    `res/xml/file_paths.xml`) untuk kasih akses `content://` ke file hasil
    jepretan kamera ke aplikasi Kamera eksternal.
  - "Pilih dari Galeri" tidak butuh permission apa pun — pakai Android
    Photo Picker (`ActivityResultContracts.PickVisualMedia`), bukan
    `ACTION_GET_CONTENT`/`READ_MEDIA_IMAGES`.
  - Backend: `php artisan storage:link` wajib sudah pernah dijalankan di
    environment yang dipakai (lokal maupun VPS produksi) supaya symlink
    `public/storage` ada — tanpa ini, upload akan sukses (file tersimpan)
    tapi `avatar_url` yang dikembalikan 404 saat diakses. Ini fitur
    upload-file PERTAMA di app, jadi kemungkinan besar symlink ini belum
    pernah dibuat sebelumnya di VPS produksi — lihat `alkaukabaweb/CLAUDE.md`
    untuk runbook deploy lengkapnya.

## 3. Titik masuk logika & navigasi

- `ProfileActivity.renderAccountInfo()` — isi nama/email dari
  `SessionManager.getUserName()`/`getEmail()`, fallback "Pengguna"/"-"
  kalau belum ada data (mis. akun lama sebelum fix persist username/email).
- `ProfileActivity.requireBearerToken()` — titik-ekstensi kalau nanti mau
  nambah aksi terproteksi baru: kembalikan `"Bearer $token"` siap pakai
  atau `null` (sambil toast) kalau belum ada token.
- Empat bottom sheet (`BottomSheetDialog`), masing-masing dengan layout XML
  sendiri:
  - `showEditProfileSheet()` (`dialog_edit_profile.xml`) → panggil
    `AuthApiService.updateProfile()`, cuma ubah `username` (nama tampilan
    di `users.name`/`users.username` backend) — **email sengaja tidak bisa
    diubah di sini**, butuh alur verifikasi ulang yang belum ada.
  - `showChangePasswordSheet()` (`dialog_change_password.xml`) → validasi
    client-side (`new_password` min 6 karakter, `confirm` harus sama)
    sebelum panggil `AuthApiService.changePassword()`. Sukses → server
    menerbitkan token baru & mencabut yang lama, token baru langsung
    disimpan ulang lewat `SessionManager.setAuthToken()` supaya user tidak
    perlu login ulang manual walau tokennya sudah berganti.
  - `showDeleteAccountSheet()` (`dialog_delete_account.xml`, dipicu setelah
    `showDeleteAccountConfirmation()` — dialog konfirmasi dulu sebelum
    sheet minta password) → `AuthApiService.deleteAccount()`, sukses →
    `performLogout()` (akun sudah tidak ada di server, jadi pasti logout
    juga di client).
  - `showInfoDialog()` (`dialog_info_simple.xml`, dipakai ulang untuk
    "Hubungi Kami" & "Kebijakan Privasi") — **isinya masih teks placeholder
    hardcode di `ProfileActivity.kt`** (lihat section 7), belum ada
    kontak/kebijakan resmi dari pemilik project.
- `showLogoutConfirmation()` → `performLogout()` → `sessionManager.clearUserData()`
  → `LoginActivity` (pola identik dengan `KonfigurasiActivity`, lihat
  [features/autentikasi.md](autentikasi.md)).
- Navigasi: semua lewat `Intent` biasa / `BottomSheetDialog`, tidak ada
  Activity turunan baru selain `ProfileActivity` sendiri.
- **Upload foto profil (2026-09-04)** — dipicu dari `ivAvatar` ATAU
  `ivAvatarBadge` di halaman utama, dan dari `ivAvatarSheet`/`ivAvatarBadgeSheet`
  di dalam sheet Edit Profil (buka action sheet baru di atas sheet Edit
  Profil yang sedang terbuka, tidak menutupnya):
  - `showAvatarActionSheet()` (`dialog_avatar_action.xml`) — 3 baris
    (Ambil Foto/Pilih dari Galeri/Hapus Foto), baris "Hapus Foto"
    disembunyikan (`visibility = GONE`) kalau `sessionManager.getAvatarUrl()`
    masih null.
  - "Ambil Foto" → `requestCameraAndCapture()` (cek/minta permission
    `CAMERA`) → `launchCamera()` (`ActivityResultContracts.TakePicture()`,
    tulis ke file di `cacheDir/images/` lewat `FileProvider`).
  - "Pilih dari Galeri" → `pickImageLauncher`
    (`ActivityResultContracts.PickVisualMedia()`, `ImageOnly`).
  - Kedua jalur berujung ke `handlePickedImage(uri)` → `Dispatchers.IO`:
    `ImageUtils.prepareAvatarFile()` (EXIF-rotate, center-crop persegi,
    downscale ~800px, compress JPEG) → `uploadAvatarFile(file)`.
  - `uploadAvatarFile()` → tampilkan `avatarUploadScrim` (overlay spinner
    di atas avatar) → `AuthApiService.uploadAvatar()` (multipart, field
    `photo`) → sukses: `sessionManager.setAvatarUrl()` +
    `renderAccountInfo()` + refresh avatar di sheet Edit Profil kalau
    sedang terbuka (`editSheetAvatarView`/`editSheetScrim`, dilacak selama
    sheet itu hidup lewat `setOnDismissListener`).
  - "Hapus Foto" → `confirmRemoveAvatar()` (dialog konfirmasi) →
    `removeAvatar()` → `AuthApiService.deleteAvatar()`.
  - `errorMessageOf(response, fallback)` — **gotcha Retrofit yang perlu
    diingat**: `response.body()` SELALU `null` untuk HTTP non-2xx (Retrofit
    cuma mem-parse body kalau sukses), jadi pesan error asli dari server
    harus dibaca manual dari `response.errorBody()`. Semua sheet lain di
    file ini (Edit Profil, Ubah Kata Sandi, Hapus Akun) masih pakai pola
    lama `body?.message ?: "<fallback generik>"` yang kena bug yang sama
    (selalu nampilkan fallback generik, bukan pesan asli, untuk request
    gagal) — belum diperbaiki di luar dua endpoint foto profil ini, lihat
    section 7.
  - `ImageUtils.loadAvatarInto(context, imageView, avatarUrl, paddingDp)` —
    helper bersama (dipakai juga oleh `MainActivity`, lihat di bawah) yang
    menampilkan foto (Glide, `circleCrop()`) atau balik ke placeholder
    ikon gold+navy kalau `avatarUrl` null. `imageTintList` WAJIB
    dibersihkan sebelum load foto asli — kalau tidak, foto ikut ke-tint
    navy seperti ikon placeholder-nya (bug yang sempat kejadian saat
    development).
- **Ikon profil di header `MainActivity` (`btnProfile`)** — sebelumnya
  cuma ikon placeholder statis, sekarang ikut menampilkan foto profil
  lewat `ImageUtils.loadAvatarInto()` yang sama. Direfresh di
  `MainActivity.onResume()` (bukan cuma `onCreate()`), karena
  `MainActivity` tidak di-recreate saat user balik dari `ProfileActivity`
  lewat tombol back — tanpa `onResume()`, ikon header tidak akan
  ter-update sampai app dibuka ulang dari awal.

## 4. Struktur & alur data

| File | Peran |
|---|---|
| `ui/profile/ProfileActivity.kt` | Satu-satunya Activity fitur ini — render info akun + 6 aksi (Edit Profil, Hubungi Kami, Kebijakan Privasi, Ubah Kata Sandi, Keluar, Hapus Akun) |
| `res/layout/activity_profile.xml` | Layar utama: kartu info akun (avatar placeholder + nama + email + tombol Edit Profil), section "Bantuan & Informasi", section "Keamanan & Akun" |
| `res/layout/dialog_edit_profile.xml` | Bottom sheet ubah nama |
| `res/layout/dialog_change_password.xml` | Bottom sheet ubah password (3 field: current/new/confirm) |
| `res/layout/dialog_delete_account.xml` | Bottom sheet konfirmasi hapus akun (field password) |
| `res/layout/dialog_info_simple.xml` | Bottom sheet generik judul+body, dipakai ulang utk Hubungi Kami & Kebijakan Privasi |
| `res/drawable/ic_person.xml` | Icon avatar placeholder — dipakai `ImageUtils.loadAvatarInto()` sebagai fallback kalau user belum/tidak punya foto |
| `api/AuthApiService.kt` | `updateProfile()`, `changePassword()`, `deleteAccount()`, dan (2026-09-04) `uploadAvatar()` (`@Multipart`, field `photo`), `deleteAvatar()` — semua butuh `@Header("Authorization")` bearer token, beda dari `login()`/`register()`/`googleLogin()` yang publik |
| `model/AuthModels.kt` | `UpdateProfileRequest`, `ChangePasswordRequest`, `DeleteAccountRequest`; `UserData` nambah field `token: String?` dan (2026-09-04) `avatar_url: String?` — otomatis ikut di SEMUA response (login/register/google_login/update_profile juga), bukan cuma upload |
| `utils/SessionManager.kt` | `getUserId()`/`setUserId()`, `getAuthToken()`/`setAuthToken()`, `clearUserData()` (hapus semua state user sekaligus), dan (2026-09-04) `getAvatarUrl()`/`setAvatarUrl()` |
| `utils/ImageUtils.kt` (baru, 2026-09-04) | `prepareAvatarFile()` — proses foto sebelum upload (EXIF-rotate, center-crop persegi, downscale ~800px, compress JPEG q85, decode pakai `inSampleSize` biar tidak OOM untuk foto kamera resolusi tinggi). `loadAvatarInto()` — helper tampilan avatar bersama, dipakai `ProfileActivity` & `MainActivity` |
| `res/layout/dialog_avatar_action.xml` (baru) | Bottom sheet 3 pilihan: Ambil Foto/Pilih dari Galeri/Hapus Foto |
| `res/xml/file_paths.xml` (baru) | Konfigurasi `FileProvider` — satu `cache-path` untuk file hasil jepretan kamera |
| `MainActivity.kt` | `renderProfileAvatar()` (dipanggil dari `onResume()`) — isi `btnProfile` di header lewat `ImageUtils.loadAvatarInto()` |

Per 2026-08-30: row `rowLogout` di `activity_profile.xml` diganti dari
`LinearLayout` teks polos ("Keluar", tanpa border) jadi
`AppCompatButton` pill outline merah (`bg_btn_outline_danger` + ikon
`ic_logout`, keduanya awalnya dibuat untuk Konfigurasi — lihat
`docs/features/konfigurasi.md`) supaya jelas ini destructive action tapi
tetap secondary (bukan tombol solid utama). Logout dipindah ke sini karena
sebelumnya ada dua tombol Logout terpisah (Konfigurasi & Profil) yang
redundan — Konfigurasi sekarang tidak punya logout sama sekali.
`ProfileActivity.kt` tidak berubah (id `rowLogout` dipertahankan, listener
lama tetap jalan di atas view baru). Sempat ada bug visual: tanpa
`paddingHorizontal`, ikon logout terlalu dekat dengan lengkungan border
pill hingga terlihat "nabrak" — diperbaiki dengan `paddingHorizontal="24dp"`.

Per 2026-08-30 (redesign lanjutan, menyeragamkan dengan Konfigurasi):
- **Hero info akun** (avatar/nama/email) dikeluarkan dari `CardView` besar
  — sekarang `LinearLayout` polos yang menyatu dengan background halaman
  (center-aligned, tanpa kotak putih). Avatar (`bg_circle_button` +
  `ic_person`) warna diganti dari `accent_yellow` ke `navy_dongker`.
  `btnEditProfile` diganti dari outline abu-abu (`bg_input_outline`) jadi
  pill kapsul latar biru muda (`bg_pill_navy_light`, drawable baru,
  reuse `@color/hero_card_bg`) + teks navy, dengan `stateListAnimator=null`
  supaya flat (tanpa shadow bawaan `Widget.MaterialComponents.Button`).
- **`rowHelp` + `rowPrivacy` digabung jadi satu card** (sebelumnya dua
  card putih terpisah) dengan satu `View` divider 1dp (`divider_soft`,
  inset 52dp mengikuti lebar ikon) di antaranya. Ditambah ikon di kiri tiap
  baris: `ic_menu_support` (headset, baru) untuk Hubungi Kami, dan
  `ic_menu_document` (baru) untuk Kebijakan Privasi.
- **`rowChangePassword`** dapat ikon `ic_menu_lock` (baru).
- Label kategori (BANTUAN & INFORMASI / KEAMANAN & AKUN) warna
  `text_secondary` → `navy_dongker`, menyamakan konvensi label kategori di
  `docs/features/konfigurasi.md`.
- **`rowLogout` dibuat flat**: `Theme.AlKaukaba` berbasis
  `Theme.MaterialComponents`, yang otomatis memberi `AppCompatButton`
  bawaan `stateListAnimator` (elevation/shadow) lewat `buttonStyle` tema —
  ini sumber drop shadow yang terlihat di review visual sebelumnya.
  Ditambah `android:stateListAnimator="@null"` + `android:elevation="0dp"`
  untuk menghilangkannya, konsisten dengan `btnEditProfile` di atas.
- **`rowDeleteAccount`** (sebelumnya card pink besar) diturunkan jadi
  `TextView` text-link kecil (13sp, `?attr/selectableItemBackground`,
  tanpa card/box) di bawah tombol Logout dengan jarak 20dp — sengaja dibuat
  tidak semenonjol Logout karena ini aksi permanen yang sangat jarang
  dipakai (mencegah salah pencet). `ProfileActivity.kt` tidak berubah sama
  sekali di seluruh redesign ini — semua id (`rowHelp`, `rowPrivacy`,
  `rowChangePassword`, `rowLogout`, `rowDeleteAccount`) dipertahankan,
  hanya tipe View & isi visualnya yang berubah.

Per 2026-08-30 (polish lanjutan): dua revisi dari feedback visual:
1. **`rowLogout` teks tidak benar-benar center** — `AppCompatButton` dengan
   `drawableStart` + `gravity="center"` men-center gabungan ikon+teks
   sebagai satu blok, bukan teks itu sendiri terhadap lebar tombol, jadi
   teks terlihat sedikit bergeser kanan. Diganti jadi `FrameLayout` (id
   `rowLogout` dipertahankan): `TextView` `match_parent` dengan
   `gravity="center"` (teks benar-benar center 100% lebar tombol) +
   `ImageView` ikon `layout_gravity="start|center_vertical"` mengambang
   independen di kiri (`marginStart=24dp`) — tidak memengaruhi posisi teks
   sama sekali. `clipToOutline="true"` ditambah supaya ripple
   (`?attr/selectableItemBackground` sebagai `android:foreground`) tidak
   tumpah keluar sudut membulat pill.
2. **Ikon `✏️`/`🗑️` di `btnEditProfile`/`rowDeleteAccount` masih emoji**,
   tidak konsisten dengan ikon vektor flat di menu lain. Diganti jadi
   vektor baru: `ic_edit.xml` (fillColor `navy_dongker` di-bake langsung ke
   path — dipakai lewat `drawableStart` pada `Button`, bukan `ImageView`
   terpisah, jadi tidak bisa pakai `app:tint` compat) untuk Edit Profil, dan
   `ic_delete.xml` (fillColor putih + `app:tint="@color/pill_red_text"` di
   `ImageView` terpisah, karena `rowDeleteAccount` sekarang `LinearLayout`
   horizontal ikon+teks, bukan `TextView` tunggal) untuk Hapus Akun.

Alur data (contoh Ubah Kata Sandi, pola sama untuk Edit Profil & Hapus
Akun): `ProfileActivity` (validasi client-side) → `requireBearerToken()` →
coroutine IO → `AuthApiService.changePassword(bearer, request)` → backend
`alkaukabaweb` (`api.php?action=change_password`, lihat `CLAUDE.md` repo
itu) → cek token via Sanctum → `Hash::check` password lama → update +
terbitkan token baru & cabut token lama → `Response<ApiResponse>` → balik
`Dispatchers.Main` → simpan token baru, toast, dismiss sheet.

Alur data upload foto profil (2026-09-04): `ProfileActivity` (pilih
sumber lewat `showAvatarActionSheet()`) → `Uri` dari kamera/galeri →
`Dispatchers.IO`: `ImageUtils.prepareAvatarFile()` (proses jadi JPEG
persegi ~800px di `cacheDir`) → `uploadAvatarFile()` →
`AuthApiService.uploadAvatar(bearer, MultipartBody.Part)` → backend
`alkaukabaweb` (`api.php?action=upload_avatar`) → hapus file avatar lama
kalau ada (`Storage::disk('public')->delete()`) → simpan file baru
(`avatars/{userId}-{timestamp}.{ext}`, nama unik per-timestamp supaya
tidak kena cache lama) → update kolom `avatar_path` → response berisi
`avatar_url` terhitung (`Storage::disk('public')->url()`) →
`Dispatchers.Main`: `sessionManager.setAvatarUrl()` →
`renderAccountInfo()` (Glide muat ulang di `ProfileActivity`) — ikon
header `MainActivity` ikut ter-update begitu user balik ke situ lewat
`onResume()`.

Per 2026-09-04 (upload foto profil — lihat section 1 & 3 untuk detail
fitur, ditulis di sini untuk histori keputusan desain):
- **Backend baru dibangun dari nol untuk fitur ini** — sebelum sesi ini,
  hanya endpoint auth (`register`/`login`/`google_login`/`update_profile`/
  `change_password`/`delete_account`) yang ada di `AuthController`. Dua
  action baru (`upload_avatar`, `delete_avatar`) ditambah mengikuti
  konvensi kode yang sama persis (`Validator::make()` manual, bentuk
  response `status`/`message`/`data` konsisten, `userResponse()` yang
  sudah ada tinggal ditambah field `avatar_url`) — lihat
  `alkaukabaweb/README.md` bagian "Konvensi kode".
- **Crop interaktif (pan/pinch) sengaja TIDAK dibangun**, walau ada di
  mockup desain awal (`/design`) — diganti auto center-crop persegi +
  downscale otomatis di `ImageUtils` supaya scope implementasi tetap
  kecil dan bisa langsung dites di device sungguhan hari itu juga.
  Kalau nanti mau ditambah, itu jadi layar baru terpisah sebelum
  `uploadAvatarFile()` dipanggil, bukan mengubah `ImageUtils` yang ada.
- **Avatar hero di halaman utama diperbesar 2x** (80dp → 160dp, badge
  kamera & scrim loading ikut diskalakan proporsional) menyusul feedback
  langsung setelah fitur ini pertama kali dites di device — avatar di
  sheet Edit Profil TIDAK ikut diperbesar (tetap 80dp), sengaja dibiarkan
  beda karena ruang di bottom sheet lebih terbatas dan permintaannya
  spesifik "halaman profile", bukan semua tempat avatar muncul.
- **Ditemukan & diperbaiki dari testing manual di device fisik** (bukan
  dari review kode/curl) — dua bug yang sempat lolos dari verifikasi
  awal, lihat section 6 untuk detail root cause: `ImageUtils.prepareAvatarFile()`
  yang selalu gagal, dan toast error upload/hapus foto yang selalu
  generik.

## 5. Dependencies & tech stack khusus

- Tidak ada tambahan khusus di luar stack umum app (`BottomSheetDialog`
  dari Material Components, Retrofit — semua sudah dipakai fitur
  Konfigurasi & Autentikasi sebelumnya).
- Backend: Laravel Sanctum (`laravel/sanctum` di `alkaukabaweb`) — sudah
  lama jadi dependency tapi baru diaktifkan 2026-08-30 lewat fitur ini.
  Detail endpoint & keputusan desain backend (kenapa auth manual via
  `PersonalAccessToken::findToken()` bukan middleware `auth:sanctum`) ada
  di `CLAUDE.md` repo `alkaukabaweb`.
- **Upload foto profil (2026-09-04)** nambah dependency baru:
  - `androidx.activity:activity-ktx:1.7.2` — ditambah eksplisit di
    `app/build.gradle` supaya `ActivityResultContracts.PickVisualMedia`
    (Photo Picker) pasti tersedia, tidak bergantung versi transitif dari
    `appcompat`.
  - `androidx.exifinterface:exifinterface:1.3.6` — baca orientasi EXIF
    supaya foto dari kamera tidak muncul miring setelah di-crop.
  - `com.github.bumptech.glide:glide:4.16.0` — **sudah lama jadi
    dependency tapi baru benar-benar dipakai mulai fitur ini** (sebelum
    2026-09-04, `Glide` ter-declare di `build.gradle` tapi tidak ada satu
    pun `Glide.with(...)` di kode).
  - **Gotcha okhttp**: `MultipartBody`/`RequestBody` untuk upload dibuat
    pakai API Java lama (`RequestBody.create(MediaType.parse(...), file)`),
    BUKAN extension function Kotlin (`file.asRequestBody(...)`,
    `"...".toMediaType()`) — karena Retrofit `2.9.0` resolve ke
    `com.squareup.okhttp3:okhttp:3.14.9` (Java, bukan Kotlin, tidak ada
    `Companion`/extension function), bukan versi 4.x yang lebih baru.
    Kalau nanti upgrade Retrofit/paksa versi okhttp lebih baru lewat
    `resolutionStrategy`, kode ini bisa disederhanakan pakai extension
    function.
  - Backend: `Illuminate\Support\Facades\Storage` (disk `public`, sudah
    bawaan Laravel, bukan dependency baru) — tapi ini pemakaian PERTAMA
    fitur file storage di `alkaukabaweb`, jadi `php artisan storage:link`
    perlu dijalankan manual sekali (lihat section 2).

## 6. Testing

Belum ada test otomatis (sama seperti fitur Autentikasi — lihat section 6
di [features/autentikasi.md](autentikasi.md)).

Verifikasi manual yang sudah dilakukan (2026-08-30):

1. Backend: 6 endpoint (`register`, `login`, `update_profile`,
   `change_password` — termasuk skenario password lama salah, dan
   `delete_account` — termasuk skenario password salah) dites lewat `curl`
   langsung, baik di lokal (`php artisan serve`) maupun setelah deploy ke
   produksi (`https://alkaukaba.com/`). Termasuk verifikasi token lama
   benar-benar tercabut setelah `change_password`/`delete_account`.
2. Android: build `assembleDebug` + install ke **emulator** (bukan device
   fisik) via `adb install -r`, login dengan akun baru, buka halaman
   Profil, konfirmasi nama/email tampil benar (bukan placeholder).

Verifikasi manual upload foto profil (2026-09-04 s.d. 2026-09-05):

1. Backend: `upload_avatar` & `delete_avatar` dites lewat `curl` (bukan
   cuma test-nya berhasil, tapi juga file benar-benar bisa diakses lewat
   `curl -I` ke `avatar_url` yang dikembalikan) — di lokal (`php artisan
   serve` + `adb reverse tcp:8000 tcp:8000`) DAN setelah deploy ke
   produksi (`https://alkaukaba.com/`). Akun test dibersihkan lagi
   (`delete_account`) setelah verifikasi produksi selesai.
2. Android: build `installDebug` ke **device fisik** yang tersambung ke
   PC (beda dari verifikasi Autentikasi sebelumnya yang pakai emulator).
   UI (avatar+badge kamera state kosong/terisi, action sheet yang
   menyembunyikan "Hapus Foto" saat belum ada foto, avatar di sheet Edit
   Profil) dikonfirmasi lewat screenshot `adb screencap`, cocok dengan
   mockup desain yang disetujui sebelumnya.
3. **Batas otomasi**: mencoba drive interaksi penuh (pilih foto asli dari
   galeri) lewat `adb shell input tap` koordinat blind terbukti tidak
   reliable — kombinasi system Photo Picker (activity terpisah, layout
   tidak bisa diprediksi) + beberapa `BottomSheetDialog` yang bisa
   menumpuk (avatar action sheet dibuka dari dalam sheet Edit Profil)
   bikin tap coordinate meleset ke aplikasi lain sama sekali (Instagram,
   pencarian home screen). Sesuai catatan di `CLAUDE.md` root ("Batasi
   percobaan verifikasi UI via adb"), percobaan tap-and-screenshot
   dihentikan setelah gagal berulang, bukan diteruskan trial-and-error.
   **Verifikasi akhir end-to-end (pilih foto sungguhan → upload → tampil
   di kedua layar) dilakukan manual oleh user di device fisiknya
   sendiri**, bukan otomatis lewat `adb`.
4. Dua bug ditemukan justru dari verifikasi manual di poin 3 (bukan dari
   review kode atau test `curl`, yang keduanya tidak menyentuh jalur ini):
   - `ImageUtils.prepareAvatarFile()` **selalu** return `null` (toast
     "Gagal memproses foto" di SETIAP percobaan) — `BitmapFactory.decodeStream()`
     dengan `inJustDecodeBounds = true` memang selalu return `null` by
     design (cuma ngisi `bounds.outWidth`/`outHeight`, bukan alokasi
     `Bitmap`), tapi kode awal salah menganggap `null` itu sebagai tanda
     gagal lewat operator Elvis (`?: return null`). Fix: cek
     `bounds.outWidth`/`outHeight` langsung setelah decode, bukan lewat
     return value `decodeStream`.
   - Toast error upload/hapus foto selalu pesan generik ("Gagal
     mengunggah foto") walau backend membalas pesan spesifik — lihat
     gotcha `errorMessageOf()` di section 3.

## 7. Known issues & TODOs

- [ ] **"Hubungi Kami" & "Kebijakan Privasi & Syarat Ketentuan" masih
      placeholder** — teks generik hardcode di
      `ProfileActivity.showHelpDialog()`/`showPrivacyDialog()`. Belum ada
      kontak resmi (email/WhatsApp) atau draft kebijakan privasi final dari
      pemilik project. Ganti isi teksnya begitu sudah ada.
- [x] ~~Foto profil belum bisa diupload~~ — **Diselesaikan 2026-09-04.**
      Backend `alkaukabaweb` dapat dua action baru (`upload_avatar`,
      `delete_avatar`), storage pakai disk `public` Laravel di VPS yang
      sama (bukan S3-compatible — cukup untuk skala app ini sekarang).
      Lihat section 1-4 untuk detail lengkap. TODO baru yang muncul dari
      fitur ini ada di bawah.
- [ ] **Tidak ada crop interaktif (pan/pinch/zoom)** — sengaja
      disederhanakan jadi auto center-crop persegi + downscale otomatis
      (lihat section 4) demi kecepatan implementasi. Kalau user butuh
      kontrol lebih (mis. foto dengan wajah di pinggir ke-crop salah),
      ini kandidat pertama untuk ditambah.
- [ ] **`AuthController::deleteAccount()` (backend) tidak menghapus file
      avatar dari storage sebelum baris user dihapus** — beda dengan
      `deleteAvatar()`/upload-ulang yang sudah benar (`Storage::disk('public')->delete()`
      dipanggil dulu). Efeknya: kalau user hapus akun sambil masih punya
      foto profil, file JPEG-nya jadi yatim (orphan) di
      `storage/app/public/avatars/` selamanya — tidak bahaya, tapi lama-lama
      makan disk VPS. Ditemukan saat verifikasi manual, belum diperbaiki
      (prioritas rendah, jarang terjadi).
- [ ] **Pola `body?.message ?: "<fallback generik>"` di sheet lain**
      (Edit Profil, Ubah Kata Sandi, Hapus Akun) belum ikut diperbaiki
      dengan `errorMessageOf()` — masih kena bug yang sama dengan yang
      diperbaiki di upload/hapus foto (lihat section 3 & 6): pesan error
      dari server untuk request GAGAL tidak pernah muncul, selalu jatuh
      ke fallback generik, karena `Response.body()` Retrofit selalu
      `null` untuk HTTP non-2xx.
- [ ] **Tidak ada validasi ukuran file eksplisit di sisi Android**
      sebelum upload — backend validasi maksimal 4MB, dan
      `ImageUtils.prepareAvatarFile()` selalu men-downscale ke ~800px
      (praktis selalu jauh di bawah 4MB), tapi tidak ada guard/pesan
      error khusus kalau suatu saat itu berubah.
- [ ] **Email tidak bisa diubah dari halaman ini** — keputusan desain
      sengaja (lihat section 3), karena ganti email biasanya butuh alur
      verifikasi ulang yang belum dibangun. Kalau nanti dibutuhkan, jangan
      cuma tambah field di `dialog_edit_profile.xml` tanpa alur verifikasi.
- [ ] **Satu token aktif per user, bukan per device** — `AuthController::userResponse()`
      di backend memanggil `$user->tokens()->delete()` sebelum menerbitkan
      token baru di SETIAP login/register/google_login. Artinya kalau user
      login di dua device, device pertama otomatis ke-logout (tokennya
      tercabut) begitu login di device kedua. Cukup untuk scope app ini
      sekarang (single-session), tapi bukan perilaku multi-device yang
      umum di app lain — perlu didesain ulang (token per device, bukan
      per user) kalau ke depan multi-device jadi kebutuhan nyata.
