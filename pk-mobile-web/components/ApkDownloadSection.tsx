'use client';

import { useState, useEffect } from 'react';
import { createPortal } from 'react-dom';
import {
  Download,
  HelpCircle,
  Smartphone,
  Usb,
  X,
  Terminal,
  FolderOpen,
  ShieldCheck,
  CheckCircle2,
  Copy,
  Check,
  ExternalLink,
  SlidersHorizontal,
  Info
} from 'lucide-react';

export function ApkDownloadSection() {
  const [showGuide, setShowGuide] = useState(false);
  const [activeTab, setActiveTab] = useState<'hp' | 'usb' | 'activation'>('hp');
  const [copiedAdb, setCopiedAdb] = useState(false);
  const [copiedLink, setCopiedLink] = useState(false);
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    setMounted(true);
  }, []);

  // Kunci scroll body saat modal aktif & dengarkan tombol ESC
  useEffect(() => {
    if (!showGuide) return;

    const originalOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';

    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        setShowGuide(false);
      }
    };

    window.addEventListener('keydown', handleKeyDown);

    return () => {
      document.body.style.overflow = originalOverflow;
      window.removeEventListener('keydown', handleKeyDown);
    };
  }, [showGuide]);

  const copyAdbCommand = () => {
    navigator.clipboard.writeText('adb install jfn-typemaster.apk');
    setCopiedAdb(true);
    setTimeout(() => setCopiedAdb(false), 2000);
  };

  const copyDownloadUrl = () => {
    const url = `${window.location.origin}/jfn-typemaster.apk`;
    navigator.clipboard.writeText(url);
    setCopiedLink(true);
    setTimeout(() => setCopiedLink(false), 2000);
  };

  // Konten Modal Panduan (Dirender via Portal agar tidak terpotong oleh header/backdrop-filter)
  const guideModal = showGuide && mounted && (
    <div
      className="fixed inset-0 z-[9999] bg-black/80 backdrop-blur-sm flex items-center justify-center p-3 sm:p-4 overflow-y-auto animate-in fade-in duration-200"
      onClick={() => setShowGuide(false)}
      role="dialog"
      aria-modal="true"
      aria-labelledby="modal-panduan-title"
    >
      <div
        className="bg-slate-900 border border-slate-800 rounded-2xl max-w-xl w-full max-h-[90vh] flex flex-col shadow-2xl relative text-slate-200 animate-in zoom-in-95 duration-200 overflow-hidden my-auto"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header Modal (Paling Atas & Fixed) */}
        <div className="p-5 sm:p-6 pb-4 border-b border-slate-800 shrink-0 relative bg-slate-900/95 backdrop-blur z-10">
          <button
            onClick={() => setShowGuide(false)}
            className="absolute top-4 right-4 p-2 rounded-xl text-slate-400 hover:text-white hover:bg-slate-800 transition cursor-pointer"
            aria-label="Tutup panduan"
          >
            <X className="w-5 h-5" />
          </button>

          <div className="flex items-center gap-3.5 pr-8">
            <div className="w-11 h-11 rounded-xl bg-emerald-500/10 border border-emerald-500/30 flex items-center justify-center text-emerald-400 shrink-0 shadow-inner">
              <Smartphone className="w-6 h-6" />
            </div>
            <div>
              <h3 id="modal-panduan-title" className="text-base sm:text-lg font-bold text-white tracking-tight">
                Panduan Install &amp; Aktivasi Keyboard
              </h3>
              <div className="flex flex-wrap items-center gap-2 mt-1 text-xs text-slate-400 font-mono">
                <span className="text-emerald-400 font-semibold bg-emerald-950/60 px-2 py-0.5 rounded border border-emerald-800/40">
                  JFN Type Master v1.0
                </span>
                <span>•</span>
                <span>Build 1</span>
                <span>•</span>
                <span className="text-slate-300">9.7 MB</span>
              </div>
            </div>
          </div>
        </div>

        {/* Tab Switcher */}
        <div className="px-5 sm:px-6 pt-3 shrink-0 bg-slate-900">
          <div className="grid grid-cols-3 gap-1.5 bg-slate-950 p-1 rounded-xl border border-slate-800 shadow-inner">
            <button
              type="button"
              onClick={() => setActiveTab('hp')}
              className={`py-2 px-2 text-xs font-semibold rounded-lg flex items-center justify-center gap-1.5 transition-all cursor-pointer ${
                activeTab === 'hp'
                  ? 'bg-emerald-600 text-white shadow-md'
                  : 'text-slate-400 hover:text-slate-200 hover:bg-slate-900'
              }`}
            >
              <Smartphone className="w-3.5 h-3.5 shrink-0" />
              <span className="truncate">Install di HP</span>
            </button>

            <button
              type="button"
              onClick={() => setActiveTab('activation')}
              className={`py-2 px-2 text-xs font-semibold rounded-lg flex items-center justify-center gap-1.5 transition-all cursor-pointer ${
                activeTab === 'activation'
                  ? 'bg-emerald-600 text-white shadow-md'
                  : 'text-slate-400 hover:text-slate-200 hover:bg-slate-900'
              }`}
            >
              <SlidersHorizontal className="w-3.5 h-3.5 shrink-0 text-amber-400" />
              <span className="truncate">Aktivasi Keyboard</span>
            </button>

            <button
              type="button"
              onClick={() => setActiveTab('usb')}
              className={`py-2 px-2 text-xs font-semibold rounded-lg flex items-center justify-center gap-1.5 transition-all cursor-pointer ${
                activeTab === 'usb'
                  ? 'bg-emerald-600 text-white shadow-md'
                  : 'text-slate-400 hover:text-slate-200 hover:bg-slate-900'
              }`}
            >
              <Usb className="w-3.5 h-3.5 shrink-0" />
              <span className="truncate">Via PC / USB</span>
            </button>
          </div>
        </div>

        {/* Body Modal (Scrollable Content) */}
        <div className="p-5 sm:p-6 overflow-y-auto space-y-4 flex-1 overscroll-contain">
          {/* TAB 1: INSTALL LANGSUNG DARI HP */}
          {activeTab === 'hp' && (
            <div className="space-y-4 text-xs text-slate-300">
              <div className="p-3.5 bg-emerald-950/30 border border-emerald-500/20 rounded-xl flex items-start gap-3 text-emerald-200">
                <ShieldCheck className="w-5 h-5 text-emerald-400 shrink-0 mt-0.5" />
                <div className="space-y-1">
                  <p className="font-semibold text-emerald-300">Cara Tercepat &amp; Paling Mudah!</p>
                  <p className="text-[11px] leading-relaxed text-emerald-200/90">
                    Buka link website ini langsung di browser <strong>Google Chrome</strong> pada HP Android Anda untuk mengunduh dan memasang dalam hitungan detik.
                  </p>
                </div>
              </div>

              {/* Tombol Bagikan / Salin Tautan Download untuk HP */}
              <div className="p-3 bg-slate-950 rounded-xl border border-slate-800 flex items-center justify-between gap-2">
                <div className="min-w-0">
                  <p className="text-[11px] text-slate-400 font-medium">Buka di HP atau kirim link ke WhatsApp:</p>
                  <p className="text-xs font-mono text-emerald-400 truncate mt-0.5">
                    {typeof window !== 'undefined' ? `${window.location.origin}/jfn-typemaster.apk` : '/jfn-typemaster.apk'}
                  </p>
                </div>
                <button
                  type="button"
                  onClick={copyDownloadUrl}
                  className="shrink-0 px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-200 hover:text-white text-xs font-medium transition flex items-center gap-1.5 border border-slate-700 cursor-pointer"
                >
                  {copiedLink ? (
                    <>
                      <Check className="w-3.5 h-3.5 text-emerald-400" />
                      <span className="text-emerald-400">Tersalin!</span>
                    </>
                  ) : (
                    <>
                      <Copy className="w-3.5 h-3.5" />
                      <span>Salin Link</span>
                    </>
                  )}
                </button>
              </div>

              {/* Langkah-langkah detail */}
              <div className="space-y-3">
                <div className="flex items-start gap-3 p-3 rounded-xl bg-slate-950/60 border border-slate-800/80">
                  <span className="w-6 h-6 rounded-full bg-emerald-500/20 text-emerald-400 font-bold flex items-center justify-center shrink-0 text-xs">
                    1
                  </span>
                  <div className="space-y-1">
                    <p className="font-semibold text-white">Unduh Berkas APK</p>
                    <p className="text-slate-400 leading-relaxed">
                      Ketuk tombol <strong>&quot;Download APK&quot;</strong> di halaman ini pada HP Anda. Berkas <code className="text-emerald-300 font-mono">jfn-typemaster.apk</code> akan mulai diunduh.
                    </p>
                  </div>
                </div>

                <div className="flex items-start gap-3 p-3 rounded-xl bg-slate-950/60 border border-slate-800/80">
                  <span className="w-6 h-6 rounded-full bg-emerald-500/20 text-emerald-400 font-bold flex items-center justify-center shrink-0 text-xs">
                    2
                  </span>
                  <div className="space-y-1">
                    <p className="font-semibold text-white">Buka Berkas &amp; Izinkan Instalasi</p>
                    <p className="text-slate-400 leading-relaxed">
                      Ketuk notifikasi unduhan selesai, atau buka folder <em>Downloads / Unduhan</em> di File Manager HP. Ketuk berkas APK untuk menginstal.
                    </p>
                    <div className="mt-2 p-2.5 bg-amber-950/30 border border-amber-600/30 rounded-lg text-amber-200 text-[11px] leading-relaxed">
                      <strong className="text-amber-300 block mb-0.5">⚠️ Jika muncul peringatan &quot;Sumber Tidak Dikenal&quot;:</strong>
                      Ketuk <strong>Setelan (Settings)</strong> ➔ aktifkan toggle <strong className="text-white">&quot;Izinkan dari sumber ini&quot;</strong> (<em>Allow from this source</em>), lalu kembali dan ketuk <strong>Instal</strong>.
                    </div>
                  </div>
                </div>

                <div className="flex items-start gap-3 p-3 rounded-xl bg-slate-950/60 border border-slate-800/80">
                  <span className="w-6 h-6 rounded-full bg-emerald-500/20 text-emerald-400 font-bold flex items-center justify-center shrink-0 text-xs">
                    3
                  </span>
                  <div className="space-y-1">
                    <p className="font-semibold text-white">Google Play Protect (Bypass Peringatan)</p>
                    <p className="text-slate-400 leading-relaxed">
                      Karena aplikasi didistribusikan internal tanpa melalui Play Store, Play Protect mungkin menampilkan notifikasi.
                    </p>
                    <div className="mt-2 p-2.5 bg-slate-900 border border-slate-700/80 rounded-lg text-slate-300 text-[11px] leading-relaxed">
                      Ketuk <strong>&quot;Detail selengkapnya&quot;</strong> (<em>More details</em>) ➔ pilih <strong className="text-emerald-400">&quot;Tetap instal&quot;</strong> (<em>Install anyway</em>).
                    </div>
                  </div>
                </div>

                <div className="flex items-start gap-3 p-3 rounded-xl bg-emerald-950/20 border border-emerald-500/30">
                  <span className="w-6 h-6 rounded-full bg-emerald-500 text-slate-950 font-bold flex items-center justify-center shrink-0 text-xs">
                    4
                  </span>
                  <div className="space-y-1">
                    <p className="font-semibold text-emerald-300">Lanjutkan ke Langkah Aktivasi</p>
                    <p className="text-slate-300 leading-relaxed">
                      Setelah selesai dipasang, buka aplikasi di HP dan ikuti langkah aktivasi keyboard pada tab berikutnya.
                    </p>
                    <button
                      type="button"
                      onClick={() => setActiveTab('activation')}
                      className="mt-2 text-xs font-semibold text-emerald-400 hover:text-emerald-300 underline flex items-center gap-1 cursor-pointer"
                    >
                      <span>Lihat Langkah Aktivasi Keyboard</span>
                      <span>➔</span>
                    </button>
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* TAB 2: AKTIVASI KEYBOARD SETELAH INSTALL */}
          {activeTab === 'activation' && (
            <div className="space-y-4 text-xs text-slate-300">
              <div className="p-3.5 bg-amber-950/30 border border-amber-500/30 rounded-xl flex items-start gap-3 text-amber-200">
                <Info className="w-5 h-5 text-amber-400 shrink-0 mt-0.5" />
                <div className="space-y-1">
                  <p className="font-semibold text-amber-300">Wajib Dilakukan Setelah Install!</p>
                  <p className="text-[11px] leading-relaxed text-amber-200/90">
                    Keyboard Android baru memerlukan izin aktif dari pengaturan sistem agar dapat digunakan saat mengetik di WhatsApp, browser, atau aplikasi lainnya.
                  </p>
                </div>
              </div>

              <div className="space-y-3">
                {/* Langkah 1 */}
                <div className="p-4 rounded-xl bg-slate-950 border border-slate-800 space-y-2">
                  <div className="flex items-center gap-2 text-emerald-400 font-semibold text-sm">
                    <span className="w-6 h-6 rounded-full bg-emerald-500/20 text-emerald-400 text-xs flex items-center justify-center font-bold">
                      1
                    </span>
                    <span>Buka Aplikasi &quot;JFN Type Master&quot; di HP</span>
                  </div>
                  <p className="text-slate-400 leading-relaxed pl-8">
                    Cari ikon keyboard bernama <strong>JFN Type Master</strong> di daftar aplikasi HP Android Anda dan buka.
                  </p>
                </div>

                {/* Langkah 2 */}
                <div className="p-4 rounded-xl bg-slate-950 border border-slate-800 space-y-2">
                  <div className="flex items-center gap-2 text-emerald-400 font-semibold text-sm">
                    <span className="w-6 h-6 rounded-full bg-emerald-500/20 text-emerald-400 text-xs flex items-center justify-center font-bold">
                      2
                    </span>
                    <span>Aktifkan di Pengaturan Keyboard</span>
                  </div>
                  <p className="text-slate-400 leading-relaxed pl-8">
                    Ketuk tombol <strong className="text-white">&quot;1. Aktifkan di Pengaturan Keyboard&quot;</strong>. Sistem akan membuka menu Keyboard Android. Nyalakan switch/saklar pada <strong className="text-emerald-400">JFN Type Master Keyboard</strong> dan pilih &quot;OK&quot;.
                  </p>
                </div>

                {/* Langkah 3 */}
                <div className="p-4 rounded-xl bg-slate-950 border border-slate-800 space-y-2">
                  <div className="flex items-center gap-2 text-emerald-400 font-semibold text-sm">
                    <span className="w-6 h-6 rounded-full bg-emerald-500/20 text-emerald-400 text-xs flex items-center justify-center font-bold">
                      3
                    </span>
                    <span>Pilih Sebagai Keyboard Aktif (Default)</span>
                  </div>
                  <p className="text-slate-400 leading-relaxed pl-8">
                    Kembali ke aplikasi, ketuk tombol <strong className="text-white">&quot;2. Pilih Sebagai Keyboard Aktif&quot;</strong>. Pilih <strong className="text-emerald-400">JFN Type Master</strong> dari daftar pilihan keyboard.
                  </p>
                </div>

                {/* Langkah 4 */}
                <div className="p-4 rounded-xl bg-emerald-950/20 border border-emerald-500/30 space-y-2">
                  <div className="flex items-center gap-2 text-emerald-400 font-semibold text-sm">
                    <CheckCircle2 className="w-5 h-5 text-emerald-400 shrink-0" />
                    <span>Selesai! Auto-Text Tersinkronisasi Otomatis</span>
                  </div>
                  <p className="text-slate-300 leading-relaxed pl-7">
                    Buka WhatsApp atau aplikasi pesan apa pun. Ketik trigger shortcut Anda (contoh: <code className="text-amber-300 font-mono bg-slate-900 px-1 py-0.5 rounded">//baca</code> atau <code className="text-amber-300 font-mono bg-slate-900 px-1 py-0.5 rounded">!rek</code>) lalu tekan Spasi (atau otomatis sesuai settingan mode). Teks template akan otomatis mengembang!
                  </p>
                </div>
              </div>
            </div>
          )}

          {/* TAB 3: VIA PC & KABEL USB */}
          {activeTab === 'usb' && (
            <div className="space-y-4 text-xs text-slate-300">
              <p className="text-slate-400">
                Jika HP tidak memiliki koneksi internet langsung, gunakan salah satu metode komputer berikut:
              </p>

              {/* Metode Transfer MTP */}
              <div className="bg-slate-950 p-4 rounded-xl border border-slate-800 space-y-2.5">
                <div className="flex items-center gap-2 text-emerald-400 font-semibold text-sm">
                  <FolderOpen className="w-4 h-4" />
                  <span>Metode 1: Salin via Kabel USB (File Transfer / MTP)</span>
                </div>
                <ol className="list-decimal list-outside ml-4 space-y-2 text-slate-300 leading-relaxed">
                  <li>Download berkas APK ke komputer ini dengan menekan tombol <strong>Download APK</strong>.</li>
                  <li>Sambungkan HP Android ke komputer menggunakan kabel data USB.</li>
                  <li>
                    Di HP, buka notifikasi USB dan pilih mode <strong className="text-emerald-300">&quot;Transfer Berkas (MTP)&quot;</strong>.
                  </li>
                  <li>
                    Buka Windows Explorer di laptop, lalu salin (copy) berkas <code className="text-amber-300 font-mono">jfn-typemaster.apk</code> ke folder <strong>Download</strong> di penyimpanan internal HP.
                  </li>
                  <li>
                    Buka aplikasi <strong>File Manager / File Saya</strong> di HP, lalu ketuk berkas APK tersebut untuk menginstal.
                  </li>
                </ol>
              </div>

              {/* Metode ADB */}
              <div className="bg-slate-950 p-4 rounded-xl border border-slate-800 space-y-2.5">
                <div className="flex items-center gap-2 text-amber-400 font-semibold text-sm">
                  <Terminal className="w-4 h-4" />
                  <span>Metode 2: Terminal ADB (Untuk Teknisi / Developer)</span>
                </div>
                <p className="text-slate-400 leading-relaxed">
                  Aktifkan <strong>USB Debugging</strong> di Pengaturan Opsi Pengembang HP Android, lalu jalankan perintah terminal:
                </p>
                <div className="relative group">
                  <code className="block bg-slate-900 p-2.5 rounded-lg text-emerald-300 font-mono text-[11px] border border-slate-800 overflow-x-auto select-all">
                    adb install jfn-typemaster.apk
                  </code>
                  <button
                    type="button"
                    onClick={copyAdbCommand}
                    className="absolute right-2 top-2 p-1.5 rounded bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white transition text-[11px] flex items-center gap-1 border border-slate-700 cursor-pointer"
                  >
                    {copiedAdb ? (
                      <>
                        <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400" />
                        <span className="text-emerald-400 font-medium">Tersalin!</span>
                      </>
                    ) : (
                      <>
                        <Copy className="w-3.5 h-3.5" />
                        <span>Salin Perintah</span>
                      </>
                    )}
                  </button>
                </div>
              </div>
            </div>
          )}
        </div>

        {/* Footer Modal (Fixed Bottom) */}
        <div className="p-4 sm:p-5 border-t border-slate-800 bg-slate-950/90 shrink-0 flex flex-col sm:flex-row items-center justify-between gap-3">
          <div className="flex items-center gap-2 text-[11px] text-slate-400 font-mono">
            <span className="text-emerald-400 font-semibold">v1.0 (Build 1)</span>
            <span>•</span>
            <span>15 Sep 2026</span>
            <span>•</span>
            <span>9.7 MB</span>
          </div>

          <div className="flex items-center gap-2 w-full sm:w-auto justify-end">
            <a
              href="/jfn-typemaster.apk"
              download="JFN_Type_Master.apk"
              className="inline-flex items-center gap-2 bg-gradient-to-r from-emerald-600 to-teal-600 hover:from-emerald-500 hover:to-teal-500 text-white text-xs px-4 py-2 rounded-xl font-semibold transition-all shadow-md shadow-emerald-950/40 border border-emerald-400/30 cursor-pointer"
            >
              <Download className="w-4 h-4" />
              <span>Download APK Sekarang</span>
            </a>

            <button
              type="button"
              onClick={() => setShowGuide(false)}
              className="bg-slate-800 hover:bg-slate-700 text-white text-xs px-4 py-2 rounded-xl font-medium transition border border-slate-700 cursor-pointer"
            >
              Tutup
            </button>
          </div>
        </div>
      </div>
    </div>
  );

  return (
    <div className="flex flex-col items-start gap-1">
      {/* Baris Tombol Download APK & Panduan */}
      <div className="flex items-center gap-2">
        {/* Tombol Download APK Android */}
        <a
          href="/jfn-typemaster.apk"
          download="JFN_Type_Master.apk"
          className="inline-flex items-center gap-2 bg-gradient-to-r from-emerald-600 to-teal-600 hover:from-emerald-500 hover:to-teal-500 text-white text-xs font-semibold px-3 py-1.5 rounded-xl transition-all shadow-md shadow-emerald-950/40 border border-emerald-400/30 group cursor-pointer"
          title="Unduh berkas instalasi Android APK (JFN Type Master)"
        >
          <Download className="w-3.5 h-3.5 text-emerald-200 group-hover:animate-bounce" />
          <span className="tracking-tight">Download APK</span>
        </a>

        {/* Tombol Buka Panduan Install */}
        <button
          type="button"
          onClick={() => setShowGuide(true)}
          className="inline-flex items-center gap-1.5 bg-slate-800/90 hover:bg-slate-700 text-slate-300 hover:text-white text-xs font-medium px-2.5 py-1.5 rounded-xl transition border border-slate-700/80 shadow-sm cursor-pointer"
          title="Buka panduan cara memasang & mengaktifkan aplikasi keyboard di HP Android"
        >
          <HelpCircle className="w-3.5 h-3.5 text-amber-400 shrink-0" />
          <span className="hidden sm:inline">Panduan Install</span>
        </button>
      </div>

      {/* Informasi Versi & Tanggal di Bawah Tombol Download */}
      <div className="flex items-center gap-1.5 text-[10px] text-slate-400 px-1 font-mono tracking-tight select-none">
        <span className="inline-flex items-center gap-1 text-emerald-400 font-semibold">
          <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse" />
          v1.0 (Build 1)
        </span>
        <span className="text-slate-600">•</span>
        <span className="text-slate-400">15 Sep 2026</span>
        <span className="text-slate-600">•</span>
        <span className="text-slate-500">9.7 MB</span>
      </div>

      {/* Render Modal via Portal ke document.body */}
      {mounted && typeof document !== 'undefined' && guideModal && createPortal(guideModal, document.body)}
    </div>
  );
}

