import { createClient } from '@supabase/supabase-js';

const SUPABASE_URL = process.env.NEXT_PUBLIC_SUPABASE_URL || 'https://qeacrkiuiwxqbkapmyrc.supabase.co';
const DEFAULT_ANON_KEY = process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY || 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InFlYWNya2l1aXd4cWJrYXBteXJjIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODkzOTc4OTgsImV4cCI6MjEwNDk3Mzg5OH0.z0zzzriAO62vYTUeXsDxiwYBov1ubOGQ3cSA1TyzR5E';

// Dapatkan key dari localStorage jika pengguna menyetel custom key di browser, atau gunakan default
export const getActiveAnonKey = (): string => {
  if (typeof window !== 'undefined') {
    const customKey = localStorage.getItem('pk_custom_supabase_key');
    if (customKey && customKey.trim().length > 0) {
      return customKey.trim();
    }
  }
  return DEFAULT_ANON_KEY;
};

let cachedClient: ReturnType<typeof createClient> | null = null;
let cachedKey: string | null = null;

export const getSupabaseClient = () => {
  const currentKey = getActiveAnonKey();
  if (!cachedClient || cachedKey !== currentKey) {
    cachedKey = currentKey;
    cachedClient = createClient(SUPABASE_URL, currentKey);
  }
  return cachedClient;
};

// Singleton instance standar
export const supabase = getSupabaseClient();

export interface ShortcutItem {
  id?: number | string;
  shortcut?: string;
  trigger_code?: string;
  expansion?: string;
  expansion_text?: string;
  category?: string;
  expansion_mode: 'INSTANT' | 'SPACE';
  user_id?: string;
  created_at?: string;
}
