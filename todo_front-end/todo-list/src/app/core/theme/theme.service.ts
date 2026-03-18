import { DOCUMENT } from '@angular/common';
import { Injectable, computed, inject, signal } from '@angular/core';

export type AppTheme = 'light' | 'dark';

@Injectable({
  providedIn: 'root',
})
export class ThemeService {
  private readonly storageKey = 'todo-list-theme';
  private readonly document = inject(DOCUMENT);
  private readonly preferredTheme = signal<AppTheme>('light');
  private readonly activeTheme = signal<AppTheme>('light');

  readonly theme = this.preferredTheme.asReadonly();
  readonly isDarkTheme = computed(() => this.activeTheme() === 'dark');

  constructor() {
    const initialTheme = this.resolveInitialTheme();

    this.preferredTheme.set(initialTheme);
    this.applyDocumentTheme(
      this.shouldForceLightTheme(this.readCurrentPath()) ? 'light' : initialTheme,
    );
  }

  toggleTheme(): void {
    this.setTheme(this.preferredTheme() === 'dark' ? 'light' : 'dark');
  }

  setTheme(theme: AppTheme): void {
    this.preferredTheme.set(theme);
    this.persistTheme(theme);
    this.applyDocumentTheme(this.shouldForceLightTheme(this.readCurrentPath()) ? 'light' : theme);
  }

  applyRouteTheme(url: string): void {
    this.applyDocumentTheme(this.shouldForceLightTheme(url) ? 'light' : this.preferredTheme());
  }

  private resolveInitialTheme(): AppTheme {
    const storedTheme = this.readStoredTheme();

    if (storedTheme) {
      return storedTheme;
    }

    if (
      typeof window !== 'undefined' &&
      typeof window.matchMedia === 'function' &&
      window.matchMedia('(prefers-color-scheme: dark)').matches
    ) {
      return 'dark';
    }

    return 'light';
  }

  private readStoredTheme(): AppTheme | null {
    if (typeof window === 'undefined') {
      return null;
    }

    try {
      const storedTheme = window.localStorage.getItem(this.storageKey);

      return storedTheme === 'dark' || storedTheme === 'light' ? storedTheme : null;
    } catch {
      return null;
    }
  }

  private applyDocumentTheme(theme: AppTheme): void {
    this.activeTheme.set(theme);
    this.document.documentElement.dataset['theme'] = theme;
    this.document.body.dataset['theme'] = theme;
  }

  private persistTheme(theme: AppTheme): void {
    if (typeof window === 'undefined') {
      return;
    }

    try {
      window.localStorage.setItem(this.storageKey, theme);
    } catch {
      // Ignore storage failures and keep the theme only in memory.
    }
  }

  private readCurrentPath(): string {
    if (typeof window === 'undefined') {
      return '';
    }

    return window.location.pathname;
  }

  private shouldForceLightTheme(url: string): boolean {
    const normalizedUrl = url.split('?')[0]?.split('#')[0]?.toLowerCase() ?? '';

    return normalizedUrl === '' || normalizedUrl === '/' || normalizedUrl.startsWith('/login');
  }
}
