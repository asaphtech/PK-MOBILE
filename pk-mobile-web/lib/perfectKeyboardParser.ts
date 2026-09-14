/**
 * Parser & Validator File Perfect Keyboard (.4pk, .kps, .txt)
 * Mengurai trigger & expansion text serta mendeteksi tombol fisik PC / function keys
 * yang tidak didukung pada keyboard smartphone Android.
 */

export interface ValidShortcut {
  lineNum: number;
  trigger: string;
  expansion: string;
}

export interface FailedShortcut {
  lineNum: number;
  rawTrigger: string;
  rawExpansion: string;
  reason: string;
  suggestion: string;
}

export interface PerfectKeyboardParseResult {
  totalParsed: number;
  validShortcuts: ValidShortcut[];
  failedShortcuts: FailedShortcut[];
}

/**
 * Daftar Tombol Fisik PC / Non-text Keys
 */
const PC_SPECIAL_KEYS = [
  'tab', 'enter', 'return', 'esc', 'escape', 'capslock', 'caps_lock', 'caps lock',
  'insert', 'ins', 'delete', 'del', 'home', 'end', 'pageup', 'page_up', 'page up',
  'pagedown', 'page_down', 'page down', 'pgup', 'pgdn', 'printscreen', 'prtsc', 'prt_sc',
  'scrolllock', 'scroll_lock', 'pause', 'break', 'numlock', 'num_lock', 'numpad',
  'backspace', 'space', 'spacebar', 'up', 'down', 'left', 'right'
];

/**
 * Pembersih karakter XML, CDATA, tag format Perfect Keyboard
 */
export function cleanTextContent(text: string): string {
  if (!text) return '';
  let s = text.trim();

  // 1. CDATA
  s = s.replace(/<!\[CDATA\[([\s\S]*?)\]\]>/gi, '$1');

  // 2. Format enter & tab khas Perfect Keyboard
  s = s.replace(/<ent__>/gi, '\n')
       .replace(/<enter>/gi, '\n')
       .replace(/<br\s*\/?>/gi, '\n')
       .replace(/<tab__>/gi, '\t');

  // 3. Entitas XML
  s = s.replace(/&amp;/g, '&')
       .replace(/&lt;/g, '<')
       .replace(/&gt;/g, '>')
       .replace(/&quot;/g, '"')
       .replace(/&apos;/g, "'");

  // 4. Entitas numerik &#...; dan &#x...;
  s = s.replace(/&#(\d+);/g, (_, dec) => {
    try { return String.fromCharCode(parseInt(dec, 10)); } catch { return ''; }
  });
  s = s.replace(/&#x([0-9a-fA-F]+);/g, (_, hex) => {
    try { return String.fromCharCode(parseInt(hex, 16)); } catch { return ''; }
  });

  // 5. Normalisasi baris baru
  s = s.replace(/\r\n/g, '\n').replace(/\r/g, '\n');

  return s.trim();
}

/**
 * Membersihkan trigger dari tag HTML/XML pembungkus
 */
export function cleanTriggerString(raw: string): string {
  if (!raw) return '';
  let s = raw.trim();
  s = s.replace(/<!\[CDATA\[([\s\S]*?)\]\]>/gi, '$1');
  s = s.replace(/<[^>]*>/g, '');
  s = s.replace(/&amp;/g, '&')
       .replace(/&lt;/g, '<')
       .replace(/&gt;/g, '>')
       .replace(/&quot;/g, '"')
       .replace(/&apos;/g, "'");
  s = s.replace(/[\uFEFF\uFFFE]/g, '');
  return s.trim();
}

/**
 * Membuat saran teks alternatif jika trigger bermasalah
 */
function generateSuggestion(rawTrigger: string): string {
  // Ambil huruf dan angka saja untuk membentuk saran yang bersih
  const alphanumeric = rawTrigger.replace(/[^a-zA-Z0-9]/g, '').toLowerCase();
  if (alphanumeric.length > 0) {
    return `Ubah trigger menjadi format teks seperti "!${alphanumeric}" atau awalan slash "//${alphanumeric}" langsung dari dashboard.`;
  }
  return 'Gunakan kode trigger teks biasa seperti //pesan atau !info.';
}

/**
 * Mengevaluasi apakah suatu trigger ditolak atau valid untuk Android
 */
function validateTrigger(
  rawTrigger: string,
  rawExpansion: string,
  seenTriggers: Map<string, number>,
  lineNum: number
): { isValid: true; trigger: string; expansion: string } | { isValid: false; failed: FailedShortcut } {
  const trigger = cleanTriggerString(rawTrigger);
  const expansion = cleanTextContent(rawExpansion);

  // 1. Cek Trigger Kosong
  if (!trigger) {
    return {
      isValid: false,
      failed: {
        lineNum,
        rawTrigger: rawTrigger || '(Kosong)',
        rawExpansion: expansion,
        reason: 'Kode trigger kosong atau tidak ditemukan.',
        suggestion: 'Tentukan kode trigger teks yang valid sebelum mengimpor.'
      }
    };
  }

  // 2. Cek Expansion Kosong
  if (!expansion) {
    return {
      isValid: false,
      failed: {
        lineNum,
        rawTrigger: trigger,
        rawExpansion: '(Kosong)',
        reason: 'Isi teks balasan (expansion text) kosong.',
        suggestion: 'Lengkapi isi teks pesan template balasan sebelum diimpor.'
      }
    };
  }

  // Bersihkan kurung pembungkus untuk deteksi tombol (misal [F1], {Enter}, <F2>)
  const strippedKey = trigger.replace(/^[\[{(<]+|[\]})>]+$/g, '').trim().toLowerCase();

  // 3. Cek Function Keys (F1 sampai F24)
  const isFunctionKey = /^f([1-9]|1[0-9]|2[0-4])$/i.test(strippedKey) ||
                        /\b(F[1-9]|F1[0-9]|F2[0-4])\b/i.test(trigger);
  if (isFunctionKey) {
    return {
      isValid: false,
      failed: {
        lineNum,
        rawTrigger: trigger,
        rawExpansion: expansion,
        reason: `Menggunakan tombol fisik PC Function Key (${trigger}) yang tidak ada pada keyboard HP.`,
        suggestion: generateSuggestion(trigger)
      }
    };
  }

  // 4. Cek Tombol Fisik Khusus PC (Tab, Enter, Esc, CapsLock, Insert, Home, PageUp, dll.)
  if (PC_SPECIAL_KEYS.includes(strippedKey)) {
    return {
      isValid: false,
      failed: {
        lineNum,
        rawTrigger: trigger,
        rawExpansion: expansion,
        reason: `Menggunakan tombol fisik PC "${trigger}" yang tidak tersedia pada keyboard smartphone.`,
        suggestion: generateSuggestion(trigger)
      }
    };
  }

  // 5. Cek Kombinasi Shortcut PC (Ctrl+, Alt+, Shift+, Win+, Cmd+)
  const hasModifierKey = /(ctrl|alt|shift|win|cmd|command|control|meta)[\s\+\-_]/i.test(trigger) ||
                         /[\+\-_](ctrl|alt|shift|win|cmd)/i.test(trigger);
  if (hasModifierKey) {
    return {
      isValid: false,
      failed: {
        lineNum,
        rawTrigger: trigger,
        rawExpansion: expansion,
        reason: `Menggunakan kombinasi tombol pintas PC (${trigger}) yang tidak didukung pada layar sentuh Android.`,
        suggestion: generateSuggestion(trigger)
      }
    };
  }

  // 6. Cek Trigger Mengandung Spasi
  if (/\s/.test(trigger)) {
    const withoutSpace = trigger.replace(/\s+/g, '_').toLowerCase();
    return {
      isValid: false,
      failed: {
        lineNum,
        rawTrigger: trigger,
        rawExpansion: expansion,
        reason: 'Trigger mengandung karakter spasi (tidak dapat dipicu otomatis saat mengetik di HP).',
        suggestion: `Hapus spasi atau sambungkan dengan garis bawah (misal: "${withoutSpace}").`
      }
    };
  }

  // 7. Auto-Rename untuk Trigger Duplikat di Dalam File yang Sama
  // Menambah angka urutan otomatis (_1, _2, dst.) agar semua data tetap bisa diimpor tanpa hilang
  const normalizedKey = trigger.toLowerCase();
  let finalTrigger = trigger;

  if (seenTriggers.has(normalizedKey)) {
    const count = seenTriggers.get(normalizedKey)! + 1;
    seenTriggers.set(normalizedKey, count);

    // Tambahkan sufiks urutan otomatis (contoh: /otosc_1, /otosc_2)
    finalTrigger = `${trigger}_${count - 1}`;
    seenTriggers.set(finalTrigger.toLowerCase(), 1);
  } else {
    seenTriggers.set(normalizedKey, 1);
  }

  // Lolos Semua Validasi
  return {
    isValid: true,
    trigger: finalTrigger,
    expansion
  };
}

/**
 * Parser Khusus Format Teks Ekspor Perfect Keyboard (.txt / .kps)
 * Memisahkan item berdasarkan delimiter {end-of-item},
 * membaca kode trigger dari baris 'at1s: <trigger>',
 * membaca isi pesan dari 'm: <pesan>',
 * dan mengonversi tag <ent__> menjadi baris baru (\n).
 */
export function parseTxtExport(fileContent: string): Array<{
  trigger_code: string;
  expansion_text: string;
  category: string;
  expansion_mode: string;
}> {
  // Split per item berdasarkan delimiter {end-of-item}
  const rawItems = fileContent.split('{end-of-item}');
  const shortcuts: Array<{
    trigger_code: string;
    expansion_text: string;
    category: string;
    expansion_mode: string;
  }> = [];
  const seenTriggers = new Map<string, number>();

  for (const item of rawItems) {
    if (!item.trim()) continue;

    let trigger = '';
    let expansion = '';

    // Ekstrak trigger dari baris 'at1s:'
    const triggerMatch = item.match(/^at1s:\s*(.+)$/m) || item.match(/^(?:at1|at2s|trigger|hotkey):\s*(.+)$/m);
    if (triggerMatch) {
      trigger = triggerMatch[1].trim();
    }

    // Ekstrak isi pesan dari 'm:'
    const messageIndex = item.indexOf('\nm: ');
    const altMessageIndex = item.indexOf('m: ');

    let rawMessage = '';
    if (messageIndex !== -1) {
      rawMessage = item.substring(messageIndex + 4);
    } else if (altMessageIndex !== -1) {
      rawMessage = item.substring(altMessageIndex + 3);
    }

    if (rawMessage) {
      // Potong jika ada properti berikutnya di luar 'm:' (jika ada)
      // Ganti tag <ent__> menjadi baris baru (\n)
      expansion = rawMessage
        .replace(/<ent__>/gi, '\n')
        .replace(/<enter>/gi, '\n')
        .replace(/<br\s*\/?>/gi, '\n')
        .replace(/\r\n/g, '\n')
        .replace(/\r/g, '\n')
        .trim();
    }

    // Jika trigger dan expansion valid, masukkan ke array dengan auto-rename duplikat
    if (trigger && expansion) {
      let finalTrigger = cleanTriggerString(trigger);
      const normalized = finalTrigger.toLowerCase();

      // Jika trigger sudah pernah muncul sebelumnya dalam file ini
      if (seenTriggers.has(normalized)) {
        const count = seenTriggers.get(normalized)! + 1;
        seenTriggers.set(normalized, count);

        // Tambahkan sufiks urutan otomatis (contoh: /otosc_1, /otosc_2)
        finalTrigger = `${finalTrigger}_${count - 1}`;
        seenTriggers.set(finalTrigger.toLowerCase(), 1);
      } else {
        seenTriggers.set(normalized, 1);
      }

      shortcuts.push({
        trigger_code: finalTrigger,
        expansion_text: expansion,
        category: 'Perfect Keyboard',
        expansion_mode: 'INSTANT'
      });
    }
  }

  return shortcuts;
}

/**
 * Parser utama konten file Perfect Keyboard (.4pk / .kps / .txt)
 */
export function parsePerfectKeyboardFile(rawText: string): PerfectKeyboardParseResult {
  const validShortcuts: ValidShortcut[] = [];
  const failedShortcuts: FailedShortcut[] = [];
  const seenTriggers = new Map<string, number>();

  const trimmed = rawText.trim();
  if (!trimmed) {
    return { totalParsed: 0, validShortcuts: [], failedShortcuts: [] };
  }

  // 1. Format Ekspor Teks Perfect Keyboard ({end-of-item})
  if (rawText.includes('{end-of-item}')) {
    parseEndOfItemFormat(rawText, (item, lineNum) => {
      const val = validateTrigger(item.trigger, item.expansion, seenTriggers, lineNum);
      if (val.isValid) {
        validShortcuts.push({ lineNum, trigger: val.trigger, expansion: val.expansion });
      } else {
        failedShortcuts.push(val.failed);
      }
    });
  }
  // 2. Format XML / Perfect Keyboard MTW
  else if (trimmed.startsWith('<') || (trimmed.includes('<tscut>') && trimmed.includes('<macroText>')) || trimmed.includes('<macro')) {
    parseXmlFormat(rawText, (item, lineNum) => {
      const val = validateTrigger(item.trigger, item.expansion, seenTriggers, lineNum);
      if (val.isValid) {
        validShortcuts.push({ lineNum, trigger: val.trigger, expansion: val.expansion });
      } else {
        failedShortcuts.push(val.failed);
      }
    });
  } else {
    // 3. Format teks baris demi baris (Delimited / Key-Value / Bulk Text)
    parseTextFormat(rawText, (item, lineNum) => {
      const val = validateTrigger(item.trigger, item.expansion, seenTriggers, lineNum);
      if (val.isValid) {
        validShortcuts.push({ lineNum, trigger: val.trigger, expansion: val.expansion });
      } else {
        failedShortcuts.push(val.failed);
      }
    });
  }

  return {
    totalParsed: validShortcuts.length + failedShortcuts.length,
    validShortcuts,
    failedShortcuts
  };
}

/**
 * Parsing teks ekspor Perfect Keyboard dengan pembatas {end-of-item}
 */
function parseEndOfItemFormat(
  fileContent: string,
  onItem: (item: { trigger: string; expansion: string }, lineNum: number) => void
) {
  const rawItems = fileContent.split('{end-of-item}');
  let currentLine = 1;

  for (let i = 0; i < rawItems.length; i++) {
    const item = rawItems[i];
    const trimmed = item.trim();
    if (!trimmed) {
      currentLine += (item.match(/\n/g) || []).length;
      continue;
    }

    const lineNum = currentLine;
    currentLine += (item.match(/\n/g) || []).length;

    let trigger = '';
    let expansion = '';

    // Ekstrak trigger dari baris 'at1s:'
    const triggerMatch = item.match(/^at1s:\s*(.+)$/m) || item.match(/^(?:at1|at2s|trigger|hotkey|shortcut|name):\s*(.+)$/m);
    if (triggerMatch) {
      trigger = triggerMatch[1].trim();
    }

    // Ekstrak isi pesan dari 'm:'
    const messageIndex = item.indexOf('\nm: ');
    const altMessageIndex = item.indexOf('m: ');

    let rawMessage = '';
    if (messageIndex !== -1) {
      rawMessage = item.substring(messageIndex + 4);
    } else if (altMessageIndex !== -1) {
      rawMessage = item.substring(altMessageIndex + 3);
    }

    if (rawMessage) {
      expansion = rawMessage
        .replace(/<ent__>/gi, '\n')
        .replace(/<enter>/gi, '\n')
        .replace(/<br\s*\/?>/gi, '\n')
        .replace(/\r\n/g, '\n')
        .replace(/\r/g, '\n')
        .trim();
    }

    if (trigger || expansion) {
      onItem({ trigger, expansion }, lineNum);
    }
  }
}

/**
 * Parsing XML / MTW Perfect Keyboard format
 */
function parseXmlFormat(
  xmlContent: string,
  onItem: (item: { trigger: string; expansion: string }, lineNum: number) => void
) {
  // Regex mencari blok <macro>...</macro>, <item>...</item>, <entry>...</entry>, <record>...</record>
  const blockRegex = /<(?:macro|record|item|shortcut|entry|macro_item|data)\b[^>]*>([\s\S]*?)<\/(?:macro|record|item|shortcut|entry|macro_item|data)>/gi;
  let match: RegExpExecArray | null;
  let blockIndex = 0;

  while ((match = blockRegex.exec(xmlContent)) !== null) {
    blockIndex++;
    const block = match[1];

    // Cek nomor baris perkiraan dari index teks
    const lineNum = (xmlContent.substring(0, match.index).match(/\n/g) || []).length + 1;

    let trigger = '';
    let expansion = '';

    // 1. Tag Perfect Keyboard MTW standar: <tscut>...</tscut> & <macroText>...</macroText>
    const tscutMatch = /<tscut\b[^>]*>([\s\S]*?)<\/tscut>/i.exec(block);
    if (tscutMatch) {
      trigger = cleanTriggerString(tscutMatch[1]);
    }

    const macroTextMatch = /<macroText\b[^>]*>([\s\S]*?)<\/macroText>/i.exec(block);
    if (macroTextMatch) {
      expansion = cleanTextContent(macroTextMatch[1]);
    }

    // 2. Tag alternatif: <hotkey>, <trigger>, <key>, <shortcut>, <name>
    if (!trigger) {
      const keyTagMatch = /<(?:hotkey|trigger|key|shortcut|name|keyword|code)\b[^>]*>([\s\S]*?)<\/(?:hotkey|trigger|key|shortcut|name|keyword|code)>/i.exec(block);
      if (keyTagMatch) {
        trigger = cleanTriggerString(keyTagMatch[1]);
      }
    }

    // 3. Tag alternatif expansion: <text>, <expansion>, <content>, <phrase>, <value>
    if (!expansion) {
      const expTagMatch = /<(?:text|expansion|content|phrase|value|replacement)\b[^>]*>([\s\S]*?)<\/(?:text|expansion|content|phrase|value|replacement)>/i.exec(block);
      if (expTagMatch) {
        expansion = cleanTextContent(expTagMatch[1]);
      }
    }

    // 4. Jika masih kosong, coba ekstrak dari atribut tag tunggal
    if (!trigger || !expansion) {
      const attrTriggerMatch = /\b(?:tscut|trigger|hotkey|shortcut|key|name)\s*=\s*"([^"]*)"/i.exec(block);
      if (attrTriggerMatch && !trigger) {
        trigger = cleanTriggerString(attrTriggerMatch[1]);
      }
      const attrExpMatch = /\b(?:macroText|text|expansion|content|value)\s*=\s*"([^"]*)"/i.exec(block);
      if (attrExpMatch && !expansion) {
        expansion = cleanTextContent(attrExpMatch[1]);
      }
    }

    if (trigger || expansion) {
      onItem({ trigger, expansion }, lineNum || blockIndex);
    }
  }

  // Jika tidak ada container tag, coba cari tag mandiri seperti <tscut>..</tscut> <macroText>..</macroText>
  if (blockIndex === 0) {
    const inlineRegex = /<tscut\b[^>]*>([\s\S]*?)<\/tscut>[\s\S]*?<macroText\b[^>]*>([\s\S]*?)<\/macroText>/gi;
    let inlineMatch: RegExpExecArray | null;
    let idx = 0;
    while ((inlineMatch = inlineRegex.exec(xmlContent)) !== null) {
      idx++;
      const lineNum = (xmlContent.substring(0, inlineMatch.index).match(/\n/g) || []).length + 1;
      onItem(
        {
          trigger: cleanTriggerString(inlineMatch[1]),
          expansion: cleanTextContent(inlineMatch[2])
        },
        lineNum || idx
      );
    }
  }
}

/**
 * Parsing teks baris demi baris (Delimited / Key-Value format)
 */
function parseTextFormat(
  textContent: string,
  onItem: (item: { trigger: string; expansion: string }, lineNum: number) => void
) {
  const lines = textContent.split(/\r?\n/);

  for (let i = 0; i < lines.length; i++) {
    const rawLine = lines[i];
    const trimmedLine = rawLine.trim();
    const lineNum = i + 1;

    // Lewati baris kosong atau komentar
    if (!trimmedLine || trimmedLine.startsWith('#') || trimmedLine.startsWith('//') || trimmedLine.startsWith(';')) {
      continue;
    }

    let trigger: string | null = null;
    let expansion: string | null = null;

    // Format 1: TRIGGER -> TEKS
    if (trimmedLine.includes('->')) {
      const parts = trimmedLine.split('->');
      trigger = parts[0];
      expansion = parts.slice(1).join('->');
    }
    // Format 2: TRIGGER = TEKS
    else if (trimmedLine.includes('=') && !trimmedLine.startsWith('=')) {
      const parts = trimmedLine.split('=');
      trigger = parts[0];
      expansion = parts.slice(1).join('=');
    }
    // Format 3: Tab Delimited (TRIGGER \t TEKS)
    else if (trimmedLine.includes('\t')) {
      const parts = trimmedLine.split('\t');
      trigger = parts[0];
      expansion = parts.slice(1).join('\t');
    }
    // Format 4: CSV (TRIGGER, TEKS)
    else if (trimmedLine.includes(',')) {
      const parts = trimmedLine.split(',');
      trigger = parts[0];
      expansion = parts.slice(1).join(',');
    }

    if (trigger !== null && expansion !== null) {
      onItem(
        {
          trigger: cleanTriggerString(trigger),
          expansion: cleanTextContent(expansion)
        },
        lineNum
      );
    }
  }
}
