import React, { useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TextInput,
  TouchableOpacity,
  ScrollView,
  KeyboardAvoidingView,
  Platform,
  ActivityIndicator,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useRouter } from 'expo-router';
import { Ionicons, FontAwesome5 } from '@expo/vector-icons';
import { registerUser, loginUser } from '@/services/api';
import { TelegramColors } from '@/constants/telegramTheme';

export default function AuthScreen() {
  const router = useRouter();

  const [mode, setMode] = useState<'login' | 'register'>('login');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [username, setUsername] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const handleAuth = async () => {
    setErrorMessage(null);
    const cleanEmail = email.trim().toLowerCase();
    const cleanPassword = password.trim();

    if (!cleanEmail || !cleanEmail.includes('@')) {
      setErrorMessage('Please enter a valid email address.');
      return;
    }

    if (cleanPassword.length < 6) {
      setErrorMessage('Password must be at least 6 characters.');
      return;
    }

    setLoading(true);

    if (mode === 'login') {
      const res = await loginUser(cleanEmail, cleanPassword);
      setLoading(false);
      if (res.ok) {
        router.replace('/');
      } else {
        setErrorMessage(res.error || 'Invalid email or password.');
      }
    } else {
      const cleanFirstName = firstName.trim();
      if (!cleanFirstName) {
        setLoading(false);
        setErrorMessage('Please enter your first name.');
        return;
      }

      const res = await registerUser(
        cleanEmail,
        cleanPassword,
        cleanFirstName,
        lastName.trim() || undefined,
        username.trim() ? username.trim().toLowerCase().replace(/[@\s]/g, '') : undefined
      );
      setLoading(false);
      if (res.ok) {
        router.replace('/');
      } else {
        setErrorMessage(res.error || 'Registration failed.');
      }
    }
  };

  return (
    <SafeAreaView edges={['top', 'bottom']} style={styles.safeArea}>
      <KeyboardAvoidingView
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
        style={{ flex: 1 }}
      >
        <ScrollView
          contentContainerStyle={styles.scrollContent}
          showsVerticalScrollIndicator={false}
        >
          {/* Logo Telegram & Titre */}
          <View style={styles.headerSection}>
            <View style={styles.logoCircle}>
              <FontAwesome5 name="telegram-plane" size={44} color="#ffffff" />
            </View>
            <Text style={styles.appTitle}>Botpro</Text>
            <Text style={styles.appSubtitle}>
              {mode === 'login'
                ? 'Sign in to access your bots and chats'
                : 'Create an account to chat with automated bots'}
            </Text>
          </View>

          {/* Sélecteur d'onglets (Login / Register) */}
          <View style={styles.tabContainer}>
            <TouchableOpacity
              style={[styles.tabButton, mode === 'login' && styles.tabButtonActive]}
              onPress={() => {
                setMode('login');
                setErrorMessage(null);
              }}
              activeOpacity={0.8}
            >
              <Text style={[styles.tabButtonText, mode === 'login' && styles.tabButtonTextActive]}>
                Sign In
              </Text>
            </TouchableOpacity>

            <TouchableOpacity
              style={[styles.tabButton, mode === 'register' && styles.tabButtonActive]}
              onPress={() => {
                setMode('register');
                setErrorMessage(null);
              }}
              activeOpacity={0.8}
            >
              <Text style={[styles.tabButtonText, mode === 'register' && styles.tabButtonTextActive]}>
                Sign Up
              </Text>
            </TouchableOpacity>
          </View>

          {/* Message d'erreur */}
          {errorMessage && (
            <View style={styles.errorBanner}>
              <Ionicons name="alert-circle" size={18} color="#e17076" style={{ marginRight: 8 }} />
              <Text style={styles.errorText}>{errorMessage}</Text>
            </View>
          )}

          {/* Formulaire */}
          <View style={styles.formCard}>
            {mode === 'register' && (
              <>
                <View style={styles.fieldRow}>
                  <TextInput
                    style={styles.input}
                    placeholder="First Name"
                    placeholderTextColor="#6c7883"
                    value={firstName}
                    onChangeText={setFirstName}
                    autoCapitalize="words"
                  />
                </View>
                <View style={styles.divider} />

                <View style={styles.fieldRow}>
                  <TextInput
                    style={styles.input}
                    placeholder="Last Name (Optional)"
                    placeholderTextColor="#6c7883"
                    value={lastName}
                    onChangeText={setLastName}
                    autoCapitalize="words"
                  />
                </View>
                <View style={styles.divider} />

                <View style={styles.fieldRow}>
                  <Text style={styles.prefixText}>@</Text>
                  <TextInput
                    style={[styles.input, { flex: 1 }]}
                    placeholder="username (optional)"
                    placeholderTextColor="#6c7883"
                    value={username}
                    onChangeText={setUsername}
                    autoCapitalize="none"
                    autoCorrect={false}
                  />
                </View>
                <View style={styles.divider} />
              </>
            )}

            <View style={styles.fieldRow}>
              <TextInput
                style={styles.input}
                placeholder="Email Address"
                placeholderTextColor="#6c7883"
                value={email}
                onChangeText={setEmail}
                keyboardType="email-address"
                autoCapitalize="none"
                autoCorrect={false}
              />
            </View>
            <View style={styles.divider} />

            <View style={styles.fieldRow}>
              <TextInput
                style={[styles.input, { flex: 1 }]}
                placeholder="Password (min 6 characters)"
                placeholderTextColor="#6c7883"
                value={password}
                onChangeText={setPassword}
                secureTextEntry={!showPassword}
              />
              <TouchableOpacity
                onPress={() => setShowPassword(!showPassword)}
                style={styles.eyeButton}
                activeOpacity={0.7}
              >
                <Ionicons
                  name={showPassword ? 'eye-off-outline' : 'eye-outline'}
                  size={20}
                  color="#8596a7"
                />
              </TouchableOpacity>
            </View>
          </View>

          {/* Bouton de validation */}
          <TouchableOpacity
            style={styles.submitButton}
            onPress={handleAuth}
            disabled={loading}
            activeOpacity={0.8}
          >
            {loading ? (
              <ActivityIndicator size="small" color="#ffffff" />
            ) : (
              <Text style={styles.submitButtonText}>
                {mode === 'login' ? 'Sign In' : 'Create Account'}
              </Text>
            )}
          </TouchableOpacity>

          {/* Footer switch */}
          <TouchableOpacity
            style={styles.switchModeButton}
            onPress={() => {
              setMode(mode === 'login' ? 'register' : 'login');
              setErrorMessage(null);
            }}
            activeOpacity={0.7}
          >
            <Text style={styles.switchModeText}>
              {mode === 'login'
                ? "Don't have an account? "
                : 'Already have an account? '}
              <Text style={styles.switchModeLink}>
                {mode === 'login' ? 'Sign Up' : 'Sign In'}
              </Text>
            </Text>
          </TouchableOpacity>
        </ScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
    backgroundColor: TelegramColors.base,
  },
  scrollContent: {
    paddingHorizontal: 24,
    paddingTop: 36,
    paddingBottom: 40,
  },
  headerSection: {
    alignItems: 'center',
    marginBottom: 32,
  },
  logoCircle: {
    width: 84,
    height: 84,
    borderRadius: 42,
    backgroundColor: '#2aabee',
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 16,
    shadowColor: '#2aabee',
    shadowOffset: { width: 0, height: 6 },
    shadowOpacity: 0.35,
    shadowRadius: 10,
    elevation: 8,
  },
  appTitle: {
    color: '#ffffff',
    fontSize: 26,
    fontWeight: '700',
    marginBottom: 6,
    letterSpacing: 0.3,
  },
  appSubtitle: {
    color: '#8596a7',
    fontSize: 14,
    textAlign: 'center',
    lineHeight: 20,
    paddingHorizontal: 16,
  },
  tabContainer: {
    flexDirection: 'row',
    backgroundColor: '#242f3d',
    borderRadius: 10,
    padding: 4,
    marginBottom: 20,
  },
  tabButton: {
    flex: 1,
    paddingVertical: 10,
    alignItems: 'center',
    borderRadius: 8,
  },
  tabButtonActive: {
    backgroundColor: '#52a6e6',
  },
  tabButtonText: {
    color: '#8596a7',
    fontSize: 14,
    fontWeight: '600',
  },
  tabButtonTextActive: {
    color: '#ffffff',
  },
  errorBanner: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: 'rgba(225, 112, 118, 0.12)',
    borderWidth: 1,
    borderColor: 'rgba(225, 112, 118, 0.4)',
    borderRadius: 10,
    paddingHorizontal: 14,
    paddingVertical: 10,
    marginBottom: 16,
  },
  errorText: {
    color: '#e17076',
    fontSize: 13,
    flex: 1,
  },
  formCard: {
    backgroundColor: '#242f3d',
    borderRadius: 12,
    overflow: 'hidden',
    marginBottom: 24,
  },
  fieldRow: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 16,
    height: 52,
  },
  prefixText: {
    color: '#8596a7',
    fontSize: 16,
    marginRight: 4,
  },
  input: {
    color: '#ffffff',
    fontSize: 15,
    padding: 0,
    flex: 1,
  },
  eyeButton: {
    padding: 6,
  },
  divider: {
    height: 0.8,
    backgroundColor: TelegramColors.border,
    marginLeft: 16,
  },
  submitButton: {
    height: 50,
    backgroundColor: '#52a6e6',
    borderRadius: 12,
    justifyContent: 'center',
    alignItems: 'center',
    elevation: 3,
    shadowColor: '#52a6e6',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.3,
    shadowRadius: 6,
  },
  submitButtonText: {
    color: '#ffffff',
    fontSize: 16,
    fontWeight: '700',
  },
  switchModeButton: {
    alignItems: 'center',
    marginTop: 24,
    padding: 10,
  },
  switchModeText: {
    color: '#8596a7',
    fontSize: 14,
  },
  switchModeLink: {
    color: '#52a6e6',
    fontWeight: '600',
  },
});
