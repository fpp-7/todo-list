import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { apiRoutes } from '../api/api-routes';
import { AuthenticationDTO, LoginResponseDTO } from './auth.dtos';

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

  login(payload: AuthenticationDTO): Observable<LoginResponseDTO> {
    return this.http.post<LoginResponseDTO>(apiRoutes.auth.login, payload);
  }

  register(payload: AuthenticationDTO): Observable<void> {
    return this.http.post<void>(apiRoutes.auth.register, payload);
  }

  requestPasswordReset(
    payload: PasswordResetRequestPayload,
  ): Observable<AccessRequestResponse> {
    return this.http.post<AccessRequestResponse>(apiRoutes.auth.forgotPassword, payload);
  }

  requestInvite(payload: InviteRequestPayload): Observable<AccessRequestResponse> {
    return this.http.post<AccessRequestResponse>(apiRoutes.auth.requestInvite, payload);
  }
}
