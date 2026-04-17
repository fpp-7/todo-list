import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TaskForm } from './task-form';

describe('TaskForm', () => {
  let component: TaskForm;
  let fixture: ComponentFixture<TaskForm>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TaskForm],
    }).compileComponents();

    fixture = TestBed.createComponent(TaskForm);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should render the task title field', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Título');
  });

  it('should keep the clicked priority marked as selected', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const findPriorityChip = (priority: string) =>
      Array.from(compiled.querySelectorAll<HTMLButtonElement>('.task-form-card__chip')).find(
        (chip) => chip.dataset['priority'] === priority,
      ) ?? null;

    const mediumChip = findPriorityChip('Média');
    const highChip = findPriorityChip('Alta');

    expect(mediumChip?.getAttribute('aria-pressed')).toBe('true');
    expect(highChip?.getAttribute('aria-pressed')).toBe('false');

    highChip?.click();
    fixture.detectChanges();

    expect(highChip?.getAttribute('aria-pressed')).toBe('true');
    expect(highChip?.classList.contains('task-form-card__chip--active')).toBeTrue();
    expect(mediumChip?.getAttribute('aria-pressed')).toBe('false');
  });
});

