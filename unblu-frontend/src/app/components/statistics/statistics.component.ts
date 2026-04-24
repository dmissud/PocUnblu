import { CommonModule } from '@angular/common';
import { AfterViewInit, Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { Chart, ChartConfiguration, registerables } from 'chart.js';
import { ConversationStatistics } from '../../models/statistics.model';
import { ApiService } from '../../services/api.service';

// Enregistrer tous les composants Chart.js
Chart.register(...registerables);

@Component({
  selector: 'app-statistics',
  imports: [CommonModule],
  templateUrl: './statistics.component.html',
  styleUrl: './statistics.component.css'
})
export class StatisticsComponent implements OnInit, AfterViewInit {
  @ViewChild('chartCanvas') chartCanvas!: ElementRef<HTMLCanvasElement>;

  files: string[] = [];
  selectedFile: string | null = null;
  statistics: ConversationStatistics | null = null;
  loading = false;
  error: string | null = null;
  chart: Chart | null = null;

  constructor(private apiService: ApiService) {}

  ngOnInit(): void {
    this.loadFiles();
  }

  ngAfterViewInit(): void {
    // Le canvas sera disponible après l'initialisation de la vue
  }

  loadFiles(): void {
    this.loading = true;
    this.error = null;

    this.apiService.getStatisticsFiles().subscribe({
      next: (files) => {
        this.files = files;
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Erreur lors du chargement des fichiers: ' + err.message;
        this.loading = false;
      }
    });
  }

  selectFile(filename: string): void {
    this.selectedFile = filename;
    this.loading = true;
    this.error = null;

    this.apiService.getStatisticsFile(filename).subscribe({
      next: (stats) => {
        this.statistics = stats;
        this.loading = false;
        // Attendre que la vue soit mise à jour avant de créer le graphique
        setTimeout(() => this.renderChart(), 100);
      },
      error: (err) => {
        this.error = 'Erreur lors du chargement des statistiques: ' + err.message;
        this.loading = false;
      }
    });
  }

  renderChart(): void {
    if (!this.statistics) {
      console.error('Pas de statistiques disponibles');
      return;
    }

    if (!this.chartCanvas || !this.chartCanvas.nativeElement) {
      console.error('Canvas non disponible');
      return;
    }

    // Détruire le graphique existant s'il y en a un
    if (this.chart) {
      this.chart.destroy();
      this.chart = null;
    }

    // Préparer les données pour le graphique - trier les dates chronologiquement
    const conversationsPerDay = this.statistics.conversationsPerDay;
    const dates = Object.keys(conversationsPerDay).sort((a, b) => {
      return new Date(a).getTime() - new Date(b).getTime();
    });
    const counts = dates.map(date => conversationsPerDay[date]);

    console.log('Création du graphique avec', dates.length, 'points de données');
    console.log('Première date:', dates[0], 'Dernière date:', dates[dates.length - 1]);

    const config: ChartConfiguration = {
      type: 'line',
      data: {
        labels: dates,
        datasets: [{
          label: 'Conversations par jour',
          data: counts,
          borderColor: 'rgb(75, 192, 192)',
          backgroundColor: 'rgba(75, 192, 192, 0.2)',
          tension: 0.1,
          fill: true
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          title: {
            display: true,
            text: 'Évolution des conversations'
          },
          legend: {
            display: true,
            position: 'top'
          },
          tooltip: {
            mode: 'index',
            intersect: false
          }
        },
        scales: {
          x: {
            type: 'category',
            display: true,
            title: {
              display: true,
              text: 'Date'
            },
            ticks: {
              maxRotation: 45,
              minRotation: 45,
              autoSkip: true,
              maxTicksLimit: 30
            }
          },
          y: {
            display: true,
            title: {
              display: true,
              text: 'Nombre de conversations'
            },
            beginAtZero: true
          }
        }
      }
    };

    try {
      this.chart = new Chart(this.chartCanvas.nativeElement, config);
      console.log('Graphique créé avec succès');
    } catch (error) {
      console.error('Erreur lors de la création du graphique:', error);
    }
  }

  getDisplayName(filename: string): string {
    // Extraire la date du nom de fichier
    // Format: conversation-stats-2026-04-23_21-00.json
    const match = filename.match(/conversation-stats-(.+)\.json/);
    if (match) {
      return match[1].replace('_', ' à ').replace(/-/g, '/');
    }
    return filename;
  }

  formatDate(dateStr: string): string {
    const date = new Date(dateStr);
    return date.toLocaleDateString('fr-FR', {
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  }

  formatNumber(num: number): string {
    return num.toFixed(2);
  }
}

// Made with Bob
