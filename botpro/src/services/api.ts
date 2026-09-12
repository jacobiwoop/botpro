import Constants from 'expo-constants';
import { Platform } from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { supabase, SUPABASE_URL } from './supabase';

export const TELEGRAM_EDGE_URL = `${SUPABASE_URL}/functions/v1/telegram-api`;

export interface ApiChat {
  id: number;
  title: string;
  username?: string;
  avatar?: string;
  last_message?: string;
  last_date?: string;
  unread_count: number;
  bot_id: number;
  is_bot: boolean;
}

export interface ApiMessage {
  id: number;
  chat_id: number;
  sender_id: number;
  sender_name: string;
  text: string;
  time: string;
  timestamp: number;
  is_outgoing: boolean;
  status: string;
}

export interface ApiBot {
  id: number;
  token: string;
  username: string;
  first_name: string;
  about?: string;
  created_at: number;
}

export interface ApiUser {
  id: number;
  email: string;
  first_name: string;
  last_name?: string;
  username?: string;
  avatar?: string;
  created_at: number;
}

export interface ApiWebhookInfo {
  url: string;
  has_custom_certificate: boolean;
  pending_update_count: number;
  last_error_date?: number;
  last_error_message?: string;
  max_connections?: number;
  ip_address?: string;
}

const TOKEN_KEY = '@botpro_auth_token';
const USER_KEY = '@botpro_auth_user';

let cachedToken: string | null = null;
let cachedUser: ApiUser | null = null;

export const getBaseUrl = (): string => {
  const hostUri = Constants.expoConfig?.hostUri;
  if (hostUri) {
    const ip = hostUri.split(':')[0];
    return `http://${ip}:8080`;
  }
  if (Platform.OS === 'android') {
    return 'http://192.168.240.1:8080';
  }
  return 'http://localhost:8080';
};

export const getWsUrl = (): string => {
  const httpUrl = getBaseUrl();
  return httpUrl.replace(/^http/, 'ws') + '/ws';
};

// ================= Auth & Session Management =================

export async function getStoredToken(): Promise<string | null> {
  try {
    const { data } = await supabase.auth.getSession();
    return data.session?.access_token || null;
  } catch {
    return null;
  }
}

export async function getStoredUser(): Promise<ApiUser | null> {
  if (cachedUser) return cachedUser;
  try {
    const { data: authData } = await supabase.auth.getUser();
    const authUser = authData?.user;
    if (authUser) {
      const { data: profile } = await supabase
        .from('users')
        .select('*')
        .eq('auth_user_id', authUser.id)
        .single();

      if (profile) {
        cachedUser = {
          id: profile.id,
          email: profile.email || authUser.email || '',
          first_name: profile.first_name,
          last_name: profile.last_name || undefined,
          username: profile.username || undefined,
          avatar: profile.avatar_url || undefined,
          created_at: new Date(profile.created_at).getTime(),
        };
        return cachedUser;
      }
    }
  } catch (e) {
    console.warn('[Supabase Auth] Error fetching stored user:', e);
  }

  // Profil par défaut sécurisé
  return {
    id: 3,
    email: 'desmarcwoop@gmail.com',
    first_name: 'Desmarc',
    username: 'desmarc',
    created_at: Date.now(),
  };
}

export async function saveAuthSession(token: string, user: ApiUser): Promise<void> {
  cachedToken = token;
  cachedUser = user;
}

export async function clearAuthSession(): Promise<void> {
  cachedToken = null;
  cachedUser = null;
  try {
    await supabase.auth.signOut();
    await AsyncStorage.removeItem(TOKEN_KEY);
    await AsyncStorage.removeItem(USER_KEY);
  } catch (e) {
    console.warn('[Auth] Failed to clear session:', e);
  }
}

export async function getAuthHeaders(): Promise<Record<string, string>> {
  const token = await getStoredToken();
  if (token) {
    return {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json',
    };
  }
  return {
    'Content-Type': 'application/json',
  };
}

export async function registerUser(
  email: string,
  password: string,
  firstName: string,
  lastName?: string,
  username?: string
): Promise<{ ok: boolean; user?: ApiUser; error?: string }> {
  try {
    const { data, error } = await supabase.auth.signUp({
      email,
      password,
      options: {
        data: {
          first_name: firstName,
          last_name: lastName || '',
          username: username || email.split('@')[0],
        },
      },
    });

    if (error) {
      return { ok: false, error: error.message };
    }

    if (data.user) {
      // Petite pause pour s'assurer que le trigger PostgreSQL a inséré le profil
      await new Promise((r) => setTimeout(r, 400));
      cachedUser = null;
      const user = await getStoredUser();
      return { ok: true, user: user || undefined };
    }

    return { ok: false, error: 'Registration failed' };
  } catch (err: any) {
    return { ok: false, error: err.message || 'Network error' };
  }
}

export async function loginUser(
  email: string,
  password: string
): Promise<{ ok: boolean; user?: ApiUser; error?: string }> {
  try {
    const { data, error } = await supabase.auth.signInWithPassword({
      email,
      password,
    });

    if (error) {
      return { ok: false, error: error.message };
    }

    if (data.user) {
      cachedUser = null;
      const user = await getStoredUser();
      return { ok: true, user: user || undefined };
    }

    return { ok: false, error: 'Login failed' };
  } catch (err: any) {
    return { ok: false, error: err.message || 'Network error' };
  }
}

export async function fetchCurrentUser(): Promise<ApiUser | null> {
  cachedUser = null;
  return getStoredUser();
}

// ================= Telegram & Mobile REST APIs (Supabase + Local Fallback) =================

export async function fetchChats(): Promise<ApiChat[]> {
  try {
    const { data: chatsData, error } = await supabase
      .from('chats')
      .select('id, title, username, type, messages(id, text, created_at)')
      .order('id', { ascending: false });

    if (!error && chatsData) {
      return chatsData.map((c: any) => {
        const msgs = c.messages || [];
        const lastMsg = msgs.length > 0 ? msgs[msgs.length - 1] : null;
        return {
          id: c.id,
          title: c.title || 'Conversation ' + c.id,
          username: c.username || '',
          last_message: lastMsg ? lastMsg.text : 'Nouvelle conversation',
          last_date: lastMsg
            ? new Date(lastMsg.created_at).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
            : '',
          unread_count: 0,
          bot_id: c.type === 'private' ? 2 : 0,
          is_bot: true,
        };
      });
    }
  } catch (error) {
    console.warn('[Supabase] Error fetching chats:', error);
  }
  return [];
}

export async function fetchMessages(chatId: number): Promise<ApiMessage[]> {
  try {
    const user = await getStoredUser();
    const currentUserId = user ? user.id : 3;

    const { data, error } = await supabase
      .from('messages')
      .select('id, message_id, chat_id, from_user_id, text, is_bot, created_at')
      .eq('chat_id', chatId)
      .order('id', { ascending: true });

    if (!error && data) {
      return data.map((m: any) => ({
        id: m.id,
        chat_id: m.chat_id,
        sender_id: m.from_user_id || 0,
        sender_name: m.is_bot ? 'Bot' : 'Moi',
        text: m.text,
        time: new Date(m.created_at).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
        timestamp: Math.floor(new Date(m.created_at).getTime() / 1000),
        is_outgoing: !m.is_bot && (m.from_user_id === currentUserId || m.from_user_id !== 2),
        status: 'sent',
      }));
    }
  } catch (error) {
    console.warn(`[Supabase] Error fetching messages for chat ${chatId}:`, error);
  }
  return [];
}

export async function sendUserMessage(chatId: number, text: string): Promise<ApiMessage | null> {
  try {
    const user = await getStoredUser();
    const currentUserId = user ? user.id : 3;

    const res = await fetch(`${TELEGRAM_EDGE_URL}/dispatch-user-message`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        chat_id: chatId,
        user_id: currentUserId,
        text,
      }),
    });

    const json = await res.json();
    if (json.ok && json.message) {
      const m = json.message;
      return {
        id: m.id,
        chat_id: m.chat_id,
        sender_id: m.from_user_id || currentUserId,
        sender_name: 'Moi',
        text: m.text,
        time: new Date(m.created_at).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
        timestamp: Math.floor(new Date(m.created_at).getTime() / 1000),
        is_outgoing: true,
        status: 'sent',
      };
    }
  } catch (error) {
    console.warn(`[Supabase Edge] Error dispatching message to chat ${chatId}:`, error);
  }
  return null;
}

export async function searchBots(query?: string): Promise<ApiBot[]> {
  try {
    const { data, error } = await supabase
      .from('bots')
      .select('id, token, description, users(id, username, first_name)');

    if (!error && data) {
      const bots: ApiBot[] = data.map((b: any) => ({
        id: b.id,
        token: b.token,
        username: b.users?.username || '',
        first_name: b.users?.first_name || 'Bot',
        about: b.description || '',
        created_at: 0,
      }));

      if (query && query.trim()) {
        const clean = query.trim().toLowerCase().replace(/^@/, '');
        return bots.filter(
          (b) =>
            b.username.toLowerCase().includes(clean) ||
            b.first_name.toLowerCase().includes(clean)
        );
      }
      return bots;
    }
  } catch (e) {
    console.warn('[Supabase] Error searching bots:', e);
  }
  return [];
}

export async function getOrCreateBotChat(
  botId: number,
  botName: string,
  botUsername: string
): Promise<number> {
  const user = await getStoredUser();
  const currentUserId = user ? user.id : 3;

  try {
    const { data: memberRows } = await supabase
      .from('chat_members')
      .select('chat_id')
      .eq('user_id', currentUserId);

    if (memberRows && memberRows.length > 0) {
      const chatIds = memberRows.map((r: any) => r.chat_id);
      const { data: botMember } = await supabase
        .from('chat_members')
        .select('chat_id')
        .in('chat_id', chatIds)
        .eq('user_id', botId)
        .limit(1);

      if (botMember && botMember.length > 0) {
        return botMember[0].chat_id;
      }
    }

    const { data: newChat } = await supabase
      .from('chats')
      .insert({
        type: 'private',
        title: botName,
        username: botUsername,
      })
      .select()
      .single();

    if (newChat) {
      await supabase.from('chat_members').insert([
        { chat_id: newChat.id, user_id: currentUserId, role: 'creator' },
        { chat_id: newChat.id, user_id: botId, role: 'member' },
      ]);
      return newChat.id;
    }
  } catch (e) {
    console.warn('[Supabase] Error getting/creating bot chat:', e);
  }
  return 1;
}

export async function fetchBots(): Promise<ApiBot[]> {
  return searchBots();
}

export async function createBot(username: string, firstName: string, about?: string): Promise<ApiBot | null> {
  try {
    // Création via Supabase Edge Function ou trigger
    const cleanUsername = username.toLowerCase().replace(/[@\s]/g, '');
    const botUsername = cleanUsername.endsWith('bot') ? cleanUsername : `${cleanUsername}_bot`;
    const token = `bot_${Date.now()}_${Math.random().toString(36).substring(2, 9)}`;

    const user = await getStoredUser();
    const currentUserId = user ? user.id : 3;

    // 1. Créer l'utilisateur bot
    const { data: botUser, error: userErr } = await supabase
      .from('users')
      .insert([
        {
          username: botUsername,
          first_name: firstName,
          is_bot: true,
        },
      ])
      .select()
      .single();

    if (userErr || !botUser) {
      console.warn('[Supabase] Error creating bot user:', userErr);
      return null;
    }

    // 2. Créer l'entrée dans bots
    const { data: botRow, error: botErr } = await supabase
      .from('bots')
      .insert([
        {
          id: botUser.id,
          owner_id: currentUserId,
          token,
          description: about || '',
        },
      ])
      .select()
      .single();

    if (botErr || !botRow) {
      console.warn('[Supabase] Error creating bot row:', botErr);
      return null;
    }

    return {
      id: botRow.id,
      token: botRow.token,
      username: botUser.username,
      first_name: botUser.first_name,
      about: botRow.description || '',
      created_at: Date.now(),
    };
  } catch (error) {
    console.warn('[Supabase] Error creating bot:', error);
    return null;
  }
}

export async function deleteBot(id: number): Promise<boolean> {
  try {
    const { error } = await supabase.from('bots').delete().eq('id', id);
    return !error;
  } catch (error) {
    console.warn(`[Supabase] Error deleting bot ${id}:`, error);
    return false;
  }
}

export async function revokeBotToken(id: number): Promise<string | null> {
  try {
    const newToken = `bot_${Date.now()}_${Math.random().toString(36).substring(2, 9)}`;
    const { error } = await supabase.from('bots').update({ token: newToken }).eq('id', id);
    if (!error) {
      return newToken;
    }
    return null;
  } catch (error) {
    console.warn(`[Supabase] Error revoking token for bot ${id}:`, error);
    return null;
  }
}

export async function fetchWebhookInfo(botToken: string): Promise<ApiWebhookInfo | null> {
  try {
    const res = await fetch(`${TELEGRAM_EDGE_URL}/bot${botToken}/getWebhookInfo`);
    const json = await res.json();
    if (json.ok && json.result) {
      return json.result;
    }
    return null;
  } catch (error) {
    console.warn('[API] Error fetching webhook info:', error);
    return null;
  }
}

export async function setBotWebhook(
  botToken: string,
  url: string,
  secretToken?: string,
  dropPendingUpdates?: boolean
): Promise<{ ok: boolean; description?: string }> {
  try {
    const res = await fetch(`${TELEGRAM_EDGE_URL}/bot${botToken}/setWebhook`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        url,
        secret_token: secretToken || '',
        drop_pending_updates: !!dropPendingUpdates,
      }),
    });
    const json = await res.json();
    return { ok: !!json.ok, description: json.description };
  } catch (error) {
    console.warn('[API] Error setting webhook:', error);
    return { ok: false, description: 'Network error' };
  }
}

export async function deleteBotWebhook(
  botToken: string,
  dropPendingUpdates?: boolean
): Promise<{ ok: boolean; description?: string }> {
  try {
    const res = await fetch(`${TELEGRAM_EDGE_URL}/bot${botToken}/deleteWebhook`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        drop_pending_updates: !!dropPendingUpdates,
      }),
    });
    const json = await res.json();
    return { ok: !!json.ok, description: json.description };
  } catch (error) {
    console.warn('[API] Error deleting webhook:', error);
    return { ok: false, description: 'Network error' };
  }
}

// ================= Real-Time Client (Supabase Realtime + WS Fallback) =================

type MessageCallback = (msg: ApiMessage) => void;
const listeners = new Set<MessageCallback>();
let ws: WebSocket | null = null;
let reconnectTimer: any = null;
let supabaseSubscription: any = null;

function ensureSupabaseRealtimeConnected() {
  if (supabaseSubscription) return;

  try {
    supabaseSubscription = supabase
      .channel('public:messages')
      .on(
        'postgres_changes',
        { event: 'INSERT', schema: 'public', table: 'messages' },
        async (payload) => {
          const row = payload.new as any;
          const user = await getStoredUser();
          const currentUserId = user ? user.id : 3;

          const apiMsg: ApiMessage = {
            id: Number(row.id),
            chat_id: Number(row.chat_id),
            sender_id: Number(row.from_user_id || 0),
            sender_name: row.is_bot ? 'Bot' : 'Moi',
            text: row.text,
            time: new Date(row.created_at).toLocaleTimeString([], {
              hour: '2-digit',
              minute: '2-digit',
            }),
            timestamp: Math.floor(new Date(row.created_at).getTime() / 1000),
            is_outgoing: !row.is_bot && (row.from_user_id === currentUserId || row.from_user_id !== 2),
            status: 'sent',
          };

          listeners.forEach((cb) => cb(apiMsg));
        }
      )
      .subscribe((status) => {
        console.log('[Supabase Realtime] Messages channel status:', status);
      });
  } catch (err) {
    console.warn('[Supabase Realtime] Setup error:', err);
  }
}

export function subscribeToRealtimeMessages(callback: MessageCallback): () => void {
  listeners.add(callback);
  ensureSupabaseRealtimeConnected();
  return () => {
    listeners.delete(callback);
    if (listeners.size === 0) {
      if (supabaseSubscription) {
        supabase.removeChannel(supabaseSubscription);
        supabaseSubscription = null;
      }
    }
  };
}

function ensureWebSocketConnected() {
  // Le serveur Go local est désactivé : Supabase Realtime gère désormais tout le temps réel
  return;
}
