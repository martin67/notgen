package se.terrassorkestern.notgen.user;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;


@Data
@PasswordMatches(message = "Lösenorden matchar inte")
public class UserDto {
    @NotNull
    @NotEmpty
    private String username;

    @NotNull
    @NotEmpty(message = "Ange ett namn")
    private String fullname;

    //@NotNull
    //@NotEmpty
    private String password;
    private String matchingPassword;

    @ValidEmail(message = "E-postadressen är ogiltig")
    @NotNull
    @NotEmpty(message = "Ange en e-postadress")
    private String email;

    Boolean enabled;
}