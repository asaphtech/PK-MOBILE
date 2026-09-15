'use client';

import { useState, useEffect, useMemo, ChangeEvent } from 'react';
import { useRouter } from 'next/navigation';
import { getSupabaseClient, ShortcutItem } from '@/lib/supabase';
import {
  Search, Plus, Upload, LogOut, Edit2, Trash2, Zap, Space,
  FileSpreadsheet, AlertCircle, CheckCircle2, RefreshCw, X,
  Tag, MessageSquare, Copy, Check, ChevronDown, Filter,
  Layers, Smartphone, ExternalLink, AlertTriangle, FolderOpen, FileText
} from 'lucide-react';
import {
  parsePerfectKeyboardFile,
  PerfectKeyboardParseResult,
  ValidShortcut,
  FailedShortcut
} from '@/lib/perfectKeyboardParser';
import { ApkDownloadSection } from '@/components/ApkDownloadSection';

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

interface WarningRowProps {
  item: FailedShortcut;
  idx: number;
  existingTriggers: string[];
  copiedExpansionIdx: number | null;
  onCopyExpansion: (text: string, idx: number) => void;
  onResolve: (item: FailedShortcut, newTrigger: string) => void;
}

/**
 * Komponen baris warning interaktif (Interactive Inline Fixer)
 * Memungkinkan pengguna langsung menetapkan trigger baru bebas duplikat
 * dan memindahkan shortcut yang tadinya tidak didukung ke daftar Valid Shortcuts.
 */
function WarningRow({
  item,
  idx,
  existingTriggers,
  copiedExpansionIdx,
  onCopyExpansion,
  onResolve
}: WarningRowProps) {
  const [customTrigger, setCustomTrigger] = useState(item.suggestedTrigger || '');
  const [error, setError] = useState('');

  // Sederhanakan format trigger (tambahkan slash jika belum ada & ganti spasi dengan garis bawah)
  const formatTrigger = (val: string) => {
    let formatted = val.trim().replace(/\s+/g, '_');
    if (formatted && !formatted.startsWith('/') && !formatted.startsWith('!')) {
      formatted = '/' + formatted;
    }
    return formatted;
  };

  const handleChange = (val: string) => {
    const formatted = formatTrigger(val);
    setCustomTrigger(formatted);

    if (!formatted) {
      setError('');
      return;
    }

    // Cek duplikat real-time terhadap DB Supabase & Valid Shortcuts
    if (existingTriggers.includes(formatted.toLowerCase())) {
      setError('Trigger sudah digunakan!');
    } else {
      setError('');
    }
  };

  const handleApply = () => {
    if (!customTrigger || error) return;
    const formatted = formatTrigger(customTrigger);
    if (!formatted || existingTriggers.includes(formatted.toLowerCase())) {
      setError('Trigger sudah digunakan!');
      return;
    }
    onResolve(item, formatted);
  };

  const expansionText = item.expansion || item.rawExpansion || item.rawMessage || '';
  const suggestionCandidate = item.suggestedTrigger || `/m_${item.lineNum}`;

  return (
    <tr className="hover:bg-amber-950/20 transition-colors border-b border-amber-900/20 bg-slate-950/40">
      <td className="p-2.5 font-mono text-slate-400 align-top text-xs">#{item.lineNum}</td>
      <td className="p-2.5 align-top">
        <span className="inline-block px-2 py-1 bg-amber-950 text-amber-400 border border-amber-800/60 rounded font-mono text-xs font-bold">
          {item.rawTrigger || '[Hotkey PC]'}
        </span>
      </td>
      {/* Kolom Isi Pesan (Expansion) */}
      <td className="p-2.5 max-w-xs align-top">
        <div className="relative group bg-slate-900 p-2 rounded text-xs text-slate-300 whitespace-pre-wrap max-h-20 overflow-y-auto border border-slate-800 font-sans shadow-inner">
          <div className="pr-6 select-text">
            {expansionText || '(Teks Kosong)'}
          </div>
          {expansionText && (
            <button
              type="button"
              onClick={() => onCopyExpansion(expansionText, idx)}
              className="absolute top-1.5 right-1.5 p-1 rounded bg-slate-800 hover:bg-slate-700 text-slate-400 hover:text-amber-300 transition-colors border border-slate-700/60"
              title="Salin isi pesan"
            >
              {copiedExpansionIdx === idx ? (
                <Check className="w-3.5 h-3.5 text-emerald-400" />
              ) : (
                <Copy className="w-3.5 h-3.5" />
              )}
            </button>
          )}
        </div>
      </td>
      {/* Kolom Alasan Tidak Diproses */}
      <td className="p-2.5 text-xs text-amber-200/90 font-medium leading-relaxed align-top min-w-[170px]">
        {item.reason}
      </td>
      {/* Kolom Saran Tindakan / Interactive Inline Fixer */}
      <td className="p-2.5 align-top min-w-[240px]">
        <div className="flex flex-col gap-1.5">
          {/* Opsi Saran Cepat */}
          <div className="flex gap-1.5 items-center flex-wrap">
            <span className="text-[10px] text-slate-400 font-medium">Saran:</span>
            <button
              type="button"
              onClick={() => handleChange(suggestionCandidate)}
              className="text-[10px] bg-amber-950 hover:bg-amber-900 text-amber-300 px-2 py-0.5 rounded border border-amber-800/60 font-mono font-bold transition-colors"
              title="Gunakan saran trigger ini"
            >
              {suggestionCandidate}
            </button>
          </div>

          {/* Kolom Input Trigger Manual + Tombol Terapkan */}
          <div className="flex gap-1.5 items-center">
            <input
              type="text"
              value={customTrigger}
              onChange={(e) => handleChange(e.target.value)}
              placeholder="/trigger_baru"
              className={`text-xs bg-slate-900 px-2.5 py-1.5 rounded-lg border font-mono w-36 focus:outline-none transition-colors ${
                error
                  ? 'border-rose-500 text-rose-300 bg-rose-950/20'
                  : 'border-slate-700 text-emerald-400 focus:border-indigo-500'
              }`}
            />
            <button
              type="button"
              onClick={handleApply}
              disabled={!customTrigger || !!error}
              className="px-3 py-1.5 text-xs bg-emerald-600 hover:bg-emerald-500 disabled:bg-slate-800 disabled:text-slate-600 text-white rounded-lg font-medium transition shadow-sm disabled:cursor-not-allowed whitespace-nowrap"
            >
              Simpan ke Valid
            </button>
          </div>

          {/* Pesan Error Duplikat */}
          {error && (
            <span className="text-[11px] text-rose-400 font-semibold flex items-center gap-1">
              <span className="w-1.5 h-1.5 rounded-full bg-rose-400 inline-block shrink-0" />
              {error}
            </span>
          )}
        </div>
      </td>
    </tr>
  );
}

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

  // Modal Import Perfect Keyboard (.4pk, .kps, .txt)
  const [isPkModalOpen, setIsPkModalOpen] = useState(false);
  const [pkFileName, setPkFileName] = useState('');
  const [pkParseResult, setPkParseResult] = useState<PerfectKeyboardParseResult | null>(null);
  const [pkActiveTab, setPkActiveTab] = useState<'failed' | 'valid'>('failed');
  const [pkSearchQuery, setPkSearchQuery] = useState('');
  const [pkCategory, setPkCategory] = useState('Perfect Keyboard');
  const [pkMode, setPkMode] = useState<'INSTANT' | 'SPACE'>('INSTANT');
  const [pkStatus, setPkStatus] = useState<{ success?: string; error?: string }>({});
  const [isPkProcessing, setIsPkProcessing] = useState(false);

  // Modal Delete Confirm
  const [deleteTarget, setDeleteTarget] = useState<ShortcutItem | null>(null);

  // Copy indicator state
  const [copiedId, setCopiedId] = useState<string | number | null>(null);
  const [copiedExpansionIdx, setCopiedExpansionIdx] = useState<number | null>(null);

  // Notification Toast
  const [toast, setToast] = useState<{ type: 'success' | 'error'; message: string } | null>(null);

  // Anti-duplikat lookup: gabungan trigger di Supabase DB + trigger yang sudah ada di Valid Shortcuts
  const existingTriggers = useMemo(() => {
    const dbList = shortcuts.map(s => (s.shortcut || s.trigger_code || '').trim().toLowerCase()).filter(Boolean);
    const validList = (pkParseResult?.validShortcuts || []).map(s => s.trigger.trim().toLowerCase()).filter(Boolean);
    return Array.from(new Set([...dbList, ...validList]));
  }, [shortcuts, pkParseResult?.validShortcuts]);

  // Filter pencarian real-time untuk daftar tidak didukung (failedShortcuts)
  const filteredFailedList = useMemo(() => {
    if (!pkParseResult?.failedShortcuts) return [];
    const q = pkSearchQuery.trim().toLowerCase();
    if (!q) return pkParseResult.failedShortcuts;
    return pkParseResult.failedShortcuts.filter(item => {
      const triggerMatch = (item.rawTrigger || '').toLowerCase().includes(q);
      const textMatch = (item.expansion || item.rawExpansion || item.rawMessage || '').toLowerCase().includes(q);
      const reasonMatch = (item.reason || '').toLowerCase().includes(q);
      return triggerMatch || textMatch || reasonMatch;
    });
  }, [pkParseResult?.failedShortcuts, pkSearchQuery]);

  // Filter pencarian real-time untuk daftar shortcut siap diimpor (validShortcuts)
  const filteredValidList = useMemo(() => {
    if (!pkParseResult?.validShortcuts) return [];
    const q = pkSearchQuery.trim().toLowerCase();
    if (!q) return pkParseResult.validShortcuts;
    return pkParseResult.validShortcuts.filter(item => {
      const triggerMatch = (item.trigger || '').toLowerCase().includes(q);
      const textMatch = (item.expansion || '').toLowerCase().includes(q);
      return triggerMatch || textMatch;
    });
  }, [pkParseResult?.validShortcuts, pkSearchQuery]);

  // Resolusi inline failed shortcut -> pindahkan ke daftar valid
  const handleResolveFailedShortcut = (item: FailedShortcut, newTrigger: string) => {
    if (!pkParseResult) return;

    const cleanTrigger = newTrigger.trim();
    if (!cleanTrigger) return;

    const newValid: ValidShortcut = {
      lineNum: item.lineNum,
      trigger: cleanTrigger,
      expansion: item.expansion || item.rawExpansion || item.rawMessage || ''
    };

    const updatedFailed = pkParseResult.failedShortcuts.filter(f => f !== item);
    const updatedValid = [...pkParseResult.validShortcuts, newValid];

    setPkParseResult({
      totalParsed: pkParseResult.totalParsed,
      validShortcuts: updatedValid,
      failedShortcuts: updatedFailed
    });

    showToast('success', `Shortcut "${cleanTrigger}" berhasil ditetapkan dan dipindahkan ke daftar Valid Shortcuts!`);
  };

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

      // 1. Dapatkan user yang sedang aktif
      const { data: { user: currentUser } } = await client.auth.getUser();
      if (!currentUser) {
        setLoading(false);
        return;
      }
      setUser(currentUser);

      // 2. Tambahkan filter .eq("user_id", currentUser.id)
      const { data, error } = await client
        .from('shortcuts')
        .select('*')
        .eq('user_id', currentUser.id)
        .order('created_at', { ascending: false });

      if (error) {
        showToast('error', `Gagal memuat data: ${error.message}`);
        console.error('Gagal mengambil data:', error);
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
      console.error('Kesalahan koneksi:', err);
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
      const { data: { user: currentUser } } = await client.auth.getUser();
      const activeUserId = currentUser?.id || user?.id;
      if (!activeUserId) {
        throw new Error('Sesi login tidak valid atau telah kedaluwarsa. Silakan login kembali.');
      }

      const cleanTrigger = formTrigger.trim().toLowerCase();
      const cleanExpansion = formExpansion.trim();

      const payload = {
        trigger_code: cleanTrigger,
        expansion_text: cleanExpansion,
        category: formCategory,
        expansion_mode: formMode,
        user_id: activeUserId, // <--- ID user yang sedang aktif
      };

      const prevTrigger = (editingItem?.trigger_code || editingItem?.shortcut || '').trim().toLowerCase();
      if (editingItem?.id && prevTrigger && prevTrigger !== cleanTrigger) {
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

  const handleCopyExpansion = (text: string, idx: number) => {
    if (!text || text === '(Teks Kosong)') return;
    navigator.clipboard.writeText(text);
    setCopiedExpansionIdx(idx);
    showToast('success', 'Isi pesan berhasil disalin ke clipboard!');
    setTimeout(() => setCopiedExpansionIdx(null), 1500);
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

      // Dapatkan user yang sedang aktif dari session/auth
      const { data: { user: currentUser } } = await client.auth.getUser();
      const activeUserId = currentUser?.id || user?.id;
      if (!activeUserId) {
        throw new Error('Sesi login tidak valid atau telah kedaluwarsa. Silakan login kembali.');
      }

      const payload = csvPreview.map(item => ({
        trigger_code: (item.trigger_code || item.shortcut || '').trim().toLowerCase(),
        expansion_text: (item.expansion_text || item.expansion || '').trim(),
        category: item.category || 'Umum',
        expansion_mode: item.expansion_mode || 'INSTANT',
        user_id: activeUserId, // <--- ID user yang sedang aktif
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

  // =========================================================================
  // HANDLER IMPORT FILE PERFECT KEYBOARD (.4pk, .kps, .txt)
  // =========================================================================
  const handlePkFileChange = async (e: ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    setPkFileName(file.name);
    setIsPkProcessing(true);
    setPkStatus({});
    setPkParseResult(null);

    try {
      // 1. Baca file secara asynchronous menggunakan FileReader / file.text()
      const text = await file.text();

      // 2. Parsing dan klasifikasi trigger valid vs tidak didukung
      const result = parsePerfectKeyboardFile(text);
      setPkParseResult(result);

      // Default tab: sorot failed jika ada yang tidak didukung agar CS waspada
      if (result.failedShortcuts.length > 0) {
        setPkActiveTab('failed');
      } else {
        setPkActiveTab('valid');
      }

      if (result.totalParsed === 0) {
        setPkStatus({ error: 'Tidak ada data shortcut atau macro yang terdeteksi di dalam file ini.' });
      }
    } catch (err: any) {
      setPkStatus({ error: `Gagal membaca file: ${err.message || err}` });
    } finally {
      setIsPkProcessing(false);
      e.target.value = '';
    }
  };

  const executePkBatchInsert = async () => {
    if (!pkParseResult || pkParseResult.validShortcuts.length === 0) return;
    setActionLoading(true);
    setPkStatus({});

    try {
      const client = getSupabaseClient();

      // Dapatkan user yang sedang aktif dari session/auth
      const { data: { user: currentUser } } = await client.auth.getUser();
      const activeUserId = currentUser?.id || user?.id;
      if (!activeUserId) {
        throw new Error('Sesi login tidak valid atau telah kedaluwarsa. Silakan login kembali.');
      }

      const payload = pkParseResult.validShortcuts.map(item => ({
        trigger_code: item.trigger.trim().toLowerCase(),
        expansion_text: item.expansion.trim(),
        category: pkCategory || 'Perfect Keyboard',
        expansion_mode: pkMode,
        user_id: activeUserId, // <--- ID user yang sedang aktif
      }));

      const { error } = await (client as any)
        .from('shortcuts')
        .upsert(payload, { onConflict: 'user_id, trigger_code' });

      if (error) throw error;

      showToast(
        'success',
        `Berhasil mengimpor ${pkParseResult.validShortcuts.length} shortcut Perfect Keyboard ke database!`
      );
      setPkStatus({
        success: `Berhasil mengimpor ${pkParseResult.validShortcuts.length} shortcut ke Supabase Cloud. Seluruh shortcut ini siap disinkronkan ke HP Android CS!`
      });
      await fetchShortcuts();
    } catch (err: any) {
      setPkStatus({ error: `Gagal menyimpan ke Supabase: ${err.message}` });
    } finally {
      setActionLoading(false);
    }
  };

  const resetPkModal = () => {
    setIsPkModalOpen(false);
    setPkParseResult(null);
    setPkFileName('');
    setPkStatus({});
    setPkSearchQuery('');
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
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 min-h-16 py-2 flex items-center justify-between">
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
            {/* Download APK & Panduan Instalasi HP */}
            <ApkDownloadSection />

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

            {/* Tombol Impor File Perfect Keyboard (.4pk) */}
            <button
              onClick={() => {
                setPkStatus({});
                setIsPkModalOpen(true);
              }}
              className="px-3.5 py-2.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-indigo-300 text-xs font-semibold flex items-center gap-2 border border-slate-700/80 transition-colors shadow-sm"
              title="Impor template dari file Perfect Keyboard (.4pk, .kps, .txt)"
            >
              <span>📁 Impor File Perfect Keyboard (.4pk)</span>
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

      {/* MODAL: Import File Perfect Keyboard (.4pk, .kps, .txt) */}
      {isPkModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 bg-black/75 backdrop-blur-md animate-in fade-in">
          <div className="w-full max-w-5xl max-h-[92vh] flex flex-col bg-slate-900 border border-slate-800 rounded-2xl shadow-2xl p-5 sm:p-7 relative">
            {/* Tombol Tutup */}
            <button
              onClick={resetPkModal}
              className="absolute right-5 top-5 p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition-colors"
            >
              <X className="w-5 h-5" />
            </button>

            {/* Header Modal */}
            <div className="mb-4 pr-8">
              <h2 className="text-lg font-bold text-white flex items-center gap-2">
                <span className="text-xl">📁</span>
                <span>Impor File Perfect Keyboard (.4pk / .kps / .txt)</span>
              </h2>
              <p className="text-xs text-slate-400 mt-1">
                Parsing otomatis template Perfect Keyboard dengan deteksi proteksi tombol fisik PC untuk keyboard HP Android.
              </p>
            </div>

            {/* Status Alert (Error / Success) */}
            {pkStatus.error && (
              <div className="mb-4 p-3 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-400 text-xs flex items-center gap-2 shrink-0">
                <AlertCircle className="w-4 h-4 shrink-0" />
                <span>{pkStatus.error}</span>
              </div>
            )}
            {pkStatus.success && (
              <div className="mb-4 p-3 rounded-xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 text-xs flex items-center gap-2 shrink-0">
                <CheckCircle2 className="w-4 h-4 shrink-0" />
                <span>{pkStatus.success}</span>
              </div>
            )}

            {/* State A: Belum Memilih File */}
            {!pkParseResult ? (
              <div className="p-10 border-2 border-dashed border-slate-700/80 hover:border-indigo-500/60 rounded-2xl bg-slate-950/60 text-center transition-all flex flex-col items-center justify-center my-6">
                <input
                  type="file"
                  accept=".4pk,.kps,.txt"
                  onChange={handlePkFileChange}
                  className="hidden"
                  id="pk_modal_file_input"
                />
                <label htmlFor="pk_modal_file_input" className="cursor-pointer flex flex-col items-center gap-3">
                  <div className="w-16 h-16 rounded-2xl bg-indigo-500/10 border border-indigo-500/20 flex items-center justify-center text-indigo-400 hover:scale-105 transition-transform">
                    {isPkProcessing ? (
                      <RefreshCw className="w-8 h-8 animate-spin text-indigo-400" />
                    ) : (
                      <FolderOpen className="w-8 h-8" />
                    )}
                  </div>
                  <div>
                    <span className="text-sm font-semibold text-slate-200 block">
                      {isPkProcessing ? 'Sedang membaca dan menganalisis berkas...' : 'Pilih atau Tarik File Perfect Keyboard (.4pk, .kps, .txt)'}
                    </span>
                    <span className="text-xs text-slate-500 block mt-1">
                      Mendukung format Macro Text Wizard (MTW), XML, key-value, dan delimited text.
                    </span>
                  </div>
                  <span className="px-5 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-semibold shadow-lg shadow-indigo-600/30 transition-colors mt-2 inline-flex items-center gap-2">
                    <Upload className="w-4 h-4" />
                    <span>Pilih Berkas dari Komputer</span>
                  </span>
                </label>
              </div>
            ) : (
              /* State B: Hasil Parsing Tersedia */
              <div className="flex-1 overflow-y-auto pr-1 space-y-4 min-h-0">
                {/* File info bar */}
                <div className="flex flex-wrap items-center justify-between p-3 rounded-xl bg-slate-950/70 border border-slate-800 text-xs">
                  <div className="flex items-center gap-2">
                    <FileText className="w-4 h-4 text-indigo-400" />
                    <span className="font-semibold text-slate-200">{pkFileName}</span>
                  </div>
                  <label htmlFor="pk_modal_file_change" className="text-indigo-400 hover:text-indigo-300 font-medium cursor-pointer text-xs underline">
                    Ganti File
                    <input
                      type="file"
                      accept=".4pk,.kps,.txt"
                      onChange={handlePkFileChange}
                      className="hidden"
                      id="pk_modal_file_change"
                    />
                  </label>
                </div>

                {/* 4. Kartu Ringkasan Status Impor */}
                <div className="grid grid-cols-3 gap-3">
                  <div className="p-3.5 rounded-xl bg-slate-950/80 border border-slate-800">
                    <div className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider">Total Terbaca</div>
                    <div className="text-2xl font-bold text-white mt-0.5">{pkParseResult.totalParsed}</div>
                    <div className="text-[11px] text-slate-500 mt-0.5">Macro dalam berkas</div>
                  </div>

                  <div className="p-3.5 rounded-xl bg-emerald-950/30 border border-emerald-500/30">
                    <div className="text-[11px] font-semibold text-emerald-400 uppercase tracking-wider flex items-center gap-1">
                      <CheckCircle2 className="w-3.5 h-3.5" />
                      <span>Valid (HP Ready)</span>
                    </div>
                    <div className="text-2xl font-bold text-emerald-400 mt-0.5">{pkParseResult.validShortcuts.length}</div>
                    <div className="text-[11px] text-emerald-500/80 mt-0.5">Siap diimpor ke DB</div>
                  </div>

                  <div className="p-3.5 rounded-xl bg-amber-950/30 border border-amber-500/30">
                    <div className="text-[11px] font-semibold text-amber-400 uppercase tracking-wider flex items-center gap-1">
                      <AlertTriangle className="w-3.5 h-3.5" />
                      <span>Tidak Didukung</span>
                    </div>
                    <div className="text-2xl font-bold text-amber-400 mt-0.5">{pkParseResult.failedShortcuts.length}</div>
                    <div className="text-[11px] text-amber-500/80 mt-0.5">Tombol PC / Tidak valid</div>
                  </div>
                </div>

                {/* Search Bar Real-Time */}
                <div className="relative">
                  <input
                    type="text"
                    value={pkSearchQuery}
                    onChange={(e) => setPkSearchQuery(e.target.value)}
                    placeholder="🔍 Cari berdasarkan kode trigger atau isi teks shortcut..."
                    className="w-full bg-slate-900 border border-slate-700/80 rounded-lg px-4 py-2.5 text-xs text-slate-200 placeholder:text-slate-500 focus:outline-none focus:border-emerald-500 font-sans shadow-inner transition-colors"
                  />
                  {pkSearchQuery && (
                    <button
                      type="button"
                      onClick={() => setPkSearchQuery('')}
                      className="absolute right-3 top-2 text-xs text-slate-400 hover:text-slate-200 bg-slate-800 hover:bg-slate-700 px-2 py-1 rounded transition-colors"
                    >
                      ✕ Clear
                    </button>
                  )}
                </div>

                {/* Tab Navigation */}
                <div className="flex items-center gap-2 border-b border-slate-800 pb-2">
                  <button
                    type="button"
                    onClick={() => setPkActiveTab('failed')}
                    className={`px-3.5 py-1.5 rounded-xl text-xs font-semibold flex items-center gap-2 transition-colors ${
                      pkActiveTab === 'failed'
                        ? 'bg-amber-500/20 text-amber-300 border border-amber-500/40'
                        : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800'
                    }`}
                  >
                    <AlertTriangle className="w-3.5 h-3.5" />
                    <span>
                      Daftar Tidak Didukung ({pkSearchQuery ? `${filteredFailedList.length} / ` : ''}{pkParseResult.failedShortcuts.length})
                    </span>
                  </button>

                  <button
                    type="button"
                    onClick={() => setPkActiveTab('valid')}
                    className={`px-3.5 py-1.5 rounded-xl text-xs font-semibold flex items-center gap-2 transition-colors ${
                      pkActiveTab === 'valid'
                        ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/40'
                        : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800'
                    }`}
                  >
                    <CheckCircle2 className="w-3.5 h-3.5" />
                    <span>
                      Shortcut Siap Diimpor ({pkSearchQuery ? `${filteredValidList.length} / ` : ''}{pkParseResult.validShortcuts.length})
                    </span>
                  </button>
                </div>

                {/* TAB 1: TABEL LAPORAN BERWARNA (WARNING / AMBER) */}
                {pkActiveTab === 'failed' && (
                  <div className="space-y-3">
                    {filteredFailedList.length === 0 ? (
                      <div className="p-8 text-center rounded-xl bg-slate-950/60 border border-slate-800 text-xs text-slate-400">
                        {pkSearchQuery ? (
                          <>
                            <Search className="w-7 h-7 text-slate-500 mx-auto mb-2" />
                            Tidak ada shortcut tidak didukung yang cocok dengan kata kunci &quot;{pkSearchQuery}&quot;.
                          </>
                        ) : (
                          <>
                            <CheckCircle2 className="w-7 h-7 text-emerald-400 mx-auto mb-2" />
                            Semua shortcut dalam file ini valid dan dapat digunakan di keyboard HP Android!
                          </>
                        )}
                      </div>
                    ) : (
                      <div className="rounded-xl border border-amber-500/30 bg-amber-950/10 overflow-hidden">
                        <div className="p-3.5 bg-amber-500/10 border-b border-amber-500/20 text-xs text-amber-300 flex items-start gap-2.5">
                          <AlertTriangle className="w-4 h-4 shrink-0 mt-0.5 text-amber-400" />
                          <div>
                            <span className="font-bold block">Peringatan Kompatibilitas Keyboard HP Android:</span>
                            <span className="text-amber-200/80 text-[11px]">
                              Shortcut di bawah ini disisihkan otomatis karena menggunakan tombol fisik PC (Function keys F1-F12, Enter, Esc), kombinasi tombol (Ctrl, Alt), atau trigger duplikat.
                            </span>
                          </div>
                        </div>
                        <div className="max-h-72 overflow-y-auto border border-amber-900/40 rounded-lg">
                          <table className="w-full text-sm text-left border-collapse">
                            <thead className="bg-amber-950/60 text-amber-300 uppercase text-xs sticky top-0 backdrop-blur z-10">
                              <tr>
                                <th className="p-2.5 border-b border-amber-900/40 w-16">BARIS</th>
                                <th className="p-2.5 border-b border-amber-900/40 w-36">TRIGGER ASLI (.4PK)</th>
                                <th className="p-2.5 border-b border-amber-900/40 min-w-[200px]">ISI PESAN (EXPANSION)</th>
                                <th className="p-2.5 border-b border-amber-900/40 min-w-[170px]">ALASAN TIDAK DIPROSES</th>
                                <th className="p-2.5 border-b border-amber-900/40 min-w-[240px]">SARAN TINDAKAN / RESOLUSI</th>
                              </tr>
                            </thead>
                            <tbody className="divide-y divide-amber-900/20 bg-slate-950/40 text-xs">
                              {filteredFailedList.map((item, idx) => (
                                <WarningRow
                                  key={`${item.lineNum}-${idx}`}
                                  item={item}
                                  idx={idx}
                                  existingTriggers={existingTriggers}
                                  copiedExpansionIdx={copiedExpansionIdx}
                                  onCopyExpansion={handleCopyExpansion}
                                  onResolve={handleResolveFailedShortcut}
                                />
                              ))}
                            </tbody>
                          </table>
                        </div>
                      </div>
                    )}
                  </div>
                )}

                {/* TAB 2: SHORTCUT LOLOS VALIDASI */}
                {pkActiveTab === 'valid' && (
                  <div className="space-y-3">
                    {filteredValidList.length === 0 ? (
                      <div className="p-8 text-center rounded-xl bg-slate-950/60 border border-slate-800 text-xs text-slate-400">
                        {pkSearchQuery ? (
                          <>
                            <Search className="w-7 h-7 text-slate-500 mx-auto mb-2" />
                            Tidak ada shortcut siap diimpor yang cocok dengan kata kunci &quot;{pkSearchQuery}&quot;.
                          </>
                        ) : (
                          'Tidak ada shortcut yang lolos validasi pada file ini. Silakan sesuaikan trigger pada file asal atau periksa daftar tidak didukung.'
                        )}
                      </div>
                    ) : (
                      <>
                        {/* Pengaturan Kategori & Mode */}
                        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 p-3.5 rounded-xl bg-slate-950 border border-slate-800 text-xs">
                          <div>
                            <label className="block text-[11px] font-semibold text-slate-400 mb-1.5">
                              Kategori untuk Data yang Diimpor:
                            </label>
                            <select
                              value={pkCategory}
                              onChange={(e) => setPkCategory(e.target.value)}
                              className="w-full bg-slate-900 border border-slate-700 text-slate-200 rounded-lg px-3 py-2 focus:outline-none focus:border-indigo-500"
                            >
                              <option value="Perfect Keyboard">Perfect Keyboard (Khusus)</option>
                              {FORM_CATEGORIES.map(c => (
                                <option key={c} value={c}>{c}</option>
                              ))}
                            </select>
                          </div>
                          <div>
                            <label className="block text-[11px] font-semibold text-slate-400 mb-1.5">
                              Mode Ekspansi Auto-Text:
                            </label>
                            <select
                              value={pkMode}
                              onChange={(e) => setPkMode(e.target.value as any)}
                              className="w-full bg-slate-900 border border-slate-700 text-slate-200 rounded-lg px-3 py-2 focus:outline-none focus:border-indigo-500"
                            >
                              <option value="INSTANT">INSTANT (Langsung ekspansi saat diketik)</option>
                              <option value="SPACE">SPACE (Ditahan hingga tombol Spasi/Enter ditekan)</option>
                            </select>
                          </div>
                        </div>

                        {/* Tabel Preview Valid */}
                        <div className="rounded-xl border border-slate-800 bg-slate-950/60 overflow-hidden">
                          <div className="max-h-56 overflow-y-auto">
                            <table className="w-full text-left border-collapse text-xs">
                              <thead>
                                <tr className="border-b border-slate-800 bg-slate-900/90 text-[11px] font-bold text-slate-400 uppercase tracking-wider sticky top-0 backdrop-blur">
                                  <th className="py-2.5 px-3 w-16">Baris</th>
                                  <th className="py-2.5 px-3 w-36">Trigger Valid</th>
                                  <th className="py-2.5 px-3">Isi Teks Balasan (Expansion)</th>
                                </tr>
                              </thead>
                              <tbody className="divide-y divide-slate-800/60">
                                {filteredValidList.map((item, idx) => (
                                  <tr key={idx} className="hover:bg-slate-900/40 transition-colors">
                                    <td className="py-2 px-3 font-mono text-slate-500">#{item.lineNum}</td>
                                    <td className="py-2 px-3 font-mono text-indigo-400 font-bold">{item.trigger}</td>
                                    <td className="py-2 px-3 text-slate-300 truncate max-w-[340px]">{item.expansion}</td>
                                  </tr>
                                ))}
                              </tbody>
                            </table>
                          </div>
                        </div>
                      </>
                    )}
                  </div>
                )}
              </div>
            )}

            {/* Footer Buttons */}
            <div className="flex items-center justify-between gap-3 pt-4 border-t border-slate-800 mt-4 shrink-0">
              <button
                type="button"
                onClick={resetPkModal}
                className="px-4 py-2.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-semibold transition-colors"
              >
                {pkStatus.success ? 'Tutup' : 'Batal'}
              </button>

              {pkParseResult && pkParseResult.validShortcuts.length > 0 && !pkStatus.success && (
                <button
                  type="button"
                  disabled={actionLoading}
                  onClick={executePkBatchInsert}
                  className="px-5 py-2.5 rounded-xl bg-gradient-to-r from-emerald-600 to-emerald-500 hover:from-emerald-500 hover:to-emerald-400 text-white text-xs font-semibold shadow-lg shadow-emerald-600/30 transition-all disabled:opacity-40 disabled:cursor-not-allowed flex items-center gap-2"
                >
                  {actionLoading ? (
                    <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                  ) : (
                    <span>Simpan {pkParseResult.validShortcuts.length} Shortcut Valid ke Supabase</span>
                  )}
                </button>
              )}
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
