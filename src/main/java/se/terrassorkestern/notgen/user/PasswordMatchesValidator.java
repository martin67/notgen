package se.terrassorkestern.notgen.user;


import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordMatchesValidator implements ConstraintValidator<PasswordMatches, Object> {

    @Override
    public boolean isValid(Object obj, ConstraintValidatorContext context) {
        var user = (UserFormData) obj;
        // oauth2 users have a null password
        if (user.getPassword() == null && user.getMatchingPassword() == null) {
            return true;
        }
        return user.getPassword().equals(user.getMatchingPassword());
    }

}