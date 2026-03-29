package br.com.project.to_do.service;

import br.com.project.to_do.model.Member;
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

    public List<Task> listar(Member member) {
        return taskRepository.findAllByMember(member);
    }

    public Task salvar(Task task, Member member) {
        task.setMember(member);
        return taskRepository.save(task);
    }

    public Task update(long id, Task taskAtualizada, Member member) {
        // 1. Busca e valida o dono
        Task taskBanco = buscarTarefaDoUsuario(id, member);

        // 2. Atualiza apenas os campos permitidos (não atualizamos o ID nem o dono)
        taskBanco.setNameTask(taskAtualizada.getNameTask());
        taskBanco.setDescription(taskAtualizada.getDescription());
        taskBanco.setCategory(taskAtualizada.getCategory());
        taskBanco.setPriority(taskAtualizada.getPriority());
        taskBanco.setDueDate(taskAtualizada.getDueDate());

        return taskRepository.save(taskBanco);
    }
    public Task concluir(long id, Member member) {
        Task taskDone = buscarTarefaDoUsuario(id, member);
        taskDone.setDone(true);
        return taskRepository.save(taskDone);
    }

    public void deletar(long id, Member member) {
        Task task = buscarTarefaDoUsuario(id, member);
        taskRepository.delete(task);
    }

    private Task buscarTarefaDoUsuario(long id, Member member) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tarefa não encontrada"));

        return task;
    }

}
