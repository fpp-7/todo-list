package br.com.project.to_do.repository;

import br.com.project.to_do.model.Member;
import br.com.project.to_do.model.Task;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findAllByMember(Member member);

    Optional<Task> findByIdAndMemberId(Long id, Long memberId);
}
