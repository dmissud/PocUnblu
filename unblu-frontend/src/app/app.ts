import {Component, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {ApiService} from './services/api.service';
import {PersonInfo} from './models/person.model';
import {TeamInfo} from './models/team.model';
import {NamedAreaInfo} from './models/named-area.model';
import {WebhookStatus} from './models/webhook.model';
import {ConversationHistoryComponent} from './components/conversation-history/conversation-history.component';
import {InactiveConversationsComponent} from './components/inactive-conversations/inactive-conversations.component';
import {WebhookSimulatorComponent} from './components/webhook-simulator/webhook-simulator.component';

@Component({
  selector: 'app-root',
  imports: [CommonModule, FormsModule, ConversationHistoryComponent, InactiveConversationsComponent, WebhookSimulatorComponent],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App implements OnInit {
  title = 'Unblu Conversation Manager';

  activeTab: 'conversations' | 'history' | 'search' | 'livekit' | 'webhook-simulator' = 'conversations';

  // Lists
  clients: PersonInfo[] = [];
  agents: PersonInfo[] = [];
  teams: TeamInfo[] = [];
  namedAreas: NamedAreaInfo[] = [];

  // Selected values
  selectedClient: PersonInfo | null = null;
  selectedAgent: PersonInfo | null = null;
  selectedTeam: TeamInfo | null = null;
  selectedNamedArea: NamedAreaInfo | null = null;

  // Form values
  directSubject: string = '';
  teamSubject: string = '';
  recipientType: 'TEAM' | 'NAMED_AREA' = 'TEAM';

  // Results
  directResult: any = null;
  teamResult: any = null;
  liveKitResult: any = null;
  error: string | null = null;

  // Loading states
  loading = false;
  loadingData = true;

  // Webhook status
  webhookStatus: WebhookStatus | null = null;
  webhookLoading = false;

  constructor(private apiService: ApiService) {}

  ngOnInit(): void {
    this.loadData();
    this.loadWebhookStatus();
  }

  loadData(): void {
    this.loadingData = true;

    Promise.all([
      this.apiService.getPersons().toPromise(),
      this.apiService.getTeams().toPromise(),
      this.apiService.getNamedAreas().toPromise()
    ]).then(([persons, teams, namedAreas]) => {
      const allPersons = persons || [];
      this.clients = allPersons.filter(p => p.personType === 'VISITOR');
      this.agents  = allPersons.filter(p => p.personType === 'AGENT');
      this.teams = teams || [];
      this.namedAreas = namedAreas || [];
      this.loadingData = false;
    }).catch(err => {
      this.error = 'Erreur lors du chargement des données: ' + err.message;
      this.loadingData = false;
    });
  }

  resetLiveKitForm(): void {
    this.selectedClient = null;
    this.liveKitResult = null;
    this.error = null;
  }

  startDirectConversation(): void {
    if (!this.selectedClient || !this.selectedAgent) {
      this.error = 'Veuillez sélectionner un client et un agent';
      return;
    }

    this.loading = true;
    this.error = null;
    this.directResult = null;

    this.apiService.startDirectConversation({
      virtualParticipantId: this.selectedClient.id,
      virtualParticipantSourceId: this.selectedClient.sourceId,
      agentParticipantId: this.selectedAgent.id,
      agentParticipantSourceId: this.selectedAgent.sourceId,
      subject: this.directSubject || 'Conversation directe'
    }).subscribe({
      next: (result) => {
        this.directResult = result;
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Erreur lors de la création de la conversation: ' + err.message;
        this.loading = false;
      }
    });
  }

  startTeamConversation(): void {
    if (!this.selectedClient) {
      this.error = 'Veuillez sélectionner un client';
      return;
    }

    if (this.recipientType === 'TEAM' && !this.selectedTeam) {
      this.error = 'Veuillez sélectionner une équipe';
      return;
    }

    if (this.recipientType === 'NAMED_AREA' && !this.selectedNamedArea) {
      this.error = 'Veuillez sélectionner une zone nommée';
      return;
    }

    this.loading = true;
    this.error = null;
    this.teamResult = null;

    const recipientId = this.recipientType === 'TEAM'
      ? this.selectedTeam!.id
      : this.selectedNamedArea!.id;

    this.apiService.startTeamConversation({
      clientId: this.selectedClient.sourceId,
      subject: this.teamSubject || `Conversation ${this.recipientType === 'TEAM' ? 'équipe' : 'zone nommée'}`,
      teamId: recipientId
    }).subscribe({
      next: (result) => {
        this.teamResult = result;
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Erreur lors de la création de la conversation: ' + err.message;
        this.loading = false;
      }
    });
  }

  startLiveKitConversation(): void {
    if (!this.selectedClient) {
      this.error = 'Veuillez sélectionner un client';
      return;
    }

    this.loading = true;
    this.error = null;
    this.liveKitResult = null;

    this.apiService.startLiveKitConversation(this.selectedClient.sourceId).subscribe({
      next: (result) => {
        this.liveKitResult = result;
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Erreur lors de la création de la conversation LiveKit: ' + err.message;
        this.loading = false;
      }
    });
  }

  resetDirectForm(): void {
    this.selectedClient = null;
    this.selectedAgent = null;
    this.directSubject = '';
    this.directResult = null;
    this.error = null;
  }

  resetTeamForm(): void {
    this.selectedClient = null;
    this.selectedTeam = null;
    this.selectedNamedArea = null;
    this.teamSubject = '';
    this.teamResult = null;
    this.error = null;
  }

  onRecipientTypeChange(): void {
    this.selectedTeam = null;
    this.selectedNamedArea = null;
  }

  openSwagger(): void {
    window.open('http://localhost:8081/swagger', '_blank');
  }

  // Webhook methods
  loadWebhookStatus(): void {
    this.apiService.getWebhookStatus().subscribe({
      next: (status) => {
        this.webhookStatus = status;
      },
      error: (err) => {
        console.error('Error loading webhook status:', err);
        this.webhookStatus = null;
      }
    });
  }

  setupWebhook(): void {
    this.webhookLoading = true;
    this.error = null;

    this.apiService.setupWebhook().subscribe({
      next: (result) => {
        this.webhookLoading = false;
        if (result.success) {
          this.loadWebhookStatus();
          console.log('Webhook setup successful:', result);
        } else {
          this.error = 'Erreur setup webhook: ' + result.message;
        }
      },
      error: (err) => {
        this.webhookLoading = false;
        this.error = 'Erreur lors du setup webhook: ' + err.message;
      }
    });
  }

  teardownWebhook(): void {
    if (!confirm('Êtes-vous sûr de vouloir arrêter le webhook ?')) {
      return;
    }

    this.webhookLoading = true;
    this.error = null;

    this.apiService.teardownWebhook(false).subscribe({
      next: () => {
        this.webhookLoading = false;
        this.loadWebhookStatus();
        console.log('Webhook teardown successful');
      },
      error: (err) => {
        this.webhookLoading = false;
        this.error = 'Erreur lors du teardown webhook: ' + err.message;
      }
    });
  }

  get webhookStatusIcon(): string {
    if (!this.webhookStatus) return '⚪';
    if (this.webhookStatus.ngrokRunning && this.webhookStatus.webhookRegistered) return '🟢';
    if (this.webhookStatus.ngrokRunning || this.webhookStatus.webhookRegistered) return '🟡';
    return '🔴';
  }

  get webhookStatusText(): string {
    if (!this.webhookStatus) return 'Non configuré';
    if (this.webhookStatus.ngrokRunning && this.webhookStatus.webhookRegistered) return 'Actif';
    if (this.webhookStatus.ngrokRunning) return 'Ngrok seul';
    if (this.webhookStatus.webhookRegistered) return 'Webhook seul';
    return 'Inactif';
  }
}
