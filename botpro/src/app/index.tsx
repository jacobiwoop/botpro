import React, { useState, useEffect, useCallback } from 'react';
import { View, FlatList, StyleSheet, RefreshControl } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useRouter } from 'expo-router';
import { Header } from '@/components/telegram/Header';
import { ChatItemRow } from '@/components/telegram/ChatItemRow';
import { FloatingActionButton } from '@/components/telegram/FloatingActionButton';
import { DrawerMenu } from '@/components/telegram/DrawerMenu';
import { BotSearchModal } from '@/components/telegram/BotSearchModal';
import { TelegramColors } from '@/constants/telegramTheme';
import { ChatItem } from '@/types/chat';
import { fetchChats, subscribeToRealtimeMessages, ApiChat, getStoredToken } from '@/services/api';
import {
  getMemoryCachedChats,
  getDiskCachedChats,
  saveCachedChats,
} from '@/services/chatCache';

function mapApiChats(apiChats: ApiChat[]): ChatItem[] {
  return apiChats.map((c: ApiChat) => ({
    id: c.id.toString(),
    name: c.title,
    avatarType: 'initials',
    avatarBg: '#5288c1',
    initials: c.title.slice(0, 2).toUpperCase(),
    lastMessage: c.last_message || 'Nouvelle conversation',
    time: c.last_date || '',
    unreadCount: c.unread_count || undefined,
    isRead: true,
  }));
}

export default function ChatListScreen() {
  const router = useRouter();
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [searchModalOpen, setSearchModalOpen] = useState(false);

  // 1. Cache L1 (RAM) synchrone à 0ms
  const initialMemoryChats = getMemoryCachedChats();
  const [chats, setChats] = useState<ChatItem[]>(
    initialMemoryChats ? mapApiChats(initialMemoryChats) : []
  );
  const [refreshing, setRefreshing] = useState(false);

  // Charger les chats depuis le disque L2 puis synchroniser avec Supabase
  const loadChats = useCallback(async () => {
    // A. Disque L2
    if (!initialMemoryChats) {
      const diskChats = await getDiskCachedChats();
      if (diskChats && diskChats.length > 0) {
        setChats(mapApiChats(diskChats));
      }
    }

    // B. Revalidation réseau
    const apiChats = await fetchChats();
    if (apiChats) {
      setChats(mapApiChats(apiChats));
      await saveCachedChats(apiChats);
    }
  }, [initialMemoryChats]);

  useEffect(() => {
    const checkAuth = async () => {
      const token = await getStoredToken();
      if (!token) {
        router.replace('/auth');
        return;
      }
      loadChats();
    };
    checkAuth();

    // S'abonner aux nouveaux messages via WebSocket pour mise à jour immédiate de la liste
    const unsubscribe = subscribeToRealtimeMessages((msg) => {
      setChats((prevChats) => {
        const chatIndex = prevChats.findIndex((c) => c.id === msg.chat_id.toString());
        if (chatIndex !== -1) {
          const updatedChat = {
            ...prevChats[chatIndex],
            lastMessage: msg.text,
            time: msg.time,
          };
          // Placer la conversation active en haut de la liste
          return [updatedChat, ...prevChats.filter((_, idx) => idx !== chatIndex)];
        } else {
          // Si c'est un nouveau chat
          loadChats();
          return prevChats;
        }
      });
    });

    return () => {
      unsubscribe();
    };
  }, [loadChats]);

  const onRefresh = async () => {
    setRefreshing(true);
    await loadChats();
    setRefreshing(false);
  };

  const handleOpenChat = (item: ChatItem) => {
    router.push({
      pathname: '/conversation',
      params: {
        id: item.id,
        name: item.name,
        initials: item.initials || item.name.slice(0, 2).toUpperCase(),
      },
    });
  };

  return (
    <View style={styles.screen}>
      {/* Zone supérieure safe area avec la couleur de l'en-tête Telegram */}
      <SafeAreaView edges={['top']} style={styles.topSafeArea}>
        <Header
          onMenuPress={() => setDrawerOpen(true)}
          onSearchPress={() => setSearchModalOpen(true)}
        />
      </SafeAreaView>

      {/* Liste des conversations */}
      <FlatList
        data={chats}
        keyExtractor={(item) => item.id}
        renderItem={({ item, index }) => (
          <ChatItemRow
            item={item}
            isLast={index === chats.length - 1}
            onPress={() => handleOpenChat(item)}
          />
        )}
        refreshControl={
          <RefreshControl
            refreshing={refreshing}
            onRefresh={onRefresh}
            tintColor={TelegramColors.accent}
            colors={[TelegramColors.accent]}
          />
        }
        contentContainerStyle={styles.listContent}
        showsVerticalScrollIndicator={false}
      />

      {/* Bouton d'action flottant (nouveau message / bot) */}
      <FloatingActionButton onPress={() => setSearchModalOpen(true)} />

      {/* Modal de recherche de Bots */}
      <BotSearchModal
        visible={searchModalOpen}
        onClose={() => setSearchModalOpen(false)}
        onSelectBot={(chatId, botName) => {
          router.push({
            pathname: '/conversation',
            params: {
              id: chatId.toString(),
              name: botName,
              initials: botName.slice(0, 2).toUpperCase(),
            },
          });
        }}
      />

      {/* Menu latéral (Drawer) ouvert au clic sur l'icône hamburger */}
      <DrawerMenu
        visible={drawerOpen}
        onClose={() => setDrawerOpen(false)}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: TelegramColors.base,
  },
  topSafeArea: {
    backgroundColor: TelegramColors.header,
  },
  listContent: {
    paddingBottom: 90, // Espace pour ne pas masquer le dernier élément sous le FAB
  },
});
