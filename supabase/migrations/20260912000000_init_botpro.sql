-- ============================================================
-- Migration: Initialisation de Botpro (PostgreSQL 17 / Supabase)
-- ============================================================

-- 1. Tables principales
CREATE TABLE IF NOT EXISTS public.users (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    telegram_id BIGINT UNIQUE,
    username TEXT UNIQUE,
    first_name TEXT NOT NULL,
    last_name TEXT,
    avatar_url TEXT,
    is_bot BOOLEAN DEFAULT FALSE,
    password_hash TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.bots (
    id BIGINT PRIMARY KEY REFERENCES public.users(id) ON DELETE CASCADE,
    token TEXT UNIQUE NOT NULL,
    owner_id BIGINT REFERENCES public.users(id) ON DELETE SET NULL,
    description TEXT,
    short_description TEXT,
    can_join_groups BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.chats (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    type TEXT NOT NULL CHECK (type IN ('private', 'group', 'supergroup', 'channel')),
    title TEXT,
    username TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.chat_members (
    chat_id BIGINT REFERENCES public.chats(id) ON DELETE CASCADE,
    user_id BIGINT REFERENCES public.users(id) ON DELETE CASCADE,
    role TEXT DEFAULT 'member',
    joined_at TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (chat_id, user_id)
);

CREATE TABLE IF NOT EXISTS public.messages (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    message_id BIGINT,
    chat_id BIGINT NOT NULL REFERENCES public.chats(id) ON DELETE CASCADE,
    from_user_id BIGINT REFERENCES public.users(id) ON DELETE SET NULL,
    text TEXT NOT NULL,
    reply_to_message_id BIGINT,
    reply_markup JSONB,
    is_bot BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.webhooks (
    bot_id BIGINT PRIMARY KEY REFERENCES public.bots(id) ON DELETE CASCADE,
    url TEXT NOT NULL,
    has_custom_certificate BOOLEAN DEFAULT FALSE,
    pending_update_count INT DEFAULT 0,
    last_error_date TIMESTAMPTZ,
    last_error_message TEXT,
    max_connections INT DEFAULT 40,
    allowed_updates TEXT[],
    secret_token TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.updates (
    update_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    bot_id BIGINT REFERENCES public.bots(id) ON DELETE CASCADE,
    message_id BIGINT REFERENCES public.messages(id) ON DELETE CASCADE,
    payload JSONB NOT NULL,
    is_delivered BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Index pour performances de requêtes et recherche
CREATE INDEX IF NOT EXISTS idx_users_username ON public.users(username);
CREATE INDEX IF NOT EXISTS idx_bots_token ON public.bots(token);
CREATE INDEX IF NOT EXISTS idx_messages_chat_id ON public.messages(chat_id);
CREATE INDEX IF NOT EXISTS idx_chat_members_user_id ON public.chat_members(user_id);
CREATE INDEX IF NOT EXISTS idx_updates_bot_id ON public.updates(bot_id);

-- 2. Trigger d'auto-incrémentation séquentielle de message_id par chat
CREATE OR REPLACE FUNCTION public.set_message_metadata()
RETURNS TRIGGER AS $$
BEGIN
    -- Attribution du message_id séquentiel au sein du chat
    IF NEW.message_id IS NULL THEN
        SELECT COALESCE(MAX(message_id), 0) + 1
        INTO NEW.message_id
        FROM public.messages
        WHERE chat_id = NEW.chat_id;
    END IF;

    -- Vérification si l'expéditeur est un bot
    IF NEW.from_user_id IS NOT NULL THEN
        SELECT is_bot INTO NEW.is_bot
        FROM public.users
        WHERE id = NEW.from_user_id;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trigger_set_message_metadata ON public.messages;
CREATE TRIGGER trigger_set_message_metadata
BEFORE INSERT ON public.messages
FOR EACH ROW
EXECUTE FUNCTION public.set_message_metadata();

-- 3. Configuration Row Level Security (RLS)
ALTER TABLE public.users ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.bots ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.chats ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.chat_members ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.webhooks ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.updates ENABLE ROW LEVEL SECURITY;

-- Politiques ouvertes pour lecture et écriture par l'application (anon & authenticated)
DO $$
BEGIN
    -- users
    DROP POLICY IF EXISTS "Allow all access to users" ON public.users;
    CREATE POLICY "Allow all access to users" ON public.users FOR ALL USING (true) WITH CHECK (true);

    -- bots
    DROP POLICY IF EXISTS "Allow all access to bots" ON public.bots;
    CREATE POLICY "Allow all access to bots" ON public.bots FOR ALL USING (true) WITH CHECK (true);

    -- chats
    DROP POLICY IF EXISTS "Allow all access to chats" ON public.chats;
    CREATE POLICY "Allow all access to chats" ON public.chats FOR ALL USING (true) WITH CHECK (true);

    -- chat_members
    DROP POLICY IF EXISTS "Allow all access to chat_members" ON public.chat_members;
    CREATE POLICY "Allow all access to chat_members" ON public.chat_members FOR ALL USING (true) WITH CHECK (true);

    -- messages
    DROP POLICY IF EXISTS "Allow all access to messages" ON public.messages;
    CREATE POLICY "Allow all access to messages" ON public.messages FOR ALL USING (true) WITH CHECK (true);

    -- webhooks
    DROP POLICY IF EXISTS "Allow all access to webhooks" ON public.webhooks;
    CREATE POLICY "Allow all access to webhooks" ON public.webhooks FOR ALL USING (true) WITH CHECK (true);

    -- updates
    DROP POLICY IF EXISTS "Allow all access to updates" ON public.updates;
    CREATE POLICY "Allow all access to updates" ON public.updates FOR ALL USING (true) WITH CHECK (true);
END $$;

-- 4. Publication Realtime pour les messages et les chats
DO $$
BEGIN
    BEGIN
        ALTER PUBLICATION supabase_realtime ADD TABLE public.messages;
    EXCEPTION WHEN duplicate_object THEN
        NULL;
    END;

    BEGIN
        ALTER PUBLICATION supabase_realtime ADD TABLE public.chats;
    EXCEPTION WHEN duplicate_object THEN
        NULL;
    END;
END $$;

-- 5. Données de test / Initiales (BotFather, Aiko AI, Utilisateur test)
INSERT INTO public.users (telegram_id, username, first_name, is_bot)
VALUES 
    (999999, 'BotFather', 'BotFather', TRUE),
    (7123456789, 'aikobot', 'Aiko AI', TRUE),
    (1001, 'desmarc', 'Desmarc', FALSE)
ON CONFLICT (username) DO NOTHING;

-- Enregistrement des bots dans la table bots
INSERT INTO public.bots (id, token, owner_id, description, short_description)
SELECT 
    u.id, 
    'botfather:master_token', 
    u.id, 
    'Official BotFather bot for Botpro', 
    'Manage Botpro bots'
FROM public.users u
WHERE u.username = 'BotFather'
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.bots (id, token, owner_id, description, short_description)
SELECT 
    u.id, 
    '7123456789:AAHk1234567890abcdefghijklmnopqrstuv', 
    (SELECT id FROM public.users WHERE username = 'desmarc'), 
    'Aiko AI Chat Bot powered by Groq', 
    'Friendly conversation AI'
FROM public.users u
WHERE u.username = 'aikobot'
ON CONFLICT (id) DO NOTHING;
