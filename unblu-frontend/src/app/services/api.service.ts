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
}
