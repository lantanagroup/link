import {IOAuthConfig} from './oauth-config';

export interface IConfig {
  apiUrl: string;
  report: 'pihc'|'pillbox';
  oauth?: IOAuthConfig;
  smart?: IOAuthConfig[];
}