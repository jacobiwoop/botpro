import React, { useEffect, useRef, useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  Modal,
  Animated,
  TouchableOpacity,
  TouchableWithoutFeedback,
  ScrollView,
  Switch,
  Easing,
  useWindowDimensions,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useRouter } from 'expo-router';
import {
  Feather,
  Ionicons,
  MaterialIcons,
  MaterialCommunityIcons,
} from '@expo/vector-icons';
import { TelegramColors } from '@/constants/telegramTheme';

interface DrawerMenuProps {
  visible: boolean;
  onClose: () => void;
}

export function DrawerMenu({ visible, onClose }: DrawerMenuProps) {
  const router = useRouter();
  const { width: screenWidth } = useWindowDimensions();
  // Couvre de gauche à presque la fin (85% de l'écran, laissant une tranche à droite comme sur Telegram)
  const drawerWidth = Math.round(screenWidth * 0.85);

  const [showAccounts, setShowAccounts] = useState(true);
  const [isNightMode, setIsNightMode] = useState(true);
  const [showModal, setShowModal] = useState(visible);

  // Valeurs animées pour la translation et l'opacité
  const slideAnim = useRef(new Animated.Value(-drawerWidth)).current;
  const fadeAnim = useRef(new Animated.Value(0)).current;

  useEffect(() => {
    if (visible) {
      setShowModal(true);
      slideAnim.setValue(-drawerWidth);
      fadeAnim.setValue(0);

      Animated.parallel([
        Animated.timing(slideAnim, {
          toValue: 0,
          duration: 290,
          easing: Easing.out(Easing.cubic), // Décélération douce et naturelle
          useNativeDriver: true,
        }),
        Animated.timing(fadeAnim, {
          toValue: 1,
          duration: 290,
          easing: Easing.out(Easing.quad),
          useNativeDriver: true,
        }),
      ]).start();
    } else if (showModal) {
      Animated.parallel([
        Animated.timing(slideAnim, {
          toValue: -drawerWidth,
          duration: 220,
          easing: Easing.in(Easing.cubic),
          useNativeDriver: true,
        }),
        Animated.timing(fadeAnim, {
          toValue: 0,
          duration: 220,
          easing: Easing.linear,
          useNativeDriver: true,
        }),
      ]).start(() => {
        setShowModal(false);
      });
    }
  }, [visible, drawerWidth]);

  const handleClose = () => {
    Animated.parallel([
      Animated.timing(slideAnim, {
        toValue: -drawerWidth,
        duration: 220,
        easing: Easing.in(Easing.cubic),
        useNativeDriver: true,
      }),
      Animated.timing(fadeAnim, {
        toValue: 0,
        duration: 220,
        easing: Easing.linear,
        useNativeDriver: true,
      }),
    ]).start(() => {
      setShowModal(false);
      onClose();
    });
  };

  if (!showModal) return null;

  return (
    <Modal
      transparent
      visible={showModal}
      animationType="none"
      onRequestClose={handleClose}
    >
      <View style={styles.overlayContainer}>
        {/* Fond sombre semi-transparent cliquable pour fermer */}
        <TouchableWithoutFeedback onPress={handleClose}>
          <Animated.View style={[styles.backdrop, { opacity: fadeAnim }]} />
        </TouchableWithoutFeedback>

        {/* Tiroir latéral animé couvrant presque tout l'écran */}
        <Animated.View
          style={[
            styles.drawerContainer,
            { width: drawerWidth, transform: [{ translateX: slideAnim }] },
          ]}
        >
          <SafeAreaView edges={['top', 'bottom']} style={styles.safeArea}>
            <ScrollView showsVerticalScrollIndicator={false} bounces={false}>
              {/* En-tête profil utilisateur */}
              <View style={styles.profileHeader}>
                {/* Grand avatar DL */}
                <View style={styles.largeAvatar}>
                  <Text style={styles.largeAvatarText}>DL</Text>
                </View>

                {/* Nom, numéro et flèche de bascule */}
                <TouchableOpacity
                  style={styles.profileInfoRow}
                  activeOpacity={0.7}
                  onPress={() => setShowAccounts((prev) => !prev)}
                >
                  <View style={styles.nameAndPhone}>
                    <Text style={styles.profileName} numberOfLines={1}>
                      darren lee
                    </Text>
                    <Text style={styles.profilePhone}>+44 7354 224381</Text>
                  </View>
                  <Feather
                    name={showAccounts ? 'chevron-up' : 'chevron-down'}
                    size={20}
                    color="#ffffff"
                  />
                </TouchableOpacity>
              </View>

              {/* Section comptes (affichée si déroulée) */}
              {showAccounts && (
                <View style={styles.accountsSection}>
                  {/* Compte actuel */}
                  <TouchableOpacity style={styles.accountRow} activeOpacity={0.7}>
                    <View style={styles.smallAvatarWrapper}>
                      <View style={styles.smallAvatar}>
                        <Text style={styles.smallAvatarText}>DL</Text>
                      </View>
                      <View style={styles.accountCheckBadge}>
                        <MaterialIcons name="check" size={10} color="#ffffff" />
                      </View>
                    </View>
                    <Text style={styles.accountName}>darren lee</Text>
                  </TouchableOpacity>

                  {/* Bouton Ajouter un compte */}
                  <TouchableOpacity style={styles.accountRow} activeOpacity={0.7}>
                    <View style={styles.addAccountIconWrapper}>
                      <Feather name="plus" size={18} color="#8596a7" />
                    </View>
                    <Text style={styles.menuItemText}>Add Account</Text>
                  </TouchableOpacity>
                </View>
              )}

              <View style={styles.divider} />

              {/* Liste des éléments du menu */}
              <View style={styles.menuSection}>
                <TouchableOpacity
                  style={styles.menuItem}
                  activeOpacity={0.7}
                  onPress={() => {
                    onClose();
                    router.push('/botfather');
                  }}
                >
                  <MaterialCommunityIcons name="robot" size={22} color="#52a6e6" style={styles.menuIcon} />
                  <Text style={[styles.menuItemText, { color: '#52a6e6', fontWeight: '600' }]}>BotFather</Text>
                  <View style={styles.botFatherBadge}>
                    <Text style={styles.botFatherBadgeText}>NEW</Text>
                  </View>
                </TouchableOpacity>

                <TouchableOpacity style={styles.menuItem} activeOpacity={0.7}>
                  <Ionicons name="people-outline" size={22} color="#8596a7" style={styles.menuIcon} />
                  <Text style={styles.menuItemText}>Contacts</Text>
                </TouchableOpacity>

                <TouchableOpacity style={styles.menuItem} activeOpacity={0.7}>
                  <Ionicons name="call-outline" size={22} color="#8596a7" style={styles.menuIcon} />
                  <Text style={styles.menuItemText}>Calls</Text>
                </TouchableOpacity>

                <TouchableOpacity style={styles.menuItem} activeOpacity={0.7}>
                  <Ionicons name="bookmark-outline" size={22} color="#8596a7" style={styles.menuIcon} />
                  <Text style={styles.menuItemText}>Saved Messages</Text>
                </TouchableOpacity>

                <TouchableOpacity style={styles.menuItem} activeOpacity={0.7}>
                  <Ionicons name="settings-outline" size={22} color="#8596a7" style={styles.menuIcon} />
                  <Text style={styles.menuItemText}>Settings</Text>
                  {/* Badge gâteau d'anniversaire */}
                  <View style={styles.settingsBadge}>
                    <MaterialCommunityIcons name="cake-variant" size={14} color="#ffffff" />
                  </View>
                </TouchableOpacity>

                <TouchableOpacity style={styles.menuItem} activeOpacity={0.7}>
                  <Ionicons name="person-add-outline" size={22} color="#8596a7" style={styles.menuIcon} />
                  <Text style={styles.menuItemText}>Invite Friends</Text>
                </TouchableOpacity>

                <TouchableOpacity style={styles.menuItem} activeOpacity={0.7}>
                  <Ionicons name="help-circle-outline" size={22} color="#8596a7" style={styles.menuIcon} />
                  <Text style={styles.menuItemText}>Help</Text>
                </TouchableOpacity>
              </View>

              <View style={styles.divider} />

              {/* Option Mode Nuit avec switch */}
              <View style={styles.nightModeRow}>
                <View style={styles.nightModeLeft}>
                  <Ionicons name="moon" size={21} color="#8596a7" style={styles.menuIcon} />
                  <Text style={styles.menuItemText}>Night Mode</Text>
                </View>
                <Switch
                  value={isNightMode}
                  onValueChange={setIsNightMode}
                  trackColor={{ false: '#3a4b5d', true: '#52a6e6' }}
                  thumbColor={isNightMode ? '#ffffff' : '#f4f3f4'}
                />
              </View>
            </ScrollView>
          </SafeAreaView>
        </Animated.View>
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  overlayContainer: {
    flex: 1,
    flexDirection: 'row',
  },
  backdrop: {
    ...StyleSheet.absoluteFill,
    backgroundColor: 'rgba(0, 0, 0, 0.6)',
  },
  drawerContainer: {
    height: '100%',
    backgroundColor: TelegramColors.base,
    elevation: 24,
    shadowColor: '#000000',
    shadowOffset: { width: 6, height: 0 },
    shadowOpacity: 0.5,
    shadowRadius: 14,
  },
  safeArea: {
    flex: 1,
  },
  profileHeader: {
    backgroundColor: TelegramColors.header,
    paddingHorizontal: 18,
    paddingTop: 16,
    paddingBottom: 14,
  },
  largeAvatar: {
    width: 60,
    height: 60,
    borderRadius: 30,
    backgroundColor: '#62b5e5',
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 16,
  },
  largeAvatarText: {
    color: '#ffffff',
    fontSize: 22,
    fontWeight: '700',
    letterSpacing: 0.5,
  },
  profileInfoRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  nameAndPhone: {
    flex: 1,
  },
  profileName: {
    color: TelegramColors.textPrimary,
    fontSize: 16,
    fontWeight: '700',
    letterSpacing: 0.2,
  },
  profilePhone: {
    color: '#8596a7',
    fontSize: 13,
    marginTop: 2,
  },
  accountsSection: {
    backgroundColor: TelegramColors.header,
    paddingHorizontal: 18,
    paddingBottom: 8,
  },
  accountRow: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 10,
  },
  smallAvatarWrapper: {
    position: 'relative',
    marginRight: 16,
  },
  smallAvatar: {
    width: 34,
    height: 34,
    borderRadius: 17,
    backgroundColor: '#62b5e5',
    justifyContent: 'center',
    alignItems: 'center',
  },
  smallAvatarText: {
    color: '#ffffff',
    fontSize: 13,
    fontWeight: '700',
  },
  accountCheckBadge: {
    position: 'absolute',
    right: -2,
    bottom: -2,
    width: 14,
    height: 14,
    borderRadius: 7,
    backgroundColor: '#2b97e2',
    justifyContent: 'center',
    alignItems: 'center',
    borderWidth: 1.5,
    borderColor: TelegramColors.header,
  },
  accountName: {
    color: TelegramColors.textPrimary,
    fontSize: 15,
    fontWeight: '600',
  },
  addAccountIconWrapper: {
    width: 34,
    height: 34,
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: 16,
  },
  divider: {
    height: 0.8,
    backgroundColor: TelegramColors.border,
  },
  menuSection: {
    paddingVertical: 6,
  },
  menuItem: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 18,
    paddingVertical: 13,
  },
  menuIcon: {
    width: 34,
    marginRight: 12,
  },
  menuItemText: {
    color: TelegramColors.textPrimary,
    fontSize: 15,
    fontWeight: '500',
    flex: 1,
  },
  settingsBadge: {
    width: 22,
    height: 22,
    borderRadius: 11,
    backgroundColor: '#52a6e6',
    justifyContent: 'center',
    alignItems: 'center',
  },
  botFatherBadge: {
    backgroundColor: '#52a6e6',
    paddingHorizontal: 7,
    paddingVertical: 2,
    borderRadius: 8,
  },
  botFatherBadgeText: {
    color: '#ffffff',
    fontSize: 10,
    fontWeight: '700',
    letterSpacing: 0.5,
  },
  nightModeRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 18,
    paddingVertical: 12,
  },
  nightModeLeft: {
    flexDirection: 'row',
    alignItems: 'center',
    flex: 1,
  },
});
