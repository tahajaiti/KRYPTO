import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { adminGuard } from './core/guards/admin.guard';
import { guestGuard } from './core/guards/guest.guard';

export const routes: Routes = [
	{
		path: '',
		loadComponent: () => import('./layout/shell-layout/shell-layout').then((m) => m.ShellLayoutComponent),
		children: [
			{
				path: '',
				pathMatch: 'full',
				loadComponent: () => import('./features/home/pages/home-page/home-page').then((m) => m.HomePageComponent)
			},
			{
				path: 'login',
				canActivate: [guestGuard],
				loadComponent: () =>
					import('./features/auth/pages/login-page/login-page').then((m) => m.LoginPageComponent)
			},
			{
				path: 'register',
				canActivate: [guestGuard],
				loadComponent: () =>
					import('./features/auth/pages/register-page/register-page').then((m) => m.RegisterPageComponent)
			},
			{
				path: 'profile',
				canActivate: [authGuard],
				loadComponent: () =>
					import('./features/auth/pages/profile-page/profile-page').then((m) => m.ProfilePageComponent)
			},
			{
				path: 'u/:username',
				canActivate: [authGuard],
				loadComponent: () =>
					import('./features/auth/pages/public-profile-page/public-profile-page').then((m) => m.PublicProfilePageComponent)
			},
			{
				path: 'users/:id',
				canActivate: [authGuard],
				loadComponent: () =>
					import('./features/auth/pages/public-profile-page/public-profile-page').then((m) => m.PublicProfilePageComponent)
			},
			{
				path: 'portfolio',
				canActivate: [authGuard],
				loadComponent: () =>
					import('./features/wallet/pages/wallet-page/wallet-page').then((m) => m.WalletPageComponent)
			},
			{
				path: 'markets',
				canActivate: [authGuard],
				loadComponent: () =>
					import('./features/coin/pages/coin-page/coin-page').then((m) => m.CoinPageComponent)
			},
			{
				path: 'markets/:id',
				canActivate: [authGuard],
				loadComponent: () =>
					import('./features/coin/pages/coin-detail-page/coin-detail-page').then((m) => m.CoinDetailPageComponent)
			},
			{
				path: 'trade',
				canActivate: [authGuard],
				loadComponent: () =>
					import('./features/trading/pages/trading-page/trading-page').then((m) => m.TradingPageComponent)
			},
			{
				path: 'leaderboard',
				canActivate: [authGuard],
				loadComponent: () =>
					import('./features/trading/pages/leaderboard-page/leaderboard-page').then((m) => m.LeaderboardPageComponent)
			},
			{
				path: 'wallet',
				redirectTo: 'portfolio',
				pathMatch: 'full'
			},
			{
				path: 'coins',
				redirectTo: 'markets',
				pathMatch: 'full'
			},
			{
				path: 'trading',
				redirectTo: 'trade',
				pathMatch: 'full'
			},
			{
				path: 'blockchain',
				redirectTo: 'trade',
				pathMatch: 'full'
			},
			{
				path: 'admin',
				canActivate: [authGuard, adminGuard],
				loadComponent: () =>
					import('./features/admin/pages/admin-dashboard-page/admin-dashboard-page').then((m) => m.AdminDashboardPageComponent)
			}
		]
	},
	{
		path: '**',
		redirectTo: ''
	}
];
