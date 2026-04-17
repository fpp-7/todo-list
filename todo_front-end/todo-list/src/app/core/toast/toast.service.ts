import { Injectable, signal } from '@angular/core';

export type ToastTone = 'info' | 'success' | 'warning' | 'error';

export type ToastOptions = {
  readonly key?: string;
  readonly title?: string;
  readonly message: string;
  readonly tone?: ToastTone;
  readonly durationMs?: number;
};

export type ToastItem = {
  readonly id: number;
  readonly key: string | null;
  readonly title: string | null;
  readonly message: string;
  readonly tone: ToastTone;
};

@Injectable({
  providedIn: 'root',
})
export class ToastService {
  private readonly defaultDurationMs = 4600;
  private readonly toastsState = signal<ToastItem[]>([]);
  private readonly timers = new Map<number, ReturnType<typeof setTimeout>>();
  private nextId = 1;

  readonly toasts = this.toastsState.asReadonly();

  show(options: ToastOptions): number {
    const tone = options.tone ?? 'info';
    const existingToast = options.key
      ? this.toastsState().find((toast) => toast.key === options.key)
      : null;

    if (existingToast) {
      const updatedToast: ToastItem = {
        ...existingToast,
        title: options.title ?? existingToast.title,
        message: options.message,
        tone,
      };

      this.toastsState.update((currentToasts) =>
        currentToasts.map((toast) => (toast.id === existingToast.id ? updatedToast : toast)),
      );
      this.scheduleDismiss(existingToast.id, options.durationMs ?? this.defaultDurationMs);
      return existingToast.id;
    }

    const toast: ToastItem = {
      id: this.nextId++,
      key: options.key ?? null,
      title: options.title ?? null,
      message: options.message,
      tone,
    };

    this.toastsState.update((currentToasts) => [...currentToasts, toast]);
    this.scheduleDismiss(toast.id, options.durationMs ?? this.defaultDurationMs);
    return toast.id;
  }

  dismiss(id: number): void {
    this.clearTimer(id);
    this.toastsState.update((currentToasts) => currentToasts.filter((toast) => toast.id !== id));
  }

  success(message: string, title?: string): number {
    return this.show({ message, title, tone: 'success' });
  }

  error(message: string, title?: string, key?: string): number {
    return this.show({ key, message, title, tone: 'error' });
  }

  warning(message: string, title?: string, key?: string): number {
    return this.show({ key, message, title, tone: 'warning' });
  }

  info(message: string, title?: string, key?: string): number {
    return this.show({ key, message, title, tone: 'info' });
  }

  private scheduleDismiss(id: number, durationMs: number): void {
    this.clearTimer(id);

    const timer = window.setTimeout(() => this.dismiss(id), durationMs);
    this.timers.set(id, timer);
  }

  private clearTimer(id: number): void {
    const timer = this.timers.get(id);

    if (timer === undefined) {
      return;
    }

    clearTimeout(timer);
    this.timers.delete(id);
  }
}
