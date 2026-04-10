import { type TaskApiRecord, type TaskPriority } from '../../core/tasks/task-api.service';

export type TaskFilter = 'Todas' | 'Hoje' | 'Em andamento' | 'Planejadas' | 'Concluídas';
export type TaskStatus = 'Hoje' | 'Em andamento' | 'Planejada' | 'Concluída';

export type TaskItem = {
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

export type TaskChanges = Partial<
  Pick<TaskItem, 'title' | 'description' | 'category' | 'status' | 'priority'>
>;

const today = new Date();

export const todayIso = today.toISOString().slice(0, 10);
export const tomorrowIso = new Date(today.getFullYear(), today.getMonth(), today.getDate() + 1)
  .toISOString()
  .slice(0, 10);

const nextWeekIso = new Date(today.getFullYear(), today.getMonth(), today.getDate() + 7)
  .toISOString()
  .slice(0, 10);

export const fallbackTasks: readonly TaskApiRecord[] = [
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
