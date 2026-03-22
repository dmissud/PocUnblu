export interface UnbluConversationInfo {
  unbluConversationId: string;
  unbluJoinUrl: string;
}

export interface StartConversationResponse {
  unbluConversationId: string;
  unbluJoinUrl: string;
  status: string;
  message: string;
}

export interface ConversationContext {
  unbluConversationId: string;
  unbluJoinUrl: string;
  teamId?: string;
  agentPersonId?: string;
}

export interface DirectConversationRequest {
  virtualParticipantSourceId: string;
  agentParticipantSourceId: string;
  subject: string;
}

export interface TeamConversationRequest {
  clientId: string;
  subject: string;
  teamId: string;
}

export interface ConversationSyncResult {
  totalScanned: number;
  newlyPersisted: number;
  alreadyExisting: number;
  errors: number;
  errorConversationIds: string[];
  message: string;
}
