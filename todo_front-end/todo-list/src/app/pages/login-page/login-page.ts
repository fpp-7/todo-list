import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { interval } from 'rxjs';
import { AccessApiService } from '../../core/auth/access-api.service';
import { SessionService } from '../../core/auth/session.service';

type LoginHighlight = {
  readonly title: string;
  readonly description: string;
};

type FeedbackTone = 'success' | 'error' | null;

@Component({
  selector: 'app-login-page',
  imports: [FormsModule, RouterLink],
  templateUrl: './login-page.html',
  styleUrl: './login-page.css',
})
export class LoginPage {
  private readonly highlightDurationMs = 4500;
  private readonly highlightTransitionMs = 320;
  private readonly destroyRef = inject(DestroyRef);
  private readonly accessApi = inject(AccessApiService);
  private readonly sessionService = inject(SessionService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private highlightSwapTimeoutId: ReturnType<typeof setTimeout> | null = null;
  private pendingHighlightIndex: number | null = null;

  protected readonly highlights: readonly LoginHighlight[] = [
    {
      title: 'Organize seu dia sem perder o ritmo.',
      description:
        'Re\u00FAna tarefas, prioridades e prazos em um painel simples para trabalhar com foco do in\u00EDcio ao fim.',
    },
    {
      title: 'Mantenha a equipe alinhada em cada entrega.',
      description:
        'Distribua atividades, acompanhe respons\u00E1veis e visualize gargalos antes que eles atrasem o seu planejamento.',
    },
    {
      title: 'Transforme pend\u00EAncias em pr\u00F3ximas a\u00E7\u00F5es.',
      description:
        'Destaque o que precisa sair hoje, acompanhe status e ganhe previsibilidade para sua rotina.',
    },
  ];

  protected readonly activeHighlightIndex = signal(0);
  protected readonly isHighlightTransitioning = signal(false);
  protected readonly showPassword = signal(false);
  protected readonly isForgotPasswordModalOpen = signal(false);
  protected readonly isInviteModalOpen = signal(false);
  protected readonly isSubmittingForgotPassword = signal(false);
  protected readonly isSubmittingInvite = signal(false);
  protected readonly isSubmittingLogin = signal(false);
  protected readonly forgotPasswordFeedback = signal('');
  protected readonly forgotPasswordFeedbackTone = signal<FeedbackTone>(null);
  protected readonly inviteFeedback = signal('');
  protected readonly inviteFeedbackTone = signal<FeedbackTone>(null);
  protected readonly loginFeedback = signal('');
  protected readonly loginFeedbackTone = signal<FeedbackTone>(null);
  protected readonly activeHighlight = computed(
    () => this.highlights[this.activeHighlightIndex()],
  );

  protected loginEmail = '';
  protected loginPassword = '';
  protected forgotPasswordEmail = '';
  protected inviteName = '';
  protected inviteEmail = '';
  protected inviteCompany = '';

  constructor() {
    this.destroyRef.onDestroy(() => this.clearHighlightSwapTimeout());

    interval(this.highlightDurationMs)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.advanceHighlight());
  }

  protected setActiveHighlight(index: number): void {
    if (index === this.activeHighlightIndex() && !this.isHighlightTransitioning()) {
      return;
    }

    this.transitionToHighlight(index);
  }

  protected togglePasswordVisibility(): void {
    this.showPassword.update((value) => !value);
  }

  protected openForgotPasswordModal(): void {
    this.resetForgotPasswordState();
    this.isForgotPasswordModalOpen.set(true);
  }

  protected closeForgotPasswordModal(): void {
    this.isForgotPasswordModalOpen.set(false);
    this.resetForgotPasswordState();
  }

  protected openInviteModal(): void {
    this.resetInviteState();
    this.isInviteModalOpen.set(true);
  }

  protected closeInviteModal(): void {
    this.isInviteModalOpen.set(false);
    this.resetInviteState();
  }

  protected handleLoginSubmit(): void {
    const login = this.loginEmail.trim().toLowerCase();
    const password = this.loginPassword.trim();

    if (!this.isValidEmail(login)) {
      this.setLoginFeedback(
        'Informe um e-mail v\u00E1lido para fazer login.',
        'error',
      );
      return;
    }

    if (!password) {
      this.setLoginFeedback('Informe sua senha para fazer login.', 'error');
      return;
    }

    this.isSubmittingLogin.set(true);

    this.accessApi
      .login({ login, password })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => {
          this.sessionService.saveSession(response);
          this.isSubmittingLogin.set(false);
          this.setLoginFeedback('Login realizado com sucesso!', 'success');
          this.router.navigateByUrl(this.getPostLoginRedirectUrl());
        },
        error: (error) => {
          this.isSubmittingLogin.set(false);
          this.setLoginFeedback(
            'Erro ao fazer login. Verifique suas credenciais.',
            'error',
          );
          console.error('Login error:', error);
        },
      });
  }

  protected submitForgotPassword(): void {
    const email = this.forgotPasswordEmail.trim().toLowerCase();

    if (!this.isValidEmail(email)) {
      this.setForgotPasswordFeedback(
        'Informe um e-mail v\u00E1lido para recuperar sua senha.',
        'error',
      );
      return;
    }

    this.isSubmittingForgotPassword.set(true);

    this.accessApi
      .requestPasswordReset({ email })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => {
          this.forgotPasswordEmail = '';
          this.isSubmittingForgotPassword.set(false);
          this.setForgotPasswordFeedback(response.message, 'success');
        },
        error: () => {
          this.isSubmittingForgotPassword.set(false);
          this.setForgotPasswordFeedback(
            'N\u00E3o foi poss\u00EDvel enviar a recupera\u00E7\u00E3o agora. Verifique o backend e tente novamente.',
            'error',
          );
        },
      });
  }

  protected submitInviteRequest(): void {
    const name = this.inviteName.trim();
    const email = this.inviteEmail.trim().toLowerCase();
    const company = this.inviteCompany.trim();

    if (!name) {
      this.setInviteFeedback('Informe seu nome para solicitar o convite.', 'error');
      return;
    }

    if (!this.isValidEmail(email)) {
      this.setInviteFeedback('Informe um e-mail v\u00E1lido para solicitar o convite.', 'error');
      return;
    }

    this.isSubmittingInvite.set(true);

    this.accessApi
      .requestInvite({
        name,
        email,
        company: company || null,
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => {
          this.inviteName = '';
          this.inviteEmail = '';
          this.inviteCompany = '';
          this.isSubmittingInvite.set(false);
          this.setInviteFeedback(response.message, 'success');
        },
        error: () => {
          this.isSubmittingInvite.set(false);
          this.setInviteFeedback(
            'N\u00E3o foi poss\u00EDvel registrar o convite agora. Verifique o backend e tente novamente.',
            'error',
          );
        },
      });
  }

  private advanceHighlight(): void {
    const baseIndex = this.pendingHighlightIndex ?? this.activeHighlightIndex();
    const nextIndex = (baseIndex + 1) % this.highlights.length;

    this.transitionToHighlight(nextIndex);
  }

  private transitionToHighlight(index: number): void {
    this.pendingHighlightIndex = index;
    this.isHighlightTransitioning.set(true);
    this.clearHighlightSwapTimeout();

    this.highlightSwapTimeoutId = window.setTimeout(() => {
      if (this.pendingHighlightIndex !== null) {
        this.activeHighlightIndex.set(this.pendingHighlightIndex);
      }

      this.pendingHighlightIndex = null;
      this.isHighlightTransitioning.set(false);
      this.highlightSwapTimeoutId = null;
    }, this.highlightTransitionMs);
  }

  private clearHighlightSwapTimeout(): void {
    if (this.highlightSwapTimeoutId === null) {
      return;
    }

    clearTimeout(this.highlightSwapTimeoutId);
    this.highlightSwapTimeoutId = null;
  }

  private resetForgotPasswordState(): void {
    this.forgotPasswordEmail = '';
    this.isSubmittingForgotPassword.set(false);
    this.forgotPasswordFeedback.set('');
    this.forgotPasswordFeedbackTone.set(null);
  }

  private resetInviteState(): void {
    this.inviteName = '';
    this.inviteEmail = '';
    this.inviteCompany = '';
    this.isSubmittingInvite.set(false);
    this.inviteFeedback.set('');
    this.inviteFeedbackTone.set(null);
  }

  private setForgotPasswordFeedback(message: string, tone: Exclude<FeedbackTone, null>): void {
    this.forgotPasswordFeedback.set(message);
    this.forgotPasswordFeedbackTone.set(tone);
  }

  private setInviteFeedback(message: string, tone: Exclude<FeedbackTone, null>): void {
    this.inviteFeedback.set(message);
    this.inviteFeedbackTone.set(tone);
  }

  private setLoginFeedback(message: string, tone: Exclude<FeedbackTone, null>): void {
    this.loginFeedback.set(message);
    this.loginFeedbackTone.set(tone);
  }

  private getPostLoginRedirectUrl(): string {
    const redirectTo = this.route.snapshot.queryParamMap.get('redirectTo');

    if (redirectTo && this.isSafeRedirectPath(redirectTo)) {
      return redirectTo;
    }

    return '/tasks';
  }

  private isSafeRedirectPath(path: string): boolean {
    return path.startsWith('/') && !path.startsWith('//') && !/^https?:\/\//i.test(path);
  }

  private isValidEmail(email: string): boolean {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
  }
}

