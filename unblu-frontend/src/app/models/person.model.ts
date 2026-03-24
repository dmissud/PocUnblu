export interface PersonInfo {
  id: string;
  sourceId: string;
  displayName: string;
  email: string | null;
  firstName?: string;
  lastName?: string;
  personType: 'VISITOR' | 'AGENT' | string;
}
