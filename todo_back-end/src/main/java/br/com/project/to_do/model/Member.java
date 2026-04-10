package br.com.project.to_do.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Collection;
import java.util.List;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Data
@Entity
@Table(name = "member")
public class Member implements UserDetails {

    public Member(String login, String password, String displayName) {
        this.login = login;
        this.password = password;
        this.displayName = displayName;
    }

    public Member() {
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public String getUsername() {
        return login;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(unique = true, nullable = false, length = 160)
    private String login;

    @Column(nullable = false, length = 100)
    private String password;

    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;

    @Column(name = "photo_data_url", columnDefinition = "TEXT")
    private String photoDataUrl;
}
