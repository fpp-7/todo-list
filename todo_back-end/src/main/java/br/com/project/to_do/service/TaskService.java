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
        if (task.getCategory() == null || task.getCategory().isBlank()) {
            task.setCategory("Pessoal");
        }

        if (task.getPriority() == null || task.getPriority().isBlank()) {
            task.setPriority("Média");
        }

        return taskRepository.save(task);
    }

    public Task update(long id, Task task) {
        Task taskUpdate = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tarefa nao encontrada"));

        taskUpdate.setName(task.getName());
        taskUpdate.setDescription(task.getDescription());
        taskUpdate.setCategory(task.getCategory());
        taskUpdate.setPriority(task.getPriority());
        taskUpdate.setDueDate(task.getDueDate());
        taskUpdate.setDone(task.isDone());

        return salvar(taskUpdate);
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
