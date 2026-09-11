import React, { useState } from 'react';
import { View, TextInput, TouchableOpacity, StyleSheet, Platform } from 'react-native';
import { Feather, Ionicons } from '@expo/vector-icons';
import { TelegramColors } from '@/constants/telegramTheme';

interface ChatInputBarProps {
  onSendMessage?: (text: string) => void;
}

export function ChatInputBar({ onSendMessage }: ChatInputBarProps) {
  const [text, setText] = useState('');

  const handleSend = () => {
    if (text.trim() && onSendMessage) {
      onSendMessage(text.trim());
      setText('');
    }
  };

  return (
    <View style={styles.container}>
      {/* Bouton pièce jointe (trombone) */}
      <TouchableOpacity style={styles.iconButton} hitSlop={{ top: 8, bottom: 8, left: 8, right: 8 }}>
        <Feather name="paperclip" size={22} color="#6f8295" />
      </TouchableOpacity>

      {/* Conteneur champ de texte façon Telegram */}
      <View style={styles.inputContainer}>
        <TextInput
          style={styles.input}
          placeholder="Message"
          placeholderTextColor="#687b8e"
          value={text}
          onChangeText={setText}
          multiline
          maxLength={1000}
        />
        {/* Bouton stickers/emojis */}
        <TouchableOpacity style={styles.emojiButton} hitSlop={{ top: 8, bottom: 8, left: 8, right: 8 }}>
          <Feather name="smile" size={20} color="#6f8295" />
        </TouchableOpacity>
      </View>

      {/* Bouton micro (ou bouton envoyer si du texte est saisi) */}
      {text.trim().length > 0 ? (
        <TouchableOpacity style={styles.sendButton} onPress={handleSend}>
          <Ionicons name="send" size={20} color="#ffffff" />
        </TouchableOpacity>
      ) : (
        <TouchableOpacity style={styles.iconButton} hitSlop={{ top: 8, bottom: 8, left: 8, right: 8 }}>
          <Feather name="mic" size={22} color="#6f8295" />
        </TouchableOpacity>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: TelegramColors.base,
    paddingHorizontal: 8,
    paddingVertical: 8,
    borderTopWidth: 0.8,
    borderTopColor: '#0f1621',
  },
  iconButton: {
    padding: 8,
  },
  inputContainer: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#0f1621',
    borderRadius: 22,
    paddingHorizontal: 14,
    paddingVertical: Platform.OS === 'ios' ? 8 : 4,
    marginHorizontal: 4,
    minHeight: 40,
    maxHeight: 100,
  },
  input: {
    flex: 1,
    color: '#ffffff',
    fontSize: 16,
    padding: 0,
  },
  emojiButton: {
    paddingLeft: 6,
  },
  sendButton: {
    width: 38,
    height: 38,
    borderRadius: 19,
    backgroundColor: TelegramColors.accent,
    justifyContent: 'center',
    alignItems: 'center',
    marginLeft: 4,
  },
});
