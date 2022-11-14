export class WebConfig {
  oauth: WebOAuthConfig = new WebOAuthConfig();
  logoPath?: string
}

export class WebOAuthConfig {
  issuer: string;
  clientId: string;
  scope: string;
}
