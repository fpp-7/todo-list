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

  it('should keep valid JWT sessions available', () => {
    const token = createJwtWithExpiration(Math.floor(Date.now() / 1000) + 3600);

    service.saveSession({
      token,
      refreshToken: 'refresh-token',
      profile: {
        id: 1,
        email: 'usuario@empresa.com',
        displayName: 'Usuario Teste',
        photoDataUrl: null,
      },
    });

    expect(service.getToken()).toBe(token);
    expect(service.getRefreshToken()).toBe('refresh-token');
    expect(service.hasToken()).toBeTrue();
    expect(service.hasActiveSession()).toBeTrue();
    expect(service.getProfile()?.email).toBe('usuario@empresa.com');
  });

  it('should clear only access token when JWT is expired', () => {
    service.saveRefreshToken('refresh-token');
    service.saveToken(createJwtWithExpiration(Math.floor(Date.now() / 1000) - 60));

    expect(service.getToken()).toBeNull();
    expect(service.getRefreshToken()).toBe('refresh-token');
    expect(service.hasToken()).toBeFalse();
    expect(service.hasActiveSession()).toBeTrue();
  });
});

function createJwtWithExpiration(exp: number): string {
  const payload = btoa(JSON.stringify({ exp }))
    .replace(/=/g, '')
    .replace(/\+/g, '-')
    .replace(/\//g, '_');

  return `header.${payload}.signature`;
}
