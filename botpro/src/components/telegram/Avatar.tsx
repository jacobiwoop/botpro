import React from 'react';
import { View, Text, Image, StyleSheet } from 'react-native';
import { FontAwesome5, Ionicons } from '@expo/vector-icons';
import { ChatItem } from '@/types/chat';

interface AvatarProps {
  item: ChatItem;
}

export function Avatar({ item }: AvatarProps) {
  if (item.avatarType === 'image' && item.avatarUri) {
    return (
      <View style={styles.container}>
        <Image source={{ uri: item.avatarUri }} style={styles.image} />
      </View>
    );
  }

  if (item.avatarType === 'telegram') {
    return (
      <View style={[styles.container, styles.telegramBg]}>
        <FontAwesome5 name="paper-plane" size={24} color="#ffffff" style={styles.telegramIcon} />
      </View>
    );
  }

  if (item.avatarType === 'initials') {
    return (
      <View style={[styles.container, { backgroundColor: item.avatarBg || '#8261e6' }]}>
        <Text style={styles.initialsText}>{item.initials}</Text>
      </View>
    );
  }

  if (item.avatarType === 'getpay') {
    return (
      <View style={[styles.container, { backgroundColor: item.avatarBg || '#0095d9' }]}>
        <View style={styles.getpayCircle}>
          <Text style={styles.getpayP}>P</Text>
        </View>
        <Text style={styles.getpayText}>GETPAY</Text>
      </View>
    );
  }

  if (item.avatarType === 'dot') {
    return (
      <View style={[styles.container, { backgroundColor: item.avatarBg || '#4e97cf' }]}>
        <View style={styles.dot} />
      </View>
    );
  }

  if (item.avatarType === 'saved') {
    return (
      <View style={[styles.container, { backgroundColor: item.avatarBg || '#4ea4e6' }]}>
        <Ionicons name="bookmark-outline" size={24} color="#ffffff" />
      </View>
    );
  }

  return <View style={[styles.container, { backgroundColor: '#334155' }]} />;
}

const styles = StyleSheet.create({
  container: {
    width: 52,
    height: 52,
    borderRadius: 26,
    overflow: 'hidden',
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: 14,
  },
  image: {
    width: '100%',
    height: '100%',
    resizeMode: 'cover',
  },
  telegramBg: {
    backgroundColor: '#2693d9',
  },
  telegramIcon: {
    marginLeft: -2,
    marginTop: -2,
  },
  initialsText: {
    color: '#ffffff',
    fontSize: 18,
    fontWeight: '600',
    letterSpacing: 1,
  },
  getpayCircle: {
    width: 22,
    height: 22,
    borderRadius: 11,
    borderWidth: 1.5,
    borderColor: '#ffffff',
    justifyContent: 'center',
    alignItems: 'center',
  },
  getpayP: {
    color: '#ffffff',
    fontSize: 11,
    fontWeight: '700',
  },
  getpayText: {
    color: '#ffffff',
    fontSize: 7,
    fontWeight: '700',
    marginTop: 1,
    letterSpacing: 0.5,
  },
  dot: {
    width: 5,
    height: 5,
    borderRadius: 2.5,
    backgroundColor: '#ffffff',
  },
});
