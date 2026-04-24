/**
 * Modèle pour les statistiques de conversations.
 */
export interface ConversationStatistics {
  generatedAt: string;
  totalConversations: number;
  conversationsPerDay: { [date: string]: number };
  weeklyAverage: number;
  monthlyAverage: number;
  overallAverage: number;
  firstConversationDate: string;
  lastConversationDate: string;
}

/**
 * Modèle pour un fichier de statistiques.
 */
export interface StatisticsFile {
  filename: string;
  displayName: string;
  date: Date;
}

// Made with Bob
