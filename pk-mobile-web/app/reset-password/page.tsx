'use client';

import { useState, useEffect, useMemo, Suspense } from 'react';
import Link from 'next/link';
import { useRouter, useSearchParams } from 'next/navigation';
import { getSupabaseClient } from '@/lib/supabase';
import {
  Lock, KeyRound, ShieldCheck, CheckCircle2, AlertCircle,
  ArrowRight, Eye, EyeOff, Check, X, ShieldAlert
} from 'lucide-react';

function ResetPasswordForm() {
  const router = useRouter();
  const searchParams = useSearchParams();

  // Ambil token / email dari query string (misal: ?token=123456 atau ?email=...)
  const urlToken = searchParams.get('token') || searchParams.get('code') || '';
  const urlEmail = searchParams.get('email') || '';

  const [token, setToken] = useState(urlToken);
  const [email, setEmail] = useState(urlEmail);
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);

  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const [isRecoverySessionActive, setIsRecoverySessionActive] = useState(false);

  // Deteksi sesi recovery Supabase secara otomatis dari URL hash / session
  useEffect(() => {
    const client = getSupabaseClient();

    // 1. Cek session yang sudah aktif
    client.auth.getSession().then(({ data: { session } }) => {
      if (session) {
        setIsRecoverySessionActive(true);
      }
    });

    // 2. Dengarkan event PASSWORD_RECOVERY dari Supabase
    const { data: { subscription } } = client.auth.onAuthStateChange((event, session) => {
      if (event === 'PASSWORD_RECOVERY' || (session && event === 'SIGNED_IN')) {
        setIsRecoverySessionActive(true);
      }
    });

    // 3. Tangkap hash fragment jika ada (#access_token=...&type=recovery)
    if (typeof window !== 'undefined' && window.location.hash) {
      const hashParams = new URLSearchParams(window.location.hash.substring(1));
      const type = hashParams.get('type');
      if (type === 'recovery') {
        setIsRecoverySessionActive(true);
      }
    }

    return () => {
      subscription.unsubscribe();
    };
  }, []);

  // Evaluasi kekuatan password (Lemah / Sedang / Kuat)
  const strengthInfo = useMemo(() => {
    if (!password) {
      return { score: 0, label: 'Belum diisi', color: 'bg-slate-700', text: 'text-slate-500' };
    }

    let score = 0;
    const hasMinLength = password.length >= 8;
    const hasNumbers = /\d/.test(password);
    const hasLower = /[a-z]/.test(password);
    const hasUpper = /[A-Z]/.test(password);
    const hasSpecial = /[^A-Za-z0-9]/.test(password);

    if (hasMinLength) score += 1;
    if (hasLower && hasUpper) score += 1;
    if (hasNumbers) score += 1;
    if (hasSpecial) score += 1;

    if (password.length < 8) {
      return { score: 1, label: 'Lemah (Minimal 8 karakter)', color: 'bg-rose-500', text: 'text-rose-400' };
    }

    if (score <= 2) {
      return { score: 2, label: 'Sedang (Bagus, kombinasikan huruf & angka)', color: 'bg-amber-500', text: 'text-amber-400' };
    }

    return { score: 3, label: 'Kuat (Sangat aman)', color: 'bg-emerald-500', text: 'text-emerald-400' };
  }, [password]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage('');
    setSuccessMessage('');

    // Validasi panjang minimal 8 karakter
    if (password.length < 8) {
      setErrorMessage('Kata sandi baru wajib memiliki panjang minimal 8 karakter!');
      return;
    }

    // Validasi konfirmasi password
    if (password !== confirmPassword) {
      setErrorMessage('Konfirmasi kata sandi tidak cocok dengan kata sandi baru!');
      return;
    }

    setLoading(true);

    try {
      const client = getSupabaseClient();

      // Kasus A: Pengguna memasukkan token / kode OTP pemulihan secara manual atau dari URL parameter
      if (token && !isRecoverySessionActive) {
        if (email.trim()) {
          const { error: otpError } = await client.auth.verifyOtp({
            email: email.trim(),
            token: token.trim(),
            type: 'recovery',
          });
          if (otpError) {
            throw new Error(`Verifikasi token pemulihan gagal: ${otpError.message}`);
          }
        }
      }

      // Kasus B: Perbarui password akun pengguna melalui updateUser
      const { data, error } = await client.auth.updateUser({
        password: password,
      });

      if (error) {
        throw error;
      }

      setSuccessMessage('Kata sandi berhasil diperbarui! Anda sekarang dapat masuk menggunakan kata sandi baru.');
      setTimeout(() => {
        router.push('/login');
      }, 2500);
    } catch (err: any) {
      setErrorMessage(
        err.message || 'Gagal mengatur ulang kata sandi. Pastikan tautan atau token pemulihan Anda masih berlaku.'
      );
    } finally {
      setLoading(false);
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
              <Lock className="w-8 h-8" />
            </div>
          </div>
          <h1 className="text-2xl font-bold text-white tracking-tight">Atur Kata Sandi Baru</h1>
          <p className="text-sm text-slate-400 mt-1">
            Buat kata sandi baru yang aman untuk akun JFN Type Master Anda
          </p>
        </div>

        {/* Card Form */}
        <div className="bg-slate-900/90 backdrop-blur-xl border border-slate-800 rounded-2xl shadow-2xl p-6 sm:p-8">
          {/* Feedback Banners */}
          {errorMessage && (
            <div className="mb-5 p-3.5 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-400 text-sm flex items-start gap-2.5 animate-in fade-in duration-200">
              <AlertCircle className="w-5 h-5 shrink-0 mt-0.5" />
              <span>{errorMessage}</span>
            </div>
          )}

          {successMessage && (
            <div className="mb-5 p-4 rounded-xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-300 text-sm space-y-2 animate-in fade-in duration-200">
              <div className="flex items-start gap-2.5">
                <CheckCircle2 className="w-5 h-5 text-emerald-400 shrink-0 mt-0.5" />
                <span className="leading-relaxed">{successMessage}</span>
              </div>
              <p className="text-xs text-slate-400 pl-7">
                Mengarahkan ke halaman Login dalam beberapa detik...
              </p>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            {/* Input Token / OTP jika ada dan belum terautentikasi oleh sesi URL */}
            {(!isRecoverySessionActive || token) && (
              <div>
                <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-2">
                  Token Pemulihan / Kode OTP {urlToken ? '(Otomatis)' : '(Opsional jika link langsung)'}
                </label>
                <div className="relative">
                  <KeyRound className="w-4 h-4 text-slate-500 absolute left-3.5 top-1/2 -translate-y-1/2" />
                  <input
                    type="text"
                    value={token}
                    onChange={(e) => setToken(e.target.value)}
                    placeholder="Masukkan token / kode OTP dari email..."
                    className="w-full bg-slate-950/80 border border-slate-800 rounded-xl pl-10 pr-4 py-2.5 text-xs font-mono text-slate-200 placeholder-slate-600 focus:outline-none focus:border-indigo-500"
                  />
                </div>
              </div>
            )}

            {/* Input Password Baru */}
            <div>
              <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-2">
                Kata Sandi Baru
              </label>
              <div className="relative">
                <Lock className="w-4 h-4 text-slate-500 absolute left-3.5 top-1/2 -translate-y-1/2" />
                <input
                  type={showPassword ? 'text' : 'password'}
                  required
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="Minimal 8 karakter..."
                  className="w-full bg-slate-950/80 border border-slate-800 rounded-xl pl-10 pr-11 py-3 text-sm text-white placeholder-slate-600 focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 transition-all"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3.5 top-1/2 -translate-y-1/2 text-slate-500 hover:text-slate-300 transition"
                  aria-label={showPassword ? 'Sembunyikan password' : 'Tampilkan password'}
                >
                  {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>

              {/* Indikator Kekuatan Password */}
              {password && (
                <div className="mt-2.5 space-y-1.5 animate-in fade-in duration-150">
                  <div className="flex items-center justify-between text-[11px]">
                    <span className="text-slate-400">Kekuatan Sandi:</span>
                    <span className={`font-semibold ${strengthInfo.text}`}>{strengthInfo.label}</span>
                  </div>
                  <div className="grid grid-cols-3 gap-1.5 h-1.5">
                    <div
                      className={`h-full rounded-full transition-all duration-300 ${
                        strengthInfo.score >= 1 ? strengthInfo.color : 'bg-slate-800'
                      }`}
                    />
                    <div
                      className={`h-full rounded-full transition-all duration-300 ${
                        strengthInfo.score >= 2 ? strengthInfo.color : 'bg-slate-800'
                      }`}
                    />
                    <div
                      className={`h-full rounded-full transition-all duration-300 ${
                        strengthInfo.score >= 3 ? strengthInfo.color : 'bg-slate-800'
                      }`}
                    />
                  </div>
                </div>
              )}
            </div>

            {/* Konfirmasi Password Baru */}
            <div>
              <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-2">
                Konfirmasi Kata Sandi Baru
              </label>
              <div className="relative">
                <Lock className="w-4 h-4 text-slate-500 absolute left-3.5 top-1/2 -translate-y-1/2" />
                <input
                  type={showConfirmPassword ? 'text' : 'password'}
                  required
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  placeholder="Ulangi kata sandi baru..."
                  className="w-full bg-slate-950/80 border border-slate-800 rounded-xl pl-10 pr-11 py-3 text-sm text-white placeholder-slate-600 focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 transition-all"
                />
                <button
                  type="button"
                  onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                  className="absolute right-3.5 top-1/2 -translate-y-1/2 text-slate-500 hover:text-slate-300 transition"
                  aria-label={showConfirmPassword ? 'Sembunyikan konfirmasi password' : 'Tampilkan konfirmasi password'}
                >
                  {showConfirmPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
              {confirmPassword && password !== confirmPassword && (
                <p className="text-xs text-rose-400 mt-1.5 flex items-center gap-1">
                  <X className="w-3.5 h-3.5 shrink-0" />
                  <span>Konfirmasi kata sandi tidak cocok.</span>
                </p>
              )}
              {confirmPassword && password === confirmPassword && (
                <p className="text-xs text-emerald-400 mt-1.5 flex items-center gap-1">
                  <Check className="w-3.5 h-3.5 shrink-0" />
                  <span>Kata sandi cocok.</span>
                </p>
              )}
            </div>

            <button
              type="submit"
              disabled={loading || password.length < 8 || password !== confirmPassword}
              className="w-full py-3.5 px-4 bg-gradient-to-r from-emerald-600 to-teal-600 hover:from-emerald-500 hover:to-teal-500 text-white font-semibold rounded-xl shadow-lg shadow-emerald-600/30 hover:shadow-emerald-600/50 flex items-center justify-center gap-2 transition-all disabled:opacity-50 disabled:cursor-not-allowed mt-4"
            >
              {loading ? (
                <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
              ) : (
                <>
                  <span>Simpan Kata Sandi Baru</span>
                  <ArrowRight className="w-4 h-4" />
                </>
              )}
            </button>
          </form>

          {/* Navigation Links */}
          <div className="mt-6 pt-5 border-t border-slate-800/80 flex items-center justify-center text-xs">
            <Link
              href="/login"
              className="text-slate-400 hover:text-indigo-400 transition-colors font-medium"
            >
              Sudah ingat kata sandi? <strong>Masuk di sini</strong>
            </Link>
          </div>
        </div>

        {/* Footer */}
        <p className="text-center text-xs text-slate-600 mt-6">
          JFN Type Master &bull; Keamanan Akun Terproteksi
        </p>
      </div>
    </div>
  );
}

export default function ResetPasswordPage() {
  return (
    <Suspense
      fallback={
        <div className="min-h-screen bg-slate-950 flex flex-col items-center justify-center text-slate-400">
          <div className="w-8 h-8 border-2 border-indigo-500 border-t-transparent rounded-full animate-spin mb-3" />
          <p className="text-xs font-mono">Memuat halaman reset password...</p>
        </div>
      }
    >
      <ResetPasswordForm />
    </Suspense>
  );
}
