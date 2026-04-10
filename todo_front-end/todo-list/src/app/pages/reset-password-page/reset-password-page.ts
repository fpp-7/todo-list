import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AccessApiService } from '../../core/auth/access-api.service';

type FeedbackTone = 'success' | 'error' | null;

@Component({
  selector: 'app-reset-password-page',
  imports: [FormsModule, RouterLink],
  templateUrl: './reset-password-page.html',
  styleUrl: '../login-page/login-page.css',
})
export class ResetPasswordPage {
  private readonly accessApi = inject(AccessApiService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  protected readonly isSubmittingReset = signal(false);
  protected readonly resetFeedback = signal('');
  protected readonly resetFeedbackTone = signal<FeedbackTone>(null);
  protected readonly showPassword = signal(false);

  protected resetToken = this.route.snapshot.queryParamMap.get('token') ?? '';
  protected newPassword = '';
  protected confirmPassword = '';

  protected togglePasswordVisibility(): void {
    this.showPassword.update((value) => !value);
  }

  protected submitResetPassword(): void {
    const token = this.resetToken.trim();
    const newPassword = this.newPassword.trim();
    const confirmPassword = this.confirmPassword.trim();

    if (!token) {
      this.setResetFeedback('Informe o token recebido por e-mail.', 'error');
      return;
    }

    if (newPassword.length < 6) {
      this.setResetFeedback('A nova senha precisa ter pelo menos 6 caracteres.', 'error');
      return;
    }

    if (newPassword !== confirmPassword) {
      this.setResetFeedback('A confirmacao precisa ser igual a nova senha.', 'error');
      return;
    }

    this.isSubmittingReset.set(true);

    this.accessApi
      .resetPassword({ token, newPassword, confirmPassword })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => {
          this.isSubmittingReset.set(false);
          this.setResetFeedback(response.message, 'success');
          this.newPassword = '';
          this.confirmPassword = '';
          window.setTimeout(() => this.router.navigate(['/login']), 900);
        },
        error: (error) => {
          this.isSubmittingReset.set(false);
          this.setResetFeedback(
            this.extractErrorMessage(error) ?? 'Nao foi possivel redefinir a senha agora.',
            'error',
          );
        },
      });
  }

  private setResetFeedback(message: string, tone: Exclude<FeedbackTone, null>): void {
    this.resetFeedback.set(message);
    this.resetFeedbackTone.set(tone);
  }

  private extractErrorMessage(error: unknown): string | null {
    if (
      error &&
      typeof error === 'object' &&
      'error' in error &&
      error.error &&
      typeof error.error === 'object' &&
      'message' in error.error &&
      typeof error.error.message === 'string'
    ) {
      return error.error.message;
    }

    return null;
  }
}
