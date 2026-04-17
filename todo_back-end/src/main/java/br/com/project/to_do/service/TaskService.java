package br.com.project.to_do.service;

import br.com.project.to_do.dto.TaskListQueryDTO;
import br.com.project.to_do.dto.TaskRequestDTO;
import br.com.project.to_do.dto.TaskStatusFilter;
import br.com.project.to_do.exception.BusinessRuleException;
import br.com.project.to_do.exception.ResourceNotFoundException;
import br.com.project.to_do.model.Member;
import br.com.project.to_do.model.Task;
import br.com.project.to_do.repository.TaskRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskService.class);
    private static final Sort DEFAULT_TASK_SORT = Sort.by(Sort.Order.desc("id"));

    private final TaskRepository taskRepository;

    public List<Task> listar(Member member) {
        log.debug("Listando tarefas do membro {}", member.getId());
        return taskRepository.findAll(byMember(member), DEFAULT_TASK_SORT);
    }

    public Page<Task> listar(Member member, TaskListQueryDTO taskListQuery) {
        validateTaskListQuery(taskListQuery);

        Pageable pageable = PageRequest.of(
                taskListQuery.resolvedPage(),
                taskListQuery.resolvedSize(),
                DEFAULT_TASK_SORT
        );
        Specification<Task> specification = buildTaskListSpecification(member, taskListQuery);

        log.debug(
                "Listando tarefas paginadas do membro {} com page={}, size={}, query='{}', status='{}'",
                member.getId(),
                pageable.getPageNumber(),
                pageable.getPageSize(),
                taskListQuery.normalizedQuery(),
                taskListQuery.status()
        );

        return taskRepository.findAll(specification, pageable);
    }

    public Task salvar(TaskRequestDTO taskRequestDTO, Member member) {
        Task task = buildTask(taskRequestDTO, new Task());
        task.setMember(member);
        log.info("Criando tarefa '{}' para o membro {}", task.getNameTask(), member.getId());
        return taskRepository.save(task);
    }

    public Task update(long id, TaskRequestDTO taskAtualizada, Member member) {
        Task taskBanco = buscarTarefaDoUsuario(id, member);
        buildTask(taskAtualizada, taskBanco);
        log.info("Atualizando tarefa {} do membro {}", id, member.getId());
        return taskRepository.save(taskBanco);
    }

    public Task concluir(long id, Member member) {
        Task taskDone = buscarTarefaDoUsuario(id, member);
        taskDone.setDone(true);
        log.info("Concluindo tarefa {} do membro {}", id, member.getId());
        return taskRepository.save(taskDone);
    }

    public void deletar(long id, Member member) {
        Task task = buscarTarefaDoUsuario(id, member);
        log.info("Excluindo tarefa {} do membro {}", id, member.getId());
        taskRepository.delete(task);
    }

    private Task buscarTarefaDoUsuario(long id, Member member) {
        return taskRepository.findByIdAndMemberId(id, member.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Tarefa não encontrada."));
    }

    private void validateTaskListQuery(TaskListQueryDTO taskListQuery) {
        if (taskListQuery.page() != null && taskListQuery.page() < 0) {
            throw new BusinessRuleException("O parametro page deve ser maior ou igual a 0.");
        }

        if (taskListQuery.size() != null && taskListQuery.size() < 1) {
            throw new BusinessRuleException("O parametro size deve ser maior que 0.");
        }

        if (taskListQuery.dueDateFrom() != null
                && taskListQuery.dueDateTo() != null
                && taskListQuery.dueDateFrom().isAfter(taskListQuery.dueDateTo())) {
            throw new BusinessRuleException("O intervalo de datas informado e invalido.");
        }
    }

    private Specification<Task> buildTaskListSpecification(Member member, TaskListQueryDTO taskListQuery) {
        Specification<Task> specification = byMember(member);
        String normalizedQuery = taskListQuery.normalizedQuery();
        String normalizedCategory = taskListQuery.normalizedCategory();
        String normalizedPriority = taskListQuery.normalizedPriority();
        TaskStatusFilter statusFilter = taskListQuery.resolvedStatus();

        if (normalizedQuery != null) {
            specification = specification.and(containsText(normalizedQuery));
        }

        if (normalizedCategory != null) {
            specification = specification.and(categoryContains(normalizedCategory));
        }

        if (normalizedPriority != null) {
            specification = specification.and(priorityEquals(normalizedPriority));
        }

        if (taskListQuery.done() != null) {
            specification = specification.and(doneEquals(taskListQuery.done()));
        }

        if (statusFilter != null) {
            specification = specification.and(statusEquals(statusFilter));
        }

        if (taskListQuery.dueDateFrom() != null) {
            specification = specification.and(dueDateFrom(taskListQuery.dueDateFrom()));
        }

        if (taskListQuery.dueDateTo() != null) {
            specification = specification.and(dueDateTo(taskListQuery.dueDateTo()));
        }

        return specification;
    }

    private Task buildTask(TaskRequestDTO taskRequestDTO, Task task) {
        task.setNameTask(taskRequestDTO.name().trim());
        task.setDescription(normalize(taskRequestDTO.description()));
        task.setCategory(normalize(taskRequestDTO.category()));
        task.setPriority(taskRequestDTO.priority().trim());
        task.setDueDate(taskRequestDTO.dueDate());
        task.setDone(taskRequestDTO.done());
        return task;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }

    private Specification<Task> byMember(Member member) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("member"), member);
    }

    private Specification<Task> containsText(String query) {
        String likePattern = "%" + query.toLowerCase(Locale.ROOT) + "%";

        return (root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.or(
                criteriaBuilder.like(criteriaBuilder.lower(root.get("nameTask")), likePattern),
                criteriaBuilder.like(
                        criteriaBuilder.lower(criteriaBuilder.coalesce(root.get("description"), "")),
                        likePattern
                ),
                criteriaBuilder.like(
                        criteriaBuilder.lower(criteriaBuilder.coalesce(root.get("category"), "")),
                        likePattern
                )
        );
    }

    private Specification<Task> categoryContains(String category) {
        String likePattern = "%" + category.toLowerCase(Locale.ROOT) + "%";

        return (root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.like(
                criteriaBuilder.lower(criteriaBuilder.coalesce(root.get("category"), "")),
                likePattern
        );
    }

    private Specification<Task> priorityEquals(String priority) {
        return (root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.equal(
                criteriaBuilder.lower(root.get("priority")),
                priority.toLowerCase(Locale.ROOT)
        );
    }

    private Specification<Task> doneEquals(boolean done) {
        return (root, criteriaQuery, criteriaBuilder) -> done
                ? criteriaBuilder.isTrue(root.get("done"))
                : criteriaBuilder.isFalse(root.get("done"));
    }

    private Specification<Task> dueDateFrom(LocalDate dueDate) {
        return (root, criteriaQuery, criteriaBuilder) ->
                criteriaBuilder.greaterThanOrEqualTo(root.get("dueDate"), dueDate);
    }

    private Specification<Task> dueDateTo(LocalDate dueDate) {
        return (root, criteriaQuery, criteriaBuilder) ->
                criteriaBuilder.lessThanOrEqualTo(root.get("dueDate"), dueDate);
    }

    private Specification<Task> statusEquals(TaskStatusFilter statusFilter) {
        LocalDate today = LocalDate.now();

        return (root, criteriaQuery, criteriaBuilder) -> switch (statusFilter) {
            case HOJE -> criteriaBuilder.and(
                    criteriaBuilder.isFalse(root.get("done")),
                    criteriaBuilder.isNotNull(root.get("dueDate")),
                    criteriaBuilder.lessThanOrEqualTo(root.get("dueDate"), today)
            );
            case EM_ANDAMENTO -> criteriaBuilder.and(
                    criteriaBuilder.isFalse(root.get("done")),
                    criteriaBuilder.isNull(root.get("dueDate"))
            );
            case PLANEJADA -> criteriaBuilder.and(
                    criteriaBuilder.isFalse(root.get("done")),
                    criteriaBuilder.isNotNull(root.get("dueDate")),
                    criteriaBuilder.greaterThan(root.get("dueDate"), today)
            );
            case CONCLUIDA -> criteriaBuilder.isTrue(root.get("done"));
        };
    }
}
