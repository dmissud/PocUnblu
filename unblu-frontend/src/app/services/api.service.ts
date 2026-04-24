import { HttpClient, HttpHeaders, HttpResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { BotInfo } from '../models/bot.model';
import { ConversationHistoryDetail, ConversationHistoryPage } from '../models/conversation-history.model';
import { ConversationSearchResponse, ConversationState } from '../models/conversation-search.model';
import {
  ConversationSyncResult,
  DirectConversationRequest,
  StartConversationResponse,
  TeamConversationRequest
} from '../models/conversation.model';
import { JobsStatusResponse, RunningJobsResponse } from '../models/job.model';
import { NamedAreaInfo } from '../models/named-area.model';
import { PersonInfo } from '../models/person.model';
import { ConversationStatistics } from '../models/statistics.model';
import { TeamInfo } from '../models/team.model';
import { WebhookSetupResult, WebhookStatus } from '../models/webhook.model';

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private readonly baseUrl = '/api/v1';
  private readonly livekitBaseUrl = '/api/v1/livekit';

  constructor(private http: HttpClient) {}

  getPersons(): Observable<PersonInfo[]> {
    return this.http.get<PersonInfo[]>(`${this.baseUrl}/persons`);
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

  startLiveKitConversation(personId: string): Observable<StartConversationResponse> {
    return this.http.post<StartConversationResponse>(`${this.livekitBaseUrl}/conversations/start`, {personId});
  }

  syncConversations(): Observable<ConversationSyncResult> {
    return this.http.post<ConversationSyncResult>(`/api/history/sync`, {});
  }

  getConversationHistory(
    page: number = 0,
    size: number = 10,
    sortField: string = 'CREATED_AT',
    sortDir: string = 'DESC'
  ): Observable<ConversationHistoryPage> {
    return this.http.get<ConversationHistoryPage>(
      `/api/history/conversations?page=${page}&size=${size}&sortField=${sortField}&sortDir=${sortDir}`
    );
  }

  getConversationDetail(conversationId: string): Observable<ConversationHistoryDetail> {
    return this.http.get<ConversationHistoryDetail>(
      `/api/history/conversations/${conversationId}`
    );
  }

  enrichConversation(conversationId: string): Observable<ConversationHistoryDetail> {
    return this.http.post<ConversationHistoryDetail>(
      `/api/history/conversations/${conversationId}/enrich`, {}
    );
  }

  searchConversationsByState(state: ConversationState): Observable<ConversationSearchResponse> {
    return this.http.get<ConversationSearchResponse>(
      `${this.baseUrl}/conversations/search`,
      {params: {state}}
    );
  }

  getBots(): Observable<BotInfo[]> {
    return this.http.get<BotInfo[]>(`${this.baseUrl}/supervision/bots`);
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

  simulateWebhookEvent(eventType: string, payload: object): Observable<HttpResponse<string>> {
    const headers = new HttpHeaders({
      'X-Unblu-Event': eventType,
      'X-Unblu-Event-Type': eventType
    });
    return this.http.post('/api/webhooks/unblu', payload, {
      headers,
      observe: 'response',
      responseType: 'text'
    });
  }

  // Jobs monitoring methods
  getJobsStatus(): Observable<JobsStatusResponse> {
    return this.http.get<JobsStatusResponse>('/api/jobs/status');
  }

  getRunningJobs(): Observable<RunningJobsResponse> {
    return this.http.get<RunningJobsResponse>('/api/jobs/running');
  }

  // Statistics methods
  getStatisticsFiles(): Observable<string[]> {
    return this.http.get<string[]>('/api/jobs/statistics/files');
  }

  getStatisticsFile(filename: string): Observable<ConversationStatistics> {
    return this.http.get<ConversationStatistics>(`/api/jobs/statistics/files/${filename}`);
  }
}
