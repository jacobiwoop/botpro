import Constants from 'expo-constants';
import { Platform } from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';

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
  if (cachedToken) return cachedToken;
  try {
    cachedToken = await AsyncStorage.getItem(TOKEN_KEY);
    return cachedToken;
  } catch {
    return null;
  }
}

export async function getStoredUser(): Promise<ApiUser | null> {
  if (cachedUser) return cachedUser;
  try {
    const raw = await AsyncStorage.getItem(USER_KEY);
    if (raw) {
      cachedUser = JSON.parse(raw);
      return cachedUser;
    }
    return null;
  } catch {
    return null;
  }
}

export async function saveAuthSession(token: string, user: ApiUser): Promise<void> {
  cachedToken = token;
  cachedUser = user;
  try {
    await AsyncStorage.setItem(TOKEN_KEY, token);
    await AsyncStorage.setItem(USER_KEY, JSON.stringify(user));
  } catch (e) {
    console.warn('[Auth] Failed to save session:', e);
  }
}

export async function clearAuthSession(): Promise<void> {
  cachedToken = null;
  cachedUser = null;
  try {
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
    const res = await fetch(`${getBaseUrl()}/api/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        email,
        password,
        first_name: firstName,
        last_name: lastName,
        username,
      }),
    });
    const json = await res.json();
    if (json.ok && json.result) {
      await saveAuthSession(json.result.token, json.result.user);
      return { ok: true, user: json.result.user };
    }
    return { ok: false, error: json.description || 'Registration failed' };
  } catch (err: any) {
    return { ok: false, error: err.message || 'Network error' };
  }
}

export async function loginUser(
  email: string,
  password: string
): Promise<{ ok: boolean; user?: ApiUser; error?: string }> {
  try {
    const res = await fetch(`${getBaseUrl()}/api/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password }),
    });
    const json = await res.json();
    if (json.ok && json.result) {
      await saveAuthSession(json.result.token, json.result.user);
      return { ok: true, user: json.result.user };
    }
    return { ok: false, error: json.description || 'Login failed' };
  } catch (err: any) {
    return { ok: false, error: err.message || 'Network error' };
  }
}

export async function fetchCurrentUser(): Promise<ApiUser | null> {
  const token = await getStoredToken();
  if (!token) return null;
  try {
    const res = await fetch(`${getBaseUrl()}/api/auth/me`, {
      headers: { 'Authorization': `Bearer ${token}` },
    });
    const json = await res.json();
    if (json.ok && json.result) {
      cachedUser = json.result;
      await AsyncStorage.setItem(USER_KEY, JSON.stringify(json.result));
      return json.result;
    }
    return null;
  } catch {
    return getStoredUser();
  }
}

// ================= Telegram & Mobile REST APIs =================

export async function fetchChats(): Promise<ApiChat[]> {
  try {
    const headers = await getAuthHeaders();
    const res = await fetch(`${getBaseUrl()}/api/chats`, { headers });
    const json = await res.json();
    if (json.ok && Array.isArray(json.result)) {
      return json.result;
    }
    return [];
  } catch (error) {
    console.warn('[API] Error fetching chats:', error);
    return [];
  }
}

export async function fetchMessages(chatId: number): Promise<ApiMessage[]> {
  try {
    const headers = await getAuthHeaders();
    const res = await fetch(`${getBaseUrl()}/api/chats/${chatId}/messages`, { headers });
    const json = await res.json();
    if (json.ok && Array.isArray(json.result)) {
      return json.result;
    }
    return [];
  } catch (error) {
    console.warn(`[API] Error fetching messages for chat ${chatId}:`, error);
    return [];
  }
}

export async function sendUserMessage(chatId: number, text: string): Promise<ApiMessage | null> {
  try {
    const headers = await getAuthHeaders();
    const res = await fetch(`${getBaseUrl()}/api/chats/${chatId}/messages`, {
      method: 'POST',
      headers,
      body: JSON.stringify({ text }),
    });
    const json = await res.json();
    if (json.ok && json.result) {
      return json.result;
    }
    return null;
  } catch (error) {
    console.warn(`[API] Error sending message to chat ${chatId}:`, error);
    return null;
  }
}

export async function fetchBots(): Promise<ApiBot[]> {
  try {
    const res = await fetch(`${getBaseUrl()}/api/bots`);
    const json = await res.json();
    if (json.ok && Array.isArray(json.result)) {
      return json.result;
    }
    return [];
  } catch (error) {
    console.warn('[API] Error fetching bots:', error);
    return [];
  }
}

export async function createBot(username: string, firstName: string, about?: string): Promise<ApiBot | null> {
  try {
    const res = await fetch(`${getBaseUrl()}/api/bots`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, first_name: firstName, about }),
    });
    const json = await res.json();
    if (json.ok && json.result) {
      return json.result;
    }
    return null;
  } catch (error) {
    console.warn('[API] Error creating bot:', error);
    return null;
  }
}

export async function deleteBot(id: number): Promise<boolean> {
  try {
    const res = await fetch(`${getBaseUrl()}/api/bots/${id}`, {
      method: 'DELETE',
    });
    const json = await res.json();
    return !!json.ok;
  } catch (error) {
    console.warn(`[API] Error deleting bot ${id}:`, error);
    return false;
  }
}

export async function revokeBotToken(id: number): Promise<string | null> {
  try {
    const res = await fetch(`${getBaseUrl()}/api/bots/${id}/revoke`, {
      method: 'POST',
    });
    const json = await res.json();
    if (json.ok && json.result && json.result.token) {
      return json.result.token;
    }
    return null;
  } catch (error) {
    console.warn(`[API] Error revoking token for bot ${id}:`, error);
    return null;
  }
}

export async function fetchWebhookInfo(botToken: string): Promise<ApiWebhookInfo | null> {
  try {
    const res = await fetch(`${getBaseUrl()}/bot${botToken}/getWebhookInfo`);
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
    const res = await fetch(`${getBaseUrl()}/bot${botToken}/setWebhook`, {
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
    const res = await fetch(`${getBaseUrl()}/bot${botToken}/deleteWebhook`, {
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

// ================= WebSocket Real-Time Client =================

type MessageCallback = (msg: ApiMessage) => void;
const listeners = new Set<MessageCallback>();
let ws: WebSocket | null = null;
let reconnectTimer: any = null;

export function subscribeToRealtimeMessages(callback: MessageCallback): () => void {
  listeners.add(callback);
  ensureWebSocketConnected();
  return () => {
    listeners.delete(callback);
    if (listeners.size === 0 && ws) {
      ws.close();
      ws = null;
    }
  };
}

function ensureWebSocketConnected() {
  if (ws && (ws.readyState === WebSocket.OPEN || ws.readyState === WebSocket.CONNECTING)) {
    return;
  }

  const url = getWsUrl();
  try {
    ws = new WebSocket(url);

    ws.onopen = () => {
      console.log('[WS] Connected to Botpro Realtime Gateway at', url);
      if (reconnectTimer) {
        clearTimeout(reconnectTimer);
        reconnectTimer = null;
      }
    };

    ws.onmessage = (event) => {
      try {
        const payload = JSON.parse(event.data);
        if (payload.event === 'message' && payload.data) {
          listeners.forEach((cb) => cb(payload.data));
        }
      } catch (err) {
        console.warn('[WS] Error parsing message:', err);
      }
    };

    ws.onerror = (err) => {
      console.warn('[WS] Socket error:', err);
    };

    ws.onclose = () => {
      ws = null;
      if (listeners.size > 0 && !reconnectTimer) {
        reconnectTimer = setTimeout(() => {
          reconnectTimer = null;
          ensureWebSocketConnected();
        }, 3000);
      }
    };
  } catch (err) {
    console.warn('[WS] Failed to instantiate WebSocket:', err);
  }
}
