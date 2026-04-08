export interface BotInfo {
  id: string;
  name: string;
  onboardingFilter: string | null;
  onboardingOrder: number | null;
  webhookStatus: string | null;
  webhookEndpoint: string | null;
}
