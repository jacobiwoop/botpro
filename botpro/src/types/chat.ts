export type AvatarType = 'telegram' | 'image' | 'initials' | 'getpay' | 'dot' | 'saved';

export interface ChatItem {
  id: string;
  name: string;
  avatarType: AvatarType;
  avatarUri?: string;
  avatarBg?: string;
  initials?: string;
  lastMessage: string;
  time: string;
  unreadCount?: number;
  isMuted?: boolean;
  isVerified?: boolean;
  isRead?: boolean; // double check pour message envoyé
}
