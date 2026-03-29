package br.com.project.to_do.repository;

import br.com.project.to_do.model.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public interface MemberRepository extends JpaRepository<Member, Long> {
    UserDetails findByLogin(String login) throws UsernameNotFoundException;
}
