import {IOAuthConfig} from './oauth-config';

export interface IConfig {
  apiUrl: string;
  getHelpUrl?: string;
  oauth?: IOAuthConfig;
  logoPath?: string;
  faviconName?: string;
}
