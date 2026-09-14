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
  seenTriggers: Set<string>,
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

  // 7. Cek Duplikat di Dalam File yang Sama
  const normalizedKey = trigger.toLowerCase();
  if (seenTriggers.has(normalizedKey)) {
    return {
      isValid: false,
      failed: {
        lineNum,
        rawTrigger: trigger,
        rawExpansion: expansion,
        reason: `Trigger duplikat ("${trigger}" sudah terdaftar pada baris sebelumnya di dalam file ini).`,
        suggestion: `Ubah kode trigger agar unik (misal: "${trigger}1" atau "${trigger}2").`
      }
    };
  }

  // Lolos Semua Validasi
  seenTriggers.add(normalizedKey);
  return {
    isValid: true,
    trigger,
    expansion
  };
}

/**
 * Parser utama konten file Perfect Keyboard (.4pk / .kps / .txt)
 */
export function parsePerfectKeyboardFile(rawText: string): PerfectKeyboardParseResult {
  const validShortcuts: ValidShortcut[] = [];
  const failedShortcuts: FailedShortcut[] = [];
  const seenTriggers = new Set<string>();

  const trimmed = rawText.trim();
  if (!trimmed) {
    return { totalParsed: 0, validShortcuts: [], failedShortcuts: [] };
  }

  // 1. Parsing jika dokumen berformat XML / Perfect Keyboard MTW
  if (trimmed.startsWith('<') || (trimmed.includes('<tscut>') && trimmed.includes('<macroText>')) || trimmed.includes('<macro')) {
    parseXmlFormat(rawText, (item, lineNum) => {
      const val = validateTrigger(item.trigger, item.expansion, seenTriggers, lineNum);
      if (val.isValid) {
        validShortcuts.push({ lineNum, trigger: val.trigger, expansion: val.expansion });
      } else {
        failedShortcuts.push(val.failed);
      }
    });
  } else {
    // 2. Parsing format teks baris demi baris (Delimited / Key-Value / Bulk Text)
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
