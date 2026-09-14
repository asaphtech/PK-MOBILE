'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { getSupabaseClient, getActiveAnonKey } from '@/lib/supabase';
import { KeyRound, Mail, Lock, ShieldCheck, ArrowRight, Settings, AlertCircle, CheckCircle2 } from 'lucide-react';

export default function LoginPage() {
  const router = useRouter();
  const [isRegister, setIsRegister] = useState(false);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  // Key override config toggle
  const [showConfig, setShowConfig] = useState(false);
  const [customKey, setCustomKey] = useState(getActiveAnonKey());

  const handleAuth = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage('');
    setSuccessMessage('');
    setLoading(true);

    try {
      const client = getSupabaseClient();

      if (isRegister) {
        if (password.length < 6) {
          setErrorMessage('Kata sandi minimal 6 karakter!');
          setLoading(false);
          return;
        }

        const { data, error } = await client.auth.signUp({
          email,
          password,
        });

        if (error) {
          setErrorMessage(error.message);
        } else {
          if (data.session) {
            setSuccessMessage('Pendaftaran berhasil! Mengarahkan ke Dashboard...');
            setTimeout(() => router.push('/dashboard'), 1200);
          } else {
            setSuccessMessage('Pendaftaran berhasil! Silakan periksa inbox email Anda untuk konfirmasi, atau coba login langsung.');
          }
        }
      } else {
        const { error } = await client.auth.signInWithPassword({
          email,
          password,
        });

        if (error) {
          setErrorMessage(error.message);
        } else {
          setSuccessMessage('Login berhasil! Mengarahkan...');
          router.push('/dashboard');
        }
      }
    } catch (err: any) {
      setErrorMessage(err.message || 'Terjadi kesalahan saat memproses permintaan.');
    } finally {
      setLoading(false);
    }
  };

  const handleSaveKey = () => {
    if (customKey.trim().length > 0) {
      localStorage.setItem('pk_custom_supabase_key', customKey.trim());
      setSuccessMessage('Supabase Anon Key berhasil disimpan di browser!');
      setShowConfig(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 flex flex-col items-center justify-center p-4 selection:bg-indigo-500 selection:text-white">
      {/* Background Glow */}
      <div className="fixed inset-0 pointer-events-none overflow-hidden">
        <div className="absolute -top-40 -left-40 w-96 h-96 bg-indigo-600/15 rounded-full blur-3xl" />
        <div className="absolute -bottom-40 -right-40 w-96 h-96 bg-emerald-500/10 rounded-full blur-3xl" />
      </div>

      <div className="w-full max-w-md z-10">
        {/* Header Branding */}
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-gradient-to-tr from-indigo-600 to-emerald-400 p-0.5 shadow-2xl shadow-indigo-500/30 mb-4">
            <div className="w-full h-full bg-slate-900 rounded-[14px] flex items-center justify-center text-emerald-400">
              <ShieldCheck className="w-8 h-8" />
            </div>
          </div>
          <h1 className="text-2xl font-bold text-white tracking-tight">JFN TYPE MASTER</h1>
          <p className="text-sm text-slate-400 mt-1">
            CS Auto-Text Management System
          </p>
        </div>

        {/* Card Form */}
        <div className="bg-slate-900/90 backdrop-blur-xl border border-slate-800 rounded-2xl shadow-2xl p-6 sm:p-8">
          {/* Tab Toggle */}
          <div className="grid grid-cols-2 p-1 bg-slate-950 rounded-xl mb-6 border border-slate-800/80">
            <button
              type="button"
              onClick={() => { setIsRegister(false); setErrorMessage(''); setSuccessMessage(''); }}
              className={`py-2.5 text-sm font-semibold rounded-lg transition-all ${
                !isRegister
                  ? 'bg-indigo-600 text-white shadow-lg shadow-indigo-600/30'
                  : 'text-slate-400 hover:text-white'
              }`}
            >
              Masuk (Login)
            </button>
            <button
              type="button"
              onClick={() => { setIsRegister(true); setErrorMessage(''); setSuccessMessage(''); }}
              className={`py-2.5 text-sm font-semibold rounded-lg transition-all ${
                isRegister
                  ? 'bg-indigo-600 text-white shadow-lg shadow-indigo-600/30'
                  : 'text-slate-400 hover:text-white'
              }`}
            >
              Daftar Akun Baru
            </button>
          </div>

          {/* Feedback Banners */}
          {errorMessage && (
            <div className="mb-4 p-3.5 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-400 text-sm flex items-start gap-2.5">
              <AlertCircle className="w-5 h-5 shrink-0 mt-0.5" />
              <span>{errorMessage}</span>
            </div>
          )}

          {successMessage && (
            <div className="mb-4 p-3.5 rounded-xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 text-sm flex items-start gap-2.5">
              <CheckCircle2 className="w-5 h-5 shrink-0 mt-0.5" />
              <span>{successMessage}</span>
            </div>
          )}

          <form onSubmit={handleAuth} className="space-y-4">
            <div>
              <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-2">
                Email CS / Admin
              </label>
              <div className="relative">
                <Mail className="w-4 h-4 text-slate-500 absolute left-3.5 top-1/2 -translate-y-1/2" />
                <input
                  type="email"
                  required
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="cs.support@pkmobile.com"
                  className="w-full bg-slate-950/80 border border-slate-800 rounded-xl pl-10 pr-4 py-3 text-sm text-white placeholder-slate-600 focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 transition-all"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-2">
                Kata Sandi
              </label>
              <div className="relative">
                <Lock className="w-4 h-4 text-slate-500 absolute left-3.5 top-1/2 -translate-y-1/2" />
                <input
                  type="password"
                  required
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="••••••••"
                  className="w-full bg-slate-950/80 border border-slate-800 rounded-xl pl-10 pr-4 py-3 text-sm text-white placeholder-slate-600 focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 transition-all"
                />
              </div>
              {isRegister && (
                <p className="text-xs text-slate-500 mt-1.5">
                  Minimal 6 karakter kombinasi huruf &amp; angka.
                </p>
              )}
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full py-3.5 px-4 bg-gradient-to-r from-indigo-600 to-indigo-500 hover:from-indigo-500 hover:to-indigo-400 text-white font-semibold rounded-xl shadow-lg shadow-indigo-600/30 hover:shadow-indigo-600/50 flex items-center justify-center gap-2 transition-all disabled:opacity-50 disabled:cursor-not-allowed mt-2"
            >
              {loading ? (
                <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
              ) : (
                <>
                  <span>{isRegister ? 'Buat Akun CS Baru' : 'Masuk ke Dashboard'}</span>
                  <ArrowRight className="w-4 h-4" />
                </>
              )}
            </button>
          </form>

          {/* Quick Config Button */}
          <div className="mt-6 pt-5 border-t border-slate-800/80 flex flex-col items-center">
            <button
              type="button"
              onClick={() => setShowConfig(!showConfig)}
              className="text-xs text-slate-400 hover:text-indigo-400 flex items-center gap-1.5 transition-colors"
            >
              <Settings className="w-3.5 h-3.5" />
              <span>{showConfig ? 'Tutup Pengaturan Kunci' : 'Atur Supabase Anon Key'}</span>
            </button>

            {showConfig && (
              <div className="w-full mt-3 p-3.5 bg-slate-950/90 border border-slate-800 rounded-xl text-left space-y-2">
                <div className="flex items-center gap-1.5 text-xs text-emerald-400 font-semibold">
                  <KeyRound className="w-3.5 h-3.5" />
                  <span>Supabase Anon Key Proyek</span>
                </div>
                <textarea
                  rows={2}
                  value={customKey}
                  onChange={(e) => setCustomKey(e.target.value)}
                  placeholder="Tempelkan Supabase Anon Key di sini..."
                  className="w-full bg-slate-900 border border-slate-700/80 rounded-lg p-2 text-xs font-mono text-slate-300 focus:outline-none focus:border-indigo-500"
                />
                <button
                  type="button"
                  onClick={handleSaveKey}
                  className="w-full py-1.5 bg-slate-800 hover:bg-slate-700 text-xs font-medium text-white rounded-lg transition-colors"
                >
                  Simpan Kunci ke Browser
                </button>
              </div>
            )}
          </div>
        </div>

        {/* Footer info */}
        <p className="text-center text-xs text-slate-600 mt-6">
          JFN Type Master &bull; Tersinkronisasi Otomatis dengan Aplikasi Android
        </p>
      </div>
    </div>
  );
}
