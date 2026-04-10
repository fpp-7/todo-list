import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { AccessApiService } from '../../core/auth/access-api.service';
import { LoginPage } from './login-page';

describe('LoginPage', () => {
  let component: LoginPage;
  let fixture: ComponentFixture<LoginPage>;
  let accessApiService: jasmine.SpyObj<AccessApiService>;
  let router: Router;

  beforeEach(async () => {
    localStorage.clear();

    accessApiService = jasmine.createSpyObj<AccessApiService>('AccessApiService', [
      'login',
      'requestPasswordReset',
      'requestInvite',
    ]);

    accessApiService.login.and.returnValue(
      of({
        token: createJwtWithExpiration(Math.floor(Date.now() / 1000) + 3600),
        refreshToken: 'refresh-token',
        profile: {
          id: 1,
          email: 'usuario@empresa.com',
          displayName: 'Usuario Teste',
          photoDataUrl: null,
        },
      }),
    );
    accessApiService.requestPasswordReset.and.returnValue(
      of({
        status: 'RECEBIDO',
        message: 'Solicitação de recuperação recebida.',
      }),
    );
    accessApiService.requestInvite.and.returnValue(
      of({
        status: 'RECEBIDO',
        message: 'Solicitação de convite registrada.',
      }),
    );

    await TestBed.configureTestingModule({
      imports: [LoginPage],
      providers: [
        provideRouter([]),
        {
          provide: AccessApiService,
          useValue: accessApiService,
        },
      ],
    }).compileComponents();

    router = TestBed.inject(Router);
    spyOn(router, 'navigateByUrl').and.returnValue(Promise.resolve(true));

    fixture = TestBed.createComponent(LoginPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should authenticate and persist the JWT session', () => {
    (component as any).loginEmail = 'usuario@empresa.com';
    (component as any).loginPassword = 'senha123';

    (component as any).handleLoginSubmit();

    expect(accessApiService.login).toHaveBeenCalledOnceWith({
      login: 'usuario@empresa.com',
      password: 'senha123',
    });
    expect(localStorage.getItem('auth-token')).toBeTruthy();
    expect(router.navigateByUrl).toHaveBeenCalledOnceWith('/tasks');
  });

  it('should call the forgot password endpoint from the modal', () => {
    (component as any).openForgotPasswordModal();
    (component as any).forgotPasswordEmail = 'usuario@empresa.com';

    (component as any).submitForgotPassword();

    expect(accessApiService.requestPasswordReset).toHaveBeenCalledOnceWith({
      email: 'usuario@empresa.com',
    });
  });

  it('should call the invite endpoint from the modal', () => {
    (component as any).openInviteModal();
    (component as any).inviteName = 'Usuário Teste';
    (component as any).inviteEmail = 'usuario@empresa.com';
    (component as any).inviteCompany = 'Produto';

    (component as any).submitInviteRequest();

    expect(accessApiService.requestInvite).toHaveBeenCalledOnceWith({
      name: 'Usuário Teste',
      email: 'usuario@empresa.com',
      company: 'Produto',
    });
  });
});

function createJwtWithExpiration(exp: number): string {
  const payload = btoa(JSON.stringify({ exp }))
    .replace(/=/g, '')
    .replace(/\+/g, '-')
    .replace(/\//g, '_');

  return `header.${payload}.signature`;
}
