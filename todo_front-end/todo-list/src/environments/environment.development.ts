const localApiHost = globalThis.location?.hostname || 'localhost';

export const environment = {
  production: false,
  apiBaseUrl: `http://${localApiHost}:8080`,
} as const;
