import React, { useState, useRef } from 'react';
import {
  View,
  FlatList,
  StyleSheet,
  KeyboardAvoidingView,
  Platform,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useRouter, useLocalSearchParams } from 'expo-router';
import { ConversationHeader } from '@/components/telegram/ConversationHeader';
import { MessageBubble } from '@/components/telegram/MessageBubble';
import { ChatInputBar } from '@/components/telegram/ChatInputBar';
import { CONVERSATION_MESSAGES } from '@/data/conversationData';
import { TelegramColors } from '@/constants/telegramTheme';
import { Message } from '@/types/conversation';

export default function ConversationScreen() {
  const router = useRouter();
  const params = useLocalSearchParams<{ name?: string; initials?: string }>();

  // Utilise les informations de l'image ("Hernes Dav", "last seen recently", "HE")
  const contactName = params.name || 'Hernes Dav';
  const contactInitials = params.initials || 'HE';

  const [messages, setMessages] = useState<Message[]>(CONVERSATION_MESSAGES);
  const flatListRef = useRef<FlatList>(null);

  const handleSendMessage = (text: string) => {
    const newMessage: Message = {
      id: `msg_${Date.now()}`,
      type: 'text',
      isOutgoing: true,
      text,
      time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      isDoubleCheck: false,
    };
    setMessages((prev) => [...prev, newMessage]);
    setTimeout(() => {
      flatListRef.current?.scrollToEnd({ animated: true });
    }, 100);
  };

  return (
    <View style={styles.screen}>
      {/* Zone safe area du haut aux couleurs de l'en-tête Telegram */}
      <SafeAreaView edges={['top']} style={styles.topSafeArea}>
        <ConversationHeader
          name={contactName}
          status="last seen recently"
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
        {/* Fil des messages */}
        <FlatList
          ref={flatListRef}
          data={messages}
          keyExtractor={(item) => item.id}
          renderItem={({ item }) => <MessageBubble message={item} />}
          contentContainerStyle={styles.messagesList}
          showsVerticalScrollIndicator={false}
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
