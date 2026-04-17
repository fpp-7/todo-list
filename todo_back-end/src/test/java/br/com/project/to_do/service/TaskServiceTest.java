package br.com.project.to_do.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.project.to_do.dto.TaskListQueryDTO;
import br.com.project.to_do.dto.TaskRequestDTO;
import br.com.project.to_do.exception.BusinessRuleException;
import br.com.project.to_do.exception.ResourceNotFoundException;
import br.com.project.to_do.model.Member;
import br.com.project.to_do.model.Task;
import br.com.project.to_do.repository.TaskRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    private TaskService taskService;
    private Member member;

    @BeforeEach
    void setUp() {
        taskService = new TaskService(taskRepository);
        member = new Member("user@example.com", "encoded", "User Example");
        member.setId(7L);
    }

    @Test
    void shouldCreateTaskForAuthenticatedMember() {
        TaskRequestDTO requestDTO = new TaskRequestDTO(
                "Nova tarefa",
                "DescriÃ§Ã£o",
                "Pessoal",
                "Alta",
                LocalDate.parse("2026-04-10"),
                false
        );

        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Task saved = taskService.salvar(requestDTO, member);

        assertThat(saved.getMember()).isEqualTo(member);
        assertThat(saved.getNameTask()).isEqualTo("Nova tarefa");
        assertThat(saved.getPriority()).isEqualTo("Alta");
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    void shouldTrimAndNormalizeOptionalFieldsWhenCreatingTask() {
        TaskRequestDTO requestDTO = new TaskRequestDTO(
                "  Nova tarefa  ",
                "   ",
                "   ",
                "  Alta  ",
                null,
                false
        );

        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Task saved = taskService.salvar(requestDTO, member);

        assertThat(saved.getNameTask()).isEqualTo("Nova tarefa");
        assertThat(saved.getDescription()).isNull();
        assertThat(saved.getCategory()).isNull();
        assertThat(saved.getPriority()).isEqualTo("Alta");
    }

    @Test
    void shouldUpdateOnlyOwnedTask() {
        Task task = new Task();
        task.setId(10L);
        task.setMember(member);
        task.setNameTask("Antiga");

        TaskRequestDTO requestDTO = new TaskRequestDTO(
                "Atualizada",
                "  Novo contexto  ",
                "  Trabalho ",
                "MÃ©dia",
                LocalDate.parse("2026-04-12"),
                true
        );

        when(taskRepository.findByIdAndMemberId(10L, 7L)).thenReturn(Optional.of(task));
        when(taskRepository.save(task)).thenReturn(task);

        Task updated = taskService.update(10L, requestDTO, member);

        assertThat(updated.getNameTask()).isEqualTo("Atualizada");
        assertThat(updated.getDescription()).isEqualTo("Novo contexto");
        assertThat(updated.getCategory()).isEqualTo("Trabalho");
        assertThat(updated.isDone()).isTrue();
    }

    @Test
    void shouldListOnlyTasksFromAuthenticatedMember() {
        Task firstTask = new Task();
        Task secondTask = new Task();
        List<Task> tasks = List.of(firstTask, secondTask);

        when(taskRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(tasks);

        List<Task> listedTasks = taskService.listar(member);

        assertThat(listedTasks).containsExactly(firstTask, secondTask);
        verify(taskRepository).findAll(any(Specification.class), any(Sort.class));
    }

    @Test
    void shouldListTasksUsingPaginationAndFilters() {
        Task task = new Task();
        task.setId(30L);
        task.setMember(member);
        Page<Task> page = new PageImpl<>(List.of(task));
        TaskListQueryDTO queryDTO = new TaskListQueryDTO(
                1,
                5,
                "planejar",
                "Produto",
                "Alta",
                false,
                "planejada",
                null,
                null
        );

        when(taskRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        Page<Task> listedTasks = taskService.listar(member, queryDTO);

        assertThat(listedTasks.getContent()).containsExactly(task);
        verify(taskRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void shouldRejectInvalidDateRangeWhenListingTasksWithFilters() {
        TaskListQueryDTO queryDTO = new TaskListQueryDTO(
                0,
                10,
                null,
                null,
                null,
                null,
                null,
                LocalDate.parse("2026-04-20"),
                LocalDate.parse("2026-04-10")
        );

        assertThatThrownBy(() -> taskService.listar(member, queryDTO))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("O intervalo de datas informado e invalido.");
    }

    @Test
    void shouldThrowWhenTaskDoesNotBelongToAuthenticatedMember() {
        TaskRequestDTO requestDTO = new TaskRequestDTO(
                "Tarefa",
                null,
                null,
                "Baixa",
                null,
                false
        );

        when(taskRepository.findByIdAndMemberId(99L, 7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.update(99L, requestDTO, member))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Tarefa não encontrada.");
    }

    @Test
    void shouldCompleteOwnedTask() {
        Task task = new Task();
        task.setId(15L);
        task.setMember(member);
        task.setDone(false);

        when(taskRepository.findByIdAndMemberId(15L, 7L)).thenReturn(Optional.of(task));
        when(taskRepository.save(task)).thenReturn(task);

        Task completed = taskService.concluir(15L, member);

        assertThat(completed.isDone()).isTrue();
        verify(taskRepository).save(task);
    }

    @Test
    void shouldDeleteOnlyOwnedTask() {
        Task task = new Task();
        task.setId(21L);
        task.setMember(member);

        when(taskRepository.findByIdAndMemberId(21L, 7L)).thenReturn(Optional.of(task));

        taskService.deletar(21L, member);

        verify(taskRepository).delete(task);
    }
}
