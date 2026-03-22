import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {PersonInfo} from '../models/person.model';
import {TeamInfo} from '../models/team.model';
import {NamedAreaInfo} from '../models/named-area.model';
import {
  ConversationSyncResult,
  DirectConversationRequest,
  StartConversationResponse,
  TeamConversationRequest
} from '../models/conversation.model';
import {WebhookSetupResult, WebhookStatus} from '../models/webhook.model';

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private readonly baseUrl = '/api/v1';

  constructor(private http: HttpClient) {}

  getClients(): Observable<PersonInfo[]> {
    return this.http.get<PersonInfo[]>(`${this.baseUrl}/persons`, {
      headers: { 'personSource': 'VIRTUAL' }
    });
  }

  getAgents(): Observable<PersonInfo[]> {
    return this.http.get<PersonInfo[]>(`${this.baseUrl}/persons`, {
      headers: { 'personSource': 'USER_DB' }
    });
  }

  getTeams(): Observable<TeamInfo[]> {
    return this.http.get<TeamInfo[]>(`${this.baseUrl}/teams`);
  }

  getNamedAreas(): Observable<NamedAreaInfo[]> {
    return this.http.get<NamedAreaInfo[]>(`${this.baseUrl}/named-areas`);
  }

  startDirectConversation(request: DirectConversationRequest): Observable<StartConversationResponse> {
    return this.http.post<StartConversationResponse>(`${this.baseUrl}/conversations/direct`, request);
  }

  startTeamConversation(request: TeamConversationRequest): Observable<StartConversationResponse> {
    const camelRequest = {
      clientId: request.clientId,
      subject: request.subject,
      origin: 'FRONTEND_TEST'
    };
    return this.http.post<StartConversationResponse>(`${this.baseUrl}/conversations/start`, camelRequest);
  }

  syncConversations(): Observable<ConversationSyncResult> {
    return this.http.post<ConversationSyncResult>(`${this.baseUrl}/conversations/sync`, {});
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
