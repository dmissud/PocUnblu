import {Component, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {ApiService} from './services/api.service';
import {PersonInfo} from './models/person.model';
import {TeamInfo} from './models/team.model';
import {WebhookStatus} from './models/webhook.model';

@Component({
  selector: 'app-root',
  imports: [CommonModule, FormsModule],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App implements OnInit {
  title = 'Unblu Conversation Manager';

  // Lists
  clients: PersonInfo[] = [];
  agents: PersonInfo[] = [];
  teams: TeamInfo[] = [];

  // Selected values
  selectedClient: PersonInfo | null = null;
  selectedAgent: PersonInfo | null = null;
  selectedTeam: TeamInfo | null = null;

  // Form values
  directSubject: string = '';
  teamSubject: string = '';

  // Results
  directResult: any = null;
  teamResult: any = null;
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
      this.apiService.getClients().toPromise(),
      this.apiService.getAgents().toPromise(),
      this.apiService.getTeams().toPromise()
    ]).then(([clients, agents, teams]) => {
      this.clients = clients || [];
      this.agents = agents || [];
      this.teams = teams || [];
      this.loadingData = false;
    }).catch(err => {
      this.error = 'Erreur lors du chargement des données: ' + err.message;
      this.loadingData = false;
    });
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
      virtualParticipantSourceId: this.selectedClient.sourceId,
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
    if (!this.selectedClient || !this.selectedTeam) {
      this.error = 'Veuillez sélectionner un client et une équipe';
      return;
    }

    this.loading = true;
    this.error = null;
    this.teamResult = null;

    this.apiService.startTeamConversation({
      clientId: this.selectedClient.sourceId,
      subject: this.teamSubject || 'Conversation équipe',
      teamId: this.selectedTeam.id
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
    this.teamSubject = '';
    this.teamResult = null;
    this.error = null;
  }

  openSwagger(): void {
    window.open('http://localhost:8081/swagger-ui', '_blank');
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
