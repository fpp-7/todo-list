import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AccessApiService } from '../../core/auth/access-api.service';

type FeedbackTone = 'success' | 'error' | null;

type ApiFieldError = {
  readonly message?: string;
};

@Component({
  selector: 'app-register-page',
  imports: [FormsModule, RouterLink],
  templateUrl: './register-page.html',
  styleUrl: '../login-page/login-page.css',
})
export class RegisterPage {
  private readonly accessApi = inject(AccessApiService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly router = inject(Router);

  protected readonly isSubmittingRegister = signal(false);
  protected readonly registerFeedback = signal('');
  protected readonly registerFeedbackTone = signal<FeedbackTone>(null);
  protected readonly showPassword = signal(false);

  protected registerFirstName = '';
  protected registerLastName = '';
  protected registerEmail = '';
  protected registerPassword = '';
  protected registerConfirmPassword = '';

  protected togglePasswordVisibility(): void {
    this.showPassword.update((value) => !value);
  }

  protected submitRegister(): void {
    const firstName = this.registerFirstName.trim();
    const lastName = this.registerLastName.trim();
    const login = this.registerEmail.trim().toLowerCase();
    const password = this.registerPassword.trim();
    const confirmPassword = this.registerConfirmPassword.trim();

    if (!firstName) {
      this.setRegisterFeedback('Informe seu nome para criar a conta.', 'error');
      return;
    }

    if (!lastName) {
      this.setRegisterFeedback('Informe seu sobrenome para criar a conta.', 'error');
      return;
    }

    if (!this.isValidEmail(login)) {
      this.setRegisterFeedback('Informe um e-mail valido para criar a conta.', 'error');
      return;
    }

    if (password.length < 6) {
      this.setRegisterFeedback('A senha precisa ter pelo menos 6 caracteres.', 'error');
      return;
    }

    if (password !== confirmPassword) {
      this.setRegisterFeedback('A confirmacao da senha precisa ser igual a senha.', 'error');
      return;
    }

    this.isSubmittingRegister.set(true);

    this.accessApi
      .register({ firstName, lastName, login, password })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => {
          this.isSubmittingRegister.set(false);
          this.setRegisterFeedback(response.message, 'success');
          this.router.navigate(['/login']);
        },
        error: (error) => {
          this.isSubmittingRegister.set(false);
          this.setRegisterFeedback(
            this.extractErrorMessage(error) ?? 'Nao foi possivel criar a conta agora.',
            'error',
          );
        },
      });
  }

  private setRegisterFeedback(message: string, tone: Exclude<FeedbackTone, null>): void {
    this.registerFeedback.set(message);
    this.registerFeedbackTone.set(tone);
  }

  private extractErrorMessage(error: unknown): string | null {
    if (this.isHttpStatusZero(error)) {
      return 'Backend indisponivel. Verifique se a API esta rodando em http://localhost:8080.';
    }

    const errorBody = this.getErrorBody(error);

    if (typeof errorBody?.message === 'string') {
      return errorBody.message;
    }

    const firstFieldError = errorBody?.fieldErrors?.[0];

    return typeof firstFieldError?.message === 'string' ? firstFieldError.message : null;
  }

  private isHttpStatusZero(error: unknown): boolean {
    return (
      error !== null &&
      typeof error === 'object' &&
      'status' in error &&
      error.status === 0
    );
  }

  private getErrorBody(error: unknown): { message?: string; fieldErrors?: ApiFieldError[] } | null {
    if (
      error === null ||
      typeof error !== 'object' ||
      !('error' in error) ||
      error.error === null ||
      typeof error.error !== 'object'
    ) {
      return null;
    }

    return error.error as { message?: string; fieldErrors?: ApiFieldError[] };
  }

  private isValidEmail(email: string): boolean {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
  }
}
