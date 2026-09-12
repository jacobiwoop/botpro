-- ============================================================
-- Migration: Synchronisation Supabase Auth -> public.users
-- ============================================================

-- 1. Ajout des colonnes de liaison
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS auth_user_id UUID UNIQUE REFERENCES auth.users(id) ON DELETE CASCADE;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS email TEXT;

CREATE INDEX IF NOT EXISTS idx_users_auth_user_id ON public.users(auth_user_id);
CREATE INDEX IF NOT EXISTS idx_users_email ON public.users(email);

-- 2. Fonction trigger pour créer automatiquement le profil Telegram et les salons de bienvenue
CREATE OR REPLACE FUNCTION public.handle_new_auth_user()
RETURNS TRIGGER AS $$
DECLARE
    new_first_name TEXT;
    new_last_name TEXT;
    new_username TEXT;
    new_telegram_id BIGINT;
    created_user_id BIGINT;
    aiko_bot_id BIGINT;
    botfather_bot_id BIGINT;
    new_chat_id BIGINT;
BEGIN
    -- Extraction des métadonnées fournies à l'inscription
    new_first_name := COALESCE(NEW.raw_user_meta_data->>'first_name', split_part(NEW.email, '@', 1));
    new_last_name := NEW.raw_user_meta_data->>'last_name';
    new_username := COALESCE(NEW.raw_user_meta_data->>'username', split_part(NEW.email, '@', 1));
    
    -- Génération d'un identifiant Telegram numérique unique
    new_telegram_id := 100000 + floor(random() * 899999)::bigint;

    -- Insertion ou mise à jour dans public.users
    INSERT INTO public.users (auth_user_id, email, telegram_id, username, first_name, last_name, is_bot)
    VALUES (NEW.id, NEW.email, new_telegram_id, new_username, new_first_name, new_last_name, FALSE)
    ON CONFLICT (auth_user_id) DO UPDATE 
    SET email = EXCLUDED.email,
        first_name = EXCLUDED.first_name,
        last_name = EXCLUDED.last_name,
        username = EXCLUDED.username
    RETURNING id INTO created_user_id;

    -- Recherche des bots officiels du système
    SELECT id INTO aiko_bot_id FROM public.users WHERE username = 'aikobot' LIMIT 1;
    SELECT id INTO botfather_bot_id FROM public.users WHERE username = 'BotFather' LIMIT 1;

    -- Création automatique de la conversation avec Aiko AI
    IF aiko_bot_id IS NOT NULL THEN
        INSERT INTO public.chats (type, title, username)
        VALUES ('private', 'Aiko AI', 'aikobot')
        RETURNING id INTO new_chat_id;

        INSERT INTO public.chat_members (chat_id, user_id, role)
        VALUES 
            (new_chat_id, created_user_id, 'creator'),
            (new_chat_id, aiko_bot_id, 'member');

        INSERT INTO public.messages (chat_id, from_user_id, text, is_bot)
        VALUES (new_chat_id, aiko_bot_id, '👋 Bonjour et bienvenue sur Botpro ! Je suis Aiko, ton assistante IA. Tape /start ou pose-moi une question ! 🌟', TRUE);
    END IF;

    -- Création automatique de la conversation avec BotFather
    IF botfather_bot_id IS NOT NULL THEN
        INSERT INTO public.chats (type, title, username)
        VALUES ('private', 'BotFather', 'BotFather')
        RETURNING id INTO new_chat_id;

        INSERT INTO public.chat_members (chat_id, user_id, role)
        VALUES 
            (new_chat_id, created_user_id, 'creator'),
            (new_chat_id, botfather_bot_id, 'member');

        INSERT INTO public.messages (chat_id, from_user_id, text, is_bot)
        VALUES (new_chat_id, botfather_bot_id, 'Je peux t''aider à créer et gérer des bots Telegram. Utilise /newbot pour créer un nouveau bot.', TRUE);
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 3. Activation du trigger sur auth.users
DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
AFTER INSERT ON auth.users
FOR EACH ROW
EXECUTE FUNCTION public.handle_new_auth_user();
