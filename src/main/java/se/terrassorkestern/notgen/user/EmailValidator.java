package se.terrassorkestern.notgen.user;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;


public class EmailValidator implements ConstraintValidator<ValidEmail, String> {

    private static final String EMAIL_PATTERN = "^[_A-Za-z0-9-+]+(.[_A-Za-z0-9-]+)*@"
            + "[A-Za-z0-9-]+(.[A-Za-z0-9]+)*(.[A-Za-z]{2,})$";

    @Override
    public boolean isValid(String email, ConstraintValidatorContext context) {
        if (email == null) {
            // Ok with null as Oauth2 users have this field disabled, and then it sends null
            return true;
        } else {
            return (validateEmail(email));
        }
    }

    public boolean validateEmail(String email) {
        var pattern = Pattern.compile(EMAIL_PATTERN);
        var matcher = pattern.matcher(email);
        return matcher.matches();
    }
}