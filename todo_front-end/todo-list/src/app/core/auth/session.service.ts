import { Injectable } from '@angular/core';
import { LoginResponseDTO, ProfileDTO, TokenRefreshResponseDTO } from './auth.dtos';

export type SessionProfile = ProfileDTO;

@Injectable({
  providedIn: 'root',
})
export class SessionService {
  private readonly SESSION_KEY = 'auth-session';
  private readonly PROFILE_KEY = 'auth-profile';

  saveSession(session: LoginResponseDTO): void {
    this.markActiveSession();
    this.saveProfile(session.profile);
  }

  saveTokens(_tokens: TokenRefreshResponseDTO): void {
    this.markActiveSession();
  }

  saveProfile(profile: SessionProfile): void {
    this.markActiveSession();
    localStorage.setItem(this.PROFILE_KEY, JSON.stringify(profile));
  }

  getToken(): string | null {
    return null;
  }

  getRefreshToken(): string | null {
    return null;
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
    this.clearSession();
  }

  clearSession(): void {
    localStorage.removeItem(this.SESSION_KEY);
    localStorage.removeItem(this.PROFILE_KEY);
  }

  hasToken(): boolean {
    return this.hasActiveSession();
  }

  hasActiveSession(): boolean {
    return localStorage.getItem(this.SESSION_KEY) === 'true' || this.getProfile() !== null;
  }

  private markActiveSession(): void {
    localStorage.setItem(this.SESSION_KEY, 'true');
  }
}
