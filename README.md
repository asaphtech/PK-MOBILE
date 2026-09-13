# PK Custom Keyboard (IME) untuk Android dengan Auto-Text / Shortcut

Aplikasi Input Method Editor (IME) kustom untuk Android yang dibangun menggunakan **Kotlin Native**. Aplikasi ini dirancang untuk menggantikan keyboard bawaan dengan fitur unggulan **Auto-Text / Shortcut Expansion** (seperti pada aplikasi *Perfect Keyboard*), di mana pengguna dapat mengetik kata kunci singkat (misalnya `omw`) dan secara otomatis diekspansi menjadi kalimat panjang (misalnya `On my way!`) saat tombol spasi ditekan.

---

## 📁 Struktur Direktori Proyek

```
PK MOBILE/
├── gradle/
│   └── wrapper/
│       └── gradle-wrapper.properties   # Konfigurasi Gradle 8.5
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── AndroidManifest.xml     # Pendaftaran BIND_INPUT_METHOD & Service
│   │       ├── java/com/pkmobile/keyboard/
│   │       │   ├── data/
│   │       │   │   ├── db/
│   │       │   │   │   ├── ShortcutEntity.kt    # Entity Room untuk shortcut
│   │       │   │   │   ├── ShortcutDao.kt       # Akses data CRUD Room
│   │       │   │   │   └── AppDatabase.kt       # Room Database singleton & pre-population
│   │       │   │   └── repository/
│   │       │   │       └── ShortcutRepository.kt# Abstraksi data layer
│   │       │   ├── engine/
│   │       │   │   └── AutoTextEngine.kt        # Buffer kata aktif & logika auto-expansion
│   │       │   ├── service/
│   │       │   │   └── CustomKeyboardService.kt # InputMethodService utama keyboard
│   │       │   └── ui/
│   │       │       ├── MainActivity.kt          # UI Aktivasi IME & Manajemen Shortcut
│   │       │       └── ShortcutAdapter.kt       # RecyclerView adapter untuk shortcut
│   │       └── res/
│   │           ├── drawable/                   # Drawable tombol, chip, dan vector icons
│   │           ├── layout/
│   │           │   ├── keyboard_view.xml        # Layout keyboard QWERTY + candidate bar
│   │           │   ├── activity_main.xml        # Layout dashboard & live test input
│   │           │   ├── item_shortcut.xml        # Layout card item list shortcut
│   │           │   └── dialog_add_shortcut.xml  # Dialog tambah shortcut baru
│   │           ├── values/
│   │           │   ├── colors.xml               # Palet tema dark sleek modern
│   │           │   ├── strings.xml              # String resources
│   │           │   └── themes.xml               # Tema aplikasi
│   │           └── xml/
│   │               └── method.xml               # Metadata Input Method subtype
│   └── build.gradle.kts                         # Konfigurasi modul app (SDK 34, KSP, Room)
├── build.gradle.kts                             # Root build configuration
├── settings.gradle.kts                          # Pengaturan root project
├── gradle.properties                            # JVM & AndroidX settings
├── gradlew & gradlew.bat                        # Skrip launcher Gradle
└── README.md
```

---

## 🛠️ Arsitektur & Komponen Utama

### 1. `CustomKeyboardService.kt`
- Me-extend `android.inputmethodservice.InputMethodService`.
- Mengimplementasikan `onCreateInputView()` untuk memuat tampilan keyboard kustom dari `keyboard_view.xml`.
- Menangani event input:
  - **Tombol Huruf/Karakter**: Mengirim teks ke kolom input aktif melalui `currentInputConnection.commitText(char, 1)` dan memasukkannya ke buffer `AutoTextEngine`.
  - **Tombol Spasi**: Memanggil `AutoTextEngine.handleSpace(currentInputConnection)`. Jika ada kata kunci shortcut yang cocok, teks shortcut yang terketik langsung dihapus dan digantikan oleh ekspansi auto-text.
  - **Tombol Backspace**: Memanggil `currentInputConnection.deleteSurroundingText(1, 0)` dan mengurangi karakter terakhir pada buffer engine.
  - **Tombol Shift / Caps**: Melakukan toggle huruf besar/kecil (Caps) secara dinamis pada tampilan tombol keyboard.
  - **Tombol Simbol (?123 / ABC)**: Mengalihkan tampilan antara alfabet QWERTY dan deretan angka/simbol.

### 2. `AutoTextEngine.kt`
- Menjaga buffer kata yang sedang diketik (`currentWordBuffer`).
- Menggunakan **In-Memory Cache (`ConcurrentHashMap`)** yang disinkronkan secara reaktif dari Room Database sehingga pencarian shortcut beroperasi dalam waktu $O(1)$ tanpa jeda I/O saat mengetik cepat.
- Saat tombol **Spasi** ditekan:
  1. Mengecek apakah kata di buffer cocok dengan shortcut.
  2. Jika cocok: `deleteSurroundingText(panjang_kata, 0)` lalu `commitText(ekspansi + " ", 1)`.
  3. Jika tidak cocok: `commitText(" ", 1)`.
  4. Mereset buffer kata.
- Menyediakan callback `onCandidateUpdateListener` untuk menampilkan live preview ekspansi di Candidate Suggestion Bar.

### 3. `AppDatabase.kt` & Room Database
- Menyimpan pasangan shortcut (kata kunci) dan teks penggantinya secara persisten.
- Otomatis mengisi data awal (*default shortcuts*) pada instalasi pertama:
  - `omw` $\to$ `On my way!`
  - `brb` $\to$ `Be right back`
  - `thx` $\to$ `Thank you so much!`
  - `btw` $\to$ `By the way`
  - `otw` $\to$ `On the way`
  - `info` $\to$ `Informasi lebih lanjut dapat menghubungi layanan kami.`

### 4. `MainActivity.kt`
- Dashboard kontrol untuk pengguna:
  - **Tombol 1**: Membuka Pengaturan Input Method Android (`Settings.ACTION_INPUT_METHOD_SETTINGS`) untuk mengaktifkan keyboard.
  - **Tombol 2**: Membuka dialog pemilih input method aktif (`InputMethodManager.showInputMethodPicker()`).
  - **Test Input Field**: Kolom input langsung di aplikasi untuk mencoba mengetik dan menguji ekspansi shortcut.
  - **Manajer Shortcut**: Menampilkan daftar shortcut Room, menghapus shortcut, dan menambah shortcut baru melalui pop-up dialog.

---

## 🚀 Panduan Build, Instalasi & Testing Menggunakan Terminal

### Prasyarat:
1. Pastikan **Java JDK 17** dan **Android SDK** terpasang di sistem.
2. Perangkat Android fisik atau Emulator Android sudah terhubung via USB Debugging.

### 1. Build APK Debug
Jalankan perintah berikut di direktori root proyek:
- **Windows (PowerShell / Command Prompt):**
  ```powershell
  .\gradlew.bat assembleDebug
  ```
- **macOS / Linux / Git Bash:**
  ```bash
  ./gradlew assembleDebug
  ```
*File APK yang dihasilkan berada di: `app/build/outputs/apk/debug/app-debug.apk`.*

### 2. Pasang (Install) APK ke Perangkat / Emulator
Gunakan Android Debug Bridge (ADB):
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 3. Cek Daftar Input Method yang Terpasang
Verifikasi apakah service keyboard sudah terdeteksi di sistem Android:
```bash
adb shell ime list -a
```
*Anda akan melihat ID keyboard: `com.pkmobile.keyboard/.service.CustomKeyboardService`.*

### 4. Aktifkan IME Service Langsung via ADB
Aktifkan keyboard tanpa harus masuk manual ke menu pengaturan sistem:
```bash
adb shell ime enable com.pkmobile.keyboard/.service.CustomKeyboardService
```

### 5. Atur Keyboard Ini Sebagai Input Method Aktif (Default)
```bash
adb shell ime set com.pkmobile.keyboard/.service.CustomKeyboardService
```

### 6. Buka Dashboard Aplikasi
```bash
adb shell am start -n com.pkmobile.keyboard/.ui.MainActivity
```

### 7. Uji Coba Fitur Auto-Text:
1. Buka aplikasi chatting (misalnya WhatsApp, Pesan, atau kolom input uji coba di `MainActivity`).
2. Ketik huruf: `o`, `m`, `w`.
3. Perhatikan suggestion bar di bagian atas keyboard menampilkan: `omw ➔ On my way!`.
4. Tekan tombol **Spasi** (atau sentuh suggestion bar).
5. **Hasil:** Teks `omw` otomatis berganti menjadi `On my way! `.
