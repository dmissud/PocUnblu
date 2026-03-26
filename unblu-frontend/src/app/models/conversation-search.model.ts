export interface ConversationSearchResponse {
  conversations: ConversationSearchItem[];
  totalCount: number;
  searchedState: string;
}

export interface ConversationSearchItem {
  conversationId: string;
  topic: string | null;
  state: string;
  createdAt: string;
  endedAt: string | null;
}

export enum ConversationState {
  ONBOARDING = 'ONBOARDING',
  ACTIVE = 'ACTIVE',
  INACTIVE = 'INACTIVE',
  ENDED = 'ENDED',
  OFFBOARDING = 'OFFBOARDING'
}
