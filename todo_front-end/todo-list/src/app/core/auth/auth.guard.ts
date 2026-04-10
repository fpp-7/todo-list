import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { SessionService } from './session.service';

export const authGuard: CanActivateFn = (_route, state) => {
  const sessionService = inject(SessionService);
  const router = inject(Router);

  if (sessionService.hasActiveSession()) {
    return true;
  }

  return router.createUrlTree(['/login'], {
    queryParams: { redirectTo: state.url },
  });
};

export const publicOnlyGuard: CanActivateFn = () => {
  const sessionService = inject(SessionService);
  const router = inject(Router);

  return sessionService.hasActiveSession() ? router.createUrlTree(['/tasks']) : true;
};
