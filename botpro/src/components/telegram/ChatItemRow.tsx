import React from 'react';
import { View, Text, StyleSheet, Pressable } from 'react-native';
import { MaterialIcons, Ionicons } from '@expo/vector-icons';
import { ChatItem } from '@/types/chat';
import { TelegramColors } from '@/constants/telegramTheme';
import { Avatar } from './Avatar';

interface ChatItemRowProps {
  item: ChatItem;
  isLast?: boolean;
  onPress?: () => void;
}

export function ChatItemRow({ item, isLast, onPress }: ChatItemRowProps) {
  return (
    <Pressable
      onPress={onPress}
      style={({ pressed }) => [
        styles.row,
        pressed && styles.rowPressed,
      ]}
    >
      <Avatar item={item} />
      <View style={[styles.contentContainer, !isLast && styles.contentBorder]}>
        {/* Ligne supérieure : Nom, badge vérifié, icône muet, heure */}
        <View style={styles.topRow}>
          <View style={styles.nameContainer}>
            <Text style={styles.name} numberOfLines={1}>
              {item.name}
            </Text>
            {item.isVerified && (
              <MaterialIcons name="verified" size={15} color={TelegramColors.verified} style={styles.verifiedIcon} />
            )}
            {item.isMuted && (
              <Ionicons name="volume-mute" size={13} color="rgba(126, 140, 155, 0.6)" style={styles.muteIcon} />
            )}
          </View>
          <View style={styles.timeContainer}>
            {item.isRead && (
              <Ionicons name="checkmark-done" size={15} color={TelegramColors.check} style={styles.checkIcon} />
            )}
            <Text style={styles.time}>{item.time}</Text>
          </View>
        </View>

        {/* Ligne inférieure : Dernier message, badge non-lu */}
        <View style={styles.bottomRow}>
          <Text style={styles.lastMessage} numberOfLines={1}>
            {item.lastMessage}
          </Text>
          {item.unreadCount !== undefined && item.unreadCount > 0 && (
            <View style={styles.badge}>
              <Text style={styles.badgeText}>{item.unreadCount}</Text>
            </View>
          )}
        </View>
      </View>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingLeft: 14,
    backgroundColor: TelegramColors.base,
  },
  rowPressed: {
    backgroundColor: TelegramColors.itemPress,
  },
  contentContainer: {
    flex: 1,
    paddingVertical: 12,
    paddingRight: 14,
    justifyContent: 'center',
  },
  contentBorder: {
    borderBottomWidth: 0.8,
    borderBottomColor: 'rgba(16, 25, 33, 0.8)',
  },
  topRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: 4,
  },
  nameContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    flex: 1,
    marginRight: 8,
  },
  name: {
    fontSize: 16,
    fontWeight: '600',
    color: TelegramColors.textPrimary,
    letterSpacing: 0.2,
  },
  verifiedIcon: {
    marginLeft: 4,
  },
  muteIcon: {
    marginLeft: 4,
  },
  timeContainer: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  checkIcon: {
    marginRight: 3,
  },
  time: {
    fontSize: 12,
    color: TelegramColors.textMuted,
  },
  bottomRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  lastMessage: {
    fontSize: 14.5,
    color: TelegramColors.textMuted,
    flex: 1,
    marginRight: 8,
  },
  badge: {
    backgroundColor: TelegramColors.badge,
    minWidth: 20,
    height: 20,
    borderRadius: 10,
    paddingHorizontal: 6,
    justifyContent: 'center',
    alignItems: 'center',
  },
  badgeText: {
    color: '#e2e8f0',
    fontSize: 11,
    fontWeight: '600',
  },
});
