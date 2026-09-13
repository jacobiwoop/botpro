import React from 'react';
import { View, Text, StyleSheet, Image, TouchableOpacity } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { Message } from '@/types/conversation';

interface MessageBubbleProps {
  message: Message;
  onImageClick?: (imageUrl: string) => void;
}

export function MessageBubble({ message, onImageClick }: MessageBubbleProps) {
  const isOutgoing = message.isOutgoing;

  // Rendu des séparateurs de date (ex: "Aujourd'hui", "12 septembre")
  if (message.type === 'date_separator') {
    return (
      <View style={styles.dateSeparatorWrapper}>
        <View style={styles.dateSeparatorPill}>
          <Text style={styles.dateSeparatorText}>{message.dateText || message.text}</Text>
        </View>
      </View>
    );
  }

  // Rendu de la bulle d'image / photo
  if (message.type === 'image' && message.imageUrl) {
    return (
      <View
        style={[
          styles.bubbleWrapper,
          isOutgoing ? styles.alignRight : styles.alignLeft,
        ]}
      >
        <TouchableOpacity
          activeOpacity={0.9}
          onPress={() => onImageClick && message.imageUrl && onImageClick(message.imageUrl)}
          style={[styles.imageBubble, isOutgoing ? styles.bubbleOut : styles.bubbleIn]}
        >
          <Image
            source={{ uri: message.imageUrl }}
            style={styles.imageContent}
            resizeMode="cover"
          />
          {message.text ? (
            <Text style={styles.imageCaption}>{message.text}</Text>
          ) : null}
          <View style={styles.imageMetaBadge}>
            <Text style={styles.imageMetaTime}>{message.time}</Text>
            {isOutgoing && (
              <Ionicons
                name={message.isDoubleCheck ? 'checkmark-done' : 'checkmark'}
                size={13}
                color="#ffffff"
                style={{ marginLeft: 3 }}
              />
            )}
          </View>
        </TouchableOpacity>
      </View>
    );
  }

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
          {message.file.thumbnailUri ? (
            <Image
              source={{ uri: message.file.thumbnailUri }}
              style={styles.fileThumbnail}
            />
          ) : (
            <View style={[styles.fileThumbnail, styles.fileIconPlaceholder]}>
              <Ionicons
                name={
                  message.file.name.endsWith('.mp3') || message.file.name.endsWith('.wav')
                    ? 'musical-notes'
                    : 'document-text'
                }
                size={28}
                color="#ffffff"
              />
            </View>
          )}
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
  fileIconPlaceholder: {
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#3390ec',
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

  // Styles Séparateur de date
  dateSeparatorWrapper: {
    alignItems: 'center',
    marginVertical: 10,
  },
  dateSeparatorPill: {
    backgroundColor: 'rgba(17, 25, 33, 0.72)',
    paddingHorizontal: 14,
    paddingVertical: 4,
    borderRadius: 12,
  },
  dateSeparatorText: {
    color: '#d6e2ee',
    fontSize: 12,
    fontWeight: '600',
  },

  // Styles Bulle d'Image
  imageBubble: {
    width: 250,
    borderRadius: 16,
    overflow: 'hidden',
    shadowColor: '#000000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.25,
    shadowRadius: 3,
    elevation: 3,
  },
  imageContent: {
    width: '100%',
    height: 180,
    backgroundColor: '#17212b',
  },
  imageCaption: {
    color: '#ffffff',
    fontSize: 14,
    paddingHorizontal: 10,
    paddingTop: 6,
    paddingBottom: 4,
  },
  imageMetaBadge: {
    position: 'absolute',
    bottom: 6,
    right: 8,
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
    paddingHorizontal: 6,
    paddingVertical: 2,
    borderRadius: 10,
    flexDirection: 'row',
    alignItems: 'center',
  },
  imageMetaTime: {
    color: '#ffffff',
    fontSize: 11,
    fontWeight: '500',
  },
});
