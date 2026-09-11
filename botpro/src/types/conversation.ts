export type MessageType = 'text' | 'file';

export interface ReplyQuote {
  senderName: string;
  text: string;
}

export interface FileAttachment {
  name: string;
  size: string;
  thumbnailUri: string;
}

export interface Message {
  id: string;
  type: MessageType;
  isOutgoing: boolean;
  text?: string;
  time: string;
  isRead?: boolean;
  isDoubleCheck?: boolean;
  replyQuote?: ReplyQuote;
  file?: FileAttachment;
}
