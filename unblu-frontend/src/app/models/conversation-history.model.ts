export interface ConversationHistoryItem {
  conversationId: string;
  topic: string | null;
  createdAt: string;
  endedAt: string | null;
  status: 'ACTIVE' | 'ENDED';
}

export interface ConversationParticipant {
  personId: string;
  displayName: string;
  participantType: 'VISITOR' | 'AGENT' | 'BOT';
}

export interface ConversationEvent {
  eventType: 'CREATED' | 'MESSAGE' | 'ENDED';
  occurredAt: string;
  messageText?: string | null;
  senderPersonId?: string | null;
  senderDisplayName?: string | null;
}

export interface ConversationHistoryDetail {
  conversationId: string;
  topic: string | null;
  createdAt: string;
  endedAt: string | null;
  status: 'ACTIVE' | 'ENDED';
  participants: ConversationParticipant[];
  events: ConversationEvent[];
}

export interface ConversationHistoryPage {
  items: ConversationHistoryItem[];
  totalItems: number;
  page: number;
  size: number;
  totalPages: number;
}
