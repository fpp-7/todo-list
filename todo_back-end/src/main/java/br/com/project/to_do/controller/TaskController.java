package br.com.project.to_do.controller;

import br.com.project.to_do.dto.TaskRequestDTO;
import br.com.project.to_do.dto.TaskResponseDTO;
import br.com.project.to_do.model.Member;
import br.com.project.to_do.service.TaskService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/task")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    public List<TaskResponseDTO> listarTasks(@AuthenticationPrincipal Member member) {
        return taskService.listar(member).stream().map(TaskResponseDTO::fromEntity).toList();
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
