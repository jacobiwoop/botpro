import React, { useState, useEffect } from 'react';
import {
  Modal,
  View,
  Text,
  TextInput,
  TouchableOpacity,
  FlatList,
  StyleSheet,
  ActivityIndicator,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Feather, MaterialCommunityIcons } from '@expo/vector-icons';
import { TelegramColors } from '@/constants/telegramTheme';
import { searchBots, ApiBot, getOrCreateBotChat } from '@/services/api';

interface BotSearchModalProps {
  visible: boolean;
  onClose: () => void;
  onSelectBot: (chatId: number, botName: string, botUsername: string) => void;
}

export function BotSearchModal({ visible, onClose, onSelectBot }: BotSearchModalProps) {
  const [query, setQuery] = useState('');
  const [bots, setBots] = useState<ApiBot[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (visible) {
      loadBots(query);
    }
  }, [visible, query]);

  const loadBots = async (q: string) => {
    setLoading(true);
    const results = await searchBots(q);
    setBots(results);
    setLoading(false);
  };

  const handlePickBot = async (bot: ApiBot) => {
    setLoading(true);
    const chatId = await getOrCreateBotChat(bot.id, bot.first_name, bot.username);
    setLoading(false);
    onClose();
    onSelectBot(chatId, bot.first_name, bot.username);
  };

  return (
    <Modal visible={visible} animationType="slide" transparent={false} onRequestClose={onClose}>
      <SafeAreaView style={styles.container}>
        {/* Header de recherche */}
        <View style={styles.header}>
          <TouchableOpacity onPress={onClose} style={styles.backButton}>
            <Feather name="arrow-left" size={24} color="#8596a7" />
          </TouchableOpacity>
          <TextInput
            style={styles.searchInput}
            placeholder="Rechercher un bot (@aikobot, BotFather)..."
            placeholderTextColor="#8596a7"
            value={query}
            onChangeText={setQuery}
            autoFocus
            autoCapitalize="none"
          />
          {query.length > 0 && (
            <TouchableOpacity onPress={() => setQuery('')} style={styles.clearButton}>
              <Feather name="x" size={20} color="#8596a7" />
            </TouchableOpacity>
          )}
        </View>

        {/* Sous-titre section */}
        <View style={styles.sectionHeader}>
          <Text style={styles.sectionTitle}>BOTS DISPONIBLES DANS BOTPRO</Text>
        </View>

        {loading ? (
          <View style={styles.loadingContainer}>
            <ActivityIndicator size="small" color={TelegramColors.accent} />
          </View>
        ) : (
          <FlatList
            data={bots}
            keyExtractor={(item) => item.id.toString()}
            renderItem={({ item }) => (
              <TouchableOpacity
                style={styles.botRow}
                onPress={() => handlePickBot(item)}
                activeOpacity={0.7}
              >
                <View style={styles.avatar}>
                  <MaterialCommunityIcons name="robot" size={26} color="#ffffff" />
                </View>
                <View style={styles.info}>
                  <View style={styles.titleRow}>
                    <Text style={styles.name}>{item.first_name}</Text>
                    <View style={styles.badge}>
                      <Text style={styles.badgeText}>BOT</Text>
                    </View>
                  </View>
                  <Text style={styles.username}>@{item.username}</Text>
                  {item.about ? <Text style={styles.about}>{item.about}</Text> : null}
                </View>
                <Feather name="chevron-right" size={20} color="#8596a7" />
              </TouchableOpacity>
            )}
            ListEmptyComponent={
              <View style={styles.emptyContainer}>
                <Text style={styles.emptyText}>Aucun bot trouvé pour « {query} »</Text>
              </View>
            }
          />
        )}
      </SafeAreaView>
    </Modal>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: TelegramColors.base,
  },
  header: {
    height: 56,
    backgroundColor: TelegramColors.header,
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 12,
    gap: 12,
  },
  backButton: {
    padding: 6,
  },
  searchInput: {
    flex: 1,
    color: '#ffffff',
    fontSize: 16,
    paddingVertical: 8,
  },
  clearButton: {
    padding: 6,
  },
  sectionHeader: {
    paddingHorizontal: 16,
    paddingVertical: 12,
    backgroundColor: '#131e2b',
  },
  sectionTitle: {
    color: TelegramColors.accent,
    fontSize: 12,
    fontWeight: '700',
    letterSpacing: 0.5,
  },
  loadingContainer: {
    padding: 30,
    alignItems: 'center',
  },
  botRow: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingVertical: 12,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: '#242f3d',
    gap: 14,
  },
  avatar: {
    width: 48,
    height: 48,
    borderRadius: 24,
    backgroundColor: '#5288c1',
    justifyContent: 'center',
    alignItems: 'center',
  },
  info: {
    flex: 1,
  },
  titleRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  name: {
    color: '#ffffff',
    fontSize: 16,
    fontWeight: '600',
  },
  badge: {
    backgroundColor: '#2b5278',
    paddingHorizontal: 6,
    paddingVertical: 2,
    borderRadius: 4,
  },
  badgeText: {
    color: '#64b5f6',
    fontSize: 10,
    fontWeight: '700',
  },
  username: {
    color: TelegramColors.accent,
    fontSize: 13,
    marginTop: 2,
  },
  about: {
    color: '#8e9dae',
    fontSize: 12,
    marginTop: 3,
  },
  emptyContainer: {
    padding: 40,
    alignItems: 'center',
  },
  emptyText: {
    color: '#8e9dae',
    fontSize: 15,
  },
});
