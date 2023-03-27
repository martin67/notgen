package se.terrassorkestern.notgen.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import se.terrassorkestern.notgen.user.AuthProvider;

import java.util.*;
import java.util.stream.Collectors;

@Getter
@Setter
@Entity
@Table(name = "user_")             // user is a reserved name in H2...
public class User {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_band",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "band_id"))
    private Set<Band> bands = new HashSet<>();
    private String username;
    private String password;
    private String fullName;
    private String displayName;
    private String email;
    private String imageUrl;
    private AuthProvider provider;
    private String providerId;
    private boolean enabled;

    @ManyToOne
    private Role role;

    public boolean isRemoteUser() {
        return (provider != AuthProvider.local);
    }

    public boolean isLocalUser() {
        return (provider == AuthProvider.local);
    }

    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();
        if (role != null) {
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

    public String getBandNames() {
        return bands.stream().map(Band::getName).collect(Collectors.joining(", "));
    }
}