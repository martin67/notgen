package se.terrassorkestern.notgen.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import se.terrassorkestern.notgen.user.AuthProvider;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "user_")             // user is a reserved name in H2...
public class User {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @ManyToOne
    @JoinColumn(name = "band_id")
    private Band band;
    private String username;
    private String password;
    private String fullName;
    private String displayName;
    private String email;
    private String imageUrl;
    private AuthProvider provider;
    private String providerId;
    private boolean enabled;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_role",
            joinColumns = @JoinColumn(name = "user_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "role_id", referencedColumnName = "id"))
    private Collection<Role> roles = new HashSet<>();

    public String getRoleNames() {
        StringBuilder sb = new StringBuilder();
        for (Role role : roles) {
            sb.append(role.getName()).append(" ");
        }
        return sb.toString();
    }

    public boolean isRemoteUser() {
        return (provider != AuthProvider.local);
    }

    public boolean isLocalUser() {
        return (provider == AuthProvider.local);
    }

    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();
        for (Role role : roles) {
            authorities.add(new SimpleGrantedAuthority(role.getName()));
            role.getPrivileges().stream()
                    .map(p -> new SimpleGrantedAuthority(p.getName()))
                    .forEach(authorities::add);
        }
        return authorities;
    }

    public boolean isEnabled() {
        return enabled;
    }

}