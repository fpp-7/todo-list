import { TestBed } from '@angular/core/testing';
import { SessionService } from './session.service';

describe('SessionService', () => {
  let service: SessionService;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({});
    service = TestBed.inject(SessionService);
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('should keep only non-sensitive session data in storage', () => {
    service.saveSession({
      token: 'access-token-from-api-body',
      refreshToken: 'refresh-token-from-api-body',
      profile: {
        id: 1,
        email: 'usuario@empresa.com',
        displayName: 'Usuario Teste',
        photoDataUrl: null,
      },
    });

    expect(service.getToken()).toBeNull();
    expect(service.getRefreshToken()).toBeNull();
    expect(localStorage.getItem('auth-session')).toBe('true');
    expect(localStorage.getItem('auth-token')).toBeNull();
    expect(localStorage.getItem('auth-refresh-token')).toBeNull();
    expect(service.hasActiveSession()).toBeTrue();
    expect(service.getProfile()?.email).toBe('usuario@empresa.com');
  });

  it('should clear session metadata', () => {
    service.saveTokens({
      token: 'access-token',
      refreshToken: 'refresh-token',
    });

    service.clearSession();

    expect(service.hasActiveSession()).toBeFalse();
    expect(localStorage.getItem('auth-session')).toBeNull();
  });
});
