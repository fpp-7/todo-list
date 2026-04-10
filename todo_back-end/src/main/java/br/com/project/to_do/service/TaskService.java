package br.com.project.to_do.service;

import br.com.project.to_do.dto.TaskRequestDTO;
import br.com.project.to_do.exception.ResourceNotFoundException;
import br.com.project.to_do.model.Member;
import br.com.project.to_do.model.Task;
import br.com.project.to_do.repository.TaskRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskService.class);

    private final TaskRepository taskRepository;

    public List<Task> listar(Member member) {
        log.debug("Listando tarefas do membro {}", member.getId());
        return taskRepository.findAllByMember(member);
    }

    public Task salvar(TaskRequestDTO taskRequestDTO, Member member) {
        Task task = buildTask(taskRequestDTO, new Task());
        task.setMember(member);
        log.info("Criando tarefa '{}' para o membro {}", task.getNameTask(), member.getId());
        return taskRepository.save(task);
    }

    public Task update(long id, TaskRequestDTO taskAtualizada, Member member) {
        Task taskBanco = buscarTarefaDoUsuario(id, member);
        buildTask(taskAtualizada, taskBanco);
        log.info("Atualizando tarefa {} do membro {}", id, member.getId());
        return taskRepository.save(taskBanco);
    }

    public Task concluir(long id, Member member) {
        Task taskDone = buscarTarefaDoUsuario(id, member);
        taskDone.setDone(true);
        log.info("Concluindo tarefa {} do membro {}", id, member.getId());
        return taskRepository.save(taskDone);
    }

    public void deletar(long id, Member member) {
        Task task = buscarTarefaDoUsuario(id, member);
        log.info("Excluindo tarefa {} do membro {}", id, member.getId());
        taskRepository.delete(task);
    }

    private Task buscarTarefaDoUsuario(long id, Member member) {
        return taskRepository.findByIdAndMemberId(id, member.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Tarefa não encontrada."));
    }

    private Task buildTask(TaskRequestDTO taskRequestDTO, Task task) {
        task.setNameTask(taskRequestDTO.name().trim());
        task.setDescription(normalize(taskRequestDTO.description()));
        task.setCategory(normalize(taskRequestDTO.category()));
        task.setPriority(taskRequestDTO.priority().trim());
        task.setDueDate(taskRequestDTO.dueDate());
        task.setDone(taskRequestDTO.done());
        return task;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }
}
