# Autentikasi (Login/Register)

## 1. Ringkasan

**Fitur**: Autentikasi — register, login (email/password), dan login via Google.

User butuh akun untuk masuk ke app (tidak ada mode "guest/tanpa akun").
Fitur ini menghubungkan app ke backend terpisah `alkaukabaweb` (Laravel) untuk
membuat akun baru, verifikasi kredensial, dan verifikasi token Google
Sign-In, lalu menyimpan status login secara lokal supaya user tidak perlu
login ulang setiap buka app.

## 2. Entry point & prasyarat

- `Splashscreen` adalah launcher activity (`android.intent.action.MAIN` +
  `LAUNCHER` di `AndroidManifest.xml`). Setelah delay 3 detik
  (`Handler.postDelayed`), dia cek `SessionManager.isLoggedIn()` dan redirect
  ke `MainActivity` (sudah login) atau `LoginActivity` (belum login).
- `LoginActivity` menampung form login, form register, dan tombol Google
  Sign-In sekaligus (lihat detail toggle di section 3).
- **Prasyarat wajib: backend `alkaukabaweb` harus hidup**, karena `AuthClient`
  memanggil `api.php?action=login|register|google_login` ke sana — bukan ke
  Aladhan API yang dipakai fitur Waktu Sholat. Tanpa backend ini, register/
  login/Google login semua akan gagal dengan error koneksi/HTTP.
  - Default `BASE_URL` sekarang: `https://alkaukaba.com/` (produksi, sudah live).
  - Untuk develop backend bareng secara lokal: ganti `BASE_URL` ke
    `http://127.0.0.1:8000/`, jalankan `php artisan serve` di project
    `alkaukabaweb`, dan `adb reverse tcp:8000 tcp:8000`.
  - Detail lengkap (kenapa perlu `adb reverse`, exception cleartext di
    `network_security_config.xml`, catatan balikin ke produksi sebelum
    build rilis) ada di section "Konfigurasi API backend (AuthClient
    BASE_URL)" pada `CLAUDE.md` root — jangan duplikat di sini, rujuk ke sana.
- Prasyarat lain: permission `INTERNET` (sudah ada di manifest app-wide,
  bukan spesifik fitur ini). Google Sign-In butuh Web Client ID yang
  di-hardcode di `LoginActivity.kt` (lihat catatan security di section 7).
- **Aset logo**: `ivLogo` di `LoginActivity` pakai
  `@drawable/icon_al_kaukaba_black` (teks hitam, kontras di atas
  `color_secondary` abu-kebiruan muda), sedangkan `Splashscreen` pakai
  `@drawable/icon_al_kaukaba_white` (teks putih, kontras di atas
  `color_primary` navy gelap) — dua varian warna dari logo yang sama, dipilih
  sesuai background masing-masing layar. Per 2026-08-30,
  `icon_al_kaukaba_black.png` di-upgrade dari 126x54px (buram) ke 1003x413px,
  di-generate dari `icon_al_kaukaba_white.png` yang sudah high-res
  (`drawable-xxxhdpi/`) lewat script PowerShell + `System.Drawing`: pixel
  putih/abu-abu (teks & tagline) di-remap ke hitam murni `#000000`
  berdasarkan kemiripan channel RGB, bulan sabit emas (`#E8BA5C`) dan alpha
  antialiasing dipertahankan apa adanya. Kalau butuh varian warna logo baru
  lagi ke depan, pakai pendekatan remap-warna yang sama dari source
  high-res yang sudah ada, daripada minta desain ulang dari nol.

## 3. Titik masuk logika & navigasi

- `LoginActivity` — satu Activity untuk **login DAN register sekaligus**,
  bukan dua Activity terpisah. Toggle antar form dilakukan dengan
  show/hide `View` biasa (`binding.groupLogin.visibility` /
  `binding.groupRegister.visibility`), dipicu oleh `tvRegister` (ke form
  register) dan `tvBackToLogin` (kembali ke form login) — bukan navigasi,
  bukan `ViewPager`/tab, cuma toggle visibility dalam satu layar.
- `LoginActivity.performLogin(email, pass)` — submit form login, panggil
  `AuthClient.instance.login()`.
- `LoginActivity.performRegister(username, email, pass)` — submit form
  register, panggil `AuthClient.instance.register()`. **Setelah register
  sukses, otomatis memanggil `performLogin(email, pass)`** (auto-login
  setelah daftar, bukan redirect balik ke form login).
- `LoginActivity.performGoogleLogin(idToken)` — kirim ID token Google ke
  backend lewat `AuthClient.instance.googleLogin()`.
- Google Sign-In pakai library lama `com.google.android.gms:play-services-auth`
  (`GoogleSignIn`, `GoogleSignInClient`, `GoogleSignInOptions`), **bukan**
  Credential Manager API yang lebih baru. Alurnya:
  `cvGoogle` diklik → `googleSignInClient.signOut()` (paksa munculkan account
  picker, bukan auto pakai akun terakhir) → `googleSignInClient.signInIntent`
  dilempar lewat `registerForActivityResult` (`googleSignInLauncher`) →
  hasil diproses dengan `GoogleSignIn.getSignedInAccountFromIntent(result.data)`
  → ambil `idToken` dari `account` → `performGoogleLogin(idToken)`.
  - Catatan implementasi: `resultCode` dari activity result **sengaja tidak
    dicek** sebelum memproses — komentar di kode menjelaskan GMS suka
    mengembalikan `RESULT_CANCELED` walau penyebabnya bukan user membatalkan
    (mis. `DEVELOPER_ERROR` karena mismatch SHA-1/package name), jadi kode
    error asli cuma bisa didapat dari `ApiException` yang dilempar
    `task.getResult(ApiException::class.java)`.
- Navigasi (semua pakai `Intent` biasa, tanpa Navigation Component):
  - `Splashscreen` → `MainActivity` (kalau sudah login) atau `LoginActivity`
    (kalau belum), tanpa extra.
  - `LoginActivity` → `MainActivity` setelah login/Google login sukses,
    tanpa extra, dengan `finish()` supaya user tidak bisa back ke layar login.
  - `KonfigurasiActivity.performLogout()` → `LoginActivity` dengan flag
    `FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TASK` (bersihkan back
    stack sepenuhnya) setelah `sessionManager.setLogin(false)`, dipicu dari
    tombol `btnLogout` → dialog konfirmasi `showLogoutConfirmation()`.
    **Per 2026-08-30**: diganti jadi `sessionManager.clearUserData()`
    (bukan cuma `setLogin(false)`) supaya token/nama/email lama ikut
    terhapus, bukan cuma flag login. `ProfileActivity` punya jalur logout
    kedua dengan pola identik — lihat [features/profil.md](profil.md).

## 4. Struktur & alur data

File yang terlibat:

| File | Peran |
|---|---|
| `Splashscreen.kt` | Entry point app; cek `SessionManager.isLoggedIn()`, redirect ke `MainActivity`/`LoginActivity` |
| `LoginActivity.kt` | Satu Activity untuk form login, form register, dan tombol Google Sign-In |
| `utils/AuthClient.kt` | Instance Retrofit **terpisah** dari `RetrofitClient`/Aladhan, `baseUrl` ke backend `alkaukabaweb`, tanpa interceptor/auth header tambahan |
| `api/AuthApiService.kt` | Interface Retrofit: `login()`, `register()`, `googleLogin()` — semua POST ke `api.php` dengan query `action=...` (bukan path REST, gaya endpoint PHP lama) |
| `model/AuthModels.kt` | `LoginRequest`, `RegisterRequest` (body request), `ApiResponse` (`status`, `message`, `data: UserData?`), `UserData` (`id`, `username`, `email`) — response envelope seragam untuk login/register |
| `model/GoogleLoginRequest.kt` | Body request khusus Google login, cuma berisi `id_token` |
| `utils/SessionManager.kt` | Persistensi status login di `SharedPreferences` ("AppSession") — `setLogin()`/`isLoggedIn()`, plus `setUserName()`/`setEmail()` (lihat catatan gap di section 7) |
| `ui/konfigurasi/KonfigurasiActivity.kt` | Sumber aksi logout (`performLogout()`) |

Alur data (login/register email-password): `LoginActivity` (input form,
validasi kosong di UI thread) → coroutine `lifecycleScope.launch(Dispatchers.IO)`
→ `AuthClient.instance.login()/register()` (Retrofit, `suspend fun`) →
backend `alkaukabaweb` (`api.php?action=login|register`) → `Response<ApiResponse>`
→ balik ke `Dispatchers.Main`, cek `response.isSuccessful` dan
`apiResponse.status == "success"` → kalau sukses, `SessionManager.setLogin(true)`
lalu pindah ke `MainActivity` (login) atau panggil ulang `performLogin()`
(register, untuk auto-login).

Alur data (Google login): `LoginActivity` (tombol Google) →
`GoogleSignInClient.signInIntent` (Activity Result API) → dapat `idToken`
dari Google → `performGoogleLogin(idToken)` → coroutine IO →
`AuthClient.instance.googleLogin(GoogleLoginRequest(idToken))` → backend
verifikasi token Google ke Google server + cek/buat user →
`Response<ApiResponse>` → sama seperti alur login biasa (`SessionManager.setLogin(true)`
→ `MainActivity`).

Alur data (cek status login saat buka app): `Splashscreen.onCreate()` →
`SessionManager(this).isLoggedIn()` (baca `SharedPreferences`, key
`"IS_LOGGED_IN"`) → redirect. Tidak ada Activity/class lain yang mengecek
status login selain `Splashscreen`; `MainActivity` dan Activity lain
mengasumsikan user sudah login begitu berhasil dibuka (tidak ada re-check
di `onResume()` misalnya).

## 5. Dependencies & tech stack khusus

- `com.google.android.gms:play-services-auth:20.7.0` — Google Sign-In classic
  (`GoogleSignIn`, `GoogleSignInClient`, `GoogleSignInOptions`). Ini
  library khusus fitur ini, tidak dipakai fitur lain di app.
- Retrofit + Gson dipakai lagi di sini (sama seperti fitur Waktu Sholat),
  tapi lewat instance terpisah (`AuthClient`, bukan `RetrofitClient`) karena
  target base URL beda (backend Laravel sendiri, bukan Aladhan API).
- `SharedPreferences` bawaan Android untuk `SessionManager` — sama pola
  dengan penyimpanan preferensi metode sholat, di `SharedPreferences` file
  yang sama (`"AppSession"`).

## 6. Testing

Belum ada test otomatis untuk fitur ini — project masih hanya punya
boilerplate `ExampleUnitTest.kt` (`app/src/test`) dan
`ExampleInstrumentedTest.kt` (`app/src/androidTest`), keduanya template
default Android Studio, tidak menguji apa pun dari fitur ini.

Verifikasi manual saat ini:

1. Pastikan backend `alkaukabaweb` hidup sesuai environment yang dipakai
   (lihat section 2 — produksi `https://alkaukaba.com/` atau lokal via
   `php artisan serve` + `adb reverse`).
2. Build & install debug APK (lihat `CLAUDE.md` root untuk perintah `gradlew`).
3. Uninstall app dulu (atau clear app data) supaya `Splashscreen` benar-benar
   redirect ke `LoginActivity` (fresh install, `IS_LOGGED_IN` belum ada).
4. Coba register akun baru (username/email/password) → pastikan toast sukses
   muncul dan app langsung auto-login masuk ke `MainActivity` tanpa perlu
   isi form login lagi.
5. Logout dari `KonfigurasiActivity` → pastikan balik ke `LoginActivity` dan
   back button tidak bisa kembali ke layar sebelumnya (back stack sudah
   di-clear).
6. Login lagi dengan akun yang sama → pastikan berhasil masuk `MainActivity`.
7. Tutup dan buka ulang app (tanpa logout) → pastikan `Splashscreen` langsung
   ke `MainActivity` tanpa perlu login ulang (validasi `isLoggedIn()` persist).
8. Coba tombol Google Sign-In → pastikan account picker muncul, dan kalau
   ada error, cek `adb logcat` filter tag `GOOGLE_AUTH` untuk `statusCode`
   dari `ApiException` (lihat catatan status validasi di section 7 sebelum
   menganggap ini bug baru).

## 7. Known issues & TODOs

- [ ] **Google Sign-In belum tervalidasi end-to-end di produksi.** Per
      `CLAUDE.md` root (per 2026-08-30): baru `register`/`login` biasa yang
      sudah dites jalan di produksi (curl & app di emulator); `google_login`
      belum. Kalau nemu masalah, cek dulu apakah `GOOGLE_CLIENT_ID` di server
      `alkaukabaweb` sama dengan Web Client ID yang dipanggil
      `requestIdToken(...)` di `LoginActivity.kt`.
- [ ] **Web Client ID Google di-hardcode langsung di `LoginActivity.kt`**
      (dipanggil di `requestIdToken(...)`). Web Client ID untuk OAuth ini
      memang secara desain bukan rahasia (tertanam di APK, dipakai untuk
      identifikasi client ke Google), tapi tetap technical debt karena tidak
      dikonfigurasi lewat `BuildConfig`/resource yang gampang diganti per
      environment (produksi vs testing) seperti `AuthClient.BASE_URL`.
- [x] ~~Tidak ada penyimpanan token/session dari backend~~ — **Diselesaikan
      2026-08-30.** Backend `alkaukabaweb` sekarang mengaktifkan Laravel
      Sanctum (paket sudah lama terpasang tapi tidak pernah dipakai) di
      `login`/`register`/`google_login` — tiap response sukses menyertakan
      `data.token` (bearer token). `LoginActivity.persistUserData()`
      menyimpannya lewat `SessionManager.setAuthToken()`. Dipakai untuk
      endpoint baru yang butuh identitas user (`update_profile`,
      `change_password`, `delete_account`) — lihat
      [features/profil.md](profil.md). `AuthApiService` mengirim token lewat
      `@Header("Authorization")` per-call (bukan `OkHttp` interceptor
      global), jadi setiap pemanggil endpoint terproteksi harus ambil token
      dari `SessionManager.getAuthToken()` sendiri dan format
      `"Bearer $token"` manual.
- [x] ~~`SessionManager.setUserName()` dan `setEmail()` didefinisikan tapi
      tidak pernah dipanggil`~~ — **Diselesaikan 2026-08-30, bug lama sejak
      awal app dibuat.** `LoginActivity.persistUserData()` sekarang
      dipanggil di ketiga jalur sukses (login/register-lalu-login/google
      login), menyimpan `id`, `username`, `email`, dan `token` dari
      `UserData` lewat `SessionManager`. Ini yang memungkinkan halaman
      Profil baru ([features/profil.md](profil.md)) menampilkan nama/email
      user sungguhan, bukan placeholder.
- [ ] Tidak ada validasi format email di form (cuma cek `isEmpty()`) — baik
      di form login maupun register.
- [ ] Tidak ada re-check status login di Activity lain selain `Splashscreen`
      (mis. token/kredensial expired di server tidak akan terdeteksi sampai
      user logout manual atau uninstall app).
- [ ] Belum ada test otomatis (lihat section 6).
