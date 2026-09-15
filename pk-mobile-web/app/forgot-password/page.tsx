'use client';

import { useState } from 'react';
import Link from 'next/link';
import { getSupabaseClient } from '@/lib/supabase';
import { Mail, ArrowLeft, ArrowRight, ShieldCheck, AlertCircle, CheckCircle2, KeyRound } from 'lucide-react';

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState('');
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const [submittedEmail, setSubmittedEmail] = useState('');

  const handleResetRequest = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage('');
    setSuccessMessage('');
    setLoading(true);

    try {
      const client = getSupabaseClient();
      const origin = typeof window !== 'undefined' ? window.location.origin : '';
      const redirectTo = `${origin}/reset-password`;

      const { error } = await client.auth.resetPasswordForEmail(email.trim(), {
        redirectTo,
      });

      if (error) {
        setErrorMessage(error.message || 'Gagal mengirim email reset kata sandi.');
      } else {
        setSubmittedEmail(email.trim());
        setSuccessMessage(
          `Tautan instruksi reset kata sandi / OTP telah dikirimkan ke ${email.trim()}. Silakan periksa kotak masuk (inbox) atau folder spam email Anda.`
        );
      }
    } catch (err: any) {
      setErrorMessage(err.message || 'Terjadi kesalahan saat memproses permintaan.');
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
              <KeyRound className="w-8 h-8" />
            </div>
          </div>
          <h1 className="text-2xl font-bold text-white tracking-tight">Lupa Kata Sandi</h1>
          <p className="text-sm text-slate-400 mt-1">
            Masukkan email terdaftar untuk menerima tautan reset password
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
            <div className="mb-5 p-4 rounded-xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-300 text-sm space-y-3 animate-in fade-in duration-200">
              <div className="flex items-start gap-2.5">
                <CheckCircle2 className="w-5 h-5 text-emerald-400 shrink-0 mt-0.5" />
                <span className="leading-relaxed">{successMessage}</span>
              </div>
              <div className="pt-2 border-t border-emerald-500/20 flex flex-col gap-2">
                <Link
                  href={`/reset-password?email=${encodeURIComponent(submittedEmail)}`}
                  className="text-xs font-semibold text-emerald-400 hover:text-emerald-300 underline flex items-center gap-1"
                >
                  <span>Sudah menerima token / link? Klik di sini untuk set password baru</span>
                  <ArrowRight className="w-3.5 h-3.5" />
                </Link>
              </div>
            </div>
          )}

          <form onSubmit={handleResetRequest} className="space-y-4">
            <div>
              <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-2">
                Email Akun CS / Admin
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
              <p className="text-xs text-slate-500 mt-1.5">
                Tautan konfirmasi pemulihan kata sandi akan dikirim ke alamat ini.
              </p>
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full py-3.5 px-4 bg-gradient-to-r from-indigo-600 to-indigo-500 hover:from-indigo-500 hover:to-indigo-400 text-white font-semibold rounded-xl shadow-lg shadow-indigo-600/30 hover:shadow-indigo-600/50 flex items-center justify-center gap-2 transition-all disabled:opacity-50 disabled:cursor-not-allowed mt-4"
            >
              {loading ? (
                <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
              ) : (
                <>
                  <span>Kirim Link Reset / OTP</span>
                  <ArrowRight className="w-4 h-4" />
                </>
              )}
            </button>
          </form>

          {/* Navigation Links */}
          <div className="mt-6 pt-5 border-t border-slate-800/80 flex items-center justify-between text-xs">
            <Link
              href="/login"
              className="text-slate-400 hover:text-indigo-400 flex items-center gap-1.5 transition-colors font-medium"
            >
              <ArrowLeft className="w-3.5 h-3.5" />
              <span>Kembali ke Login</span>
            </Link>

            <Link
              href="/reset-password"
              className="text-indigo-400 hover:text-indigo-300 transition-colors font-medium"
            >
              Set Password Baru ➔
            </Link>
          </div>
        </div>

        {/* Footer */}
        <p className="text-center text-xs text-slate-600 mt-6">
          JFN Type Master &bull; Layanan Pemulihan Akses Akun
        </p>
      </div>
    </div>
  );
}
