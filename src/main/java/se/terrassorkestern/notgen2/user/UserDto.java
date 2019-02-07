package se.terrassorkestern.notgen2.user;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Data
@PasswordMatches(message = "Lösenorden matchar inte")
class UserDto {
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