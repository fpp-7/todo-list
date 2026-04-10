import { Injectable } from '@angular/core';
import { LoginResponseDTO, ProfileDTO, TokenRefreshResponseDTO } from './auth.dtos';

export type SessionProfile = ProfileDTO;

type JwtPayload = {
  readonly exp?: number;
};

@Injectable({
  providedIn: 'root',
})
export class SessionService {
  private readonly TOKEN_KEY = 'auth-token';
  private readonly REFRESH_TOKEN_KEY = 'auth-refresh-token';
  private readonly PROFILE_KEY = 'auth-profile';

  saveSession(session: LoginResponseDTO): void {
    this.saveToken(session.token);
    this.saveRefreshToken(session.refreshToken);
    this.saveProfile(session.profile);
  }

  saveTokens(tokens: TokenRefreshResponseDTO): void {
    this.saveToken(tokens.token);
    this.saveRefreshToken(tokens.refreshToken);
  }

  saveToken(token: string): void {
    localStorage.setItem(this.TOKEN_KEY, token);
  }

  saveRefreshToken(refreshToken: string): void {
    localStorage.setItem(this.REFRESH_TOKEN_KEY, refreshToken);
  }

  saveProfile(profile: SessionProfile): void {
    localStorage.setItem(this.PROFILE_KEY, JSON.stringify(profile));
  }

  getToken(): string | null {
    const token = localStorage.getItem(this.TOKEN_KEY);

    if (!token) {
      return null;
    }

    if (this.isTokenExpired(token)) {
      this.clearAccessToken();
      return null;
    }

    return token;
  }

  getRefreshToken(): string | null {
    return localStorage.getItem(this.REFRESH_TOKEN_KEY);
  }

  getProfile(): SessionProfile | null {
    const storedProfile = localStorage.getItem(this.PROFILE_KEY);

    if (!storedProfile) {
      return null;
    }

    try {
      return JSON.parse(storedProfile) as SessionProfile;
    } catch {
      localStorage.removeItem(this.PROFILE_KEY);
      return null;
    }
  }

  clearToken(): void {
    this.clearSession();
  }

  clearAccessToken(): void {
    localStorage.removeItem(this.TOKEN_KEY);
  }

  clearSession(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.REFRESH_TOKEN_KEY);
    localStorage.removeItem(this.PROFILE_KEY);
  }

  hasToken(): boolean {
    return this.getToken() !== null;
  }

  hasActiveSession(): boolean {
    return this.hasToken() || this.getRefreshToken() !== null;
  }

  private isTokenExpired(token: string): boolean {
    const payload = this.decodeJwtPayload(token);

    if (!payload?.exp) {
      return true;
    }

    return payload.exp * 1000 <= Date.now();
  }

  private decodeJwtPayload(token: string): JwtPayload | null {
    const [, rawPayload] = token.split('.');

    if (!rawPayload) {
      return null;
    }

    try {
      const normalizedPayload = rawPayload.replace(/-/g, '+').replace(/_/g, '/');
      const paddingLength = (4 - (normalizedPayload.length % 4)) % 4;
      const paddedPayload = normalizedPayload.padEnd(
        normalizedPayload.length + paddingLength,
        '=',
      );

      return JSON.parse(atob(paddedPayload)) as JwtPayload;
    } catch {
      return null;
    }
  }
}
