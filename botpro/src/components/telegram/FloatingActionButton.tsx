import React from 'react';
import { TouchableOpacity, StyleSheet } from 'react-native';
import { MaterialCommunityIcons } from '@expo/vector-icons';
import { TelegramColors } from '@/constants/telegramTheme';

export function FloatingActionButton() {
  return (
    <TouchableOpacity
      style={styles.fab}
      activeOpacity={0.85}
      accessibilityRole="button"
      accessibilityLabel="Nouveau message"
    >
      <MaterialCommunityIcons name="pencil" size={24} color="#ffffff" />
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  fab: {
    position: 'absolute',
    right: 18,
    bottom: 24,
    width: 56,
    height: 56,
    borderRadius: 28,
    backgroundColor: TelegramColors.fab,
    justifyContent: 'center',
    alignItems: 'center',
    elevation: 6,
    shadowColor: '#000000',
    shadowOffset: { width: 0, height: 3 },
    shadowOpacity: 0.35,
    shadowRadius: 5,
  },
});
