import { Routes } from '@angular/router';
import { authGuard, publicOnlyGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'login',
  },
  {
    path: 'login',
    loadComponent: () => import('./pages/login-page/login-page').then((page) => page.LoginPage),
    canActivate: [publicOnlyGuard],
  },
  {
    path: 'register',
    loadComponent: () =>
      import('./pages/register-page/register-page').then((page) => page.RegisterPage),
    canActivate: [publicOnlyGuard],
  },
  {
    path: 'reset-password',
    loadComponent: () =>
      import('./pages/reset-password-page/reset-password-page').then(
        (page) => page.ResetPasswordPage,
      ),
    canActivate: [publicOnlyGuard],
  },
  {
    path: 'tasks',
    loadComponent: () =>
      import('./components/task-list/task-list').then((component) => component.TaskList),
    canActivate: [authGuard],
  },
  {
    path: 'dashboard',
    redirectTo: 'tasks',
  },
  {
    path: '**',
    redirectTo: 'login',
  },
];
