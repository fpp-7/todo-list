import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { AccessApiService } from '../../core/auth/access-api.service';
import { ProfileApiService } from '../../core/profile/profile-api.service';
import { TaskApiService } from '../../core/tasks/task-api.service';
import { TaskList } from './task-list';

describe('TaskList', () => {
  let component: TaskList;
  let fixture: ComponentFixture<TaskList>;
  let accessApiService: jasmine.SpyObj<AccessApiService>;
  let taskApiService: jasmine.SpyObj<TaskApiService>;
  let profileApiService: jasmine.SpyObj<ProfileApiService>;

  const taskRecord = {
    id: 1,
    name: 'Nova tarefa',
    description: 'Descrição da tarefa',
    category: 'Pessoal',
    priority: 'Média' as const,
    dueDate: null,
    done: false,
  };

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

    taskApiService.listTasks.and.returnValue(of([taskRecord]));
    taskApiService.createTask.and.returnValue(of(taskRecord));
    taskApiService.completeTask.and.returnValue(of({ ...taskRecord, done: true }));
    taskApiService.updateTask.and.returnValue(of(taskRecord));
    taskApiService.deleteTask.and.returnValue(of(void 0));
    accessApiService.logout.and.returnValue(of({ status: 'SUCCESS', message: 'Sessao encerrada.' }));
    profileApiService.getProfile.and.returnValue(
      of({
        id: 1,
        email: 'usuario@empresa.com',
        displayName: 'Usuário Teste',
        photoDataUrl: null,
      }),
    );

    await TestBed.configureTestingModule({
      imports: [TaskList],
      providers: [
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
      ],
    }).compileComponents();

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
      'Usuário Teste',
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
});
