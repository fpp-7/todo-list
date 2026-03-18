import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { AccessApiService } from '../../core/auth/access-api.service';
import { LoginPage } from './login-page';

describe('LoginPage', () => {
  let component: LoginPage;
  let fixture: ComponentFixture<LoginPage>;
  let accessApiService: jasmine.SpyObj<AccessApiService>;

  beforeEach(async () => {
    accessApiService = jasmine.createSpyObj<AccessApiService>('AccessApiService', [
      'requestPasswordReset',
      'requestInvite',
    ]);

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

    fixture = TestBed.createComponent(LoginPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
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
