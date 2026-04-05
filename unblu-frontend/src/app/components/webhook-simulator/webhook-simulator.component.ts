import {Component, Input} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {ApiService} from '../../services/api.service';
import {PersonInfo} from '../../models/person.model';

interface SimulationResult {
  statusCode: number;
  body: string;
  success: boolean;
}

@Component({
  selector: 'app-webhook-simulator',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './webhook-simulator.component.html',
  styleUrl: './webhook-simulator.component.css'
})
export class WebhookSimulatorComponent {
  @Input() clients: PersonInfo[] = [];
  @Input() agents: PersonInfo[] = [];

  // Form state
  selectedClient: PersonInfo | null = null;
  selectedAgent: PersonInfo | null = null;
  conversationId: string;
  topic: string = 'Conversation directe';
  engagementType: string = 'CHAT_REQUEST';
  locale: string = 'fr';
  state: string = 'CREATED';

  // UI state
  showPayload = false;
  loading = false;
  error: string | null = null;
  simulationResult: SimulationResult | null = null;

  readonly engagementTypes = ['CHAT_REQUEST', 'DIRECT_CHAT', 'OFFLINE_CHAT_REQUEST', 'SCHEDULE_CHAT'];
  readonly locales = ['fr', 'en', 'de'];
  readonly states = ['CREATED', 'ACTIVE'];

  constructor(private apiService: ApiService) {
    this.conversationId = this.generateId();
  }

  get generatedPayload(): string {
    return JSON.stringify(this.buildPayload(), null, 2);
  }

  generateId(): string {
    const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_';
    let result = '';
    for (let i = 0; i < 22; i++) {
      result += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    return result;
  }

  regenerateId(): void {
    this.conversationId = this.generateId();
  }

  buildPayload(): object {
    const now = Date.now();
    const accountId = 'WnVbJBHMRDCO70asbpKdHw';

    return {
      '$_type': 'ConversationCreatedEvent',
      timestamp: now,
      eventType: 'conversation.created',
      accountId,
      conversation: {
        '$_type': 'ConversationData',
        creationTimestamp: now,
        endTimestamp: null,
        id: this.conversationId,
        accountId,
        topic: this.topic || 'Conversation directe',
        state: this.state,
        initialEngagementType: this.engagementType,
        locale: this.locale,
        scheduledTimestamp: null,
        recipient: this.selectedAgent ? {
          '$_type': 'PersonData',
          id: this.selectedAgent.id,
          accountId,
          personSource: 'USER_DB',
          sourceId: this.selectedAgent.sourceId,
          firstName: this.selectedAgent.firstName || '',
          lastName: this.selectedAgent.lastName || '',
          displayName: this.selectedAgent.displayName,
          displayNameForAgent: this.selectedAgent.displayName,
          displayNameForVisitor: this.selectedAgent.displayName,
          personType: 'AGENT',
          authorizationRole: 'SUPERVISOR',
          email: this.selectedAgent.email || null,
          teamId: null,
          labels: [],
          note: '',
          links: null,
          avatar: null,
          metadata: null
        } : null,
        participants: [
          ...(this.selectedClient ? [{
            '$_type': 'ParticipantData',
            state: 'CREATED',
            personId: this.selectedClient.id,
            hidden: false,
            conversationStarred: false,
            participationType: 'CONTEXT_PERSON'
          }] : []),
          ...(this.selectedAgent ? [{
            '$_type': 'ParticipantData',
            state: 'CREATED',
            personId: this.selectedAgent.id,
            hidden: false,
            conversationStarred: false,
            participationType: 'ASSIGNED_AGENT'
          }] : [])
        ],
        externalParticipants: [],
        botParticipants: [],
        conversationVisibility: null,
        tokboxSessionId: null,
        visitorData: this.selectedClient?.sourceId || null,
        conversationTemplateId: null,
        inheritConfigurationAndTexts: null,
        links: [],
        externalMessengerChannelId: null,
        sourceId: null,
        sourceUrl: null,
        endReason: null,
        initialEngagementUrl: null,
        awaitedPersonType: 'NONE',
        awaitedPersonTypeChangeTimestamp: null,
        metadata: null,
        configuration: null,
        text: null
      }
    };
  }

  sendWebhook(): void {
    if (!this.selectedClient || !this.selectedAgent) {
      this.error = 'Veuillez sélectionner un client et un agent';
      return;
    }

    this.loading = true;
    this.error = null;
    this.simulationResult = null;

    this.apiService.simulateWebhookEvent('conversation.created', this.buildPayload()).subscribe({
      next: (response) => {
        this.simulationResult = {
          statusCode: response.status,
          body: response.body || '(réponse vide)',
          success: response.ok
        };
        this.loading = false;
      },
      error: (err) => {
        this.simulationResult = {
          statusCode: err.status || 0,
          body: err.error || err.message || 'Erreur inconnue',
          success: false
        };
        this.loading = false;
      }
    });
  }

  reset(): void {
    this.selectedClient = null;
    this.selectedAgent = null;
    this.conversationId = this.generateId();
    this.topic = 'Conversation directe';
    this.engagementType = 'CHAT_REQUEST';
    this.locale = 'fr';
    this.state = 'CREATED';
    this.showPayload = false;
    this.simulationResult = null;
    this.error = null;
  }
}
