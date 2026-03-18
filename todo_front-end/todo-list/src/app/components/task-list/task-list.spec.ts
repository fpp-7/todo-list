import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { TaskApiService } from '../../core/tasks/task-api.service';
import { TaskList } from './task-list';

describe('TaskList', () => {
  let component: TaskList;
  let fixture: ComponentFixture<TaskList>;
  let taskApiService: jasmine.SpyObj<TaskApiService>;

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
    taskApiService = jasmine.createSpyObj<TaskApiService>('TaskApiService', [
      'listTasks',
      'createTask',
      'completeTask',
      'updateTask',
      'deleteTask',
    ]);

    taskApiService.listTasks.and.returnValue(of([taskRecord]));
    taskApiService.createTask.and.returnValue(of(taskRecord));
    taskApiService.completeTask.and.returnValue(of({ ...taskRecord, done: true }));
    taskApiService.updateTask.and.returnValue(of(taskRecord));
    taskApiService.deleteTask.and.returnValue(of(void 0));

    await TestBed.configureTestingModule({
      imports: [TaskList],
      providers: [
        {
          provide: TaskApiService,
          useValue: taskApiService,
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

  it('should render the default profile name', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.task-topbar__user strong')?.textContent).toContain('Usuário');
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
});
