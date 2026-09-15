# Tasbih Digital

### 1. Ringkasan (Overview)
- **Nama fitur**: Tasbih Digital
- **Deskripsi singkat**: Penghitung dzikir digital sederhana — user ketuk
  lingkaran untuk menambah hitungan, dengan opsi target (33x/99x/100x/tanpa
  target) dan indikator "putaran" begitu hitungan mencapai kelipatan target.
  Sumber ide: card Notion "Tasbih Digital" (kategori Fitur Baru, prioritas
  Low) — dicatat sebagai "fitur sederhana namun sering dipakai user setelah
  sholat".

### 2. Entry point & prasyarat
- Dipicu dari menu grid baris ketiga di `MainActivity` (`activity_main.xml`,
  id `bt_tasbih`) — di sebelah "Masjid Terdekat".
- Prasyarat: permission `VIBRATE` (ditambahkan di `AndroidManifest.xml`) untuk
  feedback getar tiap ketukan. Tidak butuh API key/config eksternal, tidak
  butuh koneksi internet (murni lokal).

### 3. Titik masuk logika & navigasi
- `TasbihActivity` (`ui/tasbih/TasbihActivity.kt`) — satu-satunya layar
  fitur ini, tidak ada sub-halaman.
- Navigasi: `MainActivity.setupNavigation()` -> `Intent(this, TasbihActivity::class.java)`,
  tanpa extra data.
- Daftar target di-hardcode di companion object `TARGETS = intArrayOf(33, 99, 100, 0)`
  (`0` berarti tanpa target/hitungan bebas) — ketuk `tvTarget` untuk cycle ke
  index berikutnya.

### 4. Struktur & alur data
- `activity_tasbih.xml` — layout: toolbar bawaan (`view_toolbar_default.xml`,
  action icon direset jadi `ic_reset` buat tombol reset), pill target
  (`tvTarget`), label putaran (`tvRounds`), lingkaran ketuk besar
  (`circleTapArea` + `tvCount`), dan tombol undo (`btnUndo`).
- State (`count`, `targetIndex`) disimpan langsung di `SharedPreferences`
  bernama `TasbihPrefs` (bukan lewat `SessionManager` — domainnya beda dan
  cuma butuh 2 key, jadi tidak digabung ke session global) supaya hitungan
  tidak hilang kalau user keluar-masuk activity ini di sesi dzikir yang sama.
- Alur data: `View (tap) -> TasbihActivity (state in-memory) -> SharedPreferences (persist)`,
  tidak ada ViewModel/Repository karena tidak ada logika async/network.

### 5. Dependencies & tech stack khusus
- Tidak ada tambahan khusus di luar stack umum app. Vibrasi pakai
  `VibratorManager` (API 31+) / `Vibrator` (fallback di bawahnya) dari
  `android.os`, bagian standard Android SDK.

### 6. Testing
- Belum ada unit/instrumentation test otomatis untuk fitur ini (logika
  cycle target & increment/undo cukup sederhana untuk saat ini, tapi ini gap
  yang jujur perlu dicatat kalau nanti logikanya berkembang).
- Verifikasi manual: build + install debug APK, buka menu "Tasbih Digital"
  di baris ketiga home, ketuk lingkaran beberapa kali (angka & getar harus
  responsif), ketuk pill target untuk cycle 33x/99x/100x/Tanpa Target (label
  "Putaran ke-N" ikut berubah), tekan "Batalkan Hitungan Terakhir" untuk
  undo, dan tombol reset di toolbar (harus muncul dialog konfirmasi sebelum
  hitungan kembali ke 0). Tutup & buka ulang activity untuk pastikan hitungan
  & target ter-restore dari `SharedPreferences`.

### 7. Known issues & TODOs
- Icon menu (`ic_menu_tasbih`) dipakai ulang dari icon yang sama dengan
  menu "Doa & Dzikir" (beda warna tint: rose vs hijau) — pragmatis untuk versi
  awal, tapi kalau dirasa kurang bisa dibedakan secara visual, pertimbangkan
  bikin drawable dedicated di iterasi berikutnya.
- Belum ada preset nama dzikir (Subhanallah/Alhamdulillah/Allahu Akbar dst)
  — sengaja tidak ditambahkan dulu karena card Notion sumber tidak minta itu
  secara eksplisit, cuma "penghitung sederhana". Bisa jadi follow-up kalau
  user minta.
- Tidak ada reset otomatis harian — hitungan terus menumpuk sampai user
  reset manual by design (levaraging `SharedPreferences` yang persist).
