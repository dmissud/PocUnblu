import {Component, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {ApiService} from '../../services/api.service';
import {BotInfo} from '../../models/bot.model';

@Component({
  selector: 'app-supervision',
  imports: [CommonModule],
  templateUrl: './supervision.component.html',
  styleUrl: './supervision.component.css'
})
export class SupervisionComponent implements OnInit {

  bots: BotInfo[] = [];
  loading = false;
  error: string | null = null;

  constructor(private apiService: ApiService) {
  }

  ngOnInit(): void {
    this.loadBots();
  }

  loadBots(): void {
    this.loading = true;
    this.error = null;

    this.apiService.getBots().subscribe({
      next: (bots) => {
        this.bots = bots;
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Erreur lors du chargement des bots : ' + err.message;
        this.loading = false;
      }
    });
  }

  webhookStatusIcon(status: string | null): string {
    switch (status) {
      case 'ACTIVE':
        return '🟢';
      case 'INACTIVE':
        return '🔴';
      default:
        return '⚪';
    }
  }

  onboardingFilterLabel(filter: string | null): string {
    switch (filter) {
      case 'VISITORS':
        return '👤 Visitors';
      case 'ALL':
        return '👥 Tous';
      case 'NONE':
        return '— Aucun';
      default:
        return filter ?? '—';
    }
  }
}
