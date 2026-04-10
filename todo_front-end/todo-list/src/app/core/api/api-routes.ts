import { environment } from '../../../environments/environment';

const backendOrigin = environment.apiBaseUrl;

export const apiRoutes = {
  auth: {
    login: `${backendOrigin}/auth/login`,
    refresh: `${backendOrigin}/auth/refresh`,
    logout: `${backendOrigin}/auth/logout`,
    register: `${backendOrigin}/auth/register`,
    forgotPassword: `${backendOrigin}/auth/forgot-password`,
    resetPassword: `${backendOrigin}/auth/reset-password`,
    requestInvite: `${backendOrigin}/auth/invite-request`,
  },
  profile: {
    base: `${backendOrigin}/profile`,
    password: `${backendOrigin}/profile/password`,
    photo: `${backendOrigin}/profile/photo`,
  },
  tasks: {
    base: `${backendOrigin}/task`,
    list: `${backendOrigin}/task`,
    create: `${backendOrigin}/task`,
    update: (id: number) => `${backendOrigin}/task/${id}`,
    delete: (id: number) => `${backendOrigin}/task/${id}`,
    complete: (id: number) => `${backendOrigin}/task/concluir/${id}`,
  },
} as const;
