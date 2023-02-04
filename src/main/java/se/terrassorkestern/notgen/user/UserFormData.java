package se.terrassorkestern.notgen.user;

import lombok.Data;
import se.terrassorkestern.notgen.model.User;


@Data
@PasswordMatches(message = "Lösenorden matchar inte")
public class UserFormData {
    private long id;
    private String username;
    private String fullName;
    private String displayName;
    private String password;
    private String matchingPassword;
    private String email;
    private boolean enabled;
    private boolean remoteUser;

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
    }
}