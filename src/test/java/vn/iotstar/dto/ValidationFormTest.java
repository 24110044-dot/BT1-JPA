package vn.iotstar.dto;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class ValidationFormTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void registerRejectsBlankFieldsAndInvalidEmail() {
        RegisterForm form = new RegisterForm();
        form.setUserName(" ");
        form.setFullName(" ");
        form.setEmail("not-an-email");
        form.setPhone(" ");
        form.setPassword("short");
        form.setConfirmPassword("different");

        Set<ConstraintViolation<RegisterForm>> violations = validator.validate(form);

        assertFalse(violations.isEmpty());
    }

    @Test
    void resetPasswordRejectsShortPassword() {
        ResetPasswordForm form = new ResetPasswordForm();
        form.setPassword("123");
        form.setConfirmPassword("123");

        assertFalse(validator.validate(form).isEmpty());
    }

    @Test
    void otpRequiresSixDigits() {
        OtpForm form = new OtpForm();
        form.setEmail("member@example.com");
        form.setCode("12ab56");

        assertFalse(validator.validate(form).isEmpty());
    }

    @Test
    void productRejectsNegativeValuesAndMissingCategory() {
        ProductForm form = new ProductForm();
        form.setProductName(" ");
        form.setPrice(new BigDecimal("-1"));
        form.setQuantity(-1);

        Set<ConstraintViolation<ProductForm>> violations = validator.validate(form);

        assertFalse(violations.isEmpty());
    }

    @Test
    void forgotPasswordAcceptsValidEmail() {
        ForgotPasswordForm form = new ForgotPasswordForm();
        form.setEmail("member@example.com");

        assertTrue(validator.validate(form).isEmpty());
    }
}
