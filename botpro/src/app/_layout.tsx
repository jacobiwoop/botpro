import React, { useEffect } from 'react';
import { Stack, DarkTheme, ThemeProvider } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import * as SystemUI from 'expo-system-ui';

// Thème sombre Telegram pour éliminer tout flash blanc natif
const TelegramDarkTheme = {
  ...DarkTheme,
  colors: {
    ...DarkTheme.colors,
    background: '#17212b',
    card: '#242f3d',
    text: '#ffffff',
    border: '#101921',
  },
};

export default function RootLayout() {
  useEffect(() => {
    // Définit la couleur de fond native de la fenêtre Android
    SystemUI.setBackgroundColorAsync('#17212b');
  }, []);

  return (
    <SafeAreaProvider>
      <ThemeProvider value={TelegramDarkTheme}>
        <StatusBar style="light" />
        <Stack
          screenOptions={{
            headerShown: false,
            animation: 'ios_from_right',
            contentStyle: { backgroundColor: '#17212b' },
          }}
        >
          <Stack.Screen
            name="index"
            options={{
              contentStyle: { backgroundColor: '#17212b' },
            }}
          />
          <Stack.Screen
            name="conversation"
            options={{
              animation: 'ios_from_right',
              contentStyle: { backgroundColor: '#0e1621' },
            }}
          />
          <Stack.Screen
            name="botfather"
            options={{
              animation: 'ios_from_right',
              contentStyle: { backgroundColor: '#17212b' },
            }}
          />
        </Stack>
      </ThemeProvider>
    </SafeAreaProvider>
  );
}
