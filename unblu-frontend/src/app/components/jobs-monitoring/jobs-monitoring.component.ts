import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { interval, Subscription } from 'rxjs';
import { switchMap } from 'rxjs/operators';
import { JobsStatusResponse, RunningJobsResponse } from '../../models/job.model';
import { ApiService } from '../../services/api.service';

@Component({
  selector: 'app-jobs-monitoring',
  imports: [CommonModule],
  templateUrl: './jobs-monitoring.component.html',
  styleUrl: './jobs-monitoring.component.css'
})
export class JobsMonitoringComponent implements OnInit, OnDestroy {
  jobsStatus: JobsStatusResponse | null = null;
  runningJobs: RunningJobsResponse | null = null;
  loading = false;
  error: string | null = null;
  autoRefresh = true;
  private refreshSubscription?: Subscription;

  constructor(private apiService: ApiService) {}

  ngOnInit(): void {
    this.loadJobsData();
    this.startAutoRefresh();
  }

  ngOnDestroy(): void {
    this.stopAutoRefresh();
  }

  loadJobsData(): void {
    this.loading = true;
    this.error = null;

    Promise.all([
      this.apiService.getJobsStatus().toPromise(),
      this.apiService.getRunningJobs().toPromise()
    ]).then(([status, running]) => {
      this.jobsStatus = status || null;
      this.runningJobs = running || null;
      this.loading = false;
    }).catch(err => {
      this.error = 'Erreur lors du chargement des données: ' + err.message;
      this.loading = false;
    });
  }

  startAutoRefresh(): void {
    if (this.autoRefresh) {
      this.refreshSubscription = interval(5000)
        .pipe(switchMap(() => this.apiService.getJobsStatus()))
        .subscribe({
          next: (status) => {
            this.jobsStatus = status;
          },
          error: (err) => {
            console.error('Erreur lors du rafraîchissement automatique:', err);
          }
        });

      // Refresh running jobs separately
      interval(5000)
        .pipe(switchMap(() => this.apiService.getRunningJobs()))
        .subscribe({
          next: (running) => {
            this.runningJobs = running;
          },
          error: (err) => {
            console.error('Erreur lors du rafraîchissement des jobs en cours:', err);
          }
        });
    }
  }

  stopAutoRefresh(): void {
    if (this.refreshSubscription) {
      this.refreshSubscription.unsubscribe();
    }
  }

  toggleAutoRefresh(): void {
    this.autoRefresh = !this.autoRefresh;
    if (this.autoRefresh) {
      this.startAutoRefresh();
    } else {
      this.stopAutoRefresh();
    }
  }

  refresh(): void {
    this.loadJobsData();
  }

  getStateClass(state: string): string {
    switch (state) {
      case 'NORMAL':
        return 'state-normal';
      case 'PAUSED':
        return 'state-paused';
      case 'BLOCKED':
        return 'state-blocked';
      case 'ERROR':
        return 'state-error';
      default:
        return 'state-unknown';
    }
  }

  getStateIcon(state: string): string {
    switch (state) {
      case 'NORMAL':
        return '✅';
      case 'PAUSED':
        return '⏸️';
      case 'BLOCKED':
        return '🚫';
      case 'ERROR':
        return '❌';
      default:
        return '❓';
    }
  }
}

// Made with Bob
