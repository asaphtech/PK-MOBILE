'use client';

import { useState, useEffect, useMemo, ChangeEvent } from 'react';
import { useRouter } from 'next/navigation';
import { getSupabaseClient, ShortcutItem } from '@/lib/supabase';
import {
  Search, Plus, Upload, LogOut, Edit2, Trash2, Zap, Space,
  FileSpreadsheet, AlertCircle, CheckCircle2, RefreshCw, X,
  Tag, MessageSquare, Copy, Check, ChevronDown, Filter,
  Layers, Smartphone, ExternalLink
} from 'lucide-react';

const CATEGORIES = [
  'Semua Kategori',
  'Rekening & Bank',
  'Informasi & FAQ',
  'Closing & Salam',
  'Komplain & Solusi',
  'Promo & Bonus',
  'Umum'
];

const FORM_CATEGORIES = [
  'Rekening & Bank',
  'Informasi & FAQ',
  'Closing & Salam',
  'Komplain & Solusi',
  'Promo & Bonus',
  'Umum'
];

export default function DashboardPage() {
  const router = useRouter();
  const [user, setUser] = useState<any>(null);
  const [shortcuts, setShortcuts] = useState<ShortcutItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);

  // Filter & Search state
  const [searchTerm, setSearchTerm] = useState('');
  const [categoryFilter, setCategoryFilter] = useState('Semua Kategori');
  const [modeFilter, setModeFilter] = useState('ALL');

  // Modal Add / Edit
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingItem, setEditingItem] = useState<ShortcutItem | null>(null);
  const [formTrigger, setFormTrigger] = useState('');
  const [formExpansion, setFormExpansion] = useState('');
  const [formCategory, setFormCategory] = useState('Umum');
  const [formMode, setFormMode] = useState<'INSTANT' | 'SPACE'>('INSTANT');
  const [formError, setFormError] = useState('');

  // Modal CSV Import
  const [isImportModalOpen, setIsImportModalOpen] = useState(false);
  const [csvPreview, setCsvPreview] = useState<ShortcutItem[]>([]);
  const [csvFileName, setCsvFileName] = useState('');
  const [importStatus, setImportStatus] = useState<{ success?: string; error?: string }>({});

  // Modal Delete Confirm
  const [deleteTarget, setDeleteTarget] = useState<ShortcutItem | null>(null);

  // Copy indicator state
  const [copiedId, setCopiedId] = useState<string | number | null>(null);

  // Notification Toast
  const [toast, setToast] = useState<{ type: 'success' | 'error'; message: string } | null>(null);

  const showToast = (type: 'success' | 'error', message: string) => {
    setToast({ type, message });
    setTimeout(() => setToast(null), 3500);
  };

  // Fetch Session & Shortcuts
  useEffect(() => {
    const init = async () => {
      try {
        const client = getSupabaseClient();
        const { data: { session } } = await client.auth.getSession();
        if (!session) {
          router.replace('/login');
          return;
        }
        setUser(session.user);
        await fetchShortcuts();
      } catch (err) {
        console.error('Init error:', err);
        router.replace('/login');
      }
    };
    init();
  }, [router]);

  const fetchShortcuts = async () => {
    setLoading(true);
    try {
      const client = getSupabaseClient();
      const { data, error } = await client
        .from('shortcuts')
        .select('*')
        .order('id', { ascending: false });

      if (error) {
        showToast('error', `Gagal memuat data: ${error.message}`);
      } else {
        const formatted: ShortcutItem[] = (data || []).map((item: any) => ({
          id: item.id,
          shortcut: item.shortcut || item.trigger_code || '',
          trigger_code: item.trigger_code || item.shortcut || '',
          expansion: item.expansion || item.expansion_text || '',
          expansion_text: item.expansion_text || item.expansion || '',
          category: item.category || 'Umum',
          expansion_mode: item.expansion_mode === 'SPACE' ? 'SPACE' : 'INSTANT',
          user_id: item.user_id,
          created_at: item.created_at
        }));
        setShortcuts(formatted);
      }
    } catch (err: any) {
      showToast('error', `Kesalahan koneksi: ${err.message}`);
    } finally {
      setLoading(false);
    }
  };

  const handleLogout = async () => {
    try {
      const client = getSupabaseClient();
      await client.auth.signOut();
      router.replace('/login');
    } catch (err) {
      router.replace('/login');
    }
  };

  // Open Modal Tambah
  const openAddModal = () => {
    setEditingItem(null);
    setFormTrigger('');
    setFormExpansion('');
    setFormCategory('Umum');
    setFormMode('INSTANT');
    setFormError('');
    setIsModalOpen(true);
  };

  // Open Modal Edit
  const openEditModal = (item: ShortcutItem) => {
    setEditingItem(item);
    setFormTrigger(item.shortcut || item.trigger_code || '');
    setFormExpansion(item.expansion || item.expansion_text || '');
    setFormCategory(item.category || 'Umum');
    setFormMode(item.expansion_mode === 'SPACE' ? 'SPACE' : 'INSTANT');
    setFormError('');
    setIsModalOpen(true);
  };

  // Simpan Form (Insert or Update)
  const handleSaveShortcut = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formTrigger.trim() || !formExpansion.trim()) {
      setFormError('Trigger code dan expansion text tidak boleh kosong!');
      return;
    }

    setActionLoading(true);
    setFormError('');

    try {
      const client = getSupabaseClient();
      const cleanTrigger = formTrigger.trim().toLowerCase();
      const cleanExpansion = formExpansion.trim();

      const payload = {
        trigger_code: cleanTrigger,
        expansion_text: cleanExpansion,
        category: formCategory,
        expansion_mode: formMode,
        user_id: user?.id,
      };

      if (editingItem?.id && (editingItem.trigger_code || editingItem.shortcut) && (editingItem.trigger_code || editingItem.shortcut).trim().toLowerCase() !== cleanTrigger) {
        // Jika trigger code diubah, hapus trigger lama
        await (client as any).from('shortcuts').delete().eq('id', editingItem.id);
      }

      // Upsert dengan strategi onConflict: user_id, trigger_code
      const { error } = await (client as any)
        .from('shortcuts')
        .upsert(payload, { onConflict: 'user_id, trigger_code' });

      if (error) throw error;

      showToast('success', `Shortcut "${cleanTrigger}" berhasil disimpan!`);

      setIsModalOpen(false);
      await fetchShortcuts();
    } catch (err: any) {
      setFormError(err.message || 'Gagal menyimpan shortcut ke Supabase.');
    } finally {
      setActionLoading(false);
    }
  };

  // Hapus Shortcut
  const handleDeleteShortcut = async () => {
    if (!deleteTarget?.id) return;
    setActionLoading(true);

    try {
      const client = getSupabaseClient();
      const { error } = await client
        .from('shortcuts')
        .delete()
        .eq('id', deleteTarget.id);

      if (error) throw error;

      showToast('success', `Shortcut "${deleteTarget.trigger_code || deleteTarget.shortcut || ''}" berhasil dihapus.`);
      setDeleteTarget(null);
      await fetchShortcuts();
    } catch (err: any) {
      showToast('error', `Gagal menghapus shortcut: ${err.message}`);
    } finally {
      setActionLoading(false);
    }
  };

  // Copy to clipboard helper
  const handleCopyTrigger = (text: string, id: any) => {
    navigator.clipboard.writeText(text);
    setCopiedId(id);
    setTimeout(() => setCopiedId(null), 1500);
  };

  // CSV Import handler
  const handleCsvFileChange = (e: ChangeEvent<HTMLInputElement>) => {
    setImportStatus({});
    const file = e.target.files?.[0];
    if (!file) return;

    setCsvFileName(file.name);
    const reader = new FileReader();

    reader.onload = (event) => {
      const content = event.target?.result as string;
      if (!content) return;

      const lines = content.split(/\r?\n/).filter(line => line.trim().length > 0);
      const parsed: ShortcutItem[] = [];

      lines.forEach((line, index) => {
        // Skip header jika ada baris pertama trigger/shortcut
        if (index === 0 && (line.toLowerCase().includes('shortcut') || line.toLowerCase().includes('trigger'))) {
          return;
        }

        // Parse line (comma atau semicolon separated, handle quotes)
        const parts = line.split(/[;,]/).map(p => p.trim().replace(/^["']|["']$/g, ''));
        if (parts.length >= 2 && parts[0] && parts[1]) {
          const trigger = parts[0];
          const expansion = parts[1];
          const cat = parts[2] || 'Umum';
          const modeRaw = (parts[3] || 'INSTANT').toUpperCase();
          const mode = modeRaw === 'SPACE' ? 'SPACE' : 'INSTANT';

          parsed.push({
            shortcut: trigger.toLowerCase(),
            expansion: expansion,
            category: cat,
            expansion_mode: mode,
          });
        }
      });

      setCsvPreview(parsed);
      if (parsed.length === 0) {
        setImportStatus({ error: 'Tidak ada baris shortcut yang valid terdeteksi pada file CSV ini.' });
      }
    };

    reader.readAsText(file);
  };

  const executeCsvBatchInsert = async () => {
    if (csvPreview.length === 0) return;
    setActionLoading(true);
    setImportStatus({});

    try {
      const client = getSupabaseClient();
      const payload = csvPreview.map(item => ({
        trigger_code: (item.trigger_code || item.shortcut || '').trim().toLowerCase(),
        expansion_text: (item.expansion_text || item.expansion || '').trim(),
        category: item.category || 'Umum',
        expansion_mode: item.expansion_mode || 'INSTANT',
        user_id: user?.id,
      }));

      const { error } = await (client as any)
        .from('shortcuts')
        .upsert(payload, { onConflict: 'user_id, trigger_code' });

      if (error) throw error;

      showToast('success', `Berhasil mengimpor ${csvPreview.length} shortcut dari CSV!`);
      setIsImportModalOpen(false);
      setCsvPreview([]);
      setCsvFileName('');
      await fetchShortcuts();
    } catch (err: any) {
      setImportStatus({ error: `Gagal impor massal: ${err.message}` });
    } finally {
      setActionLoading(false);
    }
  };

  // Filter & Search logic
  const filteredShortcuts = useMemo(() => {
    return shortcuts.filter(item => {
      const trigger = (item.shortcut || item.trigger_code || '').toLowerCase();
      const exp = (item.expansion || item.expansion_text || '').toLowerCase();
      const search = searchTerm.toLowerCase();

      const matchSearch = trigger.includes(search) || exp.includes(search);
      const matchCat = categoryFilter === 'Semua Kategori' || item.category === categoryFilter;
      const matchMode = modeFilter === 'ALL' || item.expansion_mode === modeFilter;

      return matchSearch && matchCat && matchMode;
    });
  }, [shortcuts, searchTerm, categoryFilter, modeFilter]);

  // Quick stats
  const stats = useMemo(() => {
    const total = shortcuts.length;
    const instantCount = shortcuts.filter(s => s.expansion_mode === 'INSTANT').length;
    const spaceCount = shortcuts.filter(s => s.expansion_mode === 'SPACE').length;
    const uniqueCats = new Set(shortcuts.map(s => s.category || 'Umum')).size;
    return { total, instantCount, spaceCount, uniqueCats };
  }, [shortcuts]);

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 selection:bg-indigo-500 selection:text-white">
      {/* Toast Notification */}
      {toast && (
        <div className={`fixed bottom-6 right-6 z-50 flex items-center gap-3 px-4 py-3 rounded-xl shadow-2xl border backdrop-blur-lg animate-in fade-in slide-in-from-bottom-5 transition-all ${
          toast.type === 'success'
            ? 'bg-emerald-950/90 border-emerald-500/40 text-emerald-200'
            : 'bg-rose-950/90 border-rose-500/40 text-rose-200'
        }`}>
          {toast.type === 'success' ? (
            <CheckCircle2 className="w-5 h-5 text-emerald-400 shrink-0" />
          ) : (
            <AlertCircle className="w-5 h-5 text-rose-400 shrink-0" />
          )}
          <span className="text-sm font-medium">{toast.message}</span>
        </div>
      )}

      {/* Top Navbar */}
      <header className="sticky top-0 z-30 bg-slate-900/80 backdrop-blur-xl border-b border-slate-800">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-indigo-600 to-emerald-400 p-0.5 shadow-md shadow-indigo-600/20">
              <div className="w-full h-full bg-slate-950 rounded-[10px] flex items-center justify-center text-emerald-400 font-bold text-xs">
                JFN
              </div>
            </div>
            <div>
              <div className="flex items-center gap-2.5">
                <span className="font-bold text-white text-base tracking-tight">JFN TYPE MASTER</span>
                <span className="text-[10px] uppercase font-bold tracking-wider px-2.5 py-0.5 rounded-full bg-emerald-500/10 text-emerald-400 border border-emerald-500/25 shadow-sm shadow-emerald-500/10">
                  JFN TYPE MASTER v1.0
                </span>
              </div>
              <p className="text-xs text-slate-400 hidden sm:block">CS Auto-Text &amp; Template Management System</p>
            </div>
          </div>

          <div className="flex items-center gap-3">
            <div className="text-right hidden md:block">
              <div className="text-xs font-medium text-slate-300">{user?.email || 'Customer Support'}</div>
              <div className="text-[11px] text-emerald-400 flex items-center justify-end gap-1">
                <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse" />
                <span>Supabase Live Sync</span>
              </div>
            </div>

            <button
              onClick={handleLogout}
              className="p-2 sm:px-3 sm:py-2 rounded-xl bg-slate-800/80 hover:bg-rose-500/20 text-slate-300 hover:text-rose-400 border border-slate-700/60 hover:border-rose-500/30 text-xs font-semibold flex items-center gap-1.5 transition-all"
              title="Keluar dari Akun"
            >
              <LogOut className="w-4 h-4" />
              <span className="hidden sm:inline">Keluar</span>
            </button>
          </div>
        </div>
      </header>

      {/* Main Container */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
        {/* Quick Stats Grid */}
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
          <div className="p-4 rounded-2xl bg-slate-900/60 border border-slate-800/80 backdrop-blur">
            <div className="flex items-center justify-between text-slate-400 mb-1">
              <span className="text-xs font-semibold uppercase tracking-wider">Total Shortcut</span>
              <Layers className="w-4 h-4 text-indigo-400" />
            </div>
            <div className="text-2xl font-bold text-white">{stats.total}</div>
            <div className="text-[11px] text-slate-500 mt-1">Template terdaftar</div>
          </div>

          <div className="p-4 rounded-2xl bg-slate-900/60 border border-slate-800/80 backdrop-blur">
            <div className="flex items-center justify-between text-slate-400 mb-1">
              <span className="text-xs font-semibold uppercase tracking-wider">Mode INSTANT</span>
              <Zap className="w-4 h-4 text-emerald-400" />
            </div>
            <div className="text-2xl font-bold text-emerald-400">{stats.instantCount}</div>
            <div className="text-[11px] text-slate-500 mt-1">Otomatis saat diketik</div>
          </div>

          <div className="p-4 rounded-2xl bg-slate-900/60 border border-slate-800/80 backdrop-blur">
            <div className="flex items-center justify-between text-slate-400 mb-1">
              <span className="text-xs font-semibold uppercase tracking-wider">Mode SPACE</span>
              <Space className="w-4 h-4 text-indigo-400" />
            </div>
            <div className="text-2xl font-bold text-indigo-400">{stats.spaceCount}</div>
            <div className="text-[11px] text-slate-500 mt-1">Setelah Spasi / Enter</div>
          </div>

          <div className="p-4 rounded-2xl bg-slate-900/60 border border-slate-800/80 backdrop-blur">
            <div className="flex items-center justify-between text-slate-400 mb-1">
              <span className="text-xs font-semibold uppercase tracking-wider">Kategori CS</span>
              <Tag className="w-4 h-4 text-amber-400" />
            </div>
            <div className="text-2xl font-bold text-amber-400">{stats.uniqueCats}</div>
            <div className="text-[11px] text-slate-500 mt-1">Grup template aktif</div>
          </div>
        </div>

        {/* Action Controls & Filters */}
        <div className="flex flex-col md:flex-row gap-3 items-stretch md:items-center justify-between bg-slate-900/80 p-4 rounded-2xl border border-slate-800">
          {/* Search Box */}
          <div className="relative flex-1 min-w-[240px]">
            <Search className="w-4 h-4 text-slate-500 absolute left-3.5 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              placeholder="Cari trigger (misal: //baca, /rek) atau isi teks..."
              className="w-full bg-slate-950/80 border border-slate-800 rounded-xl pl-10 pr-4 py-2.5 text-sm text-white placeholder-slate-500 focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 transition-all"
            />
          </div>

          <div className="flex flex-wrap items-center gap-2.5">
            {/* Filter Kategori */}
            <select
              value={categoryFilter}
              onChange={(e) => setCategoryFilter(e.target.value)}
              className="bg-slate-950/80 border border-slate-800 text-slate-300 text-xs rounded-xl px-3 py-2.5 focus:outline-none focus:border-indigo-500 transition-colors cursor-pointer"
            >
              {CATEGORIES.map(cat => (
                <option key={cat} value={cat}>{cat}</option>
              ))}
            </select>

            {/* Filter Mode */}
            <select
              value={modeFilter}
              onChange={(e) => setModeFilter(e.target.value)}
              className="bg-slate-950/80 border border-slate-800 text-slate-300 text-xs rounded-xl px-3 py-2.5 focus:outline-none focus:border-indigo-500 transition-colors cursor-pointer"
            >
              <option value="ALL">Semua Mode</option>
              <option value="INSTANT">INSTANT (Instan)</option>
              <option value="SPACE">SPACE (Spasi/Enter)</option>
            </select>

            {/* Tombol Refresh */}
            <button
              onClick={fetchShortcuts}
              disabled={loading}
              className="p-2.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 transition-colors disabled:opacity-50"
              title="Perbarui Data"
            >
              <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin text-indigo-400' : ''}`} />
            </button>

            {/* Tombol Import CSV */}
            <button
              onClick={() => { setCsvPreview([]); setCsvFileName(''); setImportStatus({}); setIsImportModalOpen(true); }}
              className="px-3.5 py-2.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-semibold flex items-center gap-2 border border-slate-700/80 transition-colors"
            >
              <Upload className="w-4 h-4 text-emerald-400" />
              <span>Import CSV</span>
            </button>

            {/* Tombol Tambah Baru */}
            <button
              onClick={openAddModal}
              className="px-4 py-2.5 rounded-xl bg-gradient-to-r from-indigo-600 to-indigo-500 hover:from-indigo-500 hover:to-indigo-400 text-white text-xs font-semibold flex items-center gap-2 shadow-lg shadow-indigo-600/30 transition-all"
            >
              <Plus className="w-4 h-4" />
              <span>Tambah Shortcut</span>
            </button>
          </div>
        </div>

        {/* Tabel Shortcut */}
        <div className="bg-slate-900/60 border border-slate-800/80 rounded-2xl overflow-hidden shadow-2xl backdrop-blur">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-slate-800 bg-slate-900/90 text-[11px] font-bold text-slate-400 uppercase tracking-wider">
                  <th className="py-3.5 px-4 w-44">Trigger Code</th>
                  <th className="py-3.5 px-4 min-w-[280px]">Expansion Text (Template Balasan)</th>
                  <th className="py-3.5 px-4 w-36">Kategori</th>
                  <th className="py-3.5 px-4 w-32">Mode</th>
                  <th className="py-3.5 px-4 w-28 text-right">Aksi</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60 text-sm">
                {loading ? (
                  <tr>
                    <td colSpan={5} className="py-16 text-center text-slate-400">
                      <div className="flex flex-col items-center gap-3">
                        <div className="w-8 h-8 border-2 border-indigo-500/30 border-t-indigo-500 rounded-full animate-spin" />
                        <span className="text-xs">Memuat daftar shortcut dari Supabase Cloud...</span>
                      </div>
                    </td>
                  </tr>
                ) : filteredShortcuts.length === 0 ? (
                  <tr>
                    <td colSpan={5} className="py-16 text-center text-slate-400">
                      <div className="flex flex-col items-center gap-2 max-w-sm mx-auto">
                        <MessageSquare className="w-10 h-10 text-slate-600 stroke-[1.5]" />
                        <p className="font-semibold text-white">Tidak ada shortcut ditemukan</p>
                        <p className="text-xs text-slate-500">
                          {searchTerm || categoryFilter !== 'Semua Kategori' || modeFilter !== 'ALL'
                            ? 'Coba sesuaikan kata kunci pencarian atau filter yang dipilih.'
                            : 'Belum ada data shortcut di Supabase. Klik "Tambah Shortcut" atau "Import CSV" untuk memulai.'}
                        </p>
                      </div>
                    </td>
                  </tr>
                ) : (
                  filteredShortcuts.map((item) => {
                    const trigger = item.shortcut || item.trigger_code || '';
                    const expansion = item.expansion || item.expansion_text || '';
                    const isInstant = item.expansion_mode === 'INSTANT';

                    return (
                      <tr key={item.id} className="hover:bg-slate-800/30 transition-colors group">
                        {/* Trigger Code */}
                        <td className="py-3.5 px-4 align-top">
                          <button
                            onClick={() => handleCopyTrigger(trigger, item.id)}
                            className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-slate-950/80 border border-slate-800 text-indigo-300 font-mono text-xs font-semibold hover:border-indigo-500/50 transition-all text-left"
                            title="Klik untuk menyalin trigger"
                          >
                            <span>{trigger}</span>
                            {copiedId === item.id ? (
                              <Check className="w-3.5 h-3.5 text-emerald-400 shrink-0" />
                            ) : (
                              <Copy className="w-3 h-3 text-slate-500 opacity-0 group-hover:opacity-100 transition-opacity shrink-0" />
                            )}
                          </button>
                        </td>

                        {/* Expansion Text */}
                        <td className="py-3.5 px-4 align-top">
                          <div className="text-slate-200 text-xs leading-relaxed whitespace-pre-wrap font-sans max-h-28 overflow-y-auto pr-2">
                            {expansion}
                          </div>
                        </td>

                        {/* Kategori */}
                        <td className="py-3.5 px-4 align-top">
                          <span className="inline-block px-2.5 py-1 rounded-full text-[11px] font-medium bg-slate-800/80 text-slate-300 border border-slate-700/60">
                            {item.category || 'Umum'}
                          </span>
                        </td>

                        {/* Mode Ekspansi */}
                        <td className="py-3.5 px-4 align-top">
                          {isInstant ? (
                            <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[11px] font-bold bg-emerald-500/10 text-emerald-400 border border-emerald-500/30">
                              <span className="w-1.5 h-1.5 rounded-full bg-emerald-400" />
                              <span>INSTANT</span>
                            </span>
                          ) : (
                            <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[11px] font-bold bg-indigo-500/10 text-indigo-400 border border-indigo-500/30">
                              <span className="w-1.5 h-1.5 rounded-full bg-indigo-400" />
                              <span>SPACE</span>
                            </span>
                          )}
                        </td>

                        {/* Aksi */}
                        <td className="py-3.5 px-4 align-top text-right">
                          <div className="inline-flex items-center gap-1">
                            <button
                              onClick={() => openEditModal(item)}
                              className="p-1.5 rounded-lg hover:bg-slate-800 text-slate-400 hover:text-indigo-400 transition-colors"
                              title="Edit Shortcut"
                            >
                              <Edit2 className="w-4 h-4" />
                            </button>
                            <button
                              onClick={() => setDeleteTarget(item)}
                              className="p-1.5 rounded-lg hover:bg-rose-500/10 text-slate-400 hover:text-rose-400 transition-colors"
                              title="Hapus Shortcut"
                            >
                              <Trash2 className="w-4 h-4" />
                            </button>
                          </div>
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>

          {/* Footer status */}
          <div className="py-3 px-4 bg-slate-900/80 border-t border-slate-800 flex items-center justify-between text-xs text-slate-400">
            <span>Menampilkan {filteredShortcuts.length} dari {shortcuts.length} shortcut</span>
            <span className="text-[11px] text-slate-500">JFN Type Master v1.0</span>
          </div>
        </div>
      </main>

      {/* MODAL: Tambah / Edit Shortcut */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm animate-in fade-in">
          <div className="w-full max-w-lg bg-slate-900 border border-slate-800 rounded-2xl shadow-2xl p-6 sm:p-7 relative">
            <button
              onClick={() => setIsModalOpen(false)}
              className="absolute right-5 top-5 p-1 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition-colors"
            >
              <X className="w-5 h-5" />
            </button>

            <h2 className="text-lg font-bold text-white mb-1">
              {editingItem ? '✏️ Edit Shortcut CS' : '✨ Tambah Shortcut Baru'}
            </h2>
            <p className="text-xs text-slate-400 mb-5">
              Atur kata kunci trigger pemicu dan template ekspansi teks balasan CS.
            </p>

            {formError && (
              <div className="mb-4 p-3 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-400 text-xs flex items-center gap-2">
                <AlertCircle className="w-4 h-4 shrink-0" />
                <span>{formError}</span>
              </div>
            )}

            <form onSubmit={handleSaveShortcut} className="space-y-4">
              {/* Trigger Code */}
              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1.5">
                  Trigger Code (Kata Kunci Pemicu)
                </label>
                <input
                  type="text"
                  required
                  value={formTrigger}
                  onChange={(e) => setFormTrigger(e.target.value)}
                  placeholder="Contoh: //baca, ///stok, [F2], /rek"
                  className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3.5 py-2.5 text-sm font-mono text-white placeholder-slate-600 focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 transition-all"
                />
                <span className="text-[11px] text-slate-500 mt-1 block">
                  Mendukung slash tunggal/ganda/tripel atau tombol fungsi PC format [F1]-[F12].
                </span>
              </div>

              {/* Expansion Text */}
              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1.5">
                  Expansion Text (Template Balasan)
                </label>
                <textarea
                  required
                  rows={5}
                  value={formExpansion}
                  onChange={(e) => setFormExpansion(e.target.value)}
                  placeholder="Ketikkan pesan template balasan di sini... Mendukung baris baru (Enter)."
                  className="w-full bg-slate-950 border border-slate-800 rounded-xl p-3.5 text-sm text-white placeholder-slate-600 focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 transition-all"
                />
              </div>

              {/* Kategori Dropdown */}
              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1.5">
                  Kategori Shortcut
                </label>
                <select
                  value={formCategory}
                  onChange={(e) => setFormCategory(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3.5 py-2.5 text-sm text-slate-200 focus:outline-none focus:border-indigo-500 transition-all cursor-pointer"
                >
                  {FORM_CATEGORIES.map(cat => (
                    <option key={cat} value={cat}>{cat}</option>
                  ))}
                </select>
              </div>

              {/* Mode Ekspansi Radio Group */}
              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-2">
                  Mode Ekspansi
                </label>
                <div className="grid grid-cols-2 gap-3">
                  <label
                    onClick={() => setFormMode('INSTANT')}
                    className={`p-3 rounded-xl border flex flex-col cursor-pointer transition-all ${
                      formMode === 'INSTANT'
                        ? 'bg-emerald-500/10 border-emerald-500/50 text-emerald-300 shadow-sm shadow-emerald-500/10'
                        : 'bg-slate-950 border-slate-800 text-slate-400 hover:border-slate-700'
                    }`}
                  >
                    <div className="flex items-center gap-2 font-bold text-xs">
                      <Zap className="w-3.5 h-3.5 text-emerald-400" />
                      <span>INSTANT (Instan)</span>
                    </div>
                    <span className="text-[11px] text-slate-500 mt-1">
                      Langsung ekspansi tanpa spasi
                    </span>
                  </label>

                  <label
                    onClick={() => setFormMode('SPACE')}
                    className={`p-3 rounded-xl border flex flex-col cursor-pointer transition-all ${
                      formMode === 'SPACE'
                        ? 'bg-indigo-500/10 border-indigo-500/50 text-indigo-300 shadow-sm shadow-indigo-500/10'
                        : 'bg-slate-950 border-slate-800 text-slate-400 hover:border-slate-700'
                    }`}
                  >
                    <div className="flex items-center gap-2 font-bold text-xs">
                      <Space className="w-3.5 h-3.5 text-indigo-400" />
                      <span>SPACE (Spasi)</span>
                    </div>
                    <span className="text-[11px] text-slate-500 mt-1">
                      Tunggu tombol Spasi / Enter
                    </span>
                  </label>
                </div>
              </div>

              {/* Modal Buttons */}
              <div className="flex items-center justify-end gap-3 pt-4 border-t border-slate-800">
                <button
                  type="button"
                  onClick={() => setIsModalOpen(false)}
                  className="px-4 py-2.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-semibold transition-colors"
                >
                  Batal
                </button>
                <button
                  type="submit"
                  disabled={actionLoading}
                  className="px-5 py-2.5 rounded-xl bg-gradient-to-r from-indigo-600 to-indigo-500 hover:from-indigo-500 hover:to-indigo-400 text-white text-xs font-semibold shadow-lg shadow-indigo-600/30 transition-all disabled:opacity-50 flex items-center gap-2"
                >
                  {actionLoading ? (
                    <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                  ) : (
                    <span>{editingItem ? 'Simpan Perubahan' : 'Tambahkan Shortcut'}</span>
                  )}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* MODAL: Import Massal CSV */}
      {isImportModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm animate-in fade-in">
          <div className="w-full max-w-xl bg-slate-900 border border-slate-800 rounded-2xl shadow-2xl p-6 sm:p-7 relative">
            <button
              onClick={() => setIsImportModalOpen(false)}
              className="absolute right-5 top-5 p-1 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition-colors"
            >
              <X className="w-5 h-5" />
            </button>

            <h2 className="text-lg font-bold text-white mb-1 flex items-center gap-2">
              <FileSpreadsheet className="w-5 h-5 text-emerald-400" />
              <span>Import Massal File CSV</span>
            </h2>
            <p className="text-xs text-slate-400 mb-5">
              Unggah berkas spreadsheet .csv untuk melakukan batch insert/upsert ke tabel Supabase.
            </p>

            {importStatus.error && (
              <div className="mb-4 p-3 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-400 text-xs flex items-center gap-2">
                <AlertCircle className="w-4 h-4 shrink-0" />
                <span>{importStatus.error}</span>
              </div>
            )}

            {/* File Picker Zone */}
            <div className="p-6 border-2 border-dashed border-slate-700/80 hover:border-indigo-500/60 rounded-2xl bg-slate-950/60 text-center transition-colors mb-4">
              <input
                type="file"
                accept=".csv,text/csv"
                onChange={handleCsvFileChange}
                className="hidden"
                id="csv_input_file"
              />
              <label htmlFor="csv_input_file" className="cursor-pointer flex flex-col items-center gap-2">
                <Upload className="w-8 h-8 text-indigo-400 animate-bounce" />
                <span className="text-sm font-semibold text-slate-200">
                  {csvFileName ? csvFileName : 'Pilih atau Tarik File .CSV ke sini'}
                </span>
                <span className="text-xs text-slate-500">
                  Format baris: <code>trigger, ekspansi, [kategori], [INSTANT/SPACE]</code>
                </span>
              </label>
            </div>

            {/* Pratinjau CSV */}
            {csvPreview.length > 0 && (
              <div className="space-y-2 mb-4">
                <div className="flex items-center justify-between text-xs text-emerald-400 font-semibold">
                  <span>Pratinjau: {csvPreview.length} shortcut siap diimpor</span>
                </div>
                <div className="max-h-40 overflow-y-auto bg-slate-950 border border-slate-800 rounded-xl p-2.5 text-xs divide-y divide-slate-800/80">
                  {csvPreview.slice(0, 10).map((row, idx) => (
                    <div key={idx} className="py-1.5 flex items-center justify-between gap-2">
                      <span className="font-mono text-indigo-300 font-bold">{row.shortcut}</span>
                      <span className="text-slate-400 truncate max-w-[200px]">{row.expansion}</span>
                      <span className="text-[10px] px-1.5 py-0.5 rounded bg-slate-800 text-slate-300">
                        {row.expansion_mode}
                      </span>
                    </div>
                  ))}
                  {csvPreview.length > 10 && (
                    <div className="py-1 text-center text-[11px] text-slate-500">
                      + {csvPreview.length - 10} baris lainnya...
                    </div>
                  )}
                </div>
              </div>
            )}

            {/* Modal Buttons */}
            <div className="flex items-center justify-end gap-3 pt-4 border-t border-slate-800">
              <button
                type="button"
                onClick={() => setIsImportModalOpen(false)}
                className="px-4 py-2.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-semibold transition-colors"
              >
                Batal
              </button>
              <button
                type="button"
                disabled={csvPreview.length === 0 || actionLoading}
                onClick={executeCsvBatchInsert}
                className="px-5 py-2.5 rounded-xl bg-gradient-to-r from-emerald-600 to-emerald-500 hover:from-emerald-500 hover:to-emerald-400 text-white text-xs font-semibold shadow-lg shadow-emerald-600/30 transition-all disabled:opacity-40 disabled:cursor-not-allowed flex items-center gap-2"
              >
                {actionLoading ? (
                  <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                ) : (
                  <span>Import {csvPreview.length > 0 ? `${csvPreview.length} Data` : 'Sekarang'}</span>
                )}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* MODAL: Konfirmasi Hapus */}
      {deleteTarget && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm animate-in fade-in">
          <div className="w-full max-w-sm bg-slate-900 border border-slate-800 rounded-2xl shadow-2xl p-6 relative text-center">
            <div className="w-12 h-12 rounded-full bg-rose-500/10 border border-rose-500/30 flex items-center justify-center text-rose-400 mx-auto mb-3">
              <Trash2 className="w-6 h-6" />
            </div>
            <h3 className="text-base font-bold text-white mb-1">Hapus Shortcut?</h3>
            <p className="text-xs text-slate-400 mb-6">
              Yakin ingin menghapus trigger <span className="font-mono text-rose-300 font-semibold">{deleteTarget.shortcut}</span>? Perubahan ini akan tersinkronisasi ke aplikasi mobile.
            </p>
            <div className="grid grid-cols-2 gap-3">
              <button
                type="button"
                onClick={() => setDeleteTarget(null)}
                className="py-2.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-semibold transition-colors"
              >
                Batal
              </button>
              <button
                type="button"
                disabled={actionLoading}
                onClick={handleDeleteShortcut}
                className="py-2.5 rounded-xl bg-rose-600 hover:bg-rose-500 text-white text-xs font-semibold shadow-lg shadow-rose-600/30 transition-all disabled:opacity-50"
              >
                {actionLoading ? 'Menghapus...' : 'Ya, Hapus'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
