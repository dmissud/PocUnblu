import {Component} from '@angular/core';
import {CommonModule, DatePipe} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {ApiService} from '../../services/api.service';
import {ConversationSearchItem, ConversationState} from '../../models/conversation-search.model';

@Component({
  selector: 'app-inactive-conversations',
  imports: [CommonModule, DatePipe, FormsModule],
  templateUrl: './inactive-conversations.component.html',
  styleUrl: './inactive-conversations.component.css'
})
export class InactiveConversationsComponent {

  readonly ConversationState = ConversationState;
  readonly availableStates = Object.values(ConversationState);

  selectedState: ConversationState = ConversationState.INACTIVE;
  conversations: ConversationSearchItem[] = [];
  totalCount = 0;
  loading = false;
  error: string | null = null;
  searched = false;

  constructor(private apiService: ApiService) {
  }

  search(): void {
    this.loading = true;
    this.error = null;
    this.searched = true;

    this.apiService.searchConversationsByState(this.selectedState).subscribe({
      next: (response) => {
        this.conversations = response.conversations;
        this.totalCount = response.totalCount;
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Erreur lors de la recherche : ' + (err.error?.message ?? err.message);
        this.loading = false;
      }
    });
  }

  topicLabel(topic: string | null): string {
    return topic?.trim() ? topic : '(sans sujet)';
  }
}
