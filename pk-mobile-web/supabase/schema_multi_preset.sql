-- =========================================================================
-- SUPABASE MIGRATION: MULTI-PRESET & ISOLASI SHORTCUT TERISOLASI
-- =========================================================================
-- Jalankan skrip ini di: Supabase Dashboard > SQL Editor > Run
-- Skrip ini idempotent dan aman (tidak akan menghapus data shortcut yang sudah ada).

-- 1. Buat Tabel `presets` jika belum ada
CREATE TABLE IF NOT EXISTS public.presets (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT false,
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

-- Index untuk performa query preset per user
CREATE INDEX IF NOT EXISTS idx_presets_user_active ON public.presets(user_id, is_active);

-- 2. Tambahkan kolom `preset_id` ke tabel `shortcuts` jika belum ada
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = 'public' 
          AND table_name = 'shortcuts' 
          AND column_name = 'preset_id'
    ) THEN
        ALTER TABLE public.shortcuts ADD COLUMN preset_id TEXT;
    END IF;
END $$;

-- Index untuk filter shortcut per preset
CREATE INDEX IF NOT EXISTS idx_shortcuts_preset_id ON public.shortcuts(preset_id);

-- 3. Buat default preset untuk pengguna yang sudah ada jika belum memiliki preset
INSERT INTO public.presets (id, name, is_active, user_id)
SELECT 
    'preset_default_' || substring(id::text, 1, 8), 
    'Paket Utama (Bawaan)', 
    true, 
    id 
FROM auth.users
ON CONFLICT (id) DO NOTHING;

-- 4. Hubungkan seluruh shortcut lama yang belum memiliki preset_id ke preset default
UPDATE public.shortcuts s
SET preset_id = (
    SELECT p.id 
    FROM public.presets p 
    WHERE p.user_id = s.user_id 
    ORDER BY p.created_at ASC 
    LIMIT 1
)
WHERE s.preset_id IS NULL;

-- Fallback: Jika ada shortcut dengan user_id null atau belum terpetakan, set ke 'default_preset'
UPDATE public.shortcuts 
SET preset_id = 'default_preset' 
WHERE preset_id IS NULL;

-- 5. Perbarui Constraint Unik pada tabel `shortcuts`
-- Agar trigger yang sama dapat eksis di preset yang berbeda tanpa saling menimpa
DO $$
BEGIN
    -- Hapus constraint unik lama jika ada
    IF EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'shortcuts_user_id_trigger_code_key'
    ) THEN
        ALTER TABLE public.shortcuts DROP CONSTRAINT shortcuts_user_id_trigger_code_key;
    END IF;

    -- Buat constraint compound unik baru: (user_id, preset_id, trigger_code)
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'shortcuts_user_preset_trigger_key'
    ) THEN
        ALTER TABLE public.shortcuts 
        ADD CONSTRAINT shortcuts_user_preset_trigger_key UNIQUE (user_id, preset_id, trigger_code);
    END IF;
EXCEPTION
    WHEN others THEN 
        RAISE NOTICE 'Constraint update skipped or table structure differs: %', SQLERRM;
END $$;

-- 6. Atur Row Level Security (RLS) pada tabel `presets`
ALTER TABLE public.presets ENABLE ROW LEVEL SECURITY;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies 
        WHERE tablename = 'presets' AND policyname = 'Users can view their own presets'
    ) THEN
        CREATE POLICY "Users can view their own presets" 
        ON public.presets FOR SELECT 
        USING (auth.uid() = user_id OR user_id IS NULL);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_policies 
        WHERE tablename = 'presets' AND policyname = 'Users can insert their own presets'
    ) THEN
        CREATE POLICY "Users can insert their own presets" 
        ON public.presets FOR INSERT 
        WITH CHECK (auth.uid() = user_id OR user_id IS NULL);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_policies 
        WHERE tablename = 'presets' AND policyname = 'Users can update their own presets'
    ) THEN
        CREATE POLICY "Users can update their own presets" 
        ON public.presets FOR UPDATE 
        USING (auth.uid() = user_id OR user_id IS NULL);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_policies 
        WHERE tablename = 'presets' AND policyname = 'Users can delete their own presets'
    ) THEN
        CREATE POLICY "Users can delete their own presets" 
        ON public.presets FOR DELETE 
        USING (auth.uid() = user_id OR user_id IS NULL);
    END IF;
END $$;
