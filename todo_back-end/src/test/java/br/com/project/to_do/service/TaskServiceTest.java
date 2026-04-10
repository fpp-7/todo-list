package br.com.project.to_do.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.project.to_do.dto.TaskRequestDTO;
import br.com.project.to_do.exception.ResourceNotFoundException;
import br.com.project.to_do.model.Member;
import br.com.project.to_do.model.Task;
import br.com.project.to_do.repository.TaskRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
                "Descrição",
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
    void shouldUpdateOnlyOwnedTask() {
        Task task = new Task();
        task.setId(10L);
        task.setMember(member);
        task.setNameTask("Antiga");

        TaskRequestDTO requestDTO = new TaskRequestDTO(
                "Atualizada",
                "  Novo contexto  ",
                "  Trabalho ",
                "Média",
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
}
