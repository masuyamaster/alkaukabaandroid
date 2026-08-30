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

## 4. Struktur & alur data

| File | Peran |
|---|---|
| `ui/profile/ProfileActivity.kt` | Satu-satunya Activity fitur ini — render info akun + 6 aksi (Edit Profil, Hubungi Kami, Kebijakan Privasi, Ubah Kata Sandi, Keluar, Hapus Akun) |
| `res/layout/activity_profile.xml` | Layar utama: kartu info akun (avatar placeholder + nama + email + tombol Edit Profil), section "Bantuan & Informasi", section "Keamanan & Akun" |
| `res/layout/dialog_edit_profile.xml` | Bottom sheet ubah nama |
| `res/layout/dialog_change_password.xml` | Bottom sheet ubah password (3 field: current/new/confirm) |
| `res/layout/dialog_delete_account.xml` | Bottom sheet konfirmasi hapus akun (field password) |
| `res/layout/dialog_info_simple.xml` | Bottom sheet generik judul+body, dipakai ulang utk Hubungi Kami & Kebijakan Privasi |
| `res/drawable/ic_person.xml` | Icon avatar placeholder (dipakai juga di header `MainActivity`) |
| `api/AuthApiService.kt` | `updateProfile()`, `changePassword()`, `deleteAccount()` — semua butuh `@Header("Authorization")` bearer token, beda dari `login()`/`register()`/`googleLogin()` yang publik |
| `model/AuthModels.kt` | `UpdateProfileRequest`, `ChangePasswordRequest`, `DeleteAccountRequest`; `UserData` nambah field `token: String?` |
| `utils/SessionManager.kt` | `getUserId()`/`setUserId()`, `getAuthToken()`/`setAuthToken()`, `clearUserData()` (hapus semua state user sekaligus) |

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

## 5. Dependencies & tech stack khusus

- Tidak ada tambahan khusus di luar stack umum app (`BottomSheetDialog`
  dari Material Components, Retrofit — semua sudah dipakai fitur
  Konfigurasi & Autentikasi sebelumnya).
- Backend: Laravel Sanctum (`laravel/sanctum` di `alkaukabaweb`) — sudah
  lama jadi dependency tapi baru diaktifkan 2026-08-30 lewat fitur ini.
  Detail endpoint & keputusan desain backend (kenapa auth manual via
  `PersonalAccessToken::findToken()` bukan middleware `auth:sanctum`) ada
  di `CLAUDE.md` repo `alkaukabaweb`.

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

## 7. Known issues & TODOs

- [ ] **"Hubungi Kami" & "Kebijakan Privasi & Syarat Ketentuan" masih
      placeholder** — teks generik hardcode di
      `ProfileActivity.showHelpDialog()`/`showPrivacyDialog()`. Belum ada
      kontak resmi (email/WhatsApp) atau draft kebijakan privasi final dari
      pemilik project. Ganti isi teksnya begitu sudah ada.
- [ ] **Foto profil belum bisa diupload** — avatar di kartu info akun
      cuma icon placeholder (`ic_person`), bukan foto asli. Backend tidak
      punya endpoint upload file/storage untuk ini; kalau mau dibangun,
      butuh keputusan dulu soal storage (lokal VPS vs S3-compatible) dan
      validasi ukuran/tipe file.
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
