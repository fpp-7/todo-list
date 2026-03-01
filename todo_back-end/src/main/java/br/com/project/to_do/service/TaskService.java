package br.com.project.to_do.service;


import br.com.project.to_do.model.Task;
import br.com.project.to_do.repository.TaskRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TaskService {

    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public List<Task> listar(){
        return taskRepository.findAll();
    }

    public Task salvar(Task tasks){
        return taskRepository.save(tasks);
    }

    public Task update(long id, Task tasks){
        Task taskUpdate = taskRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Tarefa nao encontrada"));
        taskUpdate.setDescription(tasks.getDescription());
        taskUpdate.setName(tasks.getName());
        return taskRepository.save(taskUpdate);
    }

    public Task concluir(long id){
        Task taskDone = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tarefa nao encontrada"));
        taskDone.setDone(true);
        return taskRepository.save(taskDone);
    }

    public void deletar(long id){
        taskRepository.deleteById(id);
    }

}
