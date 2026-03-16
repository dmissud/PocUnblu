export interface WebhookSetupResult {
  success: boolean;
  ngrokUrl: string | null;
  webhookId: string | null;
  webhookName: string | null;
  status: string;
  message: string;
}

export interface WebhookStatus {
  ngrokRunning: boolean;
  ngrokUrl: string | null;
  webhookRegistered: boolean;
  webhookId: string | null;
  webhookName: string | null;
  webhookEndpoint: string | null;
}
