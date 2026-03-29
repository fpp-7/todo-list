package br.com.project.to_do.repository;

import br.com.project.to_do.model.Member;
import br.com.project.to_do.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findAllByMember(Member member);
}
