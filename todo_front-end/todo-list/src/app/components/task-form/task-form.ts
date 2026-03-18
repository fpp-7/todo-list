import { Component, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TaskPriority, TaskUpsertPayload } from '../../core/tasks/task-api.service';

@Component({
  selector: 'app-task-form',
  imports: [FormsModule],
  templateUrl: './task-form.html',
  styleUrl: './task-form.css',
})
export class TaskForm {
  protected readonly priorities: readonly TaskPriority[] = ['Baixa', 'Média', 'Alta'];
  readonly submitting = input(false);
  readonly taskSubmitted = output<TaskUpsertPayload>();
  protected readonly selectedPriority = signal<TaskPriority>('Média');

  protected title = '';
  protected category = 'Pessoal';
  protected dueDate = '';
  protected notes = '';

  protected setPriority(priority: TaskPriority): void {
    this.selectedPriority.set(priority);
  }

  protected submitForm(): void {
    const normalizedTitle = this.title.trim();

    if (!normalizedTitle) {
      return;
    }

    this.taskSubmitted.emit({
      name: normalizedTitle,
      description: this.notes.trim(),
      category: this.category.trim() || 'Pessoal',
      priority: this.selectedPriority(),
      dueDate: this.dueDate || null,
      done: false,
    });

    this.resetForm();
  }

  private resetForm(): void {
    this.title = '';
    this.category = 'Pessoal';
    this.dueDate = '';
    this.notes = '';
    this.selectedPriority.set('Média');
  }
}

