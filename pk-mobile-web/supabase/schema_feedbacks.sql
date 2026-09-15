-- =========================================================================
-- SUPABASE MIGRATION: TABEL FEEDBACKS (KOTAK SARAN & MASUKAN CS)
-- =========================================================================
-- Jalankan skrip ini di: Supabase Dashboard > SQL Editor > Run

CREATE TABLE IF NOT EXISTS public.feedbacks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    user_email TEXT,
    category TEXT NOT NULL,
    message TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

-- Index untuk mempercepat query per user
CREATE INDEX IF NOT EXISTS idx_feedbacks_user_id ON public.feedbacks(user_id);
CREATE INDEX IF NOT EXISTS idx_feedbacks_created_at ON public.feedbacks(created_at DESC);

-- Row Level Security (RLS)
ALTER TABLE public.feedbacks ENABLE ROW LEVEL SECURITY;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies 
        WHERE tablename = 'feedbacks' AND policyname = 'Users can view their own feedbacks'
    ) THEN
        CREATE POLICY "Users can view their own feedbacks" 
        ON public.feedbacks FOR SELECT 
        USING (auth.uid() = user_id OR user_id IS NULL);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_policies 
        WHERE tablename = 'feedbacks' AND policyname = 'Users can insert feedbacks'
    ) THEN
        CREATE POLICY "Users can insert feedbacks" 
        ON public.feedbacks FOR INSERT 
        WITH CHECK (auth.uid() = user_id OR user_id IS NULL);
    END IF;
END $$;
