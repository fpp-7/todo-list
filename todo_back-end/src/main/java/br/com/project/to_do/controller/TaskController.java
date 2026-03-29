package br.com.project.to_do.controller;

import br.com.project.to_do.model.Member;
import br.com.project.to_do.model.Task;
import br.com.project.to_do.service.TaskService;
import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = {"http://127.0.0.1:4200", "http://localhost:4200"})
@RequestMapping("/task")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public List<Task> listarTasks(@AuthenticationPrincipal Member member) {
        return taskService.listar(member);
    }

    @PutMapping("/{id}")
    public Task updateTask(@PathVariable long id, @RequestBody Task task, @AuthenticationPrincipal Member member) {
        return taskService.update(id, task, member);
    }

    @PostMapping
    public Task inserirTask(@RequestBody Task task, @AuthenticationPrincipal Member member) {
        return taskService.salvar(task, member);
    }

    @PutMapping("/concluir/{id}")
    public Task concluirTask(@PathVariable long id, @AuthenticationPrincipal Member member) {
        return taskService.concluir(id, member);
    }

    @DeleteMapping("/{id}")
    public void deleteTask(@PathVariable long id, @AuthenticationPrincipal Member member) {
        taskService.deletar(id, member);
    }
}
