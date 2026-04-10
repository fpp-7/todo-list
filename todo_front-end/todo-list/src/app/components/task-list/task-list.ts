import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import {
  TaskApiRecord,
  TaskApiService,
  TaskPriority,
  TaskUpsertPayload,
} from '../../core/tasks/task-api.service';
import { ThemeService } from '../../core/theme/theme.service';
import { SessionService } from '../../core/auth/session.service';
import { ProfileDTO } from '../../core/auth/auth.dtos';
import { AccessApiService } from '../../core/auth/access-api.service';
import { ProfileApiService } from '../../core/profile/profile-api.service';
import { TaskForm } from '../task-form/task-form';
import { environment } from '../../../environments/environment';

type TaskFilter = 'Todas' | 'Hoje' | 'Em andamento' | 'Planejadas' | 'Concluídas';
type TaskStatus = 'Hoje' | 'Em andamento' | 'Planejada' | 'Concluída';

type TaskItem = {
  readonly id: number;
  readonly title: string;
  readonly description: string;
  readonly category: string;
  readonly status: TaskStatus;
  readonly priority: TaskPriority;
  readonly dueDate: string | null;
  readonly dueLabel: string;
  readonly done: boolean;
};

type TaskChanges = Partial<Pick<TaskItem, 'title' | 'description' | 'category' | 'status' | 'priority'>>;

const today = new Date();
const todayIso = today.toISOString().slice(0, 10);
const tomorrowIso = new Date(today.getFullYear(), today.getMonth(), today.getDate() + 1)
  .toISOString()
  .slice(0, 10);
const nextWeekIso = new Date(today.getFullYear(), today.getMonth(), today.getDate() + 7)
  .toISOString()
  .slice(0, 10);

const fallbackTasks: readonly TaskApiRecord[] = [
  {
    id: 101,
    name: 'Revisar onboarding do usuário',
    description: 'Validar os ajustes finais antes de liberar o fluxo.',
    category: 'Pessoal',
    priority: 'Alta',
    dueDate: todayIso,
    done: false,
  },
  {
    id: 102,
    name: 'Separar referências para o dashboard',
    description: 'Agrupar ideias de interface e estados de carregamento.',
    category: 'Design',
    priority: 'Média',
    dueDate: null,
    done: false,
  },
  {
    id: 103,
    name: 'Organizar tarefas da próxima semana',
    description: 'Definir o que entra no próximo ciclo de entregas.',
    category: 'Planejamento',
    priority: 'Baixa',
    dueDate: nextWeekIso,
    done: false,
  },
  {
    id: 104,
    name: 'Fechar checklist do projeto',
    description: 'Conferir se todos os itens essenciais foram concluídos.',
    category: 'Pessoal',
    priority: 'Média',
    dueDate: todayIso,
    done: true,
  },
  {
    id: 105,
    name: 'Preparar briefing da próxima sprint',
    description: 'Deixar o material da próxima etapa já alinhado.',
    category: 'Produto',
    priority: 'Alta',
    dueDate: tomorrowIso,
    done: false,
  },
] as const;

@Component({
  selector: 'app-task-list',
  imports: [FormsModule, TaskForm],
  templateUrl: './task-list.html',
  styleUrl: './task-list.css',
})
export class TaskList {
  private readonly localTasksStorageKey = 'todo-list-local-tasks';
  private readonly destroyRef = inject(DestroyRef);
  private readonly taskApi = inject(TaskApiService);
  private readonly accessApi = inject(AccessApiService);
  private readonly profileApi = inject(ProfileApiService);
  private readonly themeService = inject(ThemeService);
  private readonly sessionService = inject(SessionService);
  private readonly router = inject(Router);

  protected readonly profileName = signal('Usuário');
  protected readonly profileEmail = signal('usuario@exemplo.com');
  protected readonly profileImageUrl = signal<string | null>(null);
  protected readonly isDarkTheme = this.themeService.isDarkTheme;
  protected readonly filters: readonly TaskFilter[] = [
    'Todas',
    'Hoje',
    'Em andamento',
    'Planejadas',
    'Concluídas',
  ];
  protected readonly taskStatuses: readonly TaskStatus[] = [
    'Hoje',
    'Em andamento',
    'Planejada',
    'Concluída',
  ];
  protected readonly taskPriorities: readonly TaskPriority[] = ['Baixa', 'Média', 'Alta'];

  protected readonly profileInitials = computed(() =>
    this.profileName()
      .split(' ')
      .filter(Boolean)
      .slice(0, 2)
      .map((name) => name[0]?.toUpperCase() ?? '')
      .join(''),
  );

  protected readonly selectedFilter = signal<TaskFilter>('Todas');
  protected readonly searchQuery = signal('');
  protected readonly tasks = signal<TaskItem[]>([]);
  protected readonly updatingTaskIds = signal<number[]>([]);
  protected readonly isTaskModalOpen = signal(false);
  protected readonly isEditTaskModalOpen = signal(false);
  protected readonly isProfileModalOpen = signal(false);
  protected readonly isLoading = signal(true);
  protected readonly isSavingTask = signal(false);
  protected readonly isSavingProfilePhoto = signal(false);
  protected readonly isSavingProfilePassword = signal(false);
  protected readonly usingLocalFallback = signal(false);
  protected readonly syncMessage = signal('');

  protected editingTaskId: number | null = null;
  protected editingTitle = '';
  protected editingDescription = '';
  protected editingCategory = 'Pessoal';
  protected editingStatus: TaskStatus = 'Hoje';
  protected editingPriority: TaskPriority = 'Média';
  protected profileCurrentPassword = '';
  protected profileNewPassword = '';
  protected profileConfirmPassword = '';
  protected readonly profilePasswordFeedback = signal('');
  protected readonly profilePasswordFeedbackTone = signal<'success' | 'error' | null>(null);

  protected readonly filteredTasks = computed(() => {
    const filter = this.selectedFilter();
    const query = this.searchQuery().trim().toLowerCase();
    const filterStatus = this.getFilterStatus(filter);

    return this.tasks().filter((task) => {
      const matchesFilter = !filterStatus || task.status === filterStatus;
      const matchesQuery =
        !query ||
        task.title.toLowerCase().includes(query) ||
        task.description.toLowerCase().includes(query) ||
        task.category.toLowerCase().includes(query);

      return matchesFilter && matchesQuery;
    });
  });

  protected readonly todayCount = computed(
    () => this.tasks().filter((task) => task.status === 'Hoje').length,
  );

  protected readonly inProgressCount = computed(
    () => this.tasks().filter((task) => task.status === 'Em andamento').length,
  );

  protected readonly plannedCount = computed(
    () => this.tasks().filter((task) => task.status === 'Planejada').length,
  );

  protected readonly doneCount = computed(
    () => this.tasks().filter((task) => task.status === 'Concluída').length,
  );

  constructor() {
    this.loadProfile();
    this.loadTasks();
  }

  protected logout(): void {
    this.accessApi
      .logout()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({ error: () => undefined });

    this.sessionService.clearSession();
    this.router.navigate(['/login']);
  }

  protected selectFilter(filter: TaskFilter): void {
    this.selectedFilter.set(filter);
  }

  protected setSearchQuery(value: string): void {
    this.searchQuery.set(value);
  }

  protected openTaskModal(): void {
    this.isTaskModalOpen.set(true);
  }

  protected toggleTheme(): void {
    this.themeService.toggleTheme();
  }

  protected closeTaskModal(): void {
    this.isTaskModalOpen.set(false);
  }

  protected openEditTaskModal(task: TaskItem): void {
    if (this.isTaskUpdating(task.id)) {
      return;
    }

    this.editingTaskId = task.id;
    this.editingTitle = task.title;
    this.editingDescription =
      task.description === 'Sem observações por enquanto.' ? '' : task.description;
    this.editingCategory = task.category;
    this.editingStatus = task.status;
    this.editingPriority = task.priority;
    this.isEditTaskModalOpen.set(true);
  }

  protected closeEditTaskModal(): void {
    this.isEditTaskModalOpen.set(false);
    this.resetEditingTask();
  }

  protected handleTaskCardKeydown(event: KeyboardEvent, task: TaskItem): void {
    if (event.key !== 'Enter' && event.key !== ' ') {
      return;
    }

    event.preventDefault();
    this.openEditTaskModal(task);
  }

  protected submitTaskEdit(): void {
    if (this.editingTaskId === null) {
      return;
    }

    const task = this.tasks().find((currentTask) => currentTask.id === this.editingTaskId);
    const normalizedTitle = this.editingTitle.trim();

    if (!task || !normalizedTitle) {
      return;
    }

    this.persistTaskChanges(
      task,
      {
        title: normalizedTitle,
        description: this.editingDescription,
        category: this.editingCategory,
        status: this.editingStatus,
        priority: this.editingPriority,
      },
      {
        closeEditModal: true,
        localMessage: 'Tarefa atualizada localmente.',
        remoteMessage: 'Tarefa atualizada no backend.',
        fallbackMessage: 'Backend indisponível. A tarefa ficou salva localmente.',
      },
    );
  }

  protected openProfileModal(): void {
    this.resetProfileSecurity();
    this.isProfileModalOpen.set(true);
  }

  protected closeProfileModal(): void {
    this.isProfileModalOpen.set(false);
    this.resetProfileSecurity();
  }

  protected saveProfilePassword(): void {
    if (
      !this.profileCurrentPassword ||
      !this.profileNewPassword ||
      !this.profileConfirmPassword
    ) {
      this.setProfilePasswordFeedback('Preencha todos os campos para alterar sua senha.', 'error');
      return;
    }

    if (this.profileNewPassword.length < 6) {
      this.setProfilePasswordFeedback('A nova senha precisa ter pelo menos 6 caracteres.', 'error');
      return;
    }

    if (this.profileNewPassword !== this.profileConfirmPassword) {
      this.setProfilePasswordFeedback('A confirmação da senha precisa ser igual à nova senha.', 'error');
      return;
    }

    this.isSavingProfilePassword.set(true);

    this.profileApi
      .updatePassword({
        currentPassword: this.profileCurrentPassword,
        newPassword: this.profileNewPassword,
        confirmPassword: this.profileConfirmPassword,
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => {
          this.profileCurrentPassword = '';
          this.profileNewPassword = '';
          this.profileConfirmPassword = '';
          this.isSavingProfilePassword.set(false);
          this.setProfilePasswordFeedback(response.message, 'success');
        },
        error: (error) => {
          this.isSavingProfilePassword.set(false);
          this.setProfilePasswordFeedback(
            this.extractErrorMessage(error) ?? 'Não foi possível atualizar sua senha agora.',
            'error',
          );
        },
      });
  }

  protected handleTaskSubmitted(payload: TaskUpsertPayload): void {
    this.isSavingTask.set(true);

    if (this.usingLocalFallback()) {
      this.insertLocalTask(payload);
      return;
    }

    this.taskApi
      .createTask(payload)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (task) => {
          this.tasks.update((currentTasks) => [this.mapTaskRecord(task), ...currentTasks]);
          this.isSavingTask.set(false);
          this.isTaskModalOpen.set(false);
          this.syncMessage.set('Tarefa salva no backend.');
        },
        error: () => {
          this.usingLocalFallback.set(true);
          this.syncMessage.set('Backend indisponível. A tarefa foi criada apenas localmente.');
          this.insertLocalTask(payload);
        },
      });
  }

  protected completeTask(task: TaskItem): void {
    if (task.done || this.isTaskUpdating(task.id)) {
      return;
    }

    this.persistTaskCompletion(task);
  }

  protected deleteEditingTask(): void {
    if (this.editingTaskId === null || this.isEditingCurrentTask()) {
      return;
    }

    const task = this.tasks().find((currentTask) => currentTask.id === this.editingTaskId);

    if (!task) {
      return;
    }

    if (
      typeof window !== 'undefined' &&
      !window.confirm(`Deseja excluir a tarefa "${task.title}"?`)
    ) {
      return;
    }

    this.persistTaskDeletion(task);
  }

  protected isTaskUpdating(taskId: number): boolean {
    return this.updatingTaskIds().includes(taskId);
  }

  protected isEditingCurrentTask(): boolean {
    return this.editingTaskId !== null && this.isTaskUpdating(this.editingTaskId);
  }

  protected handleProfilePhotoSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];

    if (!file || !file.type.startsWith('image/')) {
      return;
    }

    const reader = new FileReader();

    reader.onload = () => {
      if (typeof reader.result === 'string') {
        this.persistProfilePhoto(reader.result);
      }
    };

    reader.readAsDataURL(file);
    input.value = '';
  }

  protected removeProfilePhoto(): void {
    this.persistProfilePhoto(null);
  }

  private loadProfile(): void {
    const cachedProfile = this.sessionService.getProfile();

    if (cachedProfile) {
      this.applyProfile(cachedProfile);
    }

    this.profileApi
      .getProfile()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (profile) => {
          this.sessionService.saveProfile(profile);
          this.applyProfile(profile);
        },
        error: () => {
          if (!cachedProfile) {
            this.profileName.set('Usuário');
            this.profileEmail.set('usuario@exemplo.com');
          }
        },
      });
  }

  private persistProfilePhoto(photoDataUrl: string | null): void {
    this.isSavingProfilePhoto.set(true);

    this.profileApi
      .updatePhoto({ photoDataUrl })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (profile) => {
          this.sessionService.saveProfile(profile);
          this.applyProfile(profile);
          this.isSavingProfilePhoto.set(false);
          this.setProfilePasswordFeedback('Foto de perfil sincronizada com o backend.', 'success');
        },
        error: (error) => {
          this.isSavingProfilePhoto.set(false);
          this.setProfilePasswordFeedback(
            this.extractErrorMessage(error) ?? 'Não foi possível sincronizar a foto agora.',
            'error',
          );
        },
      });
  }

  private applyProfile(profile: ProfileDTO): void {
    this.profileName.set(profile.displayName || profile.email);
    this.profileEmail.set(profile.email);
    this.profileImageUrl.set(this.resolveProfilePhotoUrl(profile.photoDataUrl));
  }

  private resolveProfilePhotoUrl(photoDataUrl: string | null): string | null {
    if (!photoDataUrl) {
      return null;
    }

    if (photoDataUrl.startsWith('/')) {
      return `${environment.apiBaseUrl}${photoDataUrl}`;
    }

    return photoDataUrl;
  }

  private loadTasks(): void {
    this.isLoading.set(true);

    this.taskApi
      .listTasks()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (tasks) => {
          this.tasks.set(tasks.map((task) => this.mapTaskRecord(task)));
          this.usingLocalFallback.set(false);
          this.clearLocalTasks();
          this.syncMessage.set('Dados sincronizados com o backend.');
          this.isLoading.set(false);
        },
        error: () => {
          const localTasks = this.readLocalTasks();
          this.tasks.set((localTasks ?? fallbackTasks).map((task) => this.mapTaskRecord(task)));
          this.usingLocalFallback.set(true);
          this.syncMessage.set(
            localTasks
              ? 'Backend indisponível. Exibindo tarefas locais salvas neste navegador.'
              : 'Backend indisponível. Exibindo tarefas locais para continuar o layout.',
          );
          this.isLoading.set(false);
        },
      });
  }

  private insertLocalTask(payload: TaskUpsertPayload): void {
    const localTask = this.mapTaskRecord({
      id: Date.now(),
      ...payload,
    });

    this.tasks.update((currentTasks) => [localTask, ...currentTasks]);
    this.saveLocalTasks();
    this.isSavingTask.set(false);
    this.isTaskModalOpen.set(false);
    this.syncMessage.set('Tarefa salva localmente.');
  }

  private persistTaskCompletion(task: TaskItem): void {
    const completedTask = this.applyTaskChanges(task, { status: 'Concluída' });

    this.replaceTask(completedTask);
    this.setTaskUpdating(task.id, true);

    if (this.usingLocalFallback()) {
      this.finishTaskPersistence(task.id, 'Tarefa concluída localmente.');
      return;
    }

    this.taskApi
      .completeTask(task.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (savedTask) => {
          this.replaceTask(this.mapTaskRecord(savedTask));
          this.finishTaskPersistence(task.id, 'Tarefa concluída no backend.');
        },
        error: () => {
          this.usingLocalFallback.set(true);
          this.finishTaskPersistence(
            task.id,
            'Backend indisponível. A conclusão ficou salva localmente.',
          );
        },
      });
  }

  private persistTaskDeletion(task: TaskItem): void {
    this.removeTask(task.id);
    this.setTaskUpdating(task.id, true);

    if (this.usingLocalFallback()) {
      this.finishTaskPersistence(task.id, 'Tarefa removida localmente.', true);
      return;
    }

    this.taskApi
      .deleteTask(task.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.finishTaskPersistence(task.id, 'Tarefa removida do backend.', true);
        },
        error: () => {
          this.usingLocalFallback.set(true);
          this.finishTaskPersistence(
            task.id,
            'Backend indisponível. A exclusão ficou salva localmente.',
            true,
          );
        },
      });
  }

  private persistTaskChanges(
    task: TaskItem,
    changes: TaskChanges,
    options: {
      readonly closeEditModal?: boolean;
      readonly localMessage?: string;
      readonly remoteMessage?: string;
      readonly fallbackMessage?: string;
    } = {},
  ): void {
    const updatedTask = this.applyTaskChanges(task, changes);

    this.replaceTask(updatedTask);
    this.setTaskUpdating(task.id, true);

    if (this.usingLocalFallback()) {
      this.finishTaskPersistence(
        task.id,
        options.localMessage ?? 'Alteração salva localmente.',
        options.closeEditModal,
      );
      return;
    }

    this.taskApi
      .updateTask(task.id, this.toTaskPayload(updatedTask))
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (savedTask) => {
          this.replaceTask(this.mapTaskRecord(savedTask));
          this.finishTaskPersistence(
            task.id,
            options.remoteMessage ?? 'Tarefa atualizada no backend.',
            options.closeEditModal,
          );
        },
        error: () => {
          this.usingLocalFallback.set(true);
          this.finishTaskPersistence(
            task.id,
            options.fallbackMessage ?? 'Backend indisponível. A alteração ficou salva localmente.',
            options.closeEditModal,
          );
        },
      });
  }

  private finishTaskPersistence(taskId: number, message: string, closeEditModal = false): void {
    this.syncMessage.set(message);
    this.setTaskUpdating(taskId, false);

    if (this.usingLocalFallback()) {
      this.saveLocalTasks();
    }

    if (closeEditModal && this.editingTaskId === taskId) {
      this.closeEditTaskModal();
    }
  }

  private applyTaskChanges(task: TaskItem, changes: TaskChanges): TaskItem {
    const nextTitle = changes.title?.trim() || task.title;
    const nextDescription =
      changes.description !== undefined ? changes.description.trim() : task.description;
    const nextCategory =
      changes.category !== undefined ? changes.category.trim() || 'Pessoal' : task.category;
    const nextStatus = changes.status ?? task.status;
    const nextPriority = changes.priority ?? task.priority;

    let nextDueDate = task.dueDate;
    let nextDone = false;

    if (nextStatus === 'Concluída') {
      nextDone = true;
      nextDueDate = nextDueDate ?? todayIso;
    } else if (nextStatus === 'Hoje') {
      nextDone = false;
      nextDueDate = todayIso;
    } else if (nextStatus === 'Planejada') {
      nextDone = false;
      nextDueDate = nextDueDate && nextDueDate > todayIso ? nextDueDate : tomorrowIso;
    } else {
      nextDone = false;
      nextDueDate = null;
    }

    return this.mapTaskRecord({
      id: task.id,
      name: nextTitle,
      description: nextDescription,
      category: nextCategory,
      priority: nextPriority,
      dueDate: nextDueDate,
      done: nextDone,
    });
  }

  private toTaskPayload(task: TaskItem): TaskUpsertPayload {
    return {
      name: task.title,
      description: task.description,
      category: task.category,
      priority: task.priority,
      dueDate: task.dueDate,
      done: task.done,
    };
  }

  private setTaskUpdating(taskId: number, isUpdating: boolean): void {
    this.updatingTaskIds.update((currentIds) => {
      if (isUpdating) {
        return currentIds.includes(taskId) ? currentIds : [...currentIds, taskId];
      }

      return currentIds.filter((currentId) => currentId !== taskId);
    });
  }

  private replaceTask(updatedTask: TaskItem): void {
    this.tasks.update((currentTasks) =>
      currentTasks.map((task) => (task.id === updatedTask.id ? updatedTask : task)),
    );
  }

  private removeTask(taskId: number): void {
    this.tasks.update((currentTasks) => currentTasks.filter((task) => task.id !== taskId));
  }

  private resetEditingTask(): void {
    this.editingTaskId = null;
    this.editingTitle = '';
    this.editingDescription = '';
    this.editingCategory = 'Pessoal';
    this.editingStatus = 'Hoje';
    this.editingPriority = 'Média';
  }

  private resetProfileSecurity(): void {
    this.profileCurrentPassword = '';
    this.profileNewPassword = '';
    this.profileConfirmPassword = '';
    this.profilePasswordFeedback.set('');
    this.profilePasswordFeedbackTone.set(null);
  }

  private setProfilePasswordFeedback(
    message: string,
    tone: 'success' | 'error',
  ): void {
    this.profilePasswordFeedback.set(message);
    this.profilePasswordFeedbackTone.set(tone);
  }

  private getFilterStatus(filter: TaskFilter): TaskStatus | null {
    if (filter === 'Todas') {
      return null;
    }

    if (filter === 'Concluídas') {
      return 'Concluída';
    }

    if (filter === 'Planejadas') {
      return 'Planejada';
    }

    return filter;
  }

  private mapTaskRecord(task: TaskApiRecord): TaskItem {
    const normalizedPriority = this.normalizePriority(task.priority);

    return {
      id: task.id,
      title: task.name?.trim() || 'Tarefa sem título',
      description: task.description?.trim() || 'Sem observações por enquanto.',
      category: task.category?.trim() || 'Pessoal',
      status: this.resolveStatus(task),
      priority: normalizedPriority,
      dueDate: task.dueDate,
      dueLabel: this.formatDueLabel(task.dueDate, task.done),
      done: task.done,
    };
  }

  private normalizePriority(priority: string | null): TaskPriority {
    if (priority === 'Baixa' || priority === 'Alta') {
      return priority;
    }

    return 'Média';
  }

  private resolveStatus(task: TaskApiRecord): TaskStatus {
    if (task.done) {
      return 'Concluída';
    }

    if (task.dueDate && task.dueDate > todayIso) {
      return 'Planejada';
    }

    if (task.dueDate && task.dueDate <= todayIso) {
      return 'Hoje';
    }

    return 'Em andamento';
  }

  private formatDueLabel(dueDate: string | null, done: boolean): string {
    if (done) {
      return 'Concluída';
    }

    if (!dueDate) {
      return 'Sem prazo definido';
    }

    const formattedDate = new Intl.DateTimeFormat('pt-BR', {
      day: '2-digit',
      month: '2-digit',
    }).format(new Date(`${dueDate}T00:00:00`));

    if (dueDate === todayIso) {
      return 'Vence hoje';
    }

    if (dueDate > todayIso) {
      return `Planejada para ${formattedDate}`;
    }

    return `Atrasada desde ${formattedDate}`;
  }

  private readLocalTasks(): TaskApiRecord[] | null {
    try {
      const storedTasks = localStorage.getItem(this.localTasksStorageKey);

      if (!storedTasks) {
        return null;
      }

      const parsedTasks = JSON.parse(storedTasks);

      return Array.isArray(parsedTasks) ? (parsedTasks as TaskApiRecord[]) : null;
    } catch {
      return null;
    }
  }

  private saveLocalTasks(): void {
    try {
      localStorage.setItem(
        this.localTasksStorageKey,
        JSON.stringify(this.tasks().map((task) => this.toTaskRecord(task))),
      );
    } catch {
      this.syncMessage.set('Não foi possível persistir as tarefas locais neste navegador.');
    }
  }

  private clearLocalTasks(): void {
    try {
      localStorage.removeItem(this.localTasksStorageKey);
    } catch {
      // Ignore storage failures.
    }
  }

  private toTaskRecord(task: TaskItem): TaskApiRecord {
    return {
      id: task.id,
      name: task.title,
      description: task.description,
      category: task.category,
      priority: task.priority,
      dueDate: task.dueDate,
      done: task.done,
    };
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

