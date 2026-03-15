import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PersonInfo } from '../models/person.model';
import { TeamInfo } from '../models/team.model';
import {
  UnbluConversationInfo,
  ConversationContext,
  DirectConversationRequest,
  TeamConversationRequest
} from '../models/conversation.model';
import { WebhookSetupResult, WebhookStatus } from '../models/webhook.model';

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private readonly baseUrl = '/rest/v1';

  constructor(private http: HttpClient) {}

  getClients(): Observable<PersonInfo[]> {
    return this.http.get<PersonInfo[]>(`${this.baseUrl}/persons/clients`);
  }

  getAgents(): Observable<PersonInfo[]> {
    return this.http.get<PersonInfo[]>(`${this.baseUrl}/persons/agents`);
  }

  getTeams(): Observable<TeamInfo[]> {
    return this.http.get<TeamInfo[]>(`${this.baseUrl}/teams`);
  }

  startDirectConversation(request: DirectConversationRequest): Observable<UnbluConversationInfo> {
    return this.http.post<UnbluConversationInfo>(`${this.baseUrl}/conversations/direct`, request);
  }

  startTeamConversation(request: TeamConversationRequest): Observable<ConversationContext> {
    return this.http.post<ConversationContext>(`${this.baseUrl}/conversations/team`, request);
  }

  // Webhook setup methods
  setupWebhook(): Observable<WebhookSetupResult> {
    return this.http.post<WebhookSetupResult>(`${this.baseUrl}/webhooks/setup`, {});
  }

  getWebhookStatus(): Observable<WebhookStatus> {
    return this.http.get<WebhookStatus>(`${this.baseUrl}/webhooks/status`);
  }

  teardownWebhook(deleteWebhook: boolean = false): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/webhooks/teardown?deleteWebhook=${deleteWebhook}`);
  }
}
