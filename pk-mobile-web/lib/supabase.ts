import { createClient } from '@supabase/supabase-js';

const SUPABASE_URL = process.env.NEXT_PUBLIC_SUPABASE_URL || 'https://qeacrkiuiwxqbkapmyrc.supabase.co';
const SUPABASE_ANON_KEY = process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY || 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InFlYWNya2l1aXd4cWJrYXBteXJjIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODkzOTc4OTgsImV4cCI6MjEwNDk3Mzg5OH0.z0zzzriAO62vYTUeXsDxiwYBov1ubOGQ3cSA1TyzR5E';

export const getActiveAnonKey = (): string => {
  return SUPABASE_ANON_KEY;
};

// Singleton Supabase Client terautentikasi melalui Environment Variables
export const supabase = createClient(SUPABASE_URL, SUPABASE_ANON_KEY);

export const getSupabaseClient = () => {
  return supabase;
};

export interface PresetItem {
  id: string;
  name: string;
  is_active: boolean;
  user_id?: string;
  created_at?: string;
  shortcut_count?: number;
}

export interface ShortcutItem {
  id?: number | string;
  preset_id?: string;
  shortcut?: string;
  trigger_code?: string;
  expansion?: string;
  expansion_text?: string;
  category?: string;
  expansion_mode: 'INSTANT' | 'SPACE';
  user_id?: string;
  created_at?: string;
}

export interface FeedbackItem {
  id?: string;
  user_id?: string;
  user_email?: string;
  category: string;
  message: string;
  created_at?: string;
}
