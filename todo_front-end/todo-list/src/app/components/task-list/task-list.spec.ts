import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { AccessApiService } from '../../core/auth/access-api.service';
import { ProfileApiService } from '../../core/profile/profile-api.service';
import { TaskApiService } from '../../core/tasks/task-api.service';
import { ToastService } from '../../core/toast/toast.service';
import { TaskList } from './task-list';

describe('TaskList', () => {
  let component: TaskList;
  let fixture: ComponentFixture<TaskList>;
  let router: Router;
  let accessApiService: jasmine.SpyObj<AccessApiService>;
  let taskApiService: jasmine.SpyObj<TaskApiService>;
  let profileApiService: jasmine.SpyObj<ProfileApiService>;
  let toastService: jasmine.SpyObj<ToastService>;

  const createTaskRecord = (id: number) => ({
    id,
    name: `Tarefa ${id}`,
    description: `Descricao da tarefa ${id}`,
    category: 'Pessoal',
    priority: 'Média' as const,
    dueDate: null,
    done: false,
  });

  beforeEach(async () => {
    localStorage.clear();

    accessApiService = jasmine.createSpyObj<AccessApiService>('AccessApiService', ['logout']);
    taskApiService = jasmine.createSpyObj<TaskApiService>('TaskApiService', [
      'listTasks',
      'createTask',
      'completeTask',
      'updateTask',
      'deleteTask',
    ]);
    profileApiService = jasmine.createSpyObj<ProfileApiService>('ProfileApiService', [
      'getProfile',
      'updatePassword',
      'updatePhoto',
    ]);
    toastService = jasmine.createSpyObj<ToastService>('ToastService', [
      'success',
      'error',
      'warning',
      'info',
      'dismiss',
      'show',
    ]);

    taskApiService.listTasks.and.returnValue(of([createTaskRecord(1)]));
    taskApiService.createTask.and.returnValue(of(createTaskRecord(1)));
    taskApiService.completeTask.and.returnValue(of({ ...createTaskRecord(1), done: true }));
    taskApiService.updateTask.and.returnValue(of(createTaskRecord(1)));
    taskApiService.deleteTask.and.returnValue(of(void 0));
    accessApiService.logout.and.returnValue(
      of({ status: 'SUCCESS', message: 'Sessao encerrada.' }),
    );
    profileApiService.getProfile.and.returnValue(
      of({
        id: 1,
        email: 'usuario@empresa.com',
        displayName: 'Usuario Teste',
        photoDataUrl: null,
      }),
    );

    await TestBed.configureTestingModule({
      imports: [TaskList],
      providers: [
        provideRouter([]),
        {
          provide: AccessApiService,
          useValue: accessApiService,
        },
        {
          provide: TaskApiService,
          useValue: taskApiService,
        },
        {
          provide: ProfileApiService,
          useValue: profileApiService,
        },
        {
          provide: ToastService,
          useValue: toastService,
        },
      ],
    }).compileComponents();

    router = TestBed.inject(Router);
    spyOn(router, 'navigate').and.returnValue(Promise.resolve(true));

    fixture = TestBed.createComponent(TaskList);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should render the task dashboard brand', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.task-topbar__brand-title')?.textContent).toContain('TODO LIST');
  });

  it('should render the profile name from backend', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.task-topbar__user strong')?.textContent).toContain(
      'Usuario Teste',
    );
  });

  it('should use the complete endpoint when finishing a task', () => {
    const task = (component as any).tasks()[0];

    (component as any).completeTask(task);

    expect(taskApiService.completeTask).toHaveBeenCalledOnceWith(task.id);
    expect(taskApiService.updateTask).not.toHaveBeenCalled();
  });

  it('should use the delete endpoint when removing a task from the edit modal', () => {
    const confirmSpy = spyOn(window, 'confirm').and.returnValue(true);
    const task = (component as any).tasks()[0];

    (component as any).openEditTaskModal(task);
    (component as any).deleteEditingTask();

    expect(confirmSpy).toHaveBeenCalled();
    expect(taskApiService.deleteTask).toHaveBeenCalledOnceWith(task.id);
  });

  it('should call logout endpoint without exposing refresh token', () => {
    (component as any).logout();

    expect(accessApiService.logout).toHaveBeenCalledOnceWith();
    expect(localStorage.getItem('auth-session')).toBeNull();
  });

  it('should paginate the rendered tasks list', () => {
    const taskRecords = Array.from({ length: 7 }, (_, index) => createTaskRecord(index + 1));

    (component as any).tasks.set(
      taskRecords.map((taskRecord) => (component as any).mapTaskRecord(taskRecord)),
    );
    (component as any).syncPagination();
    fixture.detectChanges();

    let compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelectorAll('.task-row').length).toBe(6);
    expect(compiled.querySelector('.task-list-pagination__page')?.textContent).toContain(
      'Pagina 1 de 2',
    );

    (compiled.querySelector('.task-list-pagination__button--next') as HTMLButtonElement).click();
    fixture.detectChanges();

    compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelectorAll('.task-row').length).toBe(1);
    expect(compiled.querySelector('.task-list-pagination__page')?.textContent).toContain(
      'Pagina 2 de 2',
    );
    expect(compiled.textContent).toContain('Tarefa 7');
  });
});
