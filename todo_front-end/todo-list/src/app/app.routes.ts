import { Routes } from '@angular/router';
import { TaskList } from './components/task-list/task-list';
import { LoginPage } from './pages/login-page/login-page';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'login',
  },
  {
    path: 'login',
    component: LoginPage,
  },
  {
    path: 'tasks',
    component: TaskList,
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
