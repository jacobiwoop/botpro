import React, { useEffect, useRef } from 'react';
import {
  View,
  Text,
  StyleSheet,
  Modal,
  Animated,
  TouchableOpacity,
  TouchableWithoutFeedback,
  Easing,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Feather, MaterialIcons, Ionicons } from '@expo/vector-icons';
import { TelegramColors } from '@/constants/telegramTheme';

export type AttachmentOptionType =
  | 'gallery'
  | 'document'
  | 'camera'
  | 'location'
  | 'contact'
  | 'audio'
  | 'poll';

interface AttachmentBottomSheetProps {
  visible: boolean;
  onClose: () => void;
  onSelectOption: (option: AttachmentOptionType) => void;
}

interface OptionItem {
  id: AttachmentOptionType;
  label: string;
  iconName: any;
  iconType: 'feather' | 'material' | 'ionicons';
  color: string;
}

const OPTIONS_ROW_1: OptionItem[] = [
  { id: 'gallery', label: 'Galerie', iconName: 'image', iconType: 'feather', color: '#9C27B0' },
  { id: 'document', label: 'Fichier', iconName: 'file-text', iconType: 'feather', color: '#1E88E5' },
  { id: 'camera', label: 'Caméra', iconName: 'camera', iconType: 'feather', color: '#E91E63' },
  { id: 'location', label: 'Lieu', iconName: 'location-on', iconType: 'material', color: '#43A047' },
];

const OPTIONS_ROW_2: OptionItem[] = [
  { id: 'contact', label: 'Contact', iconName: 'person', iconType: 'material', color: '#00ACC1' },
  { id: 'audio', label: 'Musique', iconName: 'headphones', iconType: 'feather', color: '#FB8C00' },
  { id: 'poll', label: 'Sondage', iconName: 'bar-chart-2', iconType: 'feather', color: '#FFB300' },
];

export function AttachmentBottomSheet({
  visible,
  onClose,
  onSelectOption,
}: AttachmentBottomSheetProps) {
  const slideAnim = useRef(new Animated.Value(300)).current;
  const fadeAnim = useRef(new Animated.Value(0)).current;

  useEffect(() => {
    if (visible) {
      slideAnim.setValue(300);
      fadeAnim.setValue(0);
      Animated.parallel([
        Animated.timing(slideAnim, {
          toValue: 0,
          duration: 260,
          easing: Easing.out(Easing.cubic),
          useNativeDriver: true,
        }),
        Animated.timing(fadeAnim, {
          toValue: 1,
          duration: 260,
          useNativeDriver: true,
        }),
      ]).start();
    }
  }, [visible, slideAnim, fadeAnim]);

  const handleClose = () => {
    Animated.parallel([
      Animated.timing(slideAnim, {
        toValue: 300,
        duration: 200,
        easing: Easing.in(Easing.cubic),
        useNativeDriver: true,
      }),
      Animated.timing(fadeAnim, {
        toValue: 0,
        duration: 200,
        useNativeDriver: true,
      }),
    ]).start(() => {
      onClose();
    });
  };

  const handleSelect = (option: AttachmentOptionType) => {
    onSelectOption(option);
    handleClose();
  };

  const renderIcon = (item: OptionItem) => {
    if (item.iconType === 'feather') {
      return <Feather name={item.iconName} size={22} color="#ffffff" />;
    }
    if (item.iconType === 'material') {
      return <MaterialIcons name={item.iconName} size={24} color="#ffffff" />;
    }
    return <Ionicons name={item.iconName} size={22} color="#ffffff" />;
  };

  return (
    <Modal
      visible={visible}
      transparent
      animationType="none"
      onRequestClose={handleClose}
      statusBarTranslucent
    >
      <View style={styles.overlay}>
        {/* Scrim tap pour fermer */}
        <TouchableWithoutFeedback onPress={handleClose}>
          <Animated.View style={[styles.scrim, { opacity: fadeAnim }]} />
        </TouchableWithoutFeedback>

        {/* Contenu du Bottom Sheet */}
        <Animated.View
          style={[
            styles.sheetContainer,
            { transform: [{ translateY: slideAnim }] },
          ]}
        >
          <SafeAreaView edges={['bottom']}>
            <View style={styles.handleBar} />
            <Text style={styles.sheetTitle}>Partager du contenu</Text>

            {/* Rangée 1 */}
            <View style={styles.row}>
              {OPTIONS_ROW_1.map((item) => (
                <TouchableOpacity
                  key={item.id}
                  style={styles.optionItem}
                  onPress={() => handleSelect(item.id)}
                  activeOpacity={0.7}
                >
                  <View style={[styles.iconCircle, { backgroundColor: item.color }]}>
                    {renderIcon(item)}
                  </View>
                  <Text style={styles.optionLabel}>{item.label}</Text>
                </TouchableOpacity>
              ))}
            </View>

            {/* Rangée 2 */}
            <View style={styles.row}>
              {OPTIONS_ROW_2.map((item) => (
                <TouchableOpacity
                  key={item.id}
                  style={styles.optionItem}
                  onPress={() => handleSelect(item.id)}
                  activeOpacity={0.7}
                >
                  <View style={[styles.iconCircle, { backgroundColor: item.color }]}>
                    {renderIcon(item)}
                  </View>
                  <Text style={styles.optionLabel}>{item.label}</Text>
                </TouchableOpacity>
              ))}
              {/* Espace vide pour garder l'alignement */}
              <View style={styles.optionItem} />
            </View>
          </SafeAreaView>
        </Animated.View>
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  overlay: {
    flex: 1,
    justifyContent: 'flex-end',
  },
  scrim: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    backgroundColor: 'rgba(0, 0, 0, 0.55)',
  },
  sheetContainer: {
    backgroundColor: TelegramColors.surface,
    borderTopLeftRadius: 22,
    borderTopRightRadius: 22,
    paddingHorizontal: 20,
    paddingTop: 12,
    paddingBottom: 16,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: -4 },
    shadowOpacity: 0.25,
    shadowRadius: 10,
    elevation: 20,
  },
  handleBar: {
    width: 36,
    height: 4,
    borderRadius: 2,
    backgroundColor: 'rgba(255, 255, 255, 0.25)',
    alignSelf: 'center',
    marginBottom: 14,
  },
  sheetTitle: {
    color: '#ffffff',
    fontSize: 16,
    fontWeight: '700',
    marginBottom: 18,
    marginLeft: 4,
  },
  row: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginBottom: 16,
  },
  optionItem: {
    alignItems: 'center',
    width: 68,
  },
  iconCircle: {
    width: 52,
    height: 52,
    borderRadius: 26,
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 6,
    elevation: 4,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.2,
    shadowRadius: 4,
  },
  optionLabel: {
    color: '#d6e2ee',
    fontSize: 12,
    fontWeight: '500',
    textAlign: 'center',
  },
});
