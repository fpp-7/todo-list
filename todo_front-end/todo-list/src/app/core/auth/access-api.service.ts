import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { apiRoutes } from '../api/api-routes';
import {
  AuthenticationDTO,
  LoginResponseDTO,
  OperationStatusResponseDTO,
  PasswordResetConfirmDTO,
  RegisterRequestDTO,
  TokenRefreshResponseDTO,
} from './auth.dtos';

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

  refreshToken(): Observable<TokenRefreshResponseDTO> {
    return this.http.post<TokenRefreshResponseDTO>(apiRoutes.auth.refresh, {});
  }

  logout(): Observable<OperationStatusResponseDTO> {
    return this.http.post<OperationStatusResponseDTO>(apiRoutes.auth.logout, {});
  }

  register(payload: RegisterRequestDTO): Observable<OperationStatusResponseDTO> {
    return this.http.post<OperationStatusResponseDTO>(apiRoutes.auth.register, payload);
  }

  requestPasswordReset(
    payload: PasswordResetRequestPayload,
  ): Observable<AccessRequestResponse> {
    return this.http.post<AccessRequestResponse>(apiRoutes.auth.forgotPassword, payload);
  }

  resetPassword(payload: PasswordResetConfirmDTO): Observable<OperationStatusResponseDTO> {
    return this.http.post<OperationStatusResponseDTO>(apiRoutes.auth.resetPassword, payload);
  }

  requestInvite(payload: InviteRequestPayload): Observable<AccessRequestResponse> {
    return this.http.post<AccessRequestResponse>(apiRoutes.auth.requestInvite, payload);
  }
}
