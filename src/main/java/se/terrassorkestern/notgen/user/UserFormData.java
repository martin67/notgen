package se.terrassorkestern.notgen.user;

import lombok.Data;
import se.terrassorkestern.notgen.model.Band;
import se.terrassorkestern.notgen.model.Role;
import se.terrassorkestern.notgen.model.User;

import java.util.Set;
import java.util.UUID;


@Data
@PasswordMatches(message = "Lösenorden matchar inte")
public class UserFormData {
    private UUID id;
    private String username;
    private String fullName;
    private String displayName;
    private String password;
    private String matchingPassword;
    private String email;
    private boolean enabled;
    private boolean remoteUser;
    private Role role;
    private Set<Band> bands;

    public UserFormData() {
    }

    public UserFormData(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.fullName = user.getFullName();
        this.displayName = user.getDisplayName();
        this.email = user.getEmail();
        this.enabled = user.isEnabled();
        this.remoteUser = user.isRemoteUser();
        this.role = user.getRole();
        this.bands = user.getBands();
    }
}