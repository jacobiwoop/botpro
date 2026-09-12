import React, { useState, useRef, useEffect, useCallback } from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  FlatList,
  StyleSheet,
  KeyboardAvoidingView,
  Platform,
  Keyboard,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useRouter, useLocalSearchParams } from 'expo-router';
import { ConversationHeader } from '@/components/telegram/ConversationHeader';
import { MessageBubble } from '@/components/telegram/MessageBubble';
import { ChatInputBar } from '@/components/telegram/ChatInputBar';
import { ConversationSkeleton } from '@/components/telegram/ConversationSkeleton';
import { TelegramColors } from '@/constants/telegramTheme';
import { Message } from '@/types/conversation';
import {
  fetchMessages,
  sendUserMessage,
  subscribeToRealtimeMessages,
  ApiMessage,
} from '@/services/api';
import {
  getMemoryCachedMessages,
  getDiskCachedMessages,
  saveCachedMessages,
  appendCachedMessage,
} from '@/services/chatCache';

function formatApiMessages(apiMsgs: ApiMessage[]): Message[] {
  return apiMsgs.map((m: ApiMessage) => ({
    id: m.id.toString(),
    type: 'text',
    isOutgoing: m.is_outgoing,
    text: m.text,
    time: m.time,
    isRead: true,
    isDoubleCheck: m.is_outgoing,
  }));
}

export default function ConversationScreen() {
  const router = useRouter();
  const params = useLocalSearchParams<{ id?: string; name?: string; initials?: string }>();

  const chatId = params.id ? parseInt(params.id, 10) : 1;
  const contactName = params.name || 'Botpro Bot';
  const contactInitials = params.initials || 'BO';

  // 1. Vérifier immédiatement le Cache L1 (RAM synchrone 0ms)
  const initialCache = getMemoryCachedMessages(chatId);
  const [messages, setMessages] = useState<Message[]>(
    initialCache ? formatApiMessages(initialCache) : []
  );
  const [loading, setLoading] = useState(!initialCache || initialCache.length === 0);
  const flatListRef = useRef<FlatList>(null);

  // Charger les messages réels depuis le disque L2 puis synchroniser avec Supabase
  const loadMessages = useCallback(async () => {
    // A. Si pas de cache mémoire, vérifier le disque (AsyncStorage L2)
    if (!initialCache) {
      const diskMsgs = await getDiskCachedMessages(chatId);
      if (diskMsgs && diskMsgs.length > 0) {
        setMessages(formatApiMessages(diskMsgs));
        setLoading(false);
      }
    }

    // B. Revalidation en arrière-plan avec Supabase
    try {
      const apiMsgs = await fetchMessages(chatId);
      if (apiMsgs) {
        setMessages(formatApiMessages(apiMsgs));
        await saveCachedMessages(chatId, apiMsgs);
        setTimeout(() => {
          flatListRef.current?.scrollToEnd({ animated: false });
        }, 100);
      }
    } finally {
      setLoading(false);
    }
  }, [chatId, initialCache]);

  useEffect(() => {
    loadMessages();

    // Écouter les messages reçus en temps réel (bot ou automation externe)
    const unsubscribe = subscribeToRealtimeMessages((msg: ApiMessage) => {
      if (msg.chat_id === chatId) {
        appendCachedMessage(chatId, msg);
        setMessages((prev) => {
          // Si le message existe déjà avec le même ID
          const exists = prev.some((m) => m.id === msg.id.toString());
          if (exists) return prev;

          // Si c'est un message sortant qu'on a déjà affiché avec un tempId
          if (msg.is_outgoing) {
            const pendingIndex = prev.findIndex(
              (m) => m.isOutgoing && m.id.startsWith('temp_') && m.text === msg.text
            );
            if (pendingIndex !== -1) {
              const updated = [...prev];
              updated[pendingIndex] = {
                ...updated[pendingIndex],
                id: msg.id.toString(),
                time: msg.time,
                isDoubleCheck: true,
              };
              return updated;
            }
          }

          const incoming: Message = {
            id: msg.id.toString(),
            type: 'text',
            isOutgoing: msg.is_outgoing,
            text: msg.text,
            time: msg.time,
            isRead: true,
            isDoubleCheck: msg.is_outgoing,
          };
          return [...prev, incoming];
        });

        setTimeout(() => {
          flatListRef.current?.scrollToEnd({ animated: true });
        }, 100);
      }
    });

    return () => {
      unsubscribe();
    };
  }, [chatId, loadMessages]);

  // Défilement automatique au bas lors de l'ouverture du clavier
  useEffect(() => {
    const showEvent = Platform.OS === 'ios' ? 'keyboardWillShow' : 'keyboardDidShow';
    const sub = Keyboard.addListener(showEvent, () => {
      setTimeout(() => {
        flatListRef.current?.scrollToEnd({ animated: true });
      }, 80);
    });
    return () => sub.remove();
  }, []);

  const handleSendMessage = async (text: string) => {
    // 1. Ajout optimiste immédiat dans l'interface
    const tempId = `temp_${Date.now()}`;
    const optimisticMessage: Message = {
      id: tempId,
      type: 'text',
      isOutgoing: true,
      text,
      time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      isDoubleCheck: false,
    };

    setMessages((prev) => [...prev, optimisticMessage]);
    setTimeout(() => {
      flatListRef.current?.scrollToEnd({ animated: true });
    }, 50);

    // 2. Envoi réel à Supabase Edge Function
    const saved = await sendUserMessage(chatId, text);
    if (saved) {
      appendCachedMessage(chatId, saved);
      setMessages((prev) => {
        // Si le message avec cet ID a déjà été inséré via WebSocket
        if (prev.some((m) => m.id === saved.id.toString())) {
          return prev.filter((m) => m.id !== tempId);
        }
        return prev.map((m) =>
          m.id === tempId
            ? {
                ...m,
                id: saved.id.toString(),
                isDoubleCheck: true,
                time: saved.time,
              }
            : m
        );
      });
    }
  };

  return (
    <View style={styles.screen}>
      {/* Zone safe area du haut aux couleurs de l'en-tête Telegram */}
      <SafeAreaView edges={['top']} style={styles.topSafeArea}>
        <ConversationHeader
          name={contactName}
          status="en ligne"
          initials={contactInitials}
          onBack={() => router.back()}
        />
      </SafeAreaView>

      {/* Vue évitant le clavier */}
      <KeyboardAvoidingView
        style={styles.keyboardContainer}
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
        keyboardVerticalOffset={Platform.OS === 'ios' ? 0 : 0}
      >
        {/* Fil des messages aligné par le bas (style Telegram) */}
        {loading ? (
          <ConversationSkeleton />
        ) : messages.length === 0 ? (
          <View style={styles.botWelcomeContainer}>
            <View style={styles.botWelcomeCard}>
              <View style={styles.botAvatarContainer}>
                <Text style={styles.botAvatarText}>{contactInitials}</Text>
              </View>
              <Text style={styles.botWelcomeTitle}>Que peut faire ce bot ?</Text>
              <Text style={styles.botWelcomeDesc}>
                {contactName.toLowerCase().includes('aiko')
                  ? 'Assistante IA conversationnelle propulsée par Groq et Supabase. Pose-lui des questions, discute ou demande des conseils en français !'
                  : contactName.toLowerCase().includes('botfather')
                  ? 'Le bot officiel de Botpro pour créer de nouveaux bots et gérer leurs tokens et webhooks.'
                  : 'Bot interactif Botpro conforme au protocole Telegram Bot API.'}
              </Text>
            </View>
          </View>
        ) : (
          <FlatList
            ref={flatListRef}
            data={messages}
            keyExtractor={(item) => item.id}
            renderItem={({ item }) => <MessageBubble message={item} />}
            contentContainerStyle={[styles.messagesList, { flexGrow: 1, justifyContent: 'flex-end' }]}
            showsVerticalScrollIndicator={false}
            onContentSizeChange={() => {
              flatListRef.current?.scrollToEnd({ animated: false });
            }}
            onLayout={() => {
              flatListRef.current?.scrollToEnd({ animated: false });
            }}
          />
        )}

        {/* Barre de saisie ou Bouton DÉMARRER style Telegram */}
        <SafeAreaView edges={['bottom']} style={styles.bottomSafeArea}>
          {!loading && messages.length === 0 ? (
            <View style={styles.startBarContainer}>
              <TouchableOpacity
                style={styles.startButton}
                onPress={() => handleSendMessage('/start')}
                activeOpacity={0.8}
              >
                <Text style={styles.startButtonText}>DÉMARRER</Text>
              </TouchableOpacity>
            </View>
          ) : (
            <ChatInputBar onSendMessage={handleSendMessage} />
          )}
        </SafeAreaView>
      </KeyboardAvoidingView>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: '#0e1621', // Fond du chat Telegram
  },
  topSafeArea: {
    backgroundColor: TelegramColors.header,
  },
  keyboardContainer: {
    flex: 1,
  },
  messagesList: {
    paddingVertical: 12,
    paddingHorizontal: 8,
  },
  botWelcomeContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    paddingHorizontal: 24,
  },
  botWelcomeCard: {
    backgroundColor: '#182533',
    borderRadius: 16,
    padding: 24,
    alignItems: 'center',
    maxWidth: 320,
    borderWidth: 1,
    borderColor: '#242f3d',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.3,
    shadowRadius: 8,
    elevation: 5,
  },
  botAvatarContainer: {
    width: 64,
    height: 64,
    borderRadius: 32,
    backgroundColor: '#5288c1',
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 16,
  },
  botAvatarText: {
    color: '#ffffff',
    fontSize: 24,
    fontWeight: '700',
  },
  botWelcomeTitle: {
    color: '#ffffff',
    fontSize: 17,
    fontWeight: '700',
    marginBottom: 10,
    textAlign: 'center',
  },
  botWelcomeDesc: {
    color: '#8e9dae',
    fontSize: 14,
    lineHeight: 20,
    textAlign: 'center',
  },
  bottomSafeArea: {
    backgroundColor: TelegramColors.base,
  },
  startBarContainer: {
    paddingHorizontal: 16,
    paddingVertical: 10,
    backgroundColor: TelegramColors.base,
  },
  startButton: {
    backgroundColor: '#5288c1',
    height: 48,
    borderRadius: 24,
    justifyContent: 'center',
    alignItems: 'center',
  },
  startButtonText: {
    color: '#ffffff',
    fontSize: 16,
    fontWeight: '700',
    letterSpacing: 0.5,
  },
});
