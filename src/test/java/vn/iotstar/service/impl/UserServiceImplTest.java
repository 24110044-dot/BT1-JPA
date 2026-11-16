package vn.iotstar.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import vn.iotstar.dto.RegisterForm;
import vn.iotstar.entity.User;
import vn.iotstar.exception.DuplicateUserFieldException;
import vn.iotstar.repository.UserRepository;
import vn.iotstar.repository.VideoRepository;
import vn.iotstar.repository.AccountOtpRepository;
import vn.iotstar.service.AccountAuthService;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountAuthService accountAuthService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private VideoRepository videoRepository;

    @Mock
    private AccountOtpRepository accountOtpRepository;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void registrationDelegatesToOtpAccountFlow() {
        RegisterForm form = registrationForm();
        User user = new User();
        user.setRoleid(5);
        user.setPassWord(new BCryptPasswordEncoder().encode(form.getPassword()));
        when(accountAuthService.register(form)).thenReturn(user);

        User created = userService.register(form);

        assertEquals(5, created.getRoleid());
        org.junit.jupiter.api.Assertions.assertNotEquals(form.getPassword(), created.getPassWord());
        verify(accountAuthService).register(form);
    }

    @Test
    void registrationRejectsDuplicateUsername() {
        RegisterForm form = registrationForm();
        when(accountAuthService.register(form)).thenThrow(new DuplicateUserFieldException("userName", "duplicate"));

        assertThrows(DuplicateUserFieldException.class, () -> userService.register(form));
    }

    private RegisterForm registrationForm() {
        RegisterForm form = new RegisterForm();
        form.setUserName("student");
        form.setEmail("student@example.com");
        form.setFullName("Student Name");
        form.setPhone("0900000000");
        form.setPassword("StrongPass123");
        form.setConfirmPassword("StrongPass123");
        return form;
    }
}
