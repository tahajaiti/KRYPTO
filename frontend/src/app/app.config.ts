import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { ApplicationConfig, inject, provideAppInitializer, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { provideIcons } from '@ng-icons/core';
import {
  heroBolt, heroChartBar, heroBriefcase, heroTrophy, heroRocketLaunch,
  heroWrench, heroUsers, heroCube, heroArrowTrendingUp, heroArrowTrendingDown, heroStar,
  heroLink, heroCog6Tooth, heroPencilSquare, heroUser, heroFire,
  heroWallet, heroBookmarkSquare, heroClipboardDocumentList, heroEye,
  heroArrowPath, heroHome, heroBell, heroShieldCheck, heroDocumentText,
  heroQueueList, heroCurrencyDollar, heroTag, heroUserCircle,
  heroGift, heroClock, heroChartBarSquare, heroBookmark,
  heroArrowRightStartOnRectangle, heroXMark, heroBars3,
  heroCheckBadge, heroExclamationCircle,
  heroMagnifyingGlass, heroCursorArrowRays, heroChevronDown
} from '@ng-icons/heroicons/outline';

import { DEFAULT_API_BASE_URL, API_BASE_URL } from './core/config/api.config';
import { apiBaseUrlInterceptor } from './core/http/interceptors/api-base-url.interceptor';
import { authRefreshInterceptor } from './core/http/interceptors/auth-refresh.interceptor';
import { credentialsInterceptor } from './core/http/interceptors/credentials.interceptor';
import { AuthService } from './features/auth/services/auth.service';
import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(withInterceptors([apiBaseUrlInterceptor, credentialsInterceptor, authRefreshInterceptor])),
    provideIcons({
      heroBolt, heroChartBar, heroBriefcase, heroTrophy, heroRocketLaunch,
      heroWrench, heroUsers, heroCube, heroArrowTrendingUp, heroArrowTrendingDown, heroStar,
      heroLink, heroCog6Tooth, heroPencilSquare, heroUser, heroFire,
      heroWallet, heroBookmarkSquare, heroClipboardDocumentList, heroEye,
      heroArrowPath, heroHome, heroBell, heroShieldCheck, heroDocumentText,
      heroQueueList, heroCurrencyDollar, heroTag, heroUserCircle,
      heroGift, heroClock, heroChartBarSquare, heroBookmark,
      heroArrowRightStartOnRectangle, heroXMark, heroBars3,
      heroCheckBadge, heroExclamationCircle,
      heroMagnifyingGlass, heroCursorArrowRays, heroChevronDown
    }),
    {
      provide: API_BASE_URL,
      useValue: DEFAULT_API_BASE_URL
    },
    provideAppInitializer(() => {
      const authService = inject(AuthService);
      return firstValueFrom(authService.initializeSession());
    })
  ]
};

