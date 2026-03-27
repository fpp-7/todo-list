package br.com.project.to_do.service;

import br.com.project.to_do.model.Task;
import br.com.project.to_do.repository.TaskRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TaskService {

    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public List<Task> listar() {
        return taskRepository.findAll();
    }

    public Task salvar(Task task) {
        return taskRepository.save(task);
    }

    public Task update(long id) {
        Task taskUpdate = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tarefa nao encontrada"));
        return taskRepository.save(taskUpdate);
    }

    public Task concluir(long id) {
        Task taskDone = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tarefa nao encontrada"));
        taskDone.setDone(true);
        return taskRepository.save(taskDone);
    }

    public void deletar(long id) {
        taskRepository.deleteById(id);
    }
}
