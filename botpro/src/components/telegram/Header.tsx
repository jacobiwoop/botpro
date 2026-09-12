import React from 'react';
import { View, Text, TouchableOpacity, StyleSheet } from 'react-native';
import { Feather } from '@expo/vector-icons';
import { TelegramColors } from '@/constants/telegramTheme';

interface HeaderProps {
  onMenuPress?: () => void;
  onSearchPress?: () => void;
}

export function Header({ onMenuPress, onSearchPress }: HeaderProps) {
  return (
    <View style={styles.header}>
      <View style={styles.leftSection}>
        <TouchableOpacity
          style={styles.iconButton}
          onPress={onMenuPress}
          hitSlop={{ top: 10, bottom: 10, left: 10, right: 10 }}
          accessibilityRole="button"
          accessibilityLabel="Menu principal"
        >
          <Feather name="menu" size={22} color="#8596a7" />
        </TouchableOpacity>
        <Text style={styles.title}>Telegram</Text>
      </View>
      <TouchableOpacity
        style={styles.iconButton}
        onPress={onSearchPress}
        hitSlop={{ top: 10, bottom: 10, left: 10, right: 10 }}
        accessibilityRole="button"
        accessibilityLabel="Recherche de bots et contacts"
      >
        <Feather name="search" size={21} color="#8596a7" />
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  header: {
    height: 56,
    backgroundColor: TelegramColors.header,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 16,
    elevation: 4,
    shadowColor: '#000000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.2,
    shadowRadius: 3,
  },
  leftSection: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 20,
  },
  iconButton: {
    padding: 4,
  },
  title: {
    fontSize: 20,
    fontWeight: '700',
    color: TelegramColors.textPrimary,
    letterSpacing: 0.3,
  },
});
