package br.com.project.to_do.controller;

import br.com.project.to_do.dto.TaskListQueryDTO;
import br.com.project.to_do.dto.TaskPageResponseDTO;
import br.com.project.to_do.dto.TaskRequestDTO;
import br.com.project.to_do.dto.TaskResponseDTO;
import br.com.project.to_do.model.Member;
import br.com.project.to_do.service.TaskService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/task")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    public Object listarTasks(
            @AuthenticationPrincipal Member member,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) Boolean done,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDateTo
    ) {
        TaskListQueryDTO taskListQuery = new TaskListQueryDTO(
                page,
                size,
                query,
                category,
                priority,
                done,
                status,
                dueDateFrom,
                dueDateTo
        );

        if (!taskListQuery.hasAdvancedQuery()) {
            return taskService.listar(member).stream().map(TaskResponseDTO::fromEntity).toList();
        }

        return TaskPageResponseDTO.fromPage(taskService.listar(member, taskListQuery));
    }

    @PutMapping("/{id}")
    public TaskResponseDTO updateTask(
            @PathVariable long id,
            @RequestBody @Valid TaskRequestDTO task,
            @AuthenticationPrincipal Member member
    ) {
        return TaskResponseDTO.fromEntity(taskService.update(id, task, member));
    }

    @PostMapping
    public TaskResponseDTO inserirTask(
            @RequestBody @Valid TaskRequestDTO task,
            @AuthenticationPrincipal Member member
    ) {
        return TaskResponseDTO.fromEntity(taskService.salvar(task, member));
    }

    @PutMapping("/concluir/{id}")
    public TaskResponseDTO concluirTask(@PathVariable long id, @AuthenticationPrincipal Member member) {
        return TaskResponseDTO.fromEntity(taskService.concluir(id, member));
    }

    @DeleteMapping("/{id}")
    public void deleteTask(@PathVariable long id, @AuthenticationPrincipal Member member) {
        taskService.deletar(id, member);
    }
}
