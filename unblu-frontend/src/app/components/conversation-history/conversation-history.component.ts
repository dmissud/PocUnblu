import {Component, OnInit} from '@angular/core';
import {CommonModule, DatePipe} from '@angular/common';
import {ApiService} from '../../services/api.service';
import {
  ConversationEvent,
  ConversationHistoryDetail,
  ConversationHistoryItem,
  ConversationHistoryPage
} from '../../models/conversation-history.model';

@Component({
  selector: 'app-conversation-history',
  imports: [CommonModule, DatePipe],
  templateUrl: './conversation-history.component.html',
  styleUrl: './conversation-history.component.css'
})
export class ConversationHistoryComponent implements OnInit {

  readonly PAGE_SIZE = 10;

  // List state
  currentPage: ConversationHistoryPage | null = null;
  loadingList = false;
  listError: string | null = null;

  // Detail state
  selectedConversation: ConversationHistoryDetail | null = null;
  loadingDetail = false;
  detailError: string | null = null;

  constructor(private apiService: ApiService) {}

  ngOnInit(): void {
    this.loadPage(0);
  }

  loadPage(page: number): void {
    this.loadingList = true;
    this.listError = null;
    this.selectedConversation = null;

    this.apiService.getConversationHistory(page, this.PAGE_SIZE).subscribe({
      next: (result) => {
        this.currentPage = result;
        this.loadingList = false;
      },
      error: (err) => {
        this.listError = 'Erreur lors du chargement : ' + err.message;
        this.loadingList = false;
      }
    });
  }

  selectConversation(item: ConversationHistoryItem): void {
    if (this.selectedConversation?.conversationId === item.conversationId) {
      this.selectedConversation = null;
      return;
    }
    this.loadingDetail = true;
    this.detailError = null;

    this.apiService.getConversationDetail(item.conversationId).subscribe({
      next: (detail) => {
        this.selectedConversation = detail;
        this.loadingDetail = false;
      },
      error: (err) => {
        this.detailError = 'Erreur lors du chargement du détail : ' + err.message;
        this.loadingDetail = false;
      }
    });
  }

  isSelected(item: ConversationHistoryItem): boolean {
    return this.selectedConversation?.conversationId === item.conversationId;
  }

  closeDetail(): void {
    this.selectedConversation = null;
    this.detailError = null;
  }

  get pages(): number[] {
    if (!this.currentPage) return [];
    return Array.from({length: this.currentPage.totalPages}, (_, i) => i);
  }

  get visiblePages(): number[] {
    if (!this.currentPage) return [];
    const current = this.currentPage.page;
    const total = this.currentPage.totalPages;
    const range: number[] = [];
    for (let i = Math.max(0, current - 2); i <= Math.min(total - 1, current + 2); i++) {
      range.push(i);
    }
    return range;
  }

  eventIcon(event: ConversationEvent): string {
    switch (event.eventType) {
      case 'CREATED': return '🟢';
      case 'MESSAGE': return '💬';
      case 'ENDED':   return '🔴';
      default:        return '•';
    }
  }

  participantIcon(type: string): string {
    switch (type) {
      case 'VISITOR': return '👤';
      case 'AGENT':   return '🎧';
      case 'BOT':     return '🤖';
      default:        return '•';
    }
  }

  topicLabel(topic: string | null): string {
    return topic?.trim() ? topic : '(sans sujet)';
  }
}
