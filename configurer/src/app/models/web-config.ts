export class WebConfig {
  oauth: WebOAuthConfig = new WebOAuthConfig();
  logoPath?: string
  faviconName?: string
}

export class WebOAuthConfig {
  issuer: string;
  clientId: string;
  scope: string;
}
