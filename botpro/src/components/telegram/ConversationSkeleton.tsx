import React, { useEffect, useRef } from 'react';
import { View, StyleSheet, Animated, Easing } from 'react-native';

const SKELETON_ITEMS = [
  { id: 's1', isOutgoing: false, width: '60%', height: 42 },
  { id: 's2', isOutgoing: true, width: '45%', height: 38 },
  { id: 's3', isOutgoing: false, width: '75%', height: 58 },
  { id: 's4', isOutgoing: true, width: '35%', height: 38 },
  { id: 's5', isOutgoing: false, width: '50%', height: 42 },
  { id: 's6', isOutgoing: true, width: '65%', height: 52 },
];

export function ConversationSkeleton() {
  const pulseAnim = useRef(new Animated.Value(0.3)).current;

  useEffect(() => {
    const animation = Animated.loop(
      Animated.sequence([
        Animated.timing(pulseAnim, {
          toValue: 0.85,
          duration: 800,
          easing: Easing.inOut(Easing.ease),
          useNativeDriver: true,
        }),
        Animated.timing(pulseAnim, {
          toValue: 0.3,
          duration: 800,
          easing: Easing.inOut(Easing.ease),
          useNativeDriver: true,
        }),
      ])
    );
    animation.start();

    return () => animation.stop();
  }, [pulseAnim]);

  return (
    <View style={styles.container}>
      {SKELETON_ITEMS.map((item) => (
        <View
          key={item.id}
          style={[
            styles.bubbleWrapper,
            item.isOutgoing ? styles.outgoingWrapper : styles.incomingWrapper,
          ]}
        >
          <Animated.View
            style={[
              styles.bubble,
              item.isOutgoing ? styles.outgoingBubble : styles.incomingBubble,
              {
                width: item.width as any,
                height: item.height,
                opacity: pulseAnim,
              },
            ]}
          >
            {/* Simulation de lignes de texte internes dans la bulle */}
            <View style={styles.textLine1} />
            {item.height > 45 && <View style={styles.textLine2} />}
          </Animated.View>
        </View>
      ))}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    paddingHorizontal: 10,
    paddingVertical: 14,
    justifyContent: 'flex-end',
  },
  bubbleWrapper: {
    marginVertical: 5,
    width: '100%',
  },
  incomingWrapper: {
    alignItems: 'flex-start',
  },
  outgoingWrapper: {
    alignItems: 'flex-end',
  },
  bubble: {
    borderRadius: 16,
    paddingHorizontal: 12,
    paddingVertical: 8,
    justifyContent: 'center',
  },
  incomingBubble: {
    backgroundColor: '#202b36',
    borderBottomLeftRadius: 4,
  },
  outgoingBubble: {
    backgroundColor: '#2b5278',
    borderBottomRightRadius: 4,
  },
  textLine1: {
    height: 10,
    borderRadius: 5,
    backgroundColor: 'rgba(255, 255, 255, 0.15)',
    width: '80%',
    marginBottom: 4,
  },
  textLine2: {
    height: 10,
    borderRadius: 5,
    backgroundColor: 'rgba(255, 255, 255, 0.1)',
    width: '50%',
  },
});
