'use client';

import { useState, useEffect, useMemo, ChangeEvent } from 'react';
import { useRouter } from 'next/navigation';
import { getSupabaseClient, ShortcutItem, PresetItem, FeedbackItem } from '@/lib/supabase';
import {
  Search, Plus, Upload, LogOut, Edit2, Trash2, Zap, Space,
  FileSpreadsheet, AlertCircle, CheckCircle2, RefreshCw, X,
  Tag, MessageSquare, Copy, Check, ChevronDown, Filter,
  Layers, Smartphone, ExternalLink, AlertTriangle, FolderOpen, FileText,
  FolderPlus, Star, CheckCircle, Download, MessageSquarePlus, Send, History, FileDown
} from 'lucide-react';
import {
  parsePerfectKeyboardFile,
  cleanMacroText,
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

  const expansionText = cleanMacroText(item.expansion || item.rawExpansion || item.rawMessage || '');
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

  // Preset State (Multi-Preset & Isolasi Berkas)
  const [presets, setPresets] = useState<PresetItem[]>([]);
  const [selectedPresetId, setSelectedPresetId] = useState<string>('');
  const [isPresetLoading, setIsPresetLoading] = useState(true);

  // Preset Modals
  const [isNewPresetModalOpen, setIsNewPresetModalOpen] = useState(false);
  const [newPresetName, setNewPresetName] = useState('');
  const [isRenameModalOpen, setIsRenameModalOpen] = useState(false);
  const [renamePresetName, setRenamePresetName] = useState('');
  const [isDeletePresetModalOpen, setIsDeletePresetModalOpen] = useState(false);

  // Import Destination State (untuk isolasi impor CSV & PK)
  const [importDestination, setImportDestination] = useState<'new_preset' | 'current_preset'>('new_preset');
  const [importPresetName, setImportPresetName] = useState('');
  const [importSetAsActive, setImportSetAsActive] = useState(true);

  // Modal Add / Edit
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingItem, setEditingItem] = useState<ShortcutItem | null>(null);
  const [formTrigger, setFormTrigger] = useState('');
  const [formExpansion, setFormExpansion] = useState('');
  const [formCategory, setFormCategory] = useState('Umum');
  const [formMode, setFormMode] = useState<'INSTANT' | 'SPACE'>('SPACE'); // Default: Non-Instant / Space
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
  const [pkMode, setPkMode] = useState<'INSTANT' | 'SPACE'>('SPACE'); // Default: Non-Instant / Space
  const [pkStatus, setPkStatus] = useState<{ success?: string; error?: string }>({});
  const [isPkProcessing, setIsPkProcessing] = useState(false);

  // Modal Delete Confirm
  const [deleteTarget, setDeleteTarget] = useState<ShortcutItem | null>(null);

  // Copy indicator state
  const [copiedId, setCopiedId] = useState<string | number | null>(null);
  const [copiedExpansionIdx, setCopiedExpansionIdx] = useState<number | null>(null);

  // Notification Toast
  const [toast, setToast] = useState<{ type: 'success' | 'error'; message: string } | null>(null);

  // Modal Unduhan File Preset
  const [isDownloadModalOpen, setIsDownloadModalOpen] = useState(false);

  // Modal Saran & Masukan (Feedback)
  const [isFeedbackModalOpen, setIsFeedbackModalOpen] = useState(false);
  const [feedbackCategory, setFeedbackCategory] = useState<'Permintaan Fitur' | 'Bug / Error' | 'Usulan Shortcut Baru' | 'Performa / Lainnya'>('Permintaan Fitur');
  const [feedbackMessage, setFeedbackMessage] = useState('');
  const [feedbacks, setFeedbacks] = useState<FeedbackItem[]>([]);
  const [isFeedbackLoading, setIsFeedbackLoading] = useState(false);

  // Live Sync Status
  const [lastSyncTime, setLastSyncTime] = useState<string>('');

  // Preset saat ini yang sedang dipilih
  const currentPreset = useMemo(() => {
    return presets.find(p => p.id === selectedPresetId) || presets[0] || null;
  }, [presets, selectedPresetId]);

  // Anti-duplikat lookup: gabungan trigger di Supabase DB + trigger yang sudah ada di Valid Shortcuts
  const existingTriggers = useMemo(() => {
    const dbList = shortcuts
      .filter(s => !selectedPresetId || s.preset_id === selectedPresetId)
      .map(s => (s.shortcut || s.trigger_code || '').trim().toLowerCase())
      .filter(Boolean);
    const validList = (pkParseResult?.validShortcuts || []).map(s => s.trigger.trim().toLowerCase()).filter(Boolean);
    return Array.from(new Set([...dbList, ...validList]));
  }, [shortcuts, selectedPresetId, pkParseResult?.validShortcuts]);

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

  // Helper escape karakter XML
  const escapeXmlAttr = (str: string) => {
    return (str || '')
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&apos;');
  };

  // Unduh Preset dalam format XML (kompatibel Android dengan CDATA), JSON, atau CSV
  const handleDownloadPreset = async (targetPreset: PresetItem, format: 'xml' | 'json' | 'csv') => {
    setActionLoading(true);
    try {
      const client = getSupabaseClient();
      const { data, error } = await client
        .from('shortcuts')
        .select('*')
        .eq('preset_id', targetPreset.id);

      if (error) throw error;

      const items: ShortcutItem[] = (data || []).map((item: any) => ({
        id: item.id,
        preset_id: item.preset_id,
        shortcut: item.shortcut || item.trigger_code || '',
        trigger_code: item.trigger_code || item.shortcut || '',
        expansion: item.expansion || item.expansion_text || '',
        expansion_text: item.expansion_text || item.expansion || '',
        category: item.category || 'Umum',
        expansion_mode: item.expansion_mode === 'INSTANT' ? 'INSTANT' : 'SPACE',
      }));

      let content = '';
      let mimeType = 'text/plain';

      if (format === 'xml') {
        content = `<?xml version="1.0" encoding="utf-8"?>\n`;
        content += `<shortcuts preset="${escapeXmlAttr(targetPreset.name)}" count="${items.length}" exported="${new Date().toISOString()}">\n`;
        for (const s of items) {
          const tr = s.shortcut || s.trigger_code || '';
          const exp = s.expansion || s.expansion_text || '';
          const cat = s.category || 'Umum';
          const mode = s.expansion_mode || 'SPACE';
          content += `  <shortcut>\n`;
          content += `    <trigger><![CDATA[${tr}]]></trigger>\n`;
          content += `    <expansion><![CDATA[${exp}]]></expansion>\n`;
          content += `    <mode>${mode}</mode>\n`;
          content += `    <category><![CDATA[${cat}]]></category>\n`;
          content += `  </shortcut>\n`;
        }
        content += `</shortcuts>\n`;
        mimeType = 'application/xml';
      } else if (format === 'json') {
        content = JSON.stringify({
          preset: targetPreset.name,
          preset_id: targetPreset.id,
          exported_at: new Date().toISOString(),
          total: items.length,
          shortcuts: items.map(s => ({
            trigger: s.shortcut || s.trigger_code || '',
            expansion: s.expansion || s.expansion_text || '',
            category: s.category || 'Umum',
            expansion_mode: s.expansion_mode || 'SPACE'
          }))
        }, null, 2);
        mimeType = 'application/json';
      } else {
        content = 'Trigger,Expansion,Category,Mode\n';
        for (const s of items) {
          const tr = (s.shortcut || s.trigger_code || '').replace(/"/g, '""');
          const exp = (s.expansion || s.expansion_text || '').replace(/"/g, '""');
          const cat = (s.category || 'Umum').replace(/"/g, '""');
          const mode = s.expansion_mode || 'SPACE';
          content += `"${tr}","${exp}","${cat}","${mode}"\n`;
        }
        mimeType = 'text/csv';
      }

      const blob = new Blob([content], { type: `${mimeType};charset=utf-8` });
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      const safeName = targetPreset.name.toLowerCase().replace(/[^a-z0-9]+/g, '_');
      link.href = url;
      link.download = `${safeName}_preset.${format}`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      URL.revokeObjectURL(url);

      showToast('success', `File preset "${targetPreset.name}.${format}" (${items.length} shortcut) berhasil diunduh!`);
    } catch (err: any) {
      showToast('error', `Gagal mengunduh preset: ${err.message}`);
    } finally {
      setActionLoading(false);
    }
  };

  // Ambil riwayat masukan/saran user
  const fetchFeedbacks = async () => {
    setIsFeedbackLoading(true);
    try {
      const client = getSupabaseClient();
      const { data: { user: currentUser } } = await client.auth.getUser();
      if (!currentUser) return;
      const { data, error } = await client
        .from('feedbacks')
        .select('*')
        .eq('user_id', currentUser.id)
        .order('created_at', { ascending: false })
        .limit(10);
      if (!error && data) {
        setFeedbacks(data as FeedbackItem[]);
      }
    } catch (e) {
      console.warn('Feedbacks fetch error', e);
    } finally {
      setIsFeedbackLoading(false);
    }
  };

  // Kirim saran & komentar
  const handleSubmitFeedback = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!feedbackMessage.trim()) return;
    setActionLoading(true);
    try {
      const client = getSupabaseClient();
      const { data: { user: currentUser } } = await client.auth.getUser();
      const activeUserId = currentUser?.id || user?.id;
      const activeEmail = currentUser?.email || user?.email || 'cs@pkmobile.app';

      const newFeedback = {
        user_id: activeUserId,
        user_email: activeEmail,
        category: feedbackCategory,
        message: feedbackMessage.trim(),
      };

      const { error } = await client.from('feedbacks').insert(newFeedback);
      if (error) throw error;

      showToast('success', 'Terima kasih! Saran & masukan Anda telah terkirim.');
      setFeedbackMessage('');
      await fetchFeedbacks();
    } catch (err: any) {
      showToast('error', `Gagal mengirim masukan: ${err.message}`);
    } finally {
      setActionLoading(false);
    }
  };

  // Fetch Shortcuts untuk preset tertentu
  const fetchShortcuts = async (presetIdToUse?: string) => {
    setLoading(true);
    try {
      const client = getSupabaseClient();
      const { data: { user: currentUser } } = await client.auth.getUser();
      if (!currentUser) {
        setLoading(false);
        return;
      }
      setUser(currentUser);

      const targetPresetId = presetIdToUse !== undefined ? presetIdToUse : selectedPresetId;

      let query = client
        .from('shortcuts')
        .select('*')
        .eq('user_id', currentUser.id);

      if (targetPresetId) {
        query = query.eq('preset_id', targetPresetId);
      }

      const { data, error } = await query.order('created_at', { ascending: false });

      if (error) {
        showToast('error', `Gagal memuat data: ${error.message}`);
        console.error('Gagal mengambil data:', error);
      } else {
        const formatted: ShortcutItem[] = (data || []).map((item: any) => ({
          id: item.id,
          preset_id: item.preset_id,
          shortcut: item.shortcut || item.trigger_code || '',
          trigger_code: item.trigger_code || item.shortcut || '',
          expansion: item.expansion || item.expansion_text || '',
          expansion_text: item.expansion_text || item.expansion || '',
          category: item.category || 'Umum',
          expansion_mode: item.expansion_mode === 'INSTANT' ? 'INSTANT' : 'SPACE',
          user_id: item.user_id,
          created_at: item.created_at
        }));
        setShortcuts(formatted);
        const now = new Date();
        setLastSyncTime(now.toLocaleTimeString('id-ID', { hour: '2-digit', minute: '2-digit' }));
      }
    } catch (err: any) {
      showToast('error', `Kesalahan koneksi: ${err.message}`);
      console.error('Kesalahan koneksi:', err);
    } finally {
      setLoading(false);
    }
  };

  const fetchPresets = async (preferredPresetId?: string): Promise<string> => {
    setIsPresetLoading(true);
    try {
      const client = getSupabaseClient();
      const { data: { user: currentUser } } = await client.auth.getUser();
      if (!currentUser) return 'default_preset';

      let { data: presetData, error: presetError } = await client
        .from('presets')
        .select('*')
        .or(`user_id.eq.${currentUser.id},user_id.is.null`)
        .order('created_at', { ascending: true });

      // Jika tabel belum dibuat / kosong, siapkan preset default
      if (presetError || !presetData || presetData.length === 0) {
        const defaultId = `preset_default_${currentUser.id.slice(0, 8)}`;
        const defaultPreset: PresetItem = {
          id: defaultId,
          name: 'Paket Utama (Bawaan)',
          is_active: true,
          user_id: currentUser.id
        };
        try {
          await client.from('presets').insert(defaultPreset);
          presetData = [defaultPreset];
        } catch (e) {
          presetData = [defaultPreset];
        }
      }

      // Hitung jumlah shortcut per preset
      const { data: allShortcuts } = await client
        .from('shortcuts')
        .select('preset_id')
        .eq('user_id', currentUser.id);

      const countMap: Record<string, number> = {};
      (allShortcuts || []).forEach((row: any) => {
        const pId = row.preset_id || 'default_preset';
        countMap[pId] = (countMap[pId] || 0) + 1;
      });

      const enriched: PresetItem[] = (presetData || []).map((p: any) => ({
        id: p.id,
        name: p.name,
        is_active: Boolean(p.is_active),
        user_id: p.user_id,
        created_at: p.created_at,
        shortcut_count: countMap[p.id] || 0
      }));

      setPresets(enriched);

      // Cek apakah localStorage.getItem('active_preset_id') cocok dengan salah satu id preset di Supabase
      const savedPresetId = (typeof window !== 'undefined' ? localStorage.getItem('active_preset_id') : null) || preferredPresetId;

      let targetId = '';
      if (savedPresetId && enriched.some(p => p.id === savedPresetId)) {
        targetId = savedPresetId;
      } else {
        // Jika tidak ada di localStorage atau id tidak valid, ambil preset yang is_active = true
        const activeOne = enriched.find(p => p.is_active);
        targetId = activeOne?.id || enriched[0]?.id || 'default_preset';
      }

      // Terapkan targetId ke selectedPresetId dan update localStorage
      setSelectedPresetId(targetId);
      if (typeof window !== 'undefined' && targetId) {
        localStorage.setItem('active_preset_id', targetId);
      }

      // LANGSUNG panggil fetchShortcuts(targetId) di dalam fetchPresets() untuk mencegah race condition
      await fetchShortcuts(targetId);

      return targetId;
    } catch (err) {
      console.error('fetchPresets error:', err);
      return 'default_preset';
    } finally {
      setIsPresetLoading(false);
    }
  };

  // Fetch Session, Presets, & Shortcuts
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

        // Langsung panggil fetchPresets() yang secara atomik mencocokkan localStorage
        // dan langsung memanggil fetchShortcuts(targetId)
        await fetchPresets();
      } catch (err) {
        console.error('Init error:', err);
        router.replace('/login');
      }
    };
    init();
  }, [router]);

  const handleSelectPreset = async (presetId: string) => {
    setSelectedPresetId(presetId);
    if (typeof window !== 'undefined') {
      localStorage.setItem('active_preset_id', presetId);
    }
    await fetchShortcuts(presetId);
  };

  const handleSetActivePreset = async (presetId: string) => {
    setActionLoading(true);
    try {
      const client = getSupabaseClient();
      const { data: { user: currentUser } } = await client.auth.getUser();
      if (!currentUser) throw new Error('Sesi login tidak valid.');

      await client
        .from('presets')
        .update({ is_active: false })
        .eq('user_id', currentUser.id);

      const { error } = await client
        .from('presets')
        .update({ is_active: true })
        .eq('id', presetId);

      if (error) throw error;

      showToast('success', 'Preset berhasil diaktifkan! Keyboard HP Android CS akan menyedot preset ini saat sinkronisasi.');
      if (typeof window !== 'undefined') {
        localStorage.setItem('active_preset_id', presetId);
      }
      await fetchPresets(presetId);
    } catch (err: any) {
      showToast('error', `Gagal mengaktifkan preset: ${err.message}`);
    } finally {
      setActionLoading(false);
    }
  };

  const handleCreatePreset = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newPresetName.trim()) return;
    setActionLoading(true);
    try {
      const client = getSupabaseClient();
      const { data: { user: currentUser } } = await client.auth.getUser();
      if (!currentUser) throw new Error('Sesi login tidak valid.');

      const newId = `preset_${Date.now()}`;
      const newPreset: PresetItem = {
        id: newId,
        name: newPresetName.trim(),
        is_active: false,
        user_id: currentUser.id
      };

      const { error } = await client.from('presets').insert(newPreset);
      if (error) throw error;

      showToast('success', `Preset "${newPreset.name}" berhasil dibuat!`);
      setIsNewPresetModalOpen(false);
      setNewPresetName('');
      if (typeof window !== 'undefined') {
        localStorage.setItem('active_preset_id', newId);
      }
      await fetchPresets(newId);
    } catch (err: any) {
      showToast('error', `Gagal membuat preset: ${err.message}`);
    } finally {
      setActionLoading(false);
    }
  };

  const handleRenamePreset = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!renamePresetName.trim() || !selectedPresetId) return;
    setActionLoading(true);
    try {
      const client = getSupabaseClient();
      const { error } = await client
        .from('presets')
        .update({ name: renamePresetName.trim() })
        .eq('id', selectedPresetId);

      if (error) throw error;

      showToast('success', `Nama preset diubah menjadi "${renamePresetName.trim()}"!`);
      setIsRenameModalOpen(false);
      if (typeof window !== 'undefined') {
        localStorage.setItem('active_preset_id', selectedPresetId);
      }
      await fetchPresets(selectedPresetId);
    } catch (err: any) {
      showToast('error', `Gagal mengubah nama preset: ${err.message}`);
    } finally {
      setActionLoading(false);
    }
  };

  const handleDeletePreset = async () => {
    if (!selectedPresetId) return;
    if (presets.length <= 1) {
      showToast('error', 'Tidak dapat menghapus satu-satunya preset yang tersisa.');
      return;
    }
    setActionLoading(true);
    try {
      const client = getSupabaseClient();

      await client.from('shortcuts').delete().eq('preset_id', selectedPresetId);
      const { error } = await client.from('presets').delete().eq('id', selectedPresetId);
      if (error) throw error;

      showToast('success', 'Preset beserta shortcut di dalamnya berhasil dihapus.');
      setIsDeletePresetModalOpen(false);

      const remaining = presets.filter(p => p.id !== selectedPresetId);
      const nextId = remaining[0]?.id;
      if (typeof window !== 'undefined') {
        if (nextId) {
          localStorage.setItem('active_preset_id', nextId);
        } else {
          localStorage.removeItem('active_preset_id');
        }
      }
      await fetchPresets(nextId);
    } catch (err: any) {
      showToast('error', `Gagal menghapus preset: ${err.message}`);
    } finally {
      setActionLoading(false);
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
    setFormMode('SPACE'); // Default: NON-INSTANT / Space
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

      const targetPreset = selectedPresetId || 'default_preset';
      const cleanTrigger = formTrigger.trim().toLowerCase();
      const cleanExpansion = cleanMacroText(formExpansion.trim());

      const payload = {
        preset_id: targetPreset,
        trigger_code: cleanTrigger,
        expansion_text: cleanExpansion,
        category: formCategory,
        expansion_mode: formMode,
        user_id: activeUserId,
      };

      const prevTrigger = (editingItem?.trigger_code || editingItem?.shortcut || '').trim().toLowerCase();

      // Validasi anti-duplikat trigger dalam preset yang sama
      if (!editingItem || prevTrigger !== cleanTrigger) {
        const isDuplicate = shortcuts.some(
          s => (s.preset_id === targetPreset || !s.preset_id) &&
               (s.shortcut || s.trigger_code || '').trim().toLowerCase() === cleanTrigger &&
               s.id !== editingItem?.id
        );
        if (isDuplicate) {
          setFormError(`Trigger code "${cleanTrigger}" sudah digunakan pada preset ini! Silakan pilih trigger lain.`);
          setActionLoading(false);
          return;
        }
      }

      if (editingItem?.id && prevTrigger && prevTrigger !== cleanTrigger) {
        // Jika trigger code diubah, hapus trigger lama
        await (client as any).from('shortcuts').delete().eq('id', editingItem.id);
      }

      // Upsert dengan strategi onConflict: user_id, preset_id, trigger_code
      const { error } = await (client as any)
        .from('shortcuts')
        .upsert(payload, { onConflict: 'user_id, preset_id, trigger_code' });

      if (error) {
        // Fallback jika constraint belum di-alter di Supabase
        const { error: fallbackError } = await (client as any)
          .from('shortcuts')
          .upsert(payload, { onConflict: 'user_id, trigger_code' });
        if (fallbackError) throw fallbackError;
      }

      showToast('success', `Shortcut "${cleanTrigger}" berhasil disimpan ke preset!`);

      setIsModalOpen(false);
      if (typeof window !== 'undefined') {
        localStorage.setItem('active_preset_id', targetPreset);
      }
      await fetchPresets(targetPreset);
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
          const expansion = cleanMacroText(parts[1]);
          const cat = parts[2] || 'Umum';
          const modeRaw = (parts[3] || 'SPACE').toUpperCase();
          const mode = modeRaw === 'INSTANT' ? 'INSTANT' : 'SPACE';

          parsed.push({
            shortcut: trigger.toLowerCase(),
            expansion: expansion,
            category: cat,
            expansion_mode: mode,
          });
        }
      });

      setCsvPreview(parsed);
      setImportPresetName(file.name.replace(/\.[^/.]+$/, ''));
      setImportDestination('new_preset');
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

      let targetPresetId = selectedPresetId || 'default_preset';

      // Jika opsi Buat Preset Baru dipilih (Isolasi)
      if (importDestination === 'new_preset') {
        const newId = `preset_${Date.now()}`;
        const cleanName = (importPresetName.trim() || csvFileName || 'Impor CSV')
          .replace(/\.[^/.]+$/, '')
          .trim();

        if (importSetAsActive) {
          await client.from('presets').update({ is_active: false }).eq('user_id', activeUserId);
        }

        const newPreset: PresetItem = {
          id: newId,
          name: cleanName,
          is_active: importSetAsActive,
          user_id: activeUserId
        };

        await client.from('presets').insert(newPreset);
        targetPresetId = newId;
      }

      const payload = csvPreview.map(item => ({
        preset_id: targetPresetId,
        trigger_code: (item.trigger_code || item.shortcut || '').trim().toLowerCase(),
        expansion_text: cleanMacroText((item.expansion_text || item.expansion || '').trim()),
        category: item.category || 'Umum',
        expansion_mode: item.expansion_mode || 'SPACE',
        user_id: activeUserId,
      }));

      const { error } = await (client as any)
        .from('shortcuts')
        .upsert(payload, { onConflict: 'user_id, preset_id, trigger_code' });

      if (error) {
        const { error: fallbackError } = await (client as any)
          .from('shortcuts')
          .upsert(payload, { onConflict: 'user_id, trigger_code' });
        if (fallbackError) throw fallbackError;
      }

      showToast('success', `Berhasil mengimpor ${csvPreview.length} shortcut dari CSV ke Preset!`);
      setIsImportModalOpen(false);
      setCsvPreview([]);
      setCsvFileName('');
      setImportPresetName('');
      if (typeof window !== 'undefined') {
        localStorage.setItem('active_preset_id', targetPresetId);
      }
      await fetchPresets(targetPresetId);
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
    setImportPresetName(file.name.replace(/\.[^/.]+$/, ''));
    setImportDestination('new_preset');
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

      let targetPresetId = selectedPresetId || 'default_preset';

      // Jika opsi Buat Preset Baru dipilih (Isolasi)
      if (importDestination === 'new_preset') {
        const newId = `preset_${Date.now()}`;
        const cleanName = (importPresetName.trim() || pkFileName || 'Perfect Keyboard')
          .replace(/\.[^/.]+$/, '')
          .trim();

        if (importSetAsActive) {
          await client.from('presets').update({ is_active: false }).eq('user_id', activeUserId);
        }

        const newPreset: PresetItem = {
          id: newId,
          name: cleanName,
          is_active: importSetAsActive,
          user_id: activeUserId
        };

        await client.from('presets').insert(newPreset);
        targetPresetId = newId;
      }

      const payload = pkParseResult.validShortcuts.map(item => ({
        preset_id: targetPresetId,
        trigger_code: item.trigger.trim().toLowerCase(),
        expansion_text: cleanMacroText(item.expansion.trim()),
        category: pkCategory || 'Perfect Keyboard',
        expansion_mode: pkMode, // default 'SPACE'
        user_id: activeUserId,
      }));

      const { error } = await (client as any)
        .from('shortcuts')
        .upsert(payload, { onConflict: 'user_id, preset_id, trigger_code' });

      if (error) {
        const { error: fallbackError } = await (client as any)
          .from('shortcuts')
          .upsert(payload, { onConflict: 'user_id, trigger_code' });
        if (fallbackError) throw fallbackError;
      }

      showToast(
        'success',
        `Berhasil mengimpor ${pkParseResult.validShortcuts.length} shortcut Perfect Keyboard ke Preset!`
      );
      setPkStatus({
        success: `Berhasil mengimpor ${pkParseResult.validShortcuts.length} shortcut ke Supabase Cloud (Preset terisolasi). Seluruh shortcut ini siap disinkronkan ke HP Android CS!`
      });
      if (typeof window !== 'undefined') {
        localStorage.setItem('active_preset_id', targetPresetId);
      }
      await fetchPresets(targetPresetId);
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
      <header className="sticky top-0 z-30 bg-slate-900/90 backdrop-blur-xl border-b border-slate-800 py-3 sm:py-4">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 min-h-[64px] flex items-center justify-between gap-4">
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

          <div className="flex items-center gap-2 sm:gap-3 flex-wrap justify-end">
            {/* Download APK & Panduan Instalasi HP */}
            <ApkDownloadSection />

            {/* Riwayat & Unduh Preset */}
            <button
              onClick={() => setIsDownloadModalOpen(true)}
              className="px-3 py-2 rounded-xl bg-slate-800/90 hover:bg-slate-700 text-indigo-300 border border-indigo-500/30 text-xs font-semibold flex items-center gap-1.5 transition-all shadow-sm"
              title="Riwayat & Unduh File Preset (.xml / .json / .csv)"
            >
              <FileDown className="w-4 h-4 text-indigo-400" />
              <span className="hidden sm:inline">Unduh Preset</span>
            </button>

            {/* Saran & Masukan Tim CS */}
            <button
              onClick={() => {
                setIsFeedbackModalOpen(true);
                fetchFeedbacks();
              }}
              className="px-3 py-2 rounded-xl bg-slate-800/90 hover:bg-slate-700 text-emerald-300 border border-emerald-500/30 text-xs font-semibold flex items-center gap-1.5 transition-all shadow-sm"
              title="Kotak Saran & Masukan Tim CS"
            >
              <MessageSquarePlus className="w-4 h-4 text-emerald-400" />
              <span className="hidden sm:inline">Kotak Saran</span>
            </button>

            {/* Indikator Live Sync Status */}
            <div className="hidden lg:flex items-center gap-2 px-3 py-1.5 rounded-xl bg-slate-850/80 border border-slate-800 text-right">
              <button
                onClick={() => fetchShortcuts(selectedPresetId)}
                disabled={loading}
                className="p-1 rounded-lg hover:bg-slate-800 text-slate-400 hover:text-emerald-400 transition-colors"
                title="Refresh sinkronisasi database"
              >
                <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin text-emerald-400' : ''}`} />
              </button>
              <div>
                <div className="text-[11px] font-semibold text-slate-200">
                  {user?.email ? user.email.split('@')[0] : 'CS Agent'}
                </div>
                <div className="text-[10px] text-emerald-400 flex items-center gap-1">
                  <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse shrink-0" />
                  <span>Tersinkron ({lastSyncTime || 'Terbaru'})</span>
                </div>
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

        {/* Preset Switcher & Management Card */}
        <div className="bg-slate-900/90 border border-slate-800 p-4 sm:p-5 rounded-2xl shadow-xl backdrop-blur flex flex-col md:flex-row gap-4 items-stretch md:items-center justify-between">
          <div className="flex items-center gap-3.5 flex-1 min-w-0">
            <div className="w-11 h-11 rounded-xl bg-gradient-to-tr from-indigo-600/30 to-emerald-500/30 border border-indigo-500/30 flex items-center justify-center shrink-0 text-indigo-400 shadow-inner">
              <FolderOpen className="w-5 h-5" />
            </div>
            <div className="flex-1 min-w-0">
              <div className="flex items-center gap-2 flex-wrap mb-1.5">
                <span className="text-xs font-bold uppercase tracking-wider text-slate-400">
                  📂 Berkas / Preset Shortcut:
                </span>
                {currentPreset?.is_active ? (
                  <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-emerald-500/10 text-emerald-400 border border-emerald-500/30">
                    <CheckCircle className="w-3.5 h-3.5" />
                    Aktif di Keyboard HP
                  </span>
                ) : (
                  <button
                    onClick={() => selectedPresetId && handleSetActivePreset(selectedPresetId)}
                    disabled={actionLoading || !selectedPresetId}
                    className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-amber-500/10 hover:bg-amber-500/20 text-amber-300 border border-amber-500/40 transition-colors cursor-pointer disabled:opacity-50"
                    title="Klik untuk memasang preset ini sebagai shortcut aktif di keyboard HP"
                  >
                    <Star className="w-3 h-3 fill-amber-300/30" />
                    Pasang untuk Keyboard HP
                  </button>
                )}
              </div>

              <div className="flex items-center gap-2.5 flex-wrap">
                <select
                  value={selectedPresetId}
                  onChange={(e) => handleSelectPreset(e.target.value)}
                  disabled={isPresetLoading}
                  className="bg-slate-950 border border-slate-700/80 hover:border-indigo-500/60 rounded-xl px-3.5 py-2 text-sm font-semibold text-white focus:outline-none focus:border-indigo-500 transition-all cursor-pointer min-w-[260px] max-w-md truncate"
                >
                  {presets.map(p => (
                    <option key={p.id} value={p.id}>
                      {p.name} ({p.shortcut_count || 0} shortcut){p.is_active ? ' — ⭐ [Aktif di HP]' : ''}
                    </option>
                  ))}
                </select>
                <span className="text-xs text-slate-500">
                  {shortcuts.length} shortcut di preset ini
                </span>
              </div>
            </div>
          </div>

          {/* Preset Actions Buttons */}
          <div className="flex items-center gap-2 shrink-0 flex-wrap">
            <button
              onClick={() => {
                setNewPresetName('');
                setIsNewPresetModalOpen(true);
              }}
              className="px-3.5 py-2 rounded-xl bg-slate-800/90 hover:bg-slate-700 text-indigo-300 border border-indigo-500/30 text-xs font-semibold flex items-center gap-1.5 transition-colors shadow-sm"
              title="Buat Preset Baru Kosong"
            >
              <FolderPlus className="w-4 h-4" />
              <span>Preset Baru</span>
            </button>

            <button
              onClick={() => {
                setRenamePresetName(currentPreset?.name || '');
                setIsRenameModalOpen(true);
              }}
              className="px-3 py-2 rounded-xl bg-slate-800/90 hover:bg-slate-700 text-slate-300 border border-slate-700 text-xs font-semibold flex items-center gap-1.5 transition-colors"
              title="Ganti Nama Preset Ini"
            >
              <Edit2 className="w-3.5 h-3.5" />
              <span>Ganti Nama</span>
            </button>

            {presets.length > 1 && (
              <button
                onClick={() => setIsDeletePresetModalOpen(true)}
                className="px-3 py-2 rounded-xl bg-slate-800/90 hover:bg-rose-500/20 text-slate-400 hover:text-rose-400 border border-slate-700 hover:border-rose-500/30 text-xs font-semibold flex items-center gap-1.5 transition-colors"
                title="Hapus Preset Ini"
              >
                <Trash2 className="w-3.5 h-3.5" />
                <span>Hapus</span>
              </button>
            )}
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
              onClick={() => fetchShortcuts()}
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
                    const expansion = cleanMacroText(item.expansion || item.expansion_text || '');
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
              {/* Target Preset Indicator */}
              <div className="flex items-center justify-between p-3 rounded-xl bg-slate-950 border border-slate-800 text-xs">
                <span className="text-slate-400">Target Preset Penyimpanan:</span>
                <span className="font-semibold text-indigo-300 flex items-center gap-1.5">
                  <FolderOpen className="w-3.5 h-3.5" />
                  {currentPreset?.name || 'Paket Utama'}
                </span>
              </div>

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
                  Mode Ekspansi (Bawaan: SPACE / Non-Instant)
                </label>
                <div className="grid grid-cols-2 gap-3">
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
                      <span>SPACE (Spasi/Enter)</span>
                    </div>
                    <span className="text-[11px] text-slate-500 mt-1">
                      Menunggu Spasi/Enter (Direkomendasikan)
                    </span>
                  </label>

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
                      Otomatis ekspansi saat trigger cocok
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

            {/* Target Preset Destination */}
            {csvPreview.length > 0 && (
              <div className="bg-slate-950 p-3.5 rounded-xl border border-slate-800 space-y-2.5 mb-4 text-left">
                <span className="text-xs font-semibold text-slate-300 block">Penyimpanan Preset:</span>
                <div className="space-y-2.5">
                  <label className="flex items-start gap-2.5 text-xs text-slate-200 cursor-pointer">
                    <input
                      type="radio"
                      name="csvImportDest"
                      checked={importDestination === 'new_preset'}
                      onChange={() => setImportDestination('new_preset')}
                      className="mt-0.5 text-indigo-500"
                    />
                    <div>
                      <span className="font-semibold text-indigo-300">📦 Buat Preset Baru (Terisolasi - Aman)</span>
                      <p className="text-[11px] text-slate-400">Tidak menimpa atau mencampur shortcut berkas lain.</p>
                    </div>
                  </label>
                  {importDestination === 'new_preset' && (
                    <div className="pl-6 space-y-2">
                      <input
                        type="text"
                        value={importPresetName}
                        onChange={(e) => setImportPresetName(e.target.value)}
                        placeholder="Nama Preset Baru (misal: Paket CS CSV)"
                        className="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-1.5 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-indigo-500"
                      />
                      <label className="flex items-center gap-2 text-[11px] text-slate-300 cursor-pointer">
                        <input
                          type="checkbox"
                          checked={importSetAsActive}
                          onChange={(e) => setImportSetAsActive(e.target.checked)}
                          className="rounded text-indigo-500"
                        />
                        <span>Langsung pasang sebagai preset aktif untuk Keyboard HP CS</span>
                      </label>
                    </div>
                  )}

                  <label className="flex items-start gap-2.5 text-xs text-slate-200 cursor-pointer">
                    <input
                      type="radio"
                      name="csvImportDest"
                      checked={importDestination === 'current_preset'}
                      onChange={() => setImportDestination('current_preset')}
                      className="mt-0.5 text-indigo-500"
                    />
                    <div>
                      <span className="font-semibold text-slate-300">📥 Gabungkan ke Preset yang Sedang Dipilih</span>
                      <p className="text-[11px] text-slate-400">
                        Dimasukkan ke: <strong className="text-white">{currentPreset?.name || 'Preset Saat Ini'}</strong>
                      </p>
                    </div>
                  </label>
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
                              <option value="SPACE">SPACE (Ditahan hingga tombol Spasi/Enter ditekan - Bawaan)</option>
                              <option value="INSTANT">INSTANT (Langsung ekspansi saat diketik)</option>
                            </select>
                          </div>
                        </div>

                        {/* Target Preset Destination */}
                        <div className="bg-slate-950 p-3.5 rounded-xl border border-slate-800 space-y-2.5 text-left text-xs">
                          <span className="font-semibold text-slate-300 block">Penyimpanan Preset:</span>
                          <div className="space-y-2.5">
                            <label className="flex items-start gap-2.5 text-slate-200 cursor-pointer">
                              <input
                                type="radio"
                                name="pkImportDest"
                                checked={importDestination === 'new_preset'}
                                onChange={() => setImportDestination('new_preset')}
                                className="mt-0.5 text-indigo-500"
                              />
                              <div>
                                <span className="font-semibold text-indigo-300">📦 Buat Preset Baru (Terisolasi - Aman)</span>
                                <p className="text-[11px] text-slate-400">Tidak menimpa atau mencampur shortcut berkas lain.</p>
                              </div>
                            </label>
                            {importDestination === 'new_preset' && (
                              <div className="pl-6 space-y-2">
                                <input
                                  type="text"
                                  value={importPresetName}
                                  onChange={(e) => setImportPresetName(e.target.value)}
                                  placeholder="Nama Preset Baru (misal: Paket Perfect Keyboard CS)"
                                  className="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-1.5 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-indigo-500"
                                />
                                <label className="flex items-center gap-2 text-[11px] text-slate-300 cursor-pointer">
                                  <input
                                    type="checkbox"
                                    checked={importSetAsActive}
                                    onChange={(e) => setImportSetAsActive(e.target.checked)}
                                    className="rounded text-indigo-500"
                                  />
                                  <span>Langsung pasang sebagai preset aktif untuk Keyboard HP CS</span>
                                </label>
                              </div>
                            )}

                            <label className="flex items-start gap-2.5 text-slate-200 cursor-pointer">
                              <input
                                type="radio"
                                name="pkImportDest"
                                checked={importDestination === 'current_preset'}
                                onChange={() => setImportDestination('current_preset')}
                                className="mt-0.5 text-indigo-500"
                              />
                              <div>
                                <span className="font-semibold text-slate-300">📥 Gabungkan ke Preset yang Sedang Dipilih</span>
                                <p className="text-[11px] text-slate-400">
                                  Dimasukkan ke: <strong className="text-white">{currentPreset?.name || 'Preset Saat Ini'}</strong>
                                </p>
                              </div>
                            </label>
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
      {/* MODAL: Buat Preset Baru */}
      {isNewPresetModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm animate-in fade-in">
          <div className="w-full max-w-md bg-slate-900 border border-slate-800 rounded-2xl shadow-2xl p-6 relative">
            <div className="flex items-center justify-between pb-4 mb-4 border-b border-slate-800">
              <div className="flex items-center gap-2.5 text-white font-bold text-base">
                <FolderPlus className="w-5 h-5 text-indigo-400" />
                <span>Buat Preset Baru</span>
              </div>
              <button
                onClick={() => setIsNewPresetModalOpen(false)}
                className="p-1 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition-colors"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleCreatePreset} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1.5">
                  Nama Preset
                </label>
                <input
                  type="text"
                  required
                  value={newPresetName}
                  onChange={(e) => setNewPresetName(e.target.value)}
                  placeholder="Contoh: CS Shift Malam, CS Promo 2026..."
                  className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3.5 py-2.5 text-sm text-white placeholder-slate-600 focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 transition-all"
                />
                <span className="text-[11px] text-slate-500 mt-1 block">
                  Preset baru akan dibuat secara terisolasi tanpa shortcut bawaan.
                </span>
              </div>

              <div className="flex items-center justify-end gap-3 pt-3 border-t border-slate-800">
                <button
                  type="button"
                  onClick={() => setIsNewPresetModalOpen(false)}
                  className="px-4 py-2.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-semibold transition-colors"
                >
                  Batal
                </button>
                <button
                  type="submit"
                  disabled={actionLoading || !newPresetName.trim()}
                  className="px-5 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-semibold shadow-lg shadow-indigo-600/30 transition-all disabled:opacity-50"
                >
                  {actionLoading ? 'Menyimpan...' : 'Buat Preset'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* MODAL: Ganti Nama Preset */}
      {isRenameModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm animate-in fade-in">
          <div className="w-full max-w-md bg-slate-900 border border-slate-800 rounded-2xl shadow-2xl p-6 relative">
            <div className="flex items-center justify-between pb-4 mb-4 border-b border-slate-800">
              <div className="flex items-center gap-2.5 text-white font-bold text-base">
                <Edit2 className="w-5 h-5 text-indigo-400" />
                <span>Ganti Nama Preset</span>
              </div>
              <button
                onClick={() => setIsRenameModalOpen(false)}
                className="p-1 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition-colors"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleRenamePreset} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1.5">
                  Nama Preset Baru
                </label>
                <input
                  type="text"
                  required
                  value={renamePresetName}
                  onChange={(e) => setRenamePresetName(e.target.value)}
                  placeholder="Masukkan nama preset baru..."
                  className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3.5 py-2.5 text-sm text-white placeholder-slate-600 focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 transition-all"
                />
              </div>

              <div className="flex items-center justify-end gap-3 pt-3 border-t border-slate-800">
                <button
                  type="button"
                  onClick={() => setIsRenameModalOpen(false)}
                  className="px-4 py-2.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-semibold transition-colors"
                >
                  Batal
                </button>
                <button
                  type="submit"
                  disabled={actionLoading || !renamePresetName.trim()}
                  className="px-5 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-semibold shadow-lg shadow-indigo-600/30 transition-all disabled:opacity-50"
                >
                  {actionLoading ? 'Menyimpan...' : 'Simpan Nama'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* MODAL: Konfirmasi Hapus Preset */}
      {isDeletePresetModalOpen && currentPreset && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm animate-in fade-in">
          <div className="w-full max-w-sm bg-slate-900 border border-slate-800 rounded-2xl shadow-2xl p-6 relative text-center">
            <div className="w-12 h-12 rounded-full bg-rose-500/10 border border-rose-500/30 flex items-center justify-center text-rose-400 mx-auto mb-3">
              <Trash2 className="w-6 h-6" />
            </div>
            <h3 className="text-base font-bold text-white mb-1">Hapus Preset?</h3>
            <p className="text-xs text-slate-400 mb-6">
              Yakin ingin menghapus preset <strong className="text-rose-300">&quot;{currentPreset.name}&quot;</strong> beserta seluruh shortcut di dalamnya? Tindakan ini tidak dapat dibatalkan.
            </p>
            <div className="grid grid-cols-2 gap-3">
              <button
                type="button"
                onClick={() => setIsDeletePresetModalOpen(false)}
                className="py-2.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-semibold transition-colors"
              >
                Batal
              </button>
              <button
                type="button"
                disabled={actionLoading}
                onClick={handleDeletePreset}
                className="py-2.5 rounded-xl bg-rose-600 hover:bg-rose-500 text-white text-xs font-semibold shadow-lg shadow-rose-600/30 transition-all disabled:opacity-50"
              >
                {actionLoading ? 'Menghapus...' : 'Ya, Hapus Preset'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* MODAL: Riwayat & Unduh Berkas Preset */}
      {isDownloadModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm animate-in fade-in">
          <div className="w-full max-w-3xl bg-slate-900 border border-slate-800 rounded-2xl shadow-2xl p-6 relative max-h-[90vh] flex flex-col">
            <div className="flex items-center justify-between pb-4 mb-4 border-b border-slate-800 shrink-0">
              <div className="flex items-center gap-2.5 text-white font-bold text-base">
                <div className="w-8 h-8 rounded-lg bg-indigo-500/20 text-indigo-400 flex items-center justify-center border border-indigo-500/30">
                  <FileDown className="w-4 h-4" />
                </div>
                <div>
                  <div className="leading-tight">Riwayat &amp; Unduh File Preset</div>
                  <div className="text-[11px] text-slate-400 font-normal mt-0.5">
                    Unduh file shortcut untuk backup offline atau impor langsung ke aplikasi Android PK Mobile.
                  </div>
                </div>
              </div>
              <button
                onClick={() => setIsDownloadModalOpen(false)}
                className="p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition-colors"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="overflow-y-auto flex-1 pr-1 space-y-3">
              <div className="p-3 bg-indigo-950/40 border border-indigo-500/30 rounded-xl text-xs text-indigo-200 flex items-start gap-2.5">
                <CheckCircle2 className="w-4 h-4 text-indigo-400 shrink-0 mt-0.5" />
                <div>
                  <strong className="text-white">Format .XML Kompatibel Android:</strong> Berkas .xml diunduh dengan struktur khusus Android PK Mobile yang membungkus teks dengan <code className="text-amber-300 font-mono text-[11px]">&lt;![CDATA[...]]&gt;</code> sehingga seluruh karakter spesial (enter, kutip, tanda baca, simbol) aman 100% tanpa error saat diimpor di HP.
                </div>
              </div>

              <div className="divide-y divide-slate-800 border border-slate-800 rounded-xl overflow-hidden bg-slate-950/60">
                {presets.map((preset) => (
                  <div key={preset.id} className="p-4 flex flex-col sm:flex-row sm:items-center justify-between gap-3 hover:bg-slate-900/50 transition-colors">
                    <div>
                      <div className="flex items-center gap-2 flex-wrap">
                        <span className="font-semibold text-sm text-white">{preset.name}</span>
                        {preset.is_active && (
                          <span className="text-[10px] uppercase font-bold tracking-wider px-2 py-0.5 rounded-full bg-emerald-500/10 text-emerald-400 border border-emerald-500/30">
                            Aktif di HP
                          </span>
                        )}
                      </div>
                      <div className="text-xs text-slate-400 mt-1 flex items-center gap-3">
                        <span>📊 {preset.shortcut_count || 0} shortcut</span>
                        {preset.created_at && (
                          <span>📅 {new Date(preset.created_at).toLocaleDateString('id-ID', { day: 'numeric', month: 'short', year: 'numeric' })}</span>
                        )}
                      </div>
                    </div>

                    <div className="flex items-center gap-2 shrink-0">
                      <button
                        onClick={() => handleDownloadPreset(preset, 'xml')}
                        disabled={actionLoading}
                        className="px-3 py-1.5 rounded-lg bg-emerald-600/20 hover:bg-emerald-600/30 text-emerald-300 border border-emerald-500/40 text-xs font-bold transition-colors flex items-center gap-1.5 disabled:opacity-50"
                        title="Unduh format XML (Format resmi Aplikasi HP)"
                      >
                        <Download className="w-3.5 h-3.5" />
                        <span>.XML (HP)</span>
                      </button>

                      <button
                        onClick={() => handleDownloadPreset(preset, 'json')}
                        disabled={actionLoading}
                        className="px-3 py-1.5 rounded-lg bg-sky-600/20 hover:bg-sky-600/30 text-sky-300 border border-sky-500/40 text-xs font-bold transition-colors flex items-center gap-1.5 disabled:opacity-50"
                        title="Unduh format JSON (Cadangan Data Lengkap)"
                      >
                        <Download className="w-3.5 h-3.5" />
                        <span>.JSON</span>
                      </button>

                      <button
                        onClick={() => handleDownloadPreset(preset, 'csv')}
                        disabled={actionLoading}
                        className="px-3 py-1.5 rounded-lg bg-amber-600/20 hover:bg-amber-600/30 text-amber-300 border border-amber-500/40 text-xs font-bold transition-colors flex items-center gap-1.5 disabled:opacity-50"
                        title="Unduh format CSV (Microsoft Excel / Google Sheets)"
                      >
                        <Download className="w-3.5 h-3.5" />
                        <span>.CSV</span>
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            <div className="pt-4 mt-3 border-t border-slate-800 flex justify-end shrink-0">
              <button
                type="button"
                onClick={() => setIsDownloadModalOpen(false)}
                className="px-4 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-semibold transition-colors"
              >
                Tutup
              </button>
            </div>
          </div>
        </div>
      )}

      {/* MODAL: Kotak Saran & Komentar Tim CS */}
      {isFeedbackModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm animate-in fade-in">
          <div className="w-full max-w-2xl bg-slate-900 border border-slate-800 rounded-2xl shadow-2xl p-6 relative max-h-[90vh] flex flex-col">
            <div className="flex items-center justify-between pb-4 mb-4 border-b border-slate-800 shrink-0">
              <div className="flex items-center gap-2.5 text-white font-bold text-base">
                <div className="w-8 h-8 rounded-lg bg-emerald-500/20 text-emerald-400 flex items-center justify-center border border-emerald-500/30">
                  <MessageSquarePlus className="w-4 h-4" />
                </div>
                <div>
                  <div className="leading-tight">Kotak Saran &amp; Komentar Tim CS</div>
                  <div className="text-[11px] text-slate-400 font-normal mt-0.5">
                    Sampaikan keluhan kendala keyboard HP, permintaan fitur baru, atau usulan template shortcut.
                  </div>
                </div>
              </div>
              <button
                onClick={() => setIsFeedbackModalOpen(false)}
                className="p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition-colors"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="overflow-y-auto flex-1 pr-1 space-y-5">
              {/* Form Masukan */}
              <form onSubmit={handleSubmitFeedback} className="space-y-3.5 bg-slate-950/70 p-4 rounded-xl border border-slate-800">
                <div>
                  <label className="block text-xs font-semibold text-slate-300 mb-1.5">
                    Kategori Masukan
                  </label>
                  <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
                    {(['Permintaan Fitur', 'Bug / Error', 'Usulan Shortcut Baru', 'Performa / Lainnya'] as const).map((cat) => (
                      <button
                        key={cat}
                        type="button"
                        onClick={() => setFeedbackCategory(cat)}
                        className={`px-3 py-2 rounded-lg text-xs font-semibold border transition-all text-center ${
                          feedbackCategory === cat
                            ? 'bg-indigo-600 border-indigo-500 text-white shadow-sm'
                            : 'bg-slate-900 border-slate-800 text-slate-400 hover:border-slate-700 hover:text-slate-200'
                        }`}
                      >
                        {cat}
                      </button>
                    ))}
                  </div>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-300 mb-1.5">
                    Isi Saran / Detail Masukan
                  </label>
                  <textarea
                    rows={4}
                    required
                    value={feedbackMessage}
                    onChange={(e) => setFeedbackMessage(e.target.value)}
                    placeholder="Contoh: Tolong tambahkan shortcut untuk format no rekening baru, atau keyboard lambat saat beralih aplikasi..."
                    className="w-full bg-slate-900 border border-slate-800 rounded-xl p-3 text-xs text-slate-200 placeholder-slate-600 focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 transition-all resize-none"
                  />
                </div>

                <div className="flex justify-end">
                  <button
                    type="submit"
                    disabled={actionLoading || !feedbackMessage.trim()}
                    className="px-5 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-bold flex items-center gap-1.5 transition-all shadow-lg shadow-emerald-600/20 disabled:opacity-50"
                  >
                    <Send className="w-3.5 h-3.5" />
                    <span>{actionLoading ? 'Mengirim...' : 'Kirim Masukan'}</span>
                  </button>
                </div>
              </form>

              {/* Riwayat Feedback Sebelumnya */}
              <div>
                <h4 className="text-xs font-bold uppercase tracking-wider text-slate-400 mb-2 flex items-center gap-1.5">
                  <History className="w-3.5 h-3.5 text-indigo-400" />
                  <span>Riwayat Masukan Anda</span>
                </h4>

                {isFeedbackLoading ? (
                  <div className="p-4 text-center text-xs text-slate-500">Memuat riwayat masukan...</div>
                ) : feedbacks.length === 0 ? (
                  <div className="p-4 bg-slate-950/40 border border-slate-800/80 rounded-xl text-center text-xs text-slate-500">
                    Belum ada masukan yang Anda kirimkan.
                  </div>
                ) : (
                  <div className="space-y-2">
                    {feedbacks.map((item) => (
                      <div key={item.id} className="p-3 bg-slate-950/80 border border-slate-800 rounded-xl">
                        <div className="flex items-center justify-between gap-2 mb-1.5">
                          <span className="text-[10px] font-bold px-2 py-0.5 rounded bg-indigo-500/10 text-indigo-300 border border-indigo-500/20">
                            {item.category}
                          </span>
                          <span className="text-[10px] text-slate-500">
                            {item.created_at ? new Date(item.created_at).toLocaleString('id-ID', { dateStyle: 'short', timeStyle: 'short' }) : ''}
                          </span>
                        </div>
                        <p className="text-xs text-slate-300 whitespace-pre-wrap font-sans">
                          {item.message}
                        </p>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>

            <div className="pt-4 mt-3 border-t border-slate-800 flex justify-end shrink-0">
              <button
                type="button"
                onClick={() => setIsFeedbackModalOpen(false)}
                className="px-4 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-semibold transition-colors"
              >
                Tutup
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
