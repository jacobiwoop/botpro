import React, { useState, useRef, useEffect, useCallback } from 'react';
import {
  View,
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
import { CONVERSATION_MESSAGES } from '@/data/conversationData';
import { TelegramColors } from '@/constants/telegramTheme';
import { Message } from '@/types/conversation';
import {
  fetchMessages,
  sendUserMessage,
  subscribeToRealtimeMessages,
  ApiMessage,
} from '@/services/api';

export default function ConversationScreen() {
  const router = useRouter();
  const params = useLocalSearchParams<{ id?: string; name?: string; initials?: string }>();

  const chatId = params.id ? parseInt(params.id, 10) : 1;
  const contactName = params.name || 'Botpro Bot';
  const contactInitials = params.initials || 'BO';

  const [messages, setMessages] = useState<Message[]>(CONVERSATION_MESSAGES);
  const flatListRef = useRef<FlatList>(null);

  // Charger les messages réels du chat depuis le serveur Go
  const loadMessages = useCallback(async () => {
    const apiMsgs = await fetchMessages(chatId);
    if (apiMsgs && apiMsgs.length > 0) {
      const formatted: Message[] = apiMsgs.map((m: ApiMessage) => ({
        id: m.id.toString(),
        type: 'text',
        isOutgoing: m.is_outgoing,
        text: m.text,
        time: m.time,
        isRead: true,
        isDoubleCheck: m.is_outgoing,
      }));
      setMessages(formatted);
      setTimeout(() => {
        flatListRef.current?.scrollToEnd({ animated: false });
      }, 100);
    }
  }, [chatId]);

  useEffect(() => {
    loadMessages();

    // Écouter les messages reçus en temps réel (bot ou automation externe)
    const unsubscribe = subscribeToRealtimeMessages((msg: ApiMessage) => {
      if (msg.chat_id === chatId) {
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

    // 2. Envoi réel au serveur Go
    const saved = await sendUserMessage(chatId, text);
    if (saved) {
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

        {/* Barre de saisie en bas avec safe area pour les gestes mobiles */}
        <SafeAreaView edges={['bottom']} style={styles.bottomSafeArea}>
          <ChatInputBar onSendMessage={handleSendMessage} />
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
  bottomSafeArea: {
    backgroundColor: TelegramColors.base,
  },
});
