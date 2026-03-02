import { ApplicationConfig } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { Configuration, BASE_PATH } from './generated-sources/openapi';
import { routes } from './app.routes';
import { environment } from '../environments/environment';
import { authInterceptor } from './interceptors/auth.interceptor';

const openApiConfig = new Configuration({
  basePath: environment.apiBaseUrl,
});

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideHttpClient(withInterceptors([authInterceptor])),
    { provide: Configuration, useValue: openApiConfig },
    { provide: BASE_PATH, useValue: environment.apiBaseUrl }
  ]
};
