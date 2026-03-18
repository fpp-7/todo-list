import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { apiRoutes } from '../api/api-routes';

export type PasswordResetRequestPayload = {
  readonly email: string;
};

export type InviteRequestPayload = {
  readonly name: string;
  readonly email: string;
  readonly company: string | null;
};

export type AccessRequestResponse = {
  readonly status: string;
  readonly message: string;
};

@Injectable({
  providedIn: 'root',
})
export class AccessApiService {
  private readonly http = inject(HttpClient);

  requestPasswordReset(
    payload: PasswordResetRequestPayload,
  ): Observable<AccessRequestResponse> {
    return this.http.post<AccessRequestResponse>(apiRoutes.auth.forgotPassword, payload);
  }

  requestInvite(payload: InviteRequestPayload): Observable<AccessRequestResponse> {
    return this.http.post<AccessRequestResponse>(apiRoutes.auth.requestInvite, payload);
  }
}
