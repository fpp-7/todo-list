package br.com.project.to_do.repository;

import br.com.project.to_do.model.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {
}
