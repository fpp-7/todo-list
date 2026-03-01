package br.com.project.to_do.controller;
import br.com.project.to_do.model.Task;
import br.com.project.to_do.service.TaskService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/task")
public class TaskController {
//
    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public List<Task> listarTasks () {
        return taskService.listar();
    }

    @PutMapping("/{id}")
    public Task updateTask (@PathVariable long id, @RequestBody Task task) {
        return taskService.update(id, task);
    }

    @PostMapping
    public Task inserirTask (@RequestBody Task task) {
        return taskService.salvar(task);
    }

    @PutMapping("/concluir/{id}")
    public Task concluirTask (@PathVariable long id) {
        return taskService.concluir(id);
    }

    @DeleteMapping("/{id}")
    public void deleteTask (@PathVariable long id) {
        taskService.deletar(id);
    }

}
