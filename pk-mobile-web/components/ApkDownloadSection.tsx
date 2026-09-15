'use client';

import { useState } from 'react';
import { Download, HelpCircle, Smartphone, Usb, X, Terminal, FolderOpen, ShieldCheck, CheckCircle2 } from 'lucide-react';

export function ApkDownloadSection() {
  const [showGuide, setShowGuide] = useState(false);
  const [activeTab, setActiveTab] = useState<'hp' | 'usb'>('hp');
  const [copiedAdb, setCopiedAdb] = useState(false);

  const copyAdbCommand = () => {
    navigator.clipboard.writeText('adb install jfn-typemaster.apk');
    setCopiedAdb(true);
    setTimeout(() => setCopiedAdb(false), 2000);
  };

  return (
    <div className="flex flex-col items-start gap-1">
      {/* Baris Tombol Download APK & Panduan */}
      <div className="flex items-center gap-2">
        {/* Tombol Download APK Android */}
        <a
          href="/jfn-typemaster.apk"
          download="JFN_Type_Master.apk"
          className="inline-flex items-center gap-2 bg-gradient-to-r from-emerald-600 to-teal-600 hover:from-emerald-500 hover:to-teal-500 text-white text-xs font-semibold px-3 py-1.5 rounded-xl transition-all shadow-md shadow-emerald-950/40 border border-emerald-400/30 group"
          title="Unduh berkas instalasi Android APK (JFN Type Master)"
        >
          <Download className="w-3.5 h-3.5 text-emerald-200 group-hover:animate-bounce" />
          <span className="tracking-tight">Download APK</span>
        </a>

        {/* Tombol Buka Panduan Install */}
        <button
          type="button"
          onClick={() => setShowGuide(true)}
          className="inline-flex items-center gap-1.5 bg-slate-800/90 hover:bg-slate-700 text-slate-300 hover:text-white text-xs font-medium px-2.5 py-1.5 rounded-xl transition border border-slate-700/80 shadow-sm"
          title="Buka panduan cara memasang aplikasi ke HP Android"
        >
          <HelpCircle className="w-3.5 h-3.5 text-amber-400 shrink-0" />
          <span className="hidden sm:inline">Cara Install</span>
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

      {/* Modal Panduan Interaktif */}
      {showGuide && (
        <div
          className="fixed inset-0 z-50 bg-black/80 backdrop-blur-sm flex items-center justify-center p-3 sm:p-4 overflow-y-auto animate-in fade-in duration-150"
          onClick={(e) => {
            if (e.target === e.currentTarget) setShowGuide(false);
          }}
        >
          <div className="bg-slate-900 border border-slate-800 rounded-2xl max-w-lg w-full max-h-[85vh] sm:max-h-[90vh] flex flex-col shadow-2xl relative text-slate-200 animate-in zoom-in-95 duration-200 overflow-hidden my-auto">
            {/* Header Modal (Paling Atas & Fixed) */}
            <div className="p-5 sm:p-6 pb-4 border-b border-slate-800/80 shrink-0 relative bg-slate-900 z-10">
              {/* Tombol Tutup Silang */}
              <button
                onClick={() => setShowGuide(false)}
                className="absolute top-4 right-4 p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition"
                aria-label="Tutup dialog"
              >
                <X className="w-5 h-5" />
              </button>

              <div className="flex items-center gap-3 pr-8">
                <div className="w-10 h-10 rounded-xl bg-emerald-500/10 border border-emerald-500/30 flex items-center justify-center text-emerald-400 shrink-0">
                  <Smartphone className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="text-base font-bold text-white tracking-tight">
                    Panduan Install JFN Type Master
                  </h3>
                  <div className="flex items-center gap-2 mt-0.5 text-xs text-slate-400 font-mono">
                    <span className="text-emerald-400 font-medium">Versi: v1.0 (Build 1)</span>
                    <span>•</span>
                    <span>15 Sep 2026</span>
                    <span>•</span>
                    <span>9.7 MB</span>
                  </div>
                </div>
              </div>
            </div>

            {/* Body Modal (Scrollable Content Area) */}
            <div className="p-5 sm:p-6 overflow-y-auto space-y-4 flex-1 overscroll-contain">
              {/* Tab Switcher */}
              <div className="grid grid-cols-2 gap-2 bg-slate-950 p-1 rounded-xl border border-slate-800/80 sticky top-0 z-10 shadow-sm">
                <button
                  type="button"
                  onClick={() => setActiveTab('hp')}
                  className={`py-2 px-3 text-xs font-semibold rounded-lg flex items-center justify-center gap-2 transition-all ${
                    activeTab === 'hp'
                      ? 'bg-emerald-600 text-white shadow-md'
                      : 'text-slate-400 hover:text-slate-200 hover:bg-slate-900/60'
                  }`}
                >
                  <Smartphone className="w-3.5 h-3.5" />
                  <span>Langsung dari HP</span>
                  <span className="text-[10px] bg-emerald-400/20 text-emerald-200 px-1.5 py-0.2 rounded-full hidden sm:inline">Mudah</span>
                </button>

                <button
                  type="button"
                  onClick={() => setActiveTab('usb')}
                  className={`py-2 px-3 text-xs font-semibold rounded-lg flex items-center justify-center gap-2 transition-all ${
                    activeTab === 'usb'
                      ? 'bg-emerald-600 text-white shadow-md'
                      : 'text-slate-400 hover:text-slate-200 hover:bg-slate-900/60'
                  }`}
                >
                  <Usb className="w-3.5 h-3.5" />
                  <span>Via PC &amp; Kabel USB</span>
                </button>
              </div>

              {/* Content Tab 1: Langsung dari HP */}
              {activeTab === 'hp' && (
                <div className="space-y-3.5 text-xs text-slate-300">
                  <div className="p-3 bg-emerald-950/30 border border-emerald-500/20 rounded-xl flex items-start gap-2.5 text-emerald-200">
                    <ShieldCheck className="w-4 h-4 text-emerald-400 shrink-0 mt-0.5" />
                    <span>
                      Metode tercepat! Anda cukup membuka website ini langsung dari browser di HP Android.
                    </span>
                  </div>

                  <ol className="list-decimal list-outside ml-4 space-y-2.5 leading-relaxed text-slate-300">
                    <li>
                      Buka website ini di browser <strong>Google Chrome</strong> pada HP Android Anda.
                    </li>
                    <li>
                      Ketuk tombol{' '}
                      <span className="inline-flex items-center gap-1 bg-emerald-600/30 text-emerald-300 px-2 py-0.5 rounded font-mono font-medium border border-emerald-500/30">
                        <Download className="w-3 h-3" /> Download APK
                      </span>{' '}
                      di bagian atas halaman atau di bagian bawah modal ini.
                    </li>
                    <li>
                      Buka notifikasi unduhan yang selesai atau cari berkas{' '}
                      <code className="text-amber-300 font-mono bg-slate-950 px-1.5 py-0.5 rounded border border-slate-800">
                        jfn-typemaster.apk
                      </code>{' '}
                      di folder <em>Downloads</em>.
                    </li>
                    <li>
                      Jika muncul peringatan keamanan (Sumber Tidak Dikenal), ketuk{' '}
                      <strong>Setelan (Settings)</strong> lalu aktifkan{' '}
                      <strong className="text-emerald-400">&quot;Izinkan dari sumber ini&quot;</strong> (<em>Allow from this source</em>).
                    </li>
                    <li>
                      Jika Google Play Protect menampilkan pemberitahuan, ketuk{' '}
                      <strong>Detail selengkapnya</strong> ➔ pilih{' '}
                      <strong className="text-amber-400">&quot;Tetap instal&quot;</strong> (<em>Install anyway</em>).
                    </li>
                    <li>
                      Ketuk <strong>Instal</strong> dan tunggu hingga selesai, lalu buka aplikasi untuk mengaktifkan keyboard.
                    </li>
                  </ol>
                </div>
              )}

              {/* Content Tab 2: Via Komputer & Kabel USB */}
              {activeTab === 'usb' && (
                <div className="space-y-3 text-xs text-slate-300">
                  <p className="text-slate-400">Pilih salah satu metode transfer berkas berikut:</p>

                  {/* Metode 1: File Transfer MTP */}
                  <div className="bg-slate-950/90 p-3.5 rounded-xl border border-slate-800 space-y-2">
                    <div className="flex items-center gap-2 text-emerald-400 font-semibold">
                      <FolderOpen className="w-4 h-4" />
                      <span>Metode 1: File Transfer (MTP)</span>
                    </div>
                    <ol className="list-decimal list-outside ml-4 space-y-1.5 text-slate-300 leading-relaxed">
                      <li>Download berkas APK ke komputer/laptop ini.</li>
                      <li>Hubungkan HP ke komputer menggunakan kabel data USB.</li>
                      <li>
                        Pada notifikasi USB di HP, ubah mode sambungan ke{' '}
                        <strong className="text-emerald-300">&quot;File Transfer (MTP)&quot;</strong>.
                      </li>
                      <li>
                        Salin berkas <code className="text-amber-300 font-mono">jfn-typemaster.apk</code> ke folder{' '}
                        <code className="text-amber-300 font-mono">Download</code> di HP.
                      </li>
                      <li>
                        Buka aplikasi <strong>File Manager / My Files</strong> di HP, lalu ketuk berkas APK tersebut untuk menginstal.
                      </li>
                    </ol>
                  </div>

                  {/* Metode 2: ADB Sideload */}
                  <div className="bg-slate-950/90 p-3.5 rounded-xl border border-slate-800 space-y-2">
                    <div className="flex items-center gap-2 text-amber-400 font-semibold">
                      <Terminal className="w-4 h-4" />
                      <span>Metode 2: Terminal ADB (Developer)</span>
                    </div>
                    <p className="text-slate-400 leading-relaxed">
                      Aktifkan <strong>USB Debugging</strong> di menu Opsi Pengembang (Developer Options) pada HP, lalu jalankan perintah terminal:
                    </p>
                    <div className="relative group">
                      <code className="block bg-slate-900 p-2.5 rounded-lg text-emerald-300 font-mono text-[11px] border border-slate-800 overflow-x-auto">
                        adb install jfn-typemaster.apk
                      </code>
                      <button
                        type="button"
                        onClick={copyAdbCommand}
                        className="absolute right-2 top-2 p-1 rounded bg-slate-800 hover:bg-slate-700 text-slate-400 hover:text-white transition text-[10px] flex items-center gap-1 border border-slate-700"
                      >
                        {copiedAdb ? (
                          <>
                            <CheckCircle2 className="w-3 h-3 text-emerald-400" />
                            <span className="text-emerald-400">Tersalin!</span>
                          </>
                        ) : (
                          <span>Salin</span>
                        )}
                      </button>
                    </div>
                  </div>
                </div>
              )}
            </div>

            {/* Footer Modal (Fixed Bottom) */}
            <div className="p-4 sm:p-5 border-t border-slate-800 bg-slate-950/80 shrink-0 flex flex-col sm:flex-row items-center justify-between gap-3">
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
                  className="inline-flex items-center gap-1.5 bg-gradient-to-r from-emerald-600 to-teal-600 hover:from-emerald-500 hover:to-teal-500 text-white text-xs px-3.5 py-2 rounded-xl font-semibold transition-all shadow-md shadow-emerald-950/40 border border-emerald-400/30"
                >
                  <Download className="w-3.5 h-3.5" />
                  <span>Download APK Sekarang</span>
                </a>
                <button
                  type="button"
                  onClick={() => setShowGuide(false)}
                  className="bg-slate-800 hover:bg-slate-700 text-white text-xs px-3.5 py-2 rounded-xl font-medium transition border border-slate-700"
                >
                  Tutup Panduan
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
