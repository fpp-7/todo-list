const backendOrigin =
  typeof window === 'undefined'
    ? 'http://localhost:8080'
    : `${window.location.protocol}//${window.location.hostname}:8080`;

export const apiRoutes = {
  auth: {
    login: `${backendOrigin}/auth/login`,
    register: `${backendOrigin}/auth/register`,
    forgotPassword: `${backendOrigin}/auth/esqueci-senha`,
    requestInvite: `${backendOrigin}/auth/solicitar-convite`,
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
