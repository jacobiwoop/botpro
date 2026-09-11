import React from 'react';
import { View, Text, StyleSheet, Image } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { Message } from '@/types/conversation';

interface MessageBubbleProps {
  message: Message;
}

export function MessageBubble({ message }: MessageBubbleProps) {
  const isOutgoing = message.isOutgoing;

  // Rendu de la carte de fichier / photo jointe
  if (message.type === 'file' && message.file) {
    return (
      <View
        style={[
          styles.bubbleWrapper,
          isOutgoing ? styles.alignRight : styles.alignLeft,
        ]}
      >
        <View style={[styles.fileCard, isOutgoing ? styles.fileOut : styles.fileIn]}>
          <Image
            source={{ uri: message.file.thumbnailUri }}
            style={styles.fileThumbnail}
          />
          <View style={styles.fileInfo}>
            <Text style={styles.fileName} numberOfLines={1}>
              {message.file.name}
            </Text>
            <Text style={styles.fileSize}>{message.file.size}</Text>
            <View style={styles.timeAndStatus}>
              <Text style={styles.timeOutText}>{message.time}</Text>
              {isOutgoing && (
                <Ionicons
                  name={message.isDoubleCheck ? 'checkmark-done' : 'checkmark'}
                  size={14}
                  color="#7ea3cc"
                  style={styles.checkIcon}
                />
              )}
            </View>
          </View>
        </View>
      </View>
    );
  }

  // Rendu d'un message texte standard
  return (
    <View
      style={[
        styles.bubbleWrapper,
        isOutgoing ? styles.alignRight : styles.alignLeft,
      ]}
    >
      <View
        style={[
          styles.textBubble,
          isOutgoing ? styles.bubbleOut : styles.bubbleIn,
        ]}
      >
        {/* Citation / Réponse si présente */}
        {message.replyQuote && (
          <View style={styles.quoteContainer}>
            <Text style={styles.quoteSender}>{message.replyQuote.senderName}</Text>
            <Text style={styles.quoteText} numberOfLines={1}>
              {message.replyQuote.text}
            </Text>
          </View>
        )}

        {/* Corps du message */}
        <View style={styles.messageContent}>
          <Text style={styles.messageText}>{message.text}</Text>
          <View style={styles.timeContainer}>
            <Text style={isOutgoing ? styles.timeOutText : styles.timeInText}>
              {message.time}
            </Text>
            {isOutgoing && (
              <Ionicons
                name={message.isDoubleCheck ? 'checkmark-done' : 'checkmark'}
                size={14}
                color="#7ea3cc"
                style={styles.checkIcon}
              />
            )}
          </View>
        </View>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  bubbleWrapper: {
    marginVertical: 4,
    paddingHorizontal: 8,
    flexDirection: 'row',
  },
  alignRight: {
    justifyContent: 'flex-end',
  },
  alignLeft: {
    justifyContent: 'flex-start',
  },
  textBubble: {
    maxWidth: '82%',
    paddingHorizontal: 12,
    paddingVertical: 7,
    shadowColor: '#000000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.15,
    shadowRadius: 1,
    elevation: 1,
  },
  bubbleOut: {
    backgroundColor: '#2b5278',
    borderRadius: 16,
    borderBottomRightRadius: 4,
  },
  bubbleIn: {
    backgroundColor: '#182533',
    borderRadius: 16,
    borderBottomLeftRadius: 4,
  },
  quoteContainer: {
    borderLeftWidth: 2.5,
    borderLeftColor: '#549cda',
    paddingLeft: 8,
    marginBottom: 6,
    paddingVertical: 1,
  },
  quoteSender: {
    color: '#549cda',
    fontSize: 13,
    fontWeight: '700',
  },
  quoteText: {
    color: '#93a4b5',
    fontSize: 13,
  },
  messageContent: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    alignItems: 'flex-end',
    justifyContent: 'space-between',
    gap: 8,
  },
  messageText: {
    color: '#ffffff',
    fontSize: 15,
    lineHeight: 20,
    flexShrink: 1,
  },
  timeContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    alignSelf: 'flex-end',
    marginLeft: 'auto',
  },
  timeOutText: {
    color: '#7ea3cc',
    fontSize: 11,
  },
  timeInText: {
    color: '#6d7e8f',
    fontSize: 11,
  },
  checkIcon: {
    marginLeft: 3,
  },

  // Style des cartes de fichiers joints
  fileCard: {
    width: 260,
    padding: 6,
    borderRadius: 16,
    flexDirection: 'row',
    alignItems: 'center',
    shadowColor: '#000000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.2,
    shadowRadius: 2,
    elevation: 2,
  },
  fileOut: {
    backgroundColor: '#2b5278',
  },
  fileIn: {
    backgroundColor: '#182533',
  },
  fileThumbnail: {
    width: 60,
    height: 60,
    borderRadius: 12,
    backgroundColor: '#1c2938',
  },
  fileInfo: {
    flex: 1,
    marginLeft: 10,
    justifyContent: 'space-between',
    height: 56,
    paddingVertical: 2,
  },
  fileName: {
    color: '#ffffff',
    fontSize: 14.5,
    fontWeight: '600',
    letterSpacing: 0.2,
  },
  fileSize: {
    color: '#8fa8c6',
    fontSize: 12,
  },
  timeAndStatus: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'flex-end',
  },
});
