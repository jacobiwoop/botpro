import React, { useState } from 'react';
import { View, FlatList, StyleSheet } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useRouter } from 'expo-router';
import { Header } from '@/components/telegram/Header';
import { ChatItemRow } from '@/components/telegram/ChatItemRow';
import { FloatingActionButton } from '@/components/telegram/FloatingActionButton';
import { DrawerMenu } from '@/components/telegram/DrawerMenu';
import { CHAT_LIST_DATA } from '@/data/chatData';
import { TelegramColors } from '@/constants/telegramTheme';

export default function ChatListScreen() {
  const router = useRouter();
  const [drawerOpen, setDrawerOpen] = useState(false);

  const handleOpenChat = (name: string, initials?: string) => {
    router.push({
      pathname: '/conversation',
      params: { name, initials: initials || name.slice(0, 2).toUpperCase() },
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
        data={CHAT_LIST_DATA}
        keyExtractor={(item) => item.id}
        renderItem={({ item, index }) => (
          <ChatItemRow
            item={item}
            isLast={index === CHAT_LIST_DATA.length - 1}
            onPress={() => handleOpenChat(item.name, item.initials)}
          />
        )}
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
