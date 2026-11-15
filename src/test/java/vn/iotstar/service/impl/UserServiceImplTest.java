package vn.iotstar.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import vn.iotstar.dto.RegisterForm;
import vn.iotstar.entity.User;
import vn.iotstar.exception.DuplicateUserFieldException;
import vn.iotstar.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void registrationAlwaysCreatesUserRoleAndHashesPassword() {
        UserServiceImpl service = new UserServiceImpl(userRepository, new BCryptPasswordEncoder());
        RegisterForm form = registrationForm();
        when(userRepository.existsByUserNameIgnoreCase("student")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("student@example.com")).thenReturn(false);
        when(userRepository.existsByPhone("0900000000")).thenReturn(false);
        when(userRepository.save(org.mockito.ArgumentMatchers.any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User created = service.register(form);

        assertEquals(5, created.getRoleid());
        assertNotEquals(form.getPassword(), created.getPassWord());
        org.junit.jupiter.api.Assertions.assertTrue(new BCryptPasswordEncoder()
                .matches(form.getPassword(), created.getPassWord()));
        verify(userRepository).save(created);
    }

    @Test
    void registrationRejectsDuplicateUsername() {
        RegisterForm form = registrationForm();
        when(userRepository.existsByUserNameIgnoreCase("student")).thenReturn(true);

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
