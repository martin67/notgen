package se.terrassorkestern.notgen.user;


import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordMatchesValidator implements ConstraintValidator<PasswordMatches, Object> {

    @Override
    public void initialize(PasswordMatches constraintAnnotation) {
    }

    @Override
    public boolean isValid(Object obj, ConstraintValidatorContext context) {
        UserFormData user = (UserFormData) obj;
        // oauth2 users have a null password
        if (user.getPassword() == null && user.getMatchingPassword() == null) {
            return true;
        }
        return user.getPassword().equals(user.getMatchingPassword());
    }

}