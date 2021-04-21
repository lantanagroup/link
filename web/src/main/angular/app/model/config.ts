import {IOAuthConfig} from './oauth-config';

export interface IConfig {
  apiUrl: string;
  oauth?: IOAuthConfig;
  smart?: IOAuthConfig[];
}