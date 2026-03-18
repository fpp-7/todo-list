import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { apiRoutes } from '../api/api-routes';

export type TaskPriority = 'Baixa' | 'Média' | 'Alta';

export type TaskApiRecord = {
  readonly id: number;
  readonly name: string;
  readonly description: string;
  readonly category: string | null;
  readonly priority: TaskPriority | null;
  readonly dueDate: string | null;
  readonly done: boolean;
};

export type TaskUpsertPayload = {
  readonly name: string;
  readonly description: string;
  readonly category: string;
  readonly priority: TaskPriority;
  readonly dueDate: string | null;
  readonly done: boolean;
};

@Injectable({
  providedIn: 'root',
})
export class TaskApiService {
  private readonly http = inject(HttpClient);

  listTasks(): Observable<TaskApiRecord[]> {
    return this.http.get<TaskApiRecord[]>(apiRoutes.tasks.list);
  }

  createTask(payload: TaskUpsertPayload): Observable<TaskApiRecord> {
    return this.http.post<TaskApiRecord>(apiRoutes.tasks.create, payload);
  }

  updateTask(id: number, payload: TaskUpsertPayload): Observable<TaskApiRecord> {
    return this.http.put<TaskApiRecord>(apiRoutes.tasks.update(id), payload);
  }

  completeTask(id: number): Observable<TaskApiRecord> {
    return this.http.put<TaskApiRecord>(apiRoutes.tasks.complete(id), {});
  }

  deleteTask(id: number): Observable<void> {
    return this.http.delete<void>(apiRoutes.tasks.delete(id));
  }
}

