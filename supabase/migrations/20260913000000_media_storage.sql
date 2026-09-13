-- ============================================================
-- Migration: Support Multimédia & Stockage Supabase (Botpro)
-- ============================================================

-- 1. Évolution de la table des messages
ALTER TABLE public.messages ALTER COLUMN text DROP NOT NULL;

ALTER TABLE public.messages ADD COLUMN IF NOT EXISTS media_type TEXT DEFAULT 'text';
ALTER TABLE public.messages ADD COLUMN IF NOT EXISTS media_url TEXT;
ALTER TABLE public.messages ADD COLUMN IF NOT EXISTS file_name TEXT;
ALTER TABLE public.messages ADD COLUMN IF NOT EXISTS file_size TEXT;

-- Index pour requêtes par type de média
CREATE INDEX IF NOT EXISTS idx_messages_media_type ON public.messages(media_type);

-- 2. Création et configuration du Bucket Supabase Storage 'chat-media'
INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES (
    'chat-media',
    'chat-media',
    true,
    52428800, -- 50 Mo
    ARRAY['image/*', 'audio/*', 'video/*', 'application/*', 'text/*']
)
ON CONFLICT (id) DO UPDATE SET 
    public = true,
    file_size_limit = 52428800;

-- 3. Politiques RLS sur storage.objects pour le bucket 'chat-media'
DO $$
BEGIN
    DROP POLICY IF EXISTS "Public Read chat-media" ON storage.objects;
    CREATE POLICY "Public Read chat-media" ON storage.objects
        FOR SELECT USING (bucket_id = 'chat-media');

    DROP POLICY IF EXISTS "Allow upload chat-media" ON storage.objects;
    CREATE POLICY "Allow upload chat-media" ON storage.objects
        FOR INSERT WITH CHECK (bucket_id = 'chat-media');

    DROP POLICY IF EXISTS "Allow delete chat-media" ON storage.objects;
    CREATE POLICY "Allow delete chat-media" ON storage.objects
        FOR DELETE USING (bucket_id = 'chat-media');
END $$;
