import {Injectable} from '@angular/core';
import {OAuthService} from 'angular-oauth2-oidc';
import {HttpClient} from '@angular/common/http';
import {Router} from '@angular/router';
import {ConfigService, IOAuthConfig} from './config.service';

export interface AuthInitOptions {
  issuer: string;
  loginUrl: string;
  tokenEndpoint: string;
  launch: string;
}

interface IProfile {
  name: string;
  email?: string;
}

interface IPractitioner {
  id: string;
  identifier?: {
    use?: string;
    system: string;
    value: string;
  }[];
  address?: {
    line: string[];
    city: string;
    postalCode: string;
    state: string;
    country: string;
  }[];
  gender?: string;
  telecom?: {
    use?: string;
    system: string;
    value: string;
  }[];
  name?: {
    family: string;
    given: string[];
    prefix?: string[];
  }[];
}

@Injectable()
export class AuthService {
  private readonly OAUTH_URIS_EXT_URL = 'http://fhir-registry.smarthealthit.org/StructureDefinition/oauth-uris';
  private initialized = false;
  public token: string;
  public user: IProfile;
  public fhirBase: string;

  constructor(
      public oauthService: OAuthService,
      private http: HttpClient,
      private router: Router,
      private configService: ConfigService) {
  }

  async initLocal(config: IOAuthConfig) {
    if (this.initialized) return;

    this.oauthService.setStorage(localStorage);
    this.oauthService.configure({
      issuer: config.issuer,
      redirectUri: window.location.origin + '/',
      clientId: config.clientId,
      responseType: 'code',
      scope: config.scope,
      showDebugInformation: false,
      requestAccessToken: true,
      requireHttps: false
    });

    await this.oauthService.loadDiscoveryDocument();

    this.initialized = true;
  }

  async initSmart(options: AuthInitOptions) {
    if (this.initialized) return;

    const smartConfig = await this.configService.getSmartConfig(options.issuer);

    this.fhirBase = options.issuer;
    this.oauthService.configure({
      clientId: smartConfig.clientId,
      scope: smartConfig.scope,
      redirectUri: location.origin + '/smart-login',
      issuer: options.issuer,
      loginUrl: options.loginUrl,
      tokenEndpoint: options.tokenEndpoint,
      responseType: 'code',
      skipIssuerCheck: true,
      customQueryParams: {
        launch: options.launch,
        aud: options.issuer
      }
    });

    this.initialized = true;
  }

  async launchSmart(launch: string, issuer: string) {
    const metadataUrl = issuer + (issuer.endsWith('/') ? '' : '/') + 'metadata';
    const metadata: any = await this.http.get(metadataUrl).toPromise();

    if (metadata.rest && metadata.rest.length > 0 && metadata.rest[0].security) {
      const oauthUrisExt = metadata.rest[0].security.extension.find(e => e.url === this.OAUTH_URIS_EXT_URL);
      const authUrlExt = oauthUrisExt ? (oauthUrisExt.extension || []).find(e => e.url === 'authorize') : null;
      const tokenUrlExt = oauthUrisExt ? (oauthUrisExt.extension || []).find(e => e.url === 'token') : null;

      if (authUrlExt && authUrlExt.valueUri && tokenUrlExt && tokenUrlExt.valueUri) {
        await this.initSmart({
          issuer: issuer,
          loginUrl: authUrlExt.valueUri,
          tokenEndpoint: tokenUrlExt.valueUri,
          launch: launch
        });

        this.oauthService.initImplicitFlow(
            `launch=${launch}&` +
            `issuer=${encodeURIComponent(issuer)}&` +
            `loginUrl=${encodeURIComponent(authUrlExt.valueUri)}&` +
            `tokenEndpoint=${encodeURIComponent(tokenUrlExt.valueUri)}`);
      }
    }
  }

  async loginLocal() {
    await this.initLocal(this.configService.config.oauth);

    // Has the user already authenticated via Smart-on-FHIR?
    if (!this.user) {
      const loggedIn: boolean = await this.oauthService.tryLogin();

      try {
        if (loggedIn) {
          this.user = await this.oauthService.loadUserProfile() as any;
        }
      } catch (ex) { }

      // Force the user to login locally if they have not already logged-in via smart-on-fhir
      if (!this.user) {
        this.oauthService.initImplicitFlow();
      } else {
        this.token = this.oauthService.getIdToken();
      }
    }
  }

  private convertFhirUserToProfile(fhirUser: IPractitioner): IProfile {
    const profile = <IProfile> {};

    if (fhirUser.name && fhirUser.name.length > 0) {
      const name = fhirUser.name[0];

      if (name.family && name.given && name.given.length > 0) {
        profile.name = name.given.join(', ') + ' ' + name.family;
      } else if (name.family) {
        profile.name = name.family;
      } else if (name.given && name.given.length > 0) {
        profile.name = name.given.join(', ');
      } else {
        profile.name = 'Unknown';
      }
    }

    if (fhirUser.telecom) {
      const email = fhirUser.telecom.find(t => t.system === 'email');

      if (email && email.value) {
        if (email.value.startsWith('mailto:')) {
          profile.email = email.value.substring(7);
        } else {
          profile.email = email.value;
        }
      }
    }

    return profile;
  }

  async loginSmart(state: string) {
    const stateInfo = this.getStateInfo(state);

    await this.initSmart(stateInfo);

    const loggedIn = await this.oauthService.tryLogin();

    if (loggedIn) {
      // in smart-on-fhir, the id_token is included in the access token, so we can pass the access token
      // to the server for validation
      this.token = this.oauthService.getAccessToken();

      this.user = {
        name: 'Smart-on-FHIR User'
      };

      const idClaims: any = this.oauthService.getIdentityClaims();
      const fhirUserReference: string = idClaims ? idClaims.fhirUser || idClaims.profile : null;

      if (fhirUserReference) {
        const fhirUserUrl = stateInfo.issuer + (stateInfo.issuer.endsWith('/') ? '' : '/') + (fhirUserReference.startsWith('/') ? fhirUserReference.substring(1) : fhirUserReference);

        try {
          const fhirUser = await this.http.get<IPractitioner>(fhirUserUrl, { headers: { 'Authorization': `Bearer ${this.token}`} }).toPromise();
          this.user = this.convertFhirUserToProfile(fhirUser);
        } catch (ex) {
          console.error(`Failed to retrieve user info from ${fhirUserUrl}`);
        }
      }

      await this.router.navigate(['smart-home']);
    }
  }

  public logout() {
    this.oauthService.logOut();
    this.user = null;
  }

  private getStateInfo(state: string): AuthInitOptions {
    const ret = {};
    const stateSub = state.substring(state.indexOf(';') + 1);
    const stateSplit = decodeURIComponent(stateSub).split('&');

    for (const statePart of stateSplit) {
      const statePartKey = statePart.substring(0, statePart.indexOf('='));
      const statePartValue = statePart.substring(statePart.indexOf('=') + 1);

      ret[statePartKey] = decodeURIComponent(statePartValue);
    }

    return <AuthInitOptions> ret;
  }
}