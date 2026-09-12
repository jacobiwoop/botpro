import AsyncStorage from '@react-native-async-storage/async-storage';
import { ApiMessage, ApiChat } from './api';

// Cache L1 (RAM / Mémoire vive) pour un accès synchrone à 0 ms
const memoryMessagesCache = new Map<number, ApiMessage[]>();
let memoryChatsCache: ApiChat[] | null = null;

// Clés pour le Cache L2 (Disque / AsyncStorage)
const CHATS_CACHE_KEY = 'botpro_cache_chats';
const getMessagesKey = (chatId: number) => `botpro_cache_messages_${chatId}`;

// ================= Chats (Liste de conversations) =================

export function getMemoryCachedChats(): ApiChat[] | null {
  return memoryChatsCache;
}

export async function getDiskCachedChats(): Promise<ApiChat[]> {
  try {
    const raw = await AsyncStorage.getItem(CHATS_CACHE_KEY);
    if (raw) {
      const parsed = JSON.parse(raw);
      memoryChatsCache = parsed;
      return parsed;
    }
  } catch (e) {
    console.warn('[Cache] Error reading chats from disk:', e);
  }
  return [];
}

export async function saveCachedChats(chats: ApiChat[]): Promise<void> {
  memoryChatsCache = chats;
  try {
    await AsyncStorage.setItem(CHATS_CACHE_KEY, JSON.stringify(chats));
  } catch (e) {
    console.warn('[Cache] Error saving chats to disk:', e);
  }
}

// ================= Messages (Fil de discussion) =================

export function getMemoryCachedMessages(chatId: number): ApiMessage[] | null {
  return memoryMessagesCache.get(chatId) || null;
}

export async function getDiskCachedMessages(chatId: number): Promise<ApiMessage[]> {
  try {
    const raw = await AsyncStorage.getItem(getMessagesKey(chatId));
    if (raw) {
      const parsed: ApiMessage[] = JSON.parse(raw);
      memoryMessagesCache.set(chatId, parsed);
      return parsed;
    }
  } catch (e) {
    console.warn(`[Cache] Error reading messages for chat ${chatId} from disk:`, e);
  }
  return [];
}

export async function saveCachedMessages(chatId: number, messages: ApiMessage[]): Promise<void> {
  memoryMessagesCache.set(chatId, messages);
  try {
    await AsyncStorage.setItem(getMessagesKey(chatId), JSON.stringify(messages));
  } catch (e) {
    console.warn(`[Cache] Error saving messages for chat ${chatId} to disk:`, e);
  }
}

export async function appendCachedMessage(chatId: number, message: ApiMessage): Promise<void> {
  const current = memoryMessagesCache.get(chatId) || [];
  if (!current.some((m) => m.id === message.id)) {
    const updated = [...current, message];
    await saveCachedMessages(chatId, updated);
  }
}
