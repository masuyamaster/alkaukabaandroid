# Al-Kaukaba Android

## Menjalankan & build via CLI (tanpa Android Studio)

Project ini dikerjakan sebagian dengan emulator yang dijalankan langsung dari VSCode, bukan dari Android Studio. Jangan sarankan langkah "buka/jalankan di Android Studio" — gunakan `gradlew` dari command line, dan jalankan/hubungkan emulator via `adb`.

Jika shell tidak punya `JAVA_HOME`/`ANDROID_HOME` (umum di Windows), set dulu sebelum memanggil `gradlew` — path berikut contoh di salah satu mesin dev, sesuaikan dengan instalasi JDK 17 & Android SDK di mesin masing-masing:

```
JAVA_HOME="C:/Program Files/Microsoft/jdk-17.0.20.101-hotspot" ANDROID_HOME="C:/Android/Sdk" ./gradlew.bat compileDebugKotlin --console=plain
```

## Konfigurasi API backend (AuthClient BASE_URL)

`BASE_URL` di [`AuthClient.kt`](app/src/main/java/Site/elahady/alkaukaba/utils/AuthClient.kt) menentukan app nembak ke mana untuk `api.php?action=register|login|google_login`. Backend-nya ada di repo terpisah `alkaukabaweb` (lihat `CLAUDE.md` repo itu untuk detail infra VPS, isu nginx, dsb).

- **Produksi (default sejak 2026-08-30)**: `https://alkaukaba.com/` — API sudah live di VPS, sudah divalidasi jalan (register/login lewat curl & app di emulator).
- **Testing lokal** (kalau lagi develop backend bareng, belum mau pakai server produksi):
  1. Ganti `BASE_URL` ke `"http://127.0.0.1:8000/"`.
  2. Jalankan `php artisan serve` di project `alkaukabaweb`.
  3. `adb reverse tcp:8000 tcp:8000` supaya `127.0.0.1` di emulator diteruskan ke `localhost` mesin host.
  4. `network_security_config.xml` sudah punya exception cleartext (HTTP polos) khusus untuk `127.0.0.1` dan `10.0.2.2` — kalau testing lewat IP LAN lain, exception ini nggak berlaku dan perlu ditambah domain baru atau balik ke HTTPS.
- Jangan lupa balikin ke `https://alkaukaba.com/` lagi sebelum build rilis/testing fitur yang butuh data production (kontak Circle, dsb).
- `google_login` di produksi masih belum tervalidasi end-to-end dari app (baru dites `register`/`login` biasa) — kalau nemu masalah, cek dulu `GOOGLE_CLIENT_ID` di server sama dengan yang dipanggil app di `requestIdToken(...)`.

## Setelah mengerjakan task (fix/fitur)

Setelah mengubah kode, jangan hanya compile check — build & install APK debug ke emulator yang sedang jalan supaya perubahan bisa langsung diverifikasi:

```
"$ANDROID_HOME/platform-tools/adb.exe" devices   # pastikan emulator terdeteksi
JAVA_HOME=... ANDROID_HOME=... ./gradlew.bat installDebug --console=plain
```
