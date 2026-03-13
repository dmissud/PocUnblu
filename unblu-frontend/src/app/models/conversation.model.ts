export interface UnbluConversationInfo {
  unbluConversationId: string;
  unbluJoinUrl: string;
}

export interface ConversationContext {
  unbluConversationId: string;
  unbluJoinUrl: string;
  teamId?: string;
  agentPersonId?: string;
}

export interface DirectConversationRequest {
  clientSourceId: string;
  agentSourceId: string;
  subject: string;
}

export interface TeamConversationRequest {
  clientId: string;
  subject: string;
}
