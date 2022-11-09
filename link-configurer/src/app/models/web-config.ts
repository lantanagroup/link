export class WebConfig {
  oauth: WebOAuthConfig;
  logoPath?: string
}

export class WebOAuthConfig {
  issuer: string;
  clientId: string;
  scope: string;
}
