import React, { useState, useEffect, useMemo, useRef } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TextInput,
  TouchableOpacity,
  ScrollView,
  FlatList,
  ActivityIndicator,
  Alert,
  Animated,
  KeyboardAvoidingView,
  Platform,
  Modal,
  Switch,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useRouter } from 'expo-router';
import {
  Ionicons,
  Feather,
  MaterialCommunityIcons,
} from '@expo/vector-icons';
import * as Clipboard from 'expo-clipboard';
import {
  ApiBot,
  ApiWebhookInfo,
  fetchBots,
  createBot,
  deleteBot,
  revokeBotToken,
  fetchChats,
  fetchWebhookInfo,
  setBotWebhook,
  deleteBotWebhook,
} from '@/services/api';
import { TelegramColors } from '@/constants/telegramTheme';

// Couleurs d'avatars Telegram pour varier l'affichage
const AVATAR_COLORS = [
  '#52a6e6', // Bleu
  '#e17076', // Rouge/Corail
  '#6ecc51', // Vert
  '#efa141', // Orange
  '#a695e7', // Violet
  '#2aabee', // Cyan
];

function getAvatarColor(str: string): string {
  let hash = 0;
  for (let i = 0; i < str.length; i++) {
    hash = str.charCodeAt(i) + ((hash << 5) - hash);
  }
  const index = Math.abs(hash) % AVATAR_COLORS.length;
  return AVATAR_COLORS[index];
}

export default function BotFatherScreen() {
  const router = useRouter();

  // Mode d'affichage : 'list' | 'create' | 'details'
  const [viewMode, setViewMode] = useState<'list' | 'create' | 'details'>('list');
  const [bots, setBots] = useState<ApiBot[]>([]);
  const [selectedBot, setSelectedBot] = useState<ApiBot | null>(null);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');

  // Formulaire de création
  const [botName, setBotName] = useState('');
  const [botAbout, setBotAbout] = useState('');
  const [botUsername, setBotUsername] = useState('');
  const [creating, setCreating] = useState(false);

  // État token masqué / affiché
  const [showToken, setShowToken] = useState(false);
  const [revoking, setRevoking] = useState(false);

  // État Webhook
  const [webhookInfo, setWebhookInfo] = useState<ApiWebhookInfo | null>(null);
  const [loadingWebhook, setLoadingWebhook] = useState(false);
  const [webhookModalVisible, setWebhookModalVisible] = useState(false);
  const [webhookUrlInput, setWebhookUrlInput] = useState('');
  const [webhookSecretInput, setWebhookSecretInput] = useState('');
  const [dropPendingUpdates, setDropPendingUpdates] = useState(false);
  const [savingWebhook, setSavingWebhook] = useState(false);
  const [deletingWebhook, setDeletingWebhook] = useState(false);

  const loadWebhook = async (token: string) => {
    setLoadingWebhook(true);
    const info = await fetchWebhookInfo(token);
    setWebhookInfo(info);
    setLoadingWebhook(false);
  };

  useEffect(() => {
    if (selectedBot?.token && viewMode === 'details') {
      loadWebhook(selectedBot.token);
    }
  }, [selectedBot, viewMode]);

  const handleOpenWebhookModal = () => {
    setWebhookUrlInput(webhookInfo?.url || '');
    setWebhookSecretInput('');
    setDropPendingUpdates(false);
    setWebhookModalVisible(true);
  };

  const handleSaveWebhook = async () => {
    if (!selectedBot?.token) return;
    const cleanUrl = webhookUrlInput.trim();
    if (!cleanUrl) {
      Alert.alert('Error', 'Please enter a valid webhook URL.');
      return;
    }
    setSavingWebhook(true);
    const res = await setBotWebhook(
      selectedBot.token,
      cleanUrl,
      webhookSecretInput.trim() || undefined,
      dropPendingUpdates
    );
    setSavingWebhook(false);
    if (res.ok) {
      setWebhookModalVisible(false);
      showToast('✓ Webhook set successfully!');
      loadWebhook(selectedBot.token);
    } else {
      Alert.alert('Error', res.description || 'Failed to set webhook.');
    }
  };

  const handleDeleteWebhook = () => {
    if (!selectedBot?.token) return;
    Alert.alert(
      'Delete Webhook',
      'Are you sure you want to delete this webhook? Realtime WebSockets and Long Polling will remain active.',
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Delete',
          style: 'destructive',
          onPress: async () => {
            setDeletingWebhook(true);
            const res = await deleteBotWebhook(selectedBot.token);
            setDeletingWebhook(false);
            if (res.ok) {
              showToast('✓ Webhook deleted');
              loadWebhook(selectedBot.token);
            } else {
              Alert.alert('Error', res.description || 'Failed to delete webhook.');
            }
          },
        },
      ]
    );
  };

  // Notification Toast personnalisée
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const toastAnim = useRef(new Animated.Value(0)).current;

  const showToast = (message: string) => {
    setToastMessage(message);
    Animated.sequence([
      Animated.timing(toastAnim, {
        toValue: 1,
        duration: 250,
        useNativeDriver: true,
      }),
      Animated.delay(2200),
      Animated.timing(toastAnim, {
        toValue: 0,
        duration: 250,
        useNativeDriver: true,
      }),
    ]).start(() => {
      setToastMessage(null);
    });
  };

  // Chargement des bots existants
  const loadBots = async () => {
    setLoading(true);
    const data = await fetchBots();
    setBots(data);
    setLoading(false);
  };

  useEffect(() => {
    loadBots();
  }, []);

  // Validation du username Telegram pour bots
  const trimmedUsername = botUsername.trim();
  const isUsernameValid = useMemo(() => {
    if (trimmedUsername.length < 4) return false;
    const lower = trimmedUsername.toLowerCase();
    // Doit se terminer par 'bot' (ex: tetris_bot ou TetrisBot)
    if (!lower.endsWith('bot')) return false;
    // Caractères valides : lettres, chiffres, underscore
    return /^[a-zA-Z0-9_]+$/.test(trimmedUsername);
  }, [trimmedUsername]);

  const canCreate = botName.trim().length > 0 && isUsernameValid && !creating;

  // Création du bot
  const handleCreateBot = async () => {
    if (!canCreate) return;
    setCreating(true);
    const newBot = await createBot(trimmedUsername, botName.trim(), botAbout.trim());
    setCreating(false);

    if (newBot) {
      setBots((prev) => [newBot, ...prev]);
      setSelectedBot(newBot);
      setBotName('');
      setBotAbout('');
      setBotUsername('');
      setViewMode('details');
      showToast('✓ Bot created successfully!');
    } else {
      Alert.alert('Error', 'Failed to create bot. Username might already be taken.');
    }
  };

  // Copie du token
  const handleCopyToken = async () => {
    if (!selectedBot?.token) return;
    await Clipboard.setStringAsync(selectedBot.token);
    showToast('✓ Token copied to clipboard');
  };

  // Révocation du token
  const handleRevokeToken = () => {
    if (!selectedBot) return;
    Alert.alert(
      'Revoke Token?',
      `Are you sure you want to revoke the token for @${selectedBot.username}? Any existing integration using this token will stop working immediately.`,
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Revoke',
          style: 'destructive',
          onPress: async () => {
            setRevoking(true);
            const newToken = await revokeBotToken(selectedBot.id);
            setRevoking(false);
            if (newToken) {
              const updated = { ...selectedBot, token: newToken };
              setSelectedBot(updated);
              setBots((prev) => prev.map((b) => (b.id === updated.id ? updated : b)));
              showToast('✓ New token generated!');
            } else {
              Alert.alert('Error', 'Failed to revoke token.');
            }
          },
        },
      ]
    );
  };

  // Suppression du bot
  const handleDeleteBot = () => {
    if (!selectedBot) return;
    Alert.alert(
      'Delete Bot?',
      `Are you sure you want to permanently delete @${selectedBot.username}? All chats and updates associated with this bot will be deleted.`,
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Delete',
          style: 'destructive',
          onPress: async () => {
            const success = await deleteBot(selectedBot.id);
            if (success) {
              setBots((prev) => prev.filter((b) => b.id !== selectedBot.id));
              setSelectedBot(null);
              setViewMode('list');
              showToast('Bot deleted.');
            } else {
              Alert.alert('Error', 'Failed to delete bot.');
            }
          },
        },
      ]
    );
  };

  // Ouvrir la conversation avec ce bot
  const handleOpenChat = async () => {
    if (!selectedBot) return;
    const chats = await fetchChats();
    const existing = chats.find((c) => c.bot_id === selectedBot.id);
    if (existing) {
      router.push({
        pathname: '/conversation',
        params: {
          id: existing.id.toString(),
          title: existing.title,
          username: existing.username,
        },
      });
    } else {
      // S'il n'y a pas encore de chat, on ouvre un chat fictif ou retour
      router.push({
        pathname: '/conversation',
        params: {
          id: selectedBot.id.toString(),
          title: selectedBot.first_name,
          username: selectedBot.username,
        },
      });
    }
  };

  // Filtrage des bots pour la recherche
  const filteredBots = useMemo(() => {
    if (!searchQuery.trim()) return bots;
    const q = searchQuery.toLowerCase();
    return bots.filter(
      (b) =>
        b.first_name.toLowerCase().includes(q) ||
        b.username.toLowerCase().includes(q)
    );
  }, [bots, searchQuery]);

  // Masquage du token
  const formatMaskedToken = (token: string) => {
    if (!token) return '';
    const parts = token.split(':');
    if (parts.length === 2) {
      return `${parts[0]}:AAH••••••••••••••••••••••••••••`;
    }
    return token.substring(0, 8) + '••••••••••••••••••••••••••••';
  };

  // Navigation en arrière
  const handleBack = () => {
    if (viewMode === 'create' || viewMode === 'details') {
      setViewMode('list');
    } else {
      router.back();
    }
  };

  return (
    <SafeAreaView edges={['top', 'bottom']} style={styles.safeArea}>
      {/* Toast Animé */}
      {toastMessage && (
        <Animated.View
          style={[
            styles.toastContainer,
            {
              opacity: toastAnim,
              transform: [
                {
                  translateY: toastAnim.interpolate({
                    inputRange: [0, 1],
                    outputRange: [-20, 0],
                  }),
                },
              ],
            },
          ]}
        >
          <Text style={styles.toastText}>{toastMessage}</Text>
        </Animated.View>
      )}

      {/* Barre de navigation supérieure */}
      <View style={styles.topBar}>
        <TouchableOpacity style={styles.topBarButton} onPress={handleBack} activeOpacity={0.7}>
          <Ionicons name="arrow-back" size={24} color="#ffffff" />
        </TouchableOpacity>

        <View style={styles.topBarTitleContainer}>
          <Text style={styles.topBarTitle} numberOfLines={1}>
            {viewMode === 'list'
              ? 'BotFather'
              : viewMode === 'create'
              ? 'New bot'
              : selectedBot?.first_name || 'Bot Details'}
          </Text>
          <Text style={styles.topBarSubtitle}>
            {viewMode === 'create' ? 'bot creation' : 'bot'}
          </Text>
        </View>

        <View style={styles.topBarRight}>
          {viewMode === 'details' ? (
            <TouchableOpacity onPress={handleDeleteBot} style={styles.topBarButton} activeOpacity={0.7}>
              <Ionicons name="trash-outline" size={22} color="#e17076" />
            </TouchableOpacity>
          ) : viewMode === 'list' ? (
            <TouchableOpacity onPress={loadBots} style={styles.topBarButton} activeOpacity={0.7}>
              <Ionicons name="refresh" size={20} color="#52a6e6" />
            </TouchableOpacity>
          ) : (
            <View style={{ width: 36 }} />
          )}
        </View>
      </View>

      {/* ========================================================================= */}
      {/* VUE 1 : LISTE DES BOTS (BotFather Home)                                   */}
      {/* ========================================================================= */}
      {viewMode === 'list' && (
        <ScrollView
          style={styles.contentContainer}
          contentContainerStyle={styles.scrollContent}
          showsVerticalScrollIndicator={false}
        >
          {/* Header Profile BotFather */}
          <View style={styles.botFatherHeader}>
            <View style={styles.botFatherAvatar}>
              <MaterialCommunityIcons name="robot" size={44} color="#ffffff" />
            </View>
            <Text style={styles.botFatherTitle}>BotFather</Text>
            <Text style={styles.botFatherDescription}>
              BotFather is the one bot to rule them all. Use it to create new bot accounts and manage your existing bots.
            </Text>
          </View>

          {/* Barre de recherche */}
          <View style={styles.searchBarContainer}>
            <Ionicons name="search" size={18} color="#6c7883" style={styles.searchIcon} />
            <TextInput
              style={styles.searchInput}
              placeholder="Search bots"
              placeholderTextColor="#6c7883"
              value={searchQuery}
              onChangeText={setSearchQuery}
            />
            {searchQuery.length > 0 && (
              <TouchableOpacity onPress={() => setSearchQuery('')}>
                <Ionicons name="close-circle" size={18} color="#6c7883" />
              </TouchableOpacity>
            )}
          </View>

          {/* Bouton Créer un nouveau bot */}
          <View style={styles.sectionHeaderContainer}>
            <Text style={styles.sectionHeaderText}>My bots</Text>
          </View>

          <TouchableOpacity
            style={styles.createBotButtonRow}
            activeOpacity={0.7}
            onPress={() => setViewMode('create')}
          >
            <View style={styles.createBotIconCircle}>
              <Feather name="plus" size={22} color="#2ea6ff" />
            </View>
            <Text style={styles.createBotButtonText}>Create a New Bot</Text>
          </TouchableOpacity>

          <View style={styles.divider} />

          {/* Liste des bots */}
          {loading ? (
            <View style={styles.loadingContainer}>
              <ActivityIndicator size="small" color="#52a6e6" />
            </View>
          ) : filteredBots.length === 0 ? (
            <View style={styles.emptyContainer}>
              <Text style={styles.emptyText}>
                {bots.length === 0
                  ? 'No bots yet. Tap "Create a New Bot" above to get started.'
                  : 'No bots matching your search.'}
              </Text>
            </View>
          ) : (
            filteredBots.map((bot) => (
              <TouchableOpacity
                key={bot.id}
                style={styles.botItemRow}
                activeOpacity={0.7}
                onPress={() => {
                  setSelectedBot(bot);
                  setShowToken(false);
                  setViewMode('details');
                }}
              >
                <View
                  style={[
                    styles.botItemAvatar,
                    { backgroundColor: getAvatarColor(bot.username) },
                  ]}
                >
                  <Text style={styles.botItemAvatarText}>
                    {bot.first_name.substring(0, 1).toUpperCase()}
                  </Text>
                </View>

                <View style={styles.botItemInfo}>
                  <Text style={styles.botItemName} numberOfLines={1}>
                    {bot.first_name}
                  </Text>
                  <Text style={styles.botItemUsername} numberOfLines={1}>
                    @{bot.username}
                  </Text>
                </View>

                <Feather name="chevron-right" size={20} color="#6c7883" />
              </TouchableOpacity>
            ))
          )}
        </ScrollView>
      )}

      {/* ========================================================================= */}
      {/* VUE 2 : CRÉATION D'UN NOUVEAU BOT                                         */}
      {/* ========================================================================= */}
      {viewMode === 'create' && (
        <KeyboardAvoidingView
          behavior={Platform.OS === 'ios' ? 'padding' : undefined}
          style={{ flex: 1 }}
        >
          <ScrollView
            style={styles.contentContainer}
            contentContainerStyle={styles.createScrollContent}
            showsVerticalScrollIndicator={false}
          >
            {/* Cercle Caméra pour upload d'avatar */}
            <View style={styles.cameraUploadContainer}>
              <View style={styles.cameraCircle}>
                <Ionicons name="camera" size={38} color="#52a6e6" />
                <View style={styles.cameraPlusBadge}>
                  <Feather name="plus" size={14} color="#ffffff" />
                </View>
              </View>
            </View>

            {/* Carte Formulaire */}
            <View style={styles.formCard}>
              <View style={styles.formFieldRow}>
                <TextInput
                  style={styles.formInput}
                  placeholder="Bot Name"
                  placeholderTextColor="#6c7883"
                  value={botName}
                  onChangeText={setBotName}
                  maxLength={64}
                />
              </View>

              <View style={styles.formDivider} />

              <View style={styles.formFieldRow}>
                <TextInput
                  style={[styles.formInput, { minHeight: 44 }]}
                  placeholder="About (Optional)"
                  placeholderTextColor="#6c7883"
                  value={botAbout}
                  onChangeText={setBotAbout}
                  multiline
                  maxLength={160}
                />
              </View>

              <View style={styles.formDivider} />

              <View style={styles.formFieldRow}>
                <Text style={styles.usernamePrefix}>t.me/</Text>
                <TextInput
                  style={[styles.formInput, { flex: 1 }]}
                  placeholder="username_bot"
                  placeholderTextColor="#6c7883"
                  value={botUsername}
                  onChangeText={(text) => setBotUsername(text.replace(/[@\s]/g, ''))}
                  autoCapitalize="none"
                  autoCorrect={false}
                />
              </View>
            </View>

            {/* Aide et validation en direct */}
            <View style={styles.helperTextContainer}>
              {trimmedUsername.length === 0 ? (
                <Text style={styles.helperText}>
                  Bot usernames must end in "bot" (e.g. TetrisBot or tetris_bot). They must be unique.
                </Text>
              ) : isUsernameValid ? (
                <View style={styles.validationRow}>
                  <Ionicons name="checkmark-circle" size={16} color="#4ade80" />
                  <Text style={styles.validText}>
                    {' '}@{trimmedUsername} is available.
                  </Text>
                </View>
              ) : (
                <View style={styles.validationRow}>
                  <Ionicons name="alert-circle" size={16} color="#f59e0b" />
                  <Text style={styles.invalidText}>
                    {' '}Username must end with "bot", min 4 characters.
                  </Text>
                </View>
              )}
            </View>
          </ScrollView>

          {/* Bouton collant en bas : Create Bot */}
          <View style={styles.bottomButtonContainer}>
            <TouchableOpacity
              style={[
                styles.createSubmitButton,
                !canCreate && styles.createSubmitButtonDisabled,
              ]}
              onPress={handleCreateBot}
              disabled={!canCreate}
              activeOpacity={0.8}
            >
              {creating ? (
                <ActivityIndicator size="small" color="#ffffff" />
              ) : (
                <Text style={styles.createSubmitButtonText}>Create Bot</Text>
              )}
            </TouchableOpacity>
          </View>
        </KeyboardAvoidingView>
      )}

      {/* ========================================================================= */}
      {/* VUE 3 : DÉTAILS ET GESTION DU TOKEN DU BOT                                */}
      {/* ========================================================================= */}
      {viewMode === 'details' && selectedBot && (
        <ScrollView
          style={styles.contentContainer}
          contentContainerStyle={styles.detailsScrollContent}
          showsVerticalScrollIndicator={false}
        >
          {/* Avatar et Titre */}
          <View style={styles.detailsHeader}>
            <View
              style={[
                styles.detailsAvatar,
                { backgroundColor: getAvatarColor(selectedBot.username) },
              ]}
            >
              <Text style={styles.detailsAvatarText}>
                {selectedBot.first_name.substring(0, 1).toUpperCase()}
              </Text>
            </View>
            <Text style={styles.detailsName}>{selectedBot.first_name}</Text>
            <Text style={styles.detailsUsername}>@{selectedBot.username}</Text>
            {selectedBot.about ? (
              <Text style={styles.detailsAbout}>{selectedBot.about}</Text>
            ) : null}
          </View>

          {/* CARTE API TOKEN */}
          <View style={styles.tokenCard}>
            <View style={styles.tokenCardHeader}>
              <Ionicons name="key" size={18} color="#efa141" style={{ marginRight: 8 }} />
              <Text style={styles.tokenCardTitle}>API Token</Text>
            </View>

            <Text style={styles.tokenDescription}>
              Use this token to access the HTTP API. Keep your token secure and store it safely, it can be used by anyone to control your bot.
            </Text>

            {/* Boîte d'affichage du token avec bouton œil */}
            <View style={styles.tokenDisplayBox}>
              <Text style={styles.tokenMonospaceText} numberOfLines={showToken ? undefined : 1}>
                {showToken ? selectedBot.token : formatMaskedToken(selectedBot.token)}
              </Text>
              <TouchableOpacity
                onPress={() => setShowToken(!showToken)}
                style={styles.tokenEyeButton}
                activeOpacity={0.7}
              >
                <Ionicons
                  name={showToken ? 'eye-off-outline' : 'eye-outline'}
                  size={20}
                  color="#8596a7"
                />
              </TouchableOpacity>
            </View>

            {/* Boutons d'action : Copy et Revoke */}
            <View style={styles.tokenButtonsRow}>
              <TouchableOpacity
                style={styles.tokenCopyButton}
                activeOpacity={0.8}
                onPress={handleCopyToken}
              >
                <Ionicons name="copy-outline" size={16} color="#ffffff" style={{ marginRight: 6 }} />
                <Text style={styles.tokenCopyButtonText}>Copy</Text>
              </TouchableOpacity>

              <TouchableOpacity
                style={styles.tokenRevokeButton}
                activeOpacity={0.8}
                onPress={handleRevokeToken}
                disabled={revoking}
              >
                {revoking ? (
                  <ActivityIndicator size="small" color="#e17076" />
                ) : (
                  <>
                    <Ionicons name="refresh-outline" size={16} color="#e17076" style={{ marginRight: 6 }} />
                    <Text style={styles.tokenRevokeButtonText}>Revoke</Text>
                  </>
                )}
              </TouchableOpacity>
            </View>
          </View>

          {/* CARTE WEBHOOK INTEGRATION */}
          <View style={styles.tokenCard}>
            <View style={styles.tokenCardHeader}>
              <View style={{ flexDirection: 'row', alignItems: 'center' }}>
                <MaterialCommunityIcons name="webhook" size={18} color="#52a6e6" style={{ marginRight: 8 }} />
                <Text style={styles.tokenCardTitle}>Webhook Integration</Text>
              </View>
              {webhookInfo?.url ? (
                <View style={styles.activeBadge}>
                  <Text style={styles.activeBadgeText}>ACTIVE</Text>
                </View>
              ) : (
                <View style={styles.disabledBadge}>
                  <Text style={styles.disabledBadgeText}>OFF (STREAMING)</Text>
                </View>
              )}
            </View>

            <Text style={styles.tokenDescription}>
              {webhookInfo?.url
                ? 'Incoming updates are forwarded via HTTP POST to your external webhook receiver.'
                : 'No webhook active. Bot receives updates via instant WebSocket stream or long polling (getUpdates).'}
            </Text>

            {webhookInfo?.url ? (
              <View style={styles.webhookBox}>
                <Text style={styles.webhookUrlLabel}>URL:</Text>
                <Text style={styles.webhookUrlValue} numberOfLines={2}>
                  {webhookInfo.url}
                </Text>
                <View style={styles.webhookStatsRow}>
                  <Text style={styles.webhookStatsText}>
                    Pending: <Text style={{ color: '#ffffff', fontWeight: '700' }}>{webhookInfo.pending_update_count}</Text>
                  </Text>
                  <Text style={styles.webhookStatsText}>
                    Max conn: <Text style={{ color: '#ffffff', fontWeight: '700' }}>{webhookInfo.max_connections || 40}</Text>
                  </Text>
                </View>
                {webhookInfo.last_error_message ? (
                  <Text style={styles.webhookErrorText}>
                    ⚠ Error: {webhookInfo.last_error_message}
                  </Text>
                ) : null}
              </View>
            ) : null}

            <View style={styles.tokenButtonsRow}>
              <TouchableOpacity
                style={styles.webhookConfigureButton}
                activeOpacity={0.8}
                onPress={handleOpenWebhookModal}
              >
                <Ionicons name="settings-outline" size={16} color="#ffffff" style={{ marginRight: 6 }} />
                <Text style={styles.tokenCopyButtonText}>
                  {webhookInfo?.url ? 'Edit Webhook' : 'Set Webhook'}
                </Text>
              </TouchableOpacity>

              {webhookInfo?.url ? (
                <TouchableOpacity
                  style={styles.tokenRevokeButton}
                  activeOpacity={0.8}
                  onPress={handleDeleteWebhook}
                  disabled={deletingWebhook}
                >
                  {deletingWebhook ? (
                    <ActivityIndicator size="small" color="#e17076" />
                  ) : (
                    <>
                      <Ionicons name="trash-outline" size={16} color="#e17076" style={{ marginRight: 6 }} />
                      <Text style={styles.tokenRevokeButtonText}>Delete</Text>
                    </>
                  )}
                </TouchableOpacity>
              ) : null}
            </View>
          </View>

          {/* SECTION PARAMÈTRES */}
          <View style={styles.sectionHeaderContainer}>
            <Text style={styles.sectionHeaderText}>Settings</Text>
          </View>

          <View style={styles.groupedCard}>
            <TouchableOpacity style={styles.cardRow} activeOpacity={0.7}>
              <Text style={styles.cardRowText}>Edit Name</Text>
              <Feather name="chevron-right" size={18} color="#6c7883" />
            </TouchableOpacity>
            <View style={styles.formDivider} />
            <TouchableOpacity style={styles.cardRow} activeOpacity={0.7}>
              <Text style={styles.cardRowText}>Edit Description</Text>
              <Feather name="chevron-right" size={18} color="#6c7883" />
            </TouchableOpacity>
            <View style={styles.formDivider} />
            <TouchableOpacity style={styles.cardRow} activeOpacity={0.7}>
              <Text style={styles.cardRowText}>Edit About</Text>
              <Feather name="chevron-right" size={18} color="#6c7883" />
            </TouchableOpacity>
            <View style={styles.formDivider} />
            <TouchableOpacity style={styles.cardRow} activeOpacity={0.7}>
              <Text style={styles.cardRowText}>Bot Settings</Text>
              <Feather name="chevron-right" size={18} color="#6c7883" />
            </TouchableOpacity>
          </View>

          {/* SECTION ACTIONS */}
          <View style={styles.sectionHeaderContainer}>
            <Text style={styles.sectionHeaderText}>Actions</Text>
          </View>

          <View style={styles.groupedCard}>
            <TouchableOpacity
              style={styles.cardRow}
              activeOpacity={0.7}
              onPress={handleOpenChat}
            >
              <View style={styles.actionLeftRow}>
                <Ionicons name="chatbubble-ellipses-outline" size={20} color="#52a6e6" style={{ marginRight: 12 }} />
                <Text style={[styles.cardRowText, { color: '#52a6e6', fontWeight: '600' }]}>
                  Open Chat with Bot
                </Text>
              </View>
              <Feather name="chevron-right" size={18} color="#6c7883" />
            </TouchableOpacity>

            <View style={styles.formDivider} />

            <TouchableOpacity
              style={styles.cardRow}
              activeOpacity={0.7}
              onPress={handleDeleteBot}
            >
              <View style={styles.actionLeftRow}>
                <Ionicons name="trash-outline" size={20} color="#e17076" style={{ marginRight: 12 }} />
                <Text style={[styles.cardRowText, { color: '#e17076', fontWeight: '600' }]}>
                  Delete Bot
                </Text>
              </View>
              <Feather name="chevron-right" size={18} color="#6c7883" />
            </TouchableOpacity>
          </View>
        </ScrollView>
      )}

      {/* MODAL CONFIGURATION WEBHOOK */}
      <Modal
        visible={webhookModalVisible}
        transparent
        animationType="fade"
        onRequestClose={() => setWebhookModalVisible(false)}
      >
        <KeyboardAvoidingView
          behavior={Platform.OS === 'ios' ? 'padding' : undefined}
          style={styles.modalOverlay}
        >
          <View style={styles.modalCard}>
            <View style={styles.modalHeader}>
              <MaterialCommunityIcons name="webhook" size={24} color="#52a6e6" style={{ marginRight: 8 }} />
              <Text style={styles.modalTitle}>Configure Webhook</Text>
            </View>

            <Text style={styles.modalSubtitle}>
              Specify the HTTPS or HTTP URL where Telegram updates should be posted via HTTP POST.
            </Text>

            <Text style={styles.modalInputLabel}>Webhook URL</Text>
            <TextInput
              style={styles.modalInput}
              placeholder="http://my-domain.com/webhook"
              placeholderTextColor="#6c7883"
              value={webhookUrlInput}
              onChangeText={setWebhookUrlInput}
              autoCapitalize="none"
              keyboardType="url"
            />

            <Text style={styles.modalInputLabel}>Secret Token (Optional)</Text>
            <TextInput
              style={styles.modalInput}
              placeholder="X-Telegram-Bot-Api-Secret-Token"
              placeholderTextColor="#6c7883"
              value={webhookSecretInput}
              onChangeText={setWebhookSecretInput}
              autoCapitalize="none"
            />

            <View style={styles.modalSwitchRow}>
              <View style={{ flex: 1, paddingRight: 10 }}>
                <Text style={styles.modalSwitchLabel}>Drop Pending Updates</Text>
                <Text style={styles.modalSwitchDesc}>Clear queued updates when setting webhook</Text>
              </View>
              <Switch
                value={dropPendingUpdates}
                onValueChange={setDropPendingUpdates}
                trackColor={{ false: '#2b394a', true: '#52a6e6' }}
                thumbColor="#ffffff"
              />
            </View>

            <View style={styles.modalButtonsRow}>
              <TouchableOpacity
                style={styles.modalCancelButton}
                onPress={() => setWebhookModalVisible(false)}
                disabled={savingWebhook}
                activeOpacity={0.7}
              >
                <Text style={styles.modalCancelText}>Cancel</Text>
              </TouchableOpacity>

              <TouchableOpacity
                style={styles.modalSaveButton}
                onPress={handleSaveWebhook}
                disabled={savingWebhook}
                activeOpacity={0.8}
              >
                {savingWebhook ? (
                  <ActivityIndicator size="small" color="#ffffff" />
                ) : (
                  <Text style={styles.modalSaveText}>Save Webhook</Text>
                )}
              </TouchableOpacity>
            </View>
          </View>
        </KeyboardAvoidingView>
      </Modal>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
    backgroundColor: TelegramColors.base,
  },
  toastContainer: {
    position: 'absolute',
    top: 60,
    left: 20,
    right: 20,
    zIndex: 999,
    backgroundColor: '#242f3d',
    borderColor: '#52a6e6',
    borderWidth: 1,
    paddingVertical: 12,
    paddingHorizontal: 16,
    borderRadius: 10,
    alignItems: 'center',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.3,
    shadowRadius: 8,
    elevation: 8,
  },
  toastText: {
    color: '#ffffff',
    fontSize: 14,
    fontWeight: '600',
  },
  topBar: {
    height: 56,
    backgroundColor: TelegramColors.header,
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 8,
    borderBottomWidth: 0.8,
    borderBottomColor: TelegramColors.border,
  },
  topBarButton: {
    width: 40,
    height: 40,
    justifyContent: 'center',
    alignItems: 'center',
  },
  topBarTitleContainer: {
    flex: 1,
    marginLeft: 4,
  },
  topBarTitle: {
    color: TelegramColors.textPrimary,
    fontSize: 18,
    fontWeight: '700',
  },
  topBarSubtitle: {
    color: '#52a6e6',
    fontSize: 12,
  },
  topBarRight: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  contentContainer: {
    flex: 1,
  },
  scrollContent: {
    paddingBottom: 40,
  },

  // BotFather Home Header
  botFatherHeader: {
    alignItems: 'center',
    paddingTop: 24,
    paddingBottom: 20,
    paddingHorizontal: 24,
  },
  botFatherAvatar: {
    width: 76,
    height: 76,
    borderRadius: 38,
    backgroundColor: '#2aabee',
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 12,
    elevation: 4,
    shadowColor: '#2aabee',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.3,
    shadowRadius: 8,
  },
  botFatherTitle: {
    color: '#ffffff',
    fontSize: 22,
    fontWeight: '700',
    marginBottom: 6,
  },
  botFatherDescription: {
    color: '#8596a7',
    fontSize: 13,
    textAlign: 'center',
    lineHeight: 18,
  },

  // Search
  searchBarContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#242f3d',
    borderRadius: 10,
    marginHorizontal: 16,
    paddingHorizontal: 12,
    height: 40,
    marginBottom: 16,
  },
  searchIcon: {
    marginRight: 8,
  },
  searchInput: {
    flex: 1,
    color: '#ffffff',
    fontSize: 15,
  },

  // Section Header
  sectionHeaderContainer: {
    paddingHorizontal: 16,
    paddingTop: 16,
    paddingBottom: 8,
  },
  sectionHeaderText: {
    color: '#8596a7',
    fontSize: 13,
    fontWeight: '700',
    textTransform: 'uppercase',
    letterSpacing: 0.5,
  },

  // Create Bot Row
  createBotButtonRow: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: TelegramColors.base,
    paddingHorizontal: 16,
    paddingVertical: 12,
  },
  createBotIconCircle: {
    width: 44,
    height: 44,
    borderRadius: 22,
    backgroundColor: 'rgba(46, 166, 255, 0.12)',
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: 14,
    borderWidth: 1.2,
    borderColor: 'rgba(46, 166, 255, 0.3)',
  },
  createBotButtonText: {
    color: '#2ea6ff',
    fontSize: 16,
    fontWeight: '600',
  },
  divider: {
    height: 0.8,
    backgroundColor: TelegramColors.border,
    marginHorizontal: 16,
    marginVertical: 4,
  },

  // Bot Item Row
  botItemRow: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingVertical: 10,
  },
  botItemAvatar: {
    width: 44,
    height: 44,
    borderRadius: 22,
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: 14,
  },
  botItemAvatarText: {
    color: '#ffffff',
    fontSize: 18,
    fontWeight: '700',
  },
  botItemInfo: {
    flex: 1,
  },
  botItemName: {
    color: '#ffffff',
    fontSize: 16,
    fontWeight: '600',
  },
  botItemUsername: {
    color: '#8596a7',
    fontSize: 13,
    marginTop: 2,
  },

  loadingContainer: {
    padding: 30,
    alignItems: 'center',
  },
  emptyContainer: {
    padding: 30,
    alignItems: 'center',
  },
  emptyText: {
    color: '#8596a7',
    fontSize: 14,
    textAlign: 'center',
    lineHeight: 20,
  },

  // Create Screen
  createScrollContent: {
    paddingTop: 24,
    paddingBottom: 100,
  },
  cameraUploadContainer: {
    alignItems: 'center',
    marginBottom: 24,
  },
  cameraCircle: {
    width: 88,
    height: 88,
    borderRadius: 44,
    backgroundColor: '#242f3d',
    justifyContent: 'center',
    alignItems: 'center',
    position: 'relative',
    borderWidth: 1.5,
    borderColor: 'rgba(82, 166, 230, 0.3)',
  },
  cameraPlusBadge: {
    position: 'absolute',
    right: 2,
    bottom: 2,
    width: 24,
    height: 24,
    borderRadius: 12,
    backgroundColor: '#52a6e6',
    justifyContent: 'center',
    alignItems: 'center',
    borderWidth: 2,
    borderColor: TelegramColors.base,
  },
  formCard: {
    backgroundColor: '#242f3d',
    borderRadius: 12,
    marginHorizontal: 16,
    overflow: 'hidden',
  },
  formFieldRow: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingVertical: 12,
  },
  formInput: {
    color: '#ffffff',
    fontSize: 16,
    padding: 0,
    flex: 1,
  },
  usernamePrefix: {
    color: '#8596a7',
    fontSize: 16,
    fontWeight: '500',
    marginRight: 2,
  },
  formDivider: {
    height: 0.8,
    backgroundColor: TelegramColors.border,
    marginLeft: 16,
  },
  helperTextContainer: {
    paddingHorizontal: 20,
    paddingTop: 12,
  },
  helperText: {
    color: '#8596a7',
    fontSize: 13,
    lineHeight: 18,
  },
  validationRow: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  validText: {
    color: '#4ade80',
    fontSize: 13,
    fontWeight: '600',
  },
  invalidText: {
    color: '#f59e0b',
    fontSize: 13,
  },
  bottomButtonContainer: {
    paddingHorizontal: 16,
    paddingVertical: 14,
    backgroundColor: TelegramColors.base,
    borderTopWidth: 0.8,
    borderTopColor: TelegramColors.border,
  },
  createSubmitButton: {
    height: 48,
    backgroundColor: '#52a6e6',
    borderRadius: 12,
    justifyContent: 'center',
    alignItems: 'center',
  },
  createSubmitButtonDisabled: {
    backgroundColor: '#263442',
    opacity: 0.6,
  },
  createSubmitButtonText: {
    color: '#ffffff',
    fontSize: 16,
    fontWeight: '700',
  },

  // Details Screen
  detailsScrollContent: {
    paddingBottom: 40,
  },
  detailsHeader: {
    alignItems: 'center',
    paddingTop: 20,
    paddingBottom: 16,
    paddingHorizontal: 16,
  },
  detailsAvatar: {
    width: 80,
    height: 80,
    borderRadius: 40,
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 12,
  },
  detailsAvatarText: {
    color: '#ffffff',
    fontSize: 32,
    fontWeight: '700',
  },
  detailsName: {
    color: '#ffffff',
    fontSize: 20,
    fontWeight: '700',
  },
  detailsUsername: {
    color: '#52a6e6',
    fontSize: 14,
    marginTop: 4,
    fontWeight: '500',
  },
  detailsAbout: {
    color: '#8596a7',
    fontSize: 13,
    marginTop: 8,
    textAlign: 'center',
    paddingHorizontal: 20,
  },

  // Token Card
  tokenCard: {
    backgroundColor: '#242f3d',
    borderRadius: 12,
    marginHorizontal: 16,
    padding: 16,
    marginVertical: 10,
  },
  tokenCardHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 6,
  },
  tokenCardTitle: {
    color: '#ffffff',
    fontSize: 16,
    fontWeight: '700',
  },
  tokenDescription: {
    color: '#8596a7',
    fontSize: 12.5,
    lineHeight: 18,
    marginBottom: 12,
  },
  tokenDisplayBox: {
    backgroundColor: '#17212b',
    borderRadius: 8,
    paddingHorizontal: 12,
    paddingVertical: 10,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: 12,
    borderWidth: 0.8,
    borderColor: 'rgba(255,255,255,0.06)',
  },
  tokenMonospaceText: {
    color: '#ffffff',
    fontSize: 13,
    fontFamily: Platform.OS === 'ios' ? 'Menlo' : 'monospace',
    flex: 1,
    marginRight: 8,
  },
  tokenEyeButton: {
    padding: 4,
  },
  tokenButtonsRow: {
    flexDirection: 'row',
    gap: 10,
  },
  tokenCopyButton: {
    flex: 1,
    height: 40,
    backgroundColor: '#52a6e6',
    borderRadius: 8,
    flexDirection: 'row',
    justifyContent: 'center',
    alignItems: 'center',
  },
  tokenCopyButtonText: {
    color: '#ffffff',
    fontSize: 14,
    fontWeight: '700',
  },
  tokenRevokeButton: {
    flex: 1,
    height: 40,
    backgroundColor: 'rgba(225, 112, 118, 0.12)',
    borderWidth: 1,
    borderColor: 'rgba(225, 112, 118, 0.4)',
    borderRadius: 8,
    flexDirection: 'row',
    justifyContent: 'center',
    alignItems: 'center',
  },
  tokenRevokeButtonText: {
    color: '#e17076',
    fontSize: 14,
    fontWeight: '700',
  },

  // Grouped Card for settings & actions
  groupedCard: {
    backgroundColor: '#242f3d',
    borderRadius: 12,
    marginHorizontal: 16,
    overflow: 'hidden',
  },
  cardRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 16,
    paddingVertical: 14,
  },
  cardRowText: {
    color: '#ffffff',
    fontSize: 15,
  },
  actionLeftRow: {
    flexDirection: 'row',
    alignItems: 'center',
  },

  // Webhook styles
  activeBadge: {
    backgroundColor: 'rgba(74, 222, 128, 0.2)',
    paddingHorizontal: 6,
    paddingVertical: 2,
    borderRadius: 4,
    borderWidth: 1,
    borderColor: 'rgba(74, 222, 128, 0.5)',
  },
  activeBadgeText: {
    color: '#4ade80',
    fontSize: 10,
    fontWeight: '700',
    letterSpacing: 0.5,
  },
  disabledBadge: {
    backgroundColor: 'rgba(255, 255, 255, 0.08)',
    paddingHorizontal: 6,
    paddingVertical: 2,
    borderRadius: 4,
  },
  disabledBadgeText: {
    color: '#8596a7',
    fontSize: 10,
    fontWeight: '700',
    letterSpacing: 0.5,
  },
  webhookBox: {
    backgroundColor: '#18222d',
    borderRadius: 8,
    padding: 12,
    marginBottom: 12,
    borderWidth: 1,
    borderColor: '#2b394a',
  },
  webhookUrlLabel: {
    color: '#8596a7',
    fontSize: 11,
    marginBottom: 2,
    fontWeight: '600',
  },
  webhookUrlValue: {
    color: '#52a6e6',
    fontSize: 13,
    fontFamily: Platform.OS === 'ios' ? 'Menlo' : 'monospace',
    marginBottom: 8,
  },
  webhookStatsRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    paddingTop: 4,
    borderTopWidth: 0.5,
    borderTopColor: '#2b394a',
  },
  webhookStatsText: {
    color: '#8596a7',
    fontSize: 12,
  },
  webhookErrorText: {
    color: '#e17076',
    fontSize: 12,
    marginTop: 6,
  },
  webhookConfigureButton: {
    flex: 1,
    height: 40,
    backgroundColor: '#2b5278',
    borderRadius: 8,
    flexDirection: 'row',
    justifyContent: 'center',
    alignItems: 'center',
  },

  // Modal styles
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0, 0, 0, 0.7)',
    justifyContent: 'center',
    alignItems: 'center',
    padding: 20,
  },
  modalCard: {
    width: '100%',
    maxWidth: 400,
    backgroundColor: '#242f3d',
    borderRadius: 16,
    padding: 20,
    borderWidth: 1,
    borderColor: '#2f3b4a',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 6 },
    shadowOpacity: 0.4,
    shadowRadius: 12,
    elevation: 10,
  },
  modalHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 8,
  },
  modalTitle: {
    color: '#ffffff',
    fontSize: 18,
    fontWeight: '700',
  },
  modalSubtitle: {
    color: '#8596a7',
    fontSize: 13,
    marginBottom: 16,
    lineHeight: 18,
  },
  modalInputLabel: {
    color: '#8596a7',
    fontSize: 12,
    marginBottom: 6,
    fontWeight: '600',
  },
  modalInput: {
    backgroundColor: '#18222d',
    borderRadius: 8,
    height: 44,
    paddingHorizontal: 12,
    color: '#ffffff',
    fontSize: 14,
    marginBottom: 14,
    borderWidth: 1,
    borderColor: '#2f3b4a',
  },
  modalSwitchRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: 20,
    paddingTop: 4,
  },
  modalSwitchLabel: {
    color: '#ffffff',
    fontSize: 14,
    fontWeight: '600',
  },
  modalSwitchDesc: {
    color: '#8596a7',
    fontSize: 12,
    marginTop: 2,
  },
  modalButtonsRow: {
    flexDirection: 'row',
    gap: 10,
  },
  modalCancelButton: {
    flex: 1,
    height: 44,
    backgroundColor: 'rgba(255, 255, 255, 0.08)',
    borderRadius: 10,
    justifyContent: 'center',
    alignItems: 'center',
  },
  modalCancelText: {
    color: '#ffffff',
    fontSize: 14,
    fontWeight: '600',
  },
  modalSaveButton: {
    flex: 1,
    height: 44,
    backgroundColor: '#52a6e6',
    borderRadius: 10,
    justifyContent: 'center',
    alignItems: 'center',
  },
  modalSaveText: {
    color: '#ffffff',
    fontSize: 14,
    fontWeight: '700',
  },
});
