import React, { useState, useRef, useEffect, useCallback } from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  FlatList,
  StyleSheet,
  KeyboardAvoidingView,
  Platform,
  Keyboard,
  Modal,
  Image,
} from 'react-native';
import { SafeAreaView, useSafeAreaInsets } from 'react-native-safe-area-context';
import { useRouter, useLocalSearchParams } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import * as ImagePicker from 'expo-image-picker';
import * as DocumentPicker from 'expo-document-picker';
import { ConversationHeader } from '@/components/telegram/ConversationHeader';
import { MessageBubble } from '@/components/telegram/MessageBubble';
import { ChatInputBar } from '@/components/telegram/ChatInputBar';
import { ConversationSkeleton } from '@/components/telegram/ConversationSkeleton';
import {
  AttachmentBottomSheet,
  AttachmentOptionType,
} from '@/components/telegram/AttachmentBottomSheet';
import { TelegramColors } from '@/constants/telegramTheme';
import { Message } from '@/types/conversation';
import {
  fetchMessages,
  sendUserMessage,
  uploadChatMedia,
  subscribeToRealtimeMessages,
  ApiMessage,
} from '@/services/api';
import {
  getMemoryCachedMessages,
  getDiskCachedMessages,
  saveCachedMessages,
  appendCachedMessage,
} from '@/services/chatCache';

function formatApiMessages(apiMsgs: ApiMessage[]): Message[] {
  return apiMsgs.map((m: ApiMessage) => {
    const isPhoto = m.media_type === 'photo';
    const isDoc = m.media_type === 'document' || m.media_type === 'audio';

    let type: 'text' | 'image' | 'file' = 'text';
    if (isPhoto && m.media_url) {
      type = 'image';
    } else if (isDoc && m.media_url) {
      type = 'file';
    }

    return {
      id: m.id.toString(),
      type,
      imageUrl: isPhoto ? m.media_url : undefined,
      file: isDoc
        ? {
            name: m.file_name || (m.media_type === 'audio' ? 'Audio.mp3' : 'Document'),
            size: m.file_size || '1.0 MB',
            thumbnailUri: '',
          }
        : undefined,
      isOutgoing: m.is_outgoing,
      text: m.text || '',
      time: m.time,
      isRead: true,
      isDoubleCheck: m.is_outgoing,
    };
  });
}

export default function ConversationScreen() {
  const router = useRouter();
  const params = useLocalSearchParams<{ id?: string; name?: string; initials?: string }>();

  const chatId = params.id ? parseInt(params.id, 10) : 1;
  const contactName = params.name || 'Botpro Bot';
  const contactInitials = params.initials || 'BO';

  // 1. Vérifier immédiatement le Cache L1 (RAM synchrone 0ms)
  const initialCache = getMemoryCachedMessages(chatId);
  const [messages, setMessages] = useState<Message[]>(
    initialCache ? formatApiMessages(initialCache) : []
  );
  const [loading, setLoading] = useState(!initialCache);
  const insets = useSafeAreaInsets();
  const [keyboardVisible, setKeyboardVisible] = useState(false);
  const [showAttachmentSheet, setShowAttachmentSheet] = useState(false);
  const [selectedImageUrl, setSelectedImageUrl] = useState<string | null>(null);

  useEffect(() => {
    const showSub = Keyboard.addListener(
      Platform.OS === 'ios' ? 'keyboardWillShow' : 'keyboardDidShow',
      () => {
        setKeyboardVisible(true);
        setTimeout(() => {
          flatListRef.current?.scrollToEnd({ animated: true });
        }, 100);
      }
    );
    const hideSub = Keyboard.addListener(
      Platform.OS === 'ios' ? 'keyboardWillHide' : 'keyboardDidHide',
      () => setKeyboardVisible(false)
    );
    return () => {
      showSub.remove();
      hideSub.remove();
    };
  }, []);

  const flatListRef = useRef<FlatList>(null);

  // Charger les messages réels depuis le disque L2 puis synchroniser avec Supabase
  const loadMessages = useCallback(async () => {
    // A. Si pas de cache mémoire, vérifier le disque (AsyncStorage L2)
    const diskMsgs = await getDiskCachedMessages(chatId);
    if (diskMsgs && diskMsgs.length > 0) {
      setMessages((prev) => {
        if (prev.length === 0) return formatApiMessages(diskMsgs);
        return prev;
      });
      setLoading(false);
    }

    // B. Revalidation en arrière-plan avec Supabase
    try {
      const apiMsgs = await fetchMessages(chatId);
      if (apiMsgs) {
        setMessages((prev) => {
          const localExtras = prev.filter(
            (m) =>
              m.id.startsWith('temp_') ||
              m.id.startsWith('img_') ||
              m.id.startsWith('doc_') ||
              m.id.startsWith('poll_') ||
              m.id.startsWith('contact_') ||
              m.id.startsWith('loc_') ||
              m.id.startsWith('audio_')
          );
          return [...formatApiMessages(apiMsgs), ...localExtras];
        });
        await saveCachedMessages(chatId, apiMsgs);
        setTimeout(() => {
          flatListRef.current?.scrollToEnd({ animated: false });
        }, 100);
      }
    } finally {
      setLoading(false);
    }
  }, [chatId]);

  useEffect(() => {
    loadMessages();

    // Écouter les messages reçus en temps réel (bot ou automation externe)
    const unsubscribe = subscribeToRealtimeMessages((msg: ApiMessage) => {
      if (msg.chat_id === chatId) {
        appendCachedMessage(chatId, msg);
        setMessages((prev) => {
          // Si le message existe déjà avec le même ID
          const exists = prev.some((m) => m.id === msg.id.toString());
          if (exists) return prev;

          // Si c'est un message sortant qu'on a déjà affiché avec un tempId
          if (msg.is_outgoing) {
            const pendingIndex = prev.findIndex(
              (m) => m.isOutgoing && m.id.startsWith('temp_') && m.text === msg.text
            );
            if (pendingIndex !== -1) {
              const updated = [...prev];
              updated[pendingIndex] = {
                ...updated[pendingIndex],
                id: msg.id.toString(),
                time: msg.time,
                isDoubleCheck: true,
              };
              return updated;
            }
          }

          const incoming: Message = {
            id: msg.id.toString(),
            type: 'text',
            isOutgoing: msg.is_outgoing,
            text: msg.text,
            time: msg.time,
            isRead: true,
            isDoubleCheck: msg.is_outgoing,
          };
          return [...prev, incoming];
        });

        setTimeout(() => {
          flatListRef.current?.scrollToEnd({ animated: true });
        }, 100);
      }
    });

    return () => {
      unsubscribe();
    };
  }, [chatId, loadMessages]);

  // Défilement automatique au bas lors de l'ouverture du clavier
  useEffect(() => {
    const showEvent = Platform.OS === 'ios' ? 'keyboardWillShow' : 'keyboardDidShow';
    const sub = Keyboard.addListener(showEvent, () => {
      setTimeout(() => {
        flatListRef.current?.scrollToEnd({ animated: true });
      }, 80);
    });
    return () => sub.remove();
  }, []);

  const handleSendMessage = async (text: string) => {
    // 1. Ajout optimiste immédiat dans l'interface
    const tempId = `temp_${Date.now()}`;
    const optimisticMessage: Message = {
      id: tempId,
      type: 'text',
      isOutgoing: true,
      text,
      time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      isDoubleCheck: false,
    };

    setMessages((prev) => [...prev, optimisticMessage]);
    setTimeout(() => {
      flatListRef.current?.scrollToEnd({ animated: true });
    }, 50);

    // 2. Envoi réel à Supabase Edge Function
    const saved = await sendUserMessage(chatId, text);
    if (saved) {
      appendCachedMessage(chatId, saved);
      setMessages((prev) => {
        // Si le message avec cet ID a déjà été inséré via WebSocket
        if (prev.some((m) => m.id === saved.id.toString())) {
          return prev.filter((m) => m.id !== tempId);
        }
        return prev.map((m) =>
          m.id === tempId
            ? {
                ...m,
                id: saved.id.toString(),
                isDoubleCheck: true,
                time: saved.time,
              }
            : m
        );
      });
    }
  };

  const handleSelectAttachment = async (type: AttachmentOptionType) => {
    const currentTime = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

    if (type === 'gallery') {
      const result = await ImagePicker.launchImageLibraryAsync({
        mediaTypes: ['images'],
        allowsEditing: false,
        quality: 0.85,
      });

      if (!result.canceled && result.assets && result.assets.length > 0) {
        const asset = result.assets[0];
        const tempId = `img_${Date.now()}`;
        const newMsg: Message = {
          id: tempId,
          type: 'image',
          isOutgoing: true,
          imageUrl: asset.uri,
          text: 'Photo partagée 📸',
          time: currentTime,
          isDoubleCheck: false,
        };
        setMessages((prev) => [...prev, newMsg]);
        setTimeout(() => flatListRef.current?.scrollToEnd({ animated: true }), 100);

        const upload = await uploadChatMedia(asset.uri, 'image/jpeg');
        const mediaUrl = upload ? upload.publicUrl : undefined;
        const saved = await sendUserMessage(chatId, 'Photo partagée 📸', 'photo', mediaUrl);
        if (saved) {
          appendCachedMessage(chatId, saved);
          setMessages((prev) =>
            prev.map((m) =>
              m.id === tempId
                ? {
                    ...m,
                    id: saved.id.toString(),
                    imageUrl: mediaUrl || m.imageUrl,
                    isDoubleCheck: true,
                    time: saved.time,
                  }
                : m
            )
          );
        }
      }
    } else if (type === 'camera') {
      const result = await ImagePicker.launchCameraAsync({
        mediaTypes: ['images'],
        quality: 0.85,
      });

      if (!result.canceled && result.assets && result.assets.length > 0) {
        const asset = result.assets[0];
        const tempId = `img_${Date.now()}`;
        const newMsg: Message = {
          id: tempId,
          type: 'image',
          isOutgoing: true,
          imageUrl: asset.uri,
          text: 'Photo caméra 📸',
          time: currentTime,
          isDoubleCheck: false,
        };
        setMessages((prev) => [...prev, newMsg]);
        setTimeout(() => flatListRef.current?.scrollToEnd({ animated: true }), 100);

        const upload = await uploadChatMedia(asset.uri, 'image/jpeg');
        const mediaUrl = upload ? upload.publicUrl : undefined;
        const saved = await sendUserMessage(chatId, 'Photo caméra 📸', 'photo', mediaUrl);
        if (saved) {
          appendCachedMessage(chatId, saved);
          setMessages((prev) =>
            prev.map((m) =>
              m.id === tempId
                ? {
                    ...m,
                    id: saved.id.toString(),
                    imageUrl: mediaUrl || m.imageUrl,
                    isDoubleCheck: true,
                    time: saved.time,
                  }
                : m
            )
          );
        }
      }
    } else if (type === 'document') {
      const result = await DocumentPicker.getDocumentAsync({
        copyToCacheDirectory: true,
      });

      if (!result.canceled && result.assets && result.assets.length > 0) {
        const file = result.assets[0];
        const sizeStr = file.size
          ? `${(file.size / (1024 * 1024)).toFixed(1)} MB`
          : '2.4 MB';
        const tempId = `doc_${Date.now()}`;
        const newMsg: Message = {
          id: tempId,
          type: 'file',
          isOutgoing: true,
          file: {
            name: file.name,
            size: sizeStr,
            thumbnailUri: '',
          },
          time: currentTime,
          isDoubleCheck: false,
        };
        setMessages((prev) => [...prev, newMsg]);
        setTimeout(() => flatListRef.current?.scrollToEnd({ animated: true }), 100);

        const upload = await uploadChatMedia(file.uri, file.mimeType || 'application/pdf', file.name);
        const mediaUrl = upload ? upload.publicUrl : undefined;
        const saved = await sendUserMessage(chatId, file.name, 'document', mediaUrl, file.name, sizeStr);
        if (saved) {
          appendCachedMessage(chatId, saved);
          setMessages((prev) =>
            prev.map((m) =>
              m.id === tempId
                ? {
                    ...m,
                    id: saved.id.toString(),
                    isDoubleCheck: true,
                    time: saved.time,
                  }
                : m
            )
          );
        }
      }
    } else if (type === 'contact') {
      const text = '👤 Contact partagé :\nMartha Craig\n📱 +81 3-1234-5678';
      const tempId = `contact_${Date.now()}`;
      setMessages((prev) => [
        ...prev,
        {
          id: tempId,
          type: 'text',
          isOutgoing: true,
          text,
          time: currentTime,
          isDoubleCheck: false,
        },
      ]);
      setTimeout(() => flatListRef.current?.scrollToEnd({ animated: true }), 100);

      const saved = await sendUserMessage(chatId, text, 'text');
      if (saved) {
        appendCachedMessage(chatId, saved);
        setMessages((prev) =>
          prev.map((m) =>
            m.id === tempId
              ? { ...m, id: saved.id.toString(), isDoubleCheck: true, time: saved.time }
              : m
          )
        );
      }
    } else if (type === 'location') {
      const text = '📍 Position partagée :\nTour de Tokyo, Minato City, Tokyo 105-0011';
      const tempId = `loc_${Date.now()}`;
      setMessages((prev) => [
        ...prev,
        {
          id: tempId,
          type: 'text',
          isOutgoing: true,
          text,
          time: currentTime,
          isDoubleCheck: false,
        },
      ]);
      setTimeout(() => flatListRef.current?.scrollToEnd({ animated: true }), 100);

      const saved = await sendUserMessage(chatId, text, 'text');
      if (saved) {
        appendCachedMessage(chatId, saved);
        setMessages((prev) =>
          prev.map((m) =>
            m.id === tempId
              ? { ...m, id: saved.id.toString(), isDoubleCheck: true, time: saved.time }
              : m
          )
        );
      }
    } else if (type === 'audio') {
      const tempId = `audio_${Date.now()}`;
      const fileName = 'Audio_001.mp3';
      const fileSize = '3.2 MB';
      setMessages((prev) => [
        ...prev,
        {
          id: tempId,
          type: 'file',
          isOutgoing: true,
          file: {
            name: fileName,
            size: fileSize,
            thumbnailUri: '',
          },
          time: currentTime,
          isDoubleCheck: false,
        },
      ]);
      setTimeout(() => flatListRef.current?.scrollToEnd({ animated: true }), 100);

      const saved = await sendUserMessage(chatId, 'Audio vocal partagé 🎵', 'audio', undefined, fileName, fileSize);
      if (saved) {
        appendCachedMessage(chatId, saved);
        setMessages((prev) =>
          prev.map((m) =>
            m.id === tempId
              ? { ...m, id: saved.id.toString(), isDoubleCheck: true, time: saved.time }
              : m
          )
        );
      }
    } else if (type === 'poll') {
      const text = '📊 Sondage :\nQuand se retrouve-t-on pour dîner ?\n▫️ Ce soir à 20h\n▫️ Demain midi\n▫️ Ce week-end';
      const tempId = `poll_${Date.now()}`;
      setMessages((prev) => [
        ...prev,
        {
          id: tempId,
          type: 'text',
          isOutgoing: true,
          text,
          time: currentTime,
          isDoubleCheck: false,
        },
      ]);
      setTimeout(() => flatListRef.current?.scrollToEnd({ animated: true }), 100);

      const saved = await sendUserMessage(chatId, text, 'text');
      if (saved) {
        appendCachedMessage(chatId, saved);
        setMessages((prev) =>
          prev.map((m) =>
            m.id === tempId
              ? { ...m, id: saved.id.toString(), isDoubleCheck: true, time: saved.time }
              : m
          )
        );
      }
    }
  };

  return (
    <View style={styles.screen}>
      {/* Zone safe area du haut aux couleurs de l'en-tête Telegram */}
      <SafeAreaView edges={['top']} style={styles.topSafeArea}>
        <ConversationHeader
          name={contactName}
          status="en ligne"
          initials={contactInitials}
          onBack={() => router.back()}
        />
      </SafeAreaView>

      {/* Vue évitant le clavier */}
      <KeyboardAvoidingView
        style={styles.keyboardContainer}
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      >
        {/* Fil des messages aligné par le bas (style Telegram) */}
        {loading ? (
          <ConversationSkeleton />
        ) : messages.length === 0 ? (
          <View style={styles.botWelcomeContainer}>
            <View style={styles.botWelcomeCard}>
              <View style={styles.botAvatarContainer}>
                <Text style={styles.botAvatarText}>{contactInitials}</Text>
              </View>
              <Text style={styles.botWelcomeTitle}>Que peut faire ce bot ?</Text>
              <Text style={styles.botWelcomeDesc}>
                {contactName.toLowerCase().includes('aiko')
                  ? 'Assistante IA conversationnelle propulsée par Groq et Supabase. Pose-lui des questions, discute ou demande des conseils en français !'
                  : contactName.toLowerCase().includes('botfather')
                  ? 'Le bot officiel de Botpro pour créer de nouveaux bots et gérer leurs tokens et webhooks.'
                  : 'Bot interactif Botpro conforme au protocole Telegram Bot API.'}
              </Text>
            </View>
          </View>
        ) : (
          <FlatList
            ref={flatListRef}
            data={messages}
            keyExtractor={(item) => item.id}
            renderItem={({ item }) => (
              <MessageBubble
                message={item}
                onImageClick={(url) => setSelectedImageUrl(url)}
              />
            )}
            contentContainerStyle={[styles.messagesList, { flexGrow: 1, justifyContent: 'flex-end' }]}
            showsVerticalScrollIndicator={false}
            onContentSizeChange={() => {
              flatListRef.current?.scrollToEnd({ animated: false });
            }}
            onLayout={() => {
              flatListRef.current?.scrollToEnd({ animated: false });
            }}
          />
        )}

        {/* Barre de saisie ou Bouton DÉMARRER style Telegram */}
        <View
          style={[
            styles.bottomSafeArea,
            { paddingBottom: keyboardVisible ? 0 : Math.max(insets.bottom, 4) },
          ]}
        >
          {!loading && messages.length === 0 ? (
            <View style={styles.startBarContainer}>
              <TouchableOpacity
                style={styles.startButton}
                onPress={() => handleSendMessage('/start')}
                activeOpacity={0.8}
              >
                <Text style={styles.startButtonText}>DÉMARRER</Text>
              </TouchableOpacity>
            </View>
          ) : (
            <ChatInputBar
              onSendMessage={handleSendMessage}
              onAttachmentPress={() => setShowAttachmentSheet(true)}
            />
          )}
        </View>
      </KeyboardAvoidingView>

      {/* Visionneuse de Photo Plein Écran */}
      <Modal
        visible={!!selectedImageUrl}
        transparent
        animationType="fade"
        onRequestClose={() => setSelectedImageUrl(null)}
        statusBarTranslucent
      >
        <View style={styles.lightboxContainer}>
          <SafeAreaView edges={['top']} style={styles.lightboxHeader}>
            <TouchableOpacity
              style={styles.lightboxCloseBtn}
              onPress={() => setSelectedImageUrl(null)}
              hitSlop={{ top: 12, bottom: 12, left: 12, right: 12 }}
            >
              <Ionicons name="close" size={26} color="#ffffff" />
            </TouchableOpacity>
          </SafeAreaView>

          {selectedImageUrl && (
            <TouchableOpacity
              activeOpacity={1}
              style={styles.lightboxImageWrapper}
              onPress={() => setSelectedImageUrl(null)}
            >
              <Image
                source={{ uri: selectedImageUrl }}
                style={styles.lightboxImage}
                resizeMode="contain"
              />
            </TouchableOpacity>
          )}
        </View>
      </Modal>

      {/* Volet Trombone / Pièces Jointes */}
      <AttachmentBottomSheet
        visible={showAttachmentSheet}
        onClose={() => setShowAttachmentSheet(false)}
        onSelectOption={handleSelectAttachment}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: '#0e1621', // Fond du chat Telegram
  },
  topSafeArea: {
    backgroundColor: TelegramColors.header,
  },
  keyboardContainer: {
    flex: 1,
  },
  messagesList: {
    paddingVertical: 12,
    paddingHorizontal: 8,
  },
  botWelcomeContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    paddingHorizontal: 24,
  },
  botWelcomeCard: {
    backgroundColor: '#182533',
    borderRadius: 16,
    padding: 24,
    alignItems: 'center',
    maxWidth: 320,
    borderWidth: 1,
    borderColor: '#242f3d',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.3,
    shadowRadius: 8,
    elevation: 5,
  },
  botAvatarContainer: {
    width: 64,
    height: 64,
    borderRadius: 32,
    backgroundColor: '#5288c1',
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 16,
  },
  botAvatarText: {
    color: '#ffffff',
    fontSize: 24,
    fontWeight: '700',
  },
  botWelcomeTitle: {
    color: '#ffffff',
    fontSize: 17,
    fontWeight: '700',
    marginBottom: 10,
    textAlign: 'center',
  },
  botWelcomeDesc: {
    color: '#8e9dae',
    fontSize: 14,
    lineHeight: 20,
    textAlign: 'center',
  },
  bottomSafeArea: {
    backgroundColor: TelegramColors.base,
  },
  startBarContainer: {
    paddingHorizontal: 16,
    paddingVertical: 10,
    backgroundColor: TelegramColors.base,
  },
  startButton: {
    backgroundColor: '#5288c1',
    height: 48,
    borderRadius: 24,
    justifyContent: 'center',
    alignItems: 'center',
  },
  startButtonText: {
    color: '#ffffff',
    fontSize: 16,
    fontWeight: '700',
    letterSpacing: 0.5,
  },
  lightboxContainer: {
    flex: 1,
    backgroundColor: '#000000',
    justifyContent: 'center',
  },
  lightboxHeader: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    zIndex: 10,
    flexDirection: 'row',
    justifyContent: 'flex-end',
    paddingHorizontal: 16,
    paddingVertical: 10,
  },
  lightboxCloseBtn: {
    width: 44,
    height: 44,
    borderRadius: 22,
    backgroundColor: 'rgba(255, 255, 255, 0.2)',
    justifyContent: 'center',
    alignItems: 'center',
  },
  lightboxImageWrapper: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  lightboxImage: {
    width: '100%',
    height: '100%',
  },
});
