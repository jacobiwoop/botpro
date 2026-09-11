import React, { useState, useEffect, useCallback } from 'react';
import { View, FlatList, StyleSheet, RefreshControl } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useRouter } from 'expo-router';
import { Header } from '@/components/telegram/Header';
import { ChatItemRow } from '@/components/telegram/ChatItemRow';
import { FloatingActionButton } from '@/components/telegram/FloatingActionButton';
import { DrawerMenu } from '@/components/telegram/DrawerMenu';
import { CHAT_LIST_DATA } from '@/data/chatData';
import { TelegramColors } from '@/constants/telegramTheme';
import { ChatItem } from '@/types/chat';
import { fetchChats, subscribeToRealtimeMessages, ApiChat } from '@/services/api';

export default function ChatListScreen() {
  const router = useRouter();
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [chats, setChats] = useState<ChatItem[]>(CHAT_LIST_DATA);
  const [refreshing, setRefreshing] = useState(false);

  // Charger les chats depuis l'API Go
  const loadChats = useCallback(async () => {
    const apiChats = await fetchChats();
    if (apiChats && apiChats.length > 0) {
      const mapped: ChatItem[] = apiChats.map((c: ApiChat) => ({
        id: c.id.toString(),
        name: c.title,
        avatarType: 'initials',
        avatarBg: '#5288c1',
        initials: c.title.slice(0, 2).toUpperCase(),
        lastMessage: c.last_message || 'Nouvelle conversation',
        time: c.last_date || 'Aujourd\'hui',
        unreadCount: c.unread_count || undefined,
        isRead: true,
      }));

      // Fusionner avec le reste des données de démo en évitant les doublons d'id et de nom
      const nonBotDemo = CHAT_LIST_DATA.filter(
        (d) => !mapped.some((m) => m.id === d.id || m.name.toLowerCase() === d.name.toLowerCase())
      );
      setChats([...mapped, ...nonBotDemo]);
    }
  }, []);

  useEffect(() => {
    loadChats();

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
        <Header onMenuPress={() => setDrawerOpen(true)} />
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

      {/* Bouton d'action flottant (nouveau message) */}
      <FloatingActionButton />

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
