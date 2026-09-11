import React from 'react';
import { View, Text, TouchableOpacity, StyleSheet, Image } from 'react-native';
import { Feather } from '@expo/vector-icons';
import { TelegramColors } from '@/constants/telegramTheme';

interface ConversationHeaderProps {
  name: string;
  status: string;
  avatarUri?: string;
  initials?: string;
  onBack: () => void;
}

export function ConversationHeader({
  name,
  status,
  avatarUri,
  initials = 'HE',
  onBack,
}: ConversationHeaderProps) {
  return (
    <View style={styles.header}>
      {/* Section gauche : Flèche retour + Avatar + Infos contact */}
      <View style={styles.leftSection}>
        <TouchableOpacity
          style={styles.backButton}
          onPress={onBack}
          hitSlop={{ top: 10, bottom: 10, left: 10, right: 10 }}
          accessibilityRole="button"
          accessibilityLabel="Retour"
        >
          <Feather name="arrow-left" size={23} color="#ffffff" />
        </TouchableOpacity>

        {/* Avatar du contact */}
        <View style={styles.avatarContainer}>
          {avatarUri ? (
            <Image source={{ uri: avatarUri }} style={styles.avatarImage} />
          ) : (
            <View style={styles.initialsAvatar}>
              <Text style={styles.initialsText}>{initials}</Text>
            </View>
          )}
        </View>

        {/* Nom et statut */}
        <View style={styles.titleContainer}>
          <Text style={styles.name} numberOfLines={1}>
            {name}
          </Text>
          <Text style={styles.status} numberOfLines={1}>
            {status}
          </Text>
        </View>
      </View>

      {/* Section droite : Menu 3 points (SANS le bouton appel, comme demandé) */}
      <TouchableOpacity
        style={styles.menuButton}
        hitSlop={{ top: 10, bottom: 10, left: 10, right: 10 }}
        accessibilityRole="button"
        accessibilityLabel="Options"
      >
        <Feather name="more-vertical" size={21} color="#8596a7" />
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
    paddingHorizontal: 12,
    elevation: 4,
    shadowColor: '#000000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.2,
    shadowRadius: 3,
  },
  leftSection: {
    flexDirection: 'row',
    alignItems: 'center',
    flex: 1,
  },
  backButton: {
    padding: 6,
    marginRight: 6,
  },
  avatarContainer: {
    width: 38,
    height: 38,
    borderRadius: 19,
    overflow: 'hidden',
    marginRight: 10,
  },
  avatarImage: {
    width: '100%',
    height: '100%',
    resizeMode: 'cover',
  },
  initialsAvatar: {
    width: '100%',
    height: '100%',
    backgroundColor: '#4e9bd9',
    justifyContent: 'center',
    alignItems: 'center',
  },
  initialsText: {
    color: '#ffffff',
    fontSize: 14,
    fontWeight: '700',
  },
  titleContainer: {
    justifyContent: 'center',
    flex: 1,
  },
  name: {
    fontSize: 16.5,
    fontWeight: '700',
    color: TelegramColors.textPrimary,
    letterSpacing: 0.2,
  },
  status: {
    fontSize: 12,
    color: TelegramColors.textMuted,
    marginTop: 1,
  },
  menuButton: {
    padding: 6,
  },
});
