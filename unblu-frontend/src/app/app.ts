import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { ApiService } from './services/api.service';
import { PersonInfo } from './models/person.model';
import { TeamInfo } from './models/team.model';

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

  constructor(private apiService: ApiService) {}

  ngOnInit(): void {
    this.loadData();
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
      clientSourceId: this.selectedClient.sourceId,
      agentSourceId: this.selectedAgent.sourceId,
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
      subject: this.teamSubject || 'Conversation équipe'
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
}
