package vn.iotstar.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import vn.iotstar.dto.RegisterForm;
import vn.iotstar.entity.AccountOtp;
import vn.iotstar.entity.User;
import vn.iotstar.exception.OtpVerificationException;
import vn.iotstar.repository.AccountOtpRepository;
import vn.iotstar.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AccountAuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountOtpRepository otpRepository;

    @Mock
    private EmailService emailService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private AccountAuthService accountAuthService;

    @BeforeEach
    void setUp() {
        accountAuthService = new AccountAuthService(userRepository, otpRepository, passwordEncoder, emailService,
                Clock.fixed(Instant.parse("2026-11-16T07:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void registrationCreatesInactiveUserAndStoresOnlyOtpHash() {
        RegisterForm form = registrationForm();
        when(userRepository.existsByUserNameIgnoreCase("student")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("student@example.com")).thenReturn(false);
        when(userRepository.existsByPhone("0900000000")).thenReturn(false);
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(7);
            return saved;
        });
        when(otpRepository.findFirstByUser_IdAndPurposeOrderByCreatedAtDesc(7, "ACTIVATION"))
                .thenReturn(Optional.empty());

        User user = accountAuthService.register(form);

        assertFalse(user.getActive());
        assertTrue(passwordEncoder.matches(form.getPassword(), user.getPassWord()));
        ArgumentCaptor<AccountOtp> otpCaptor = ArgumentCaptor.forClass(AccountOtp.class);
        verify(otpRepository).save(otpCaptor.capture());
        AccountOtp otp = otpCaptor.getValue();
        assertEquals(0, otp.getAttempts());
        assertFalse(otp.getCodeHash().matches("\\d{6}"));
        assertNotEquals("000000", otp.getCodeHash());
        verify(emailService).sendOtp(org.mockito.ArgumentMatchers.eq("student@example.com"),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.matches("\\d{6}"),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void invalidOtpIncrementsAttemptsAndReportsRemainingAttempts() {
        User user = new User();
        user.setId(9);
        user.setEmail("owner@example.com");
        user.setActive(false);
        AccountOtp otp = new AccountOtp();
        otp.setUser(user);
        otp.setPurpose("ACTIVATION");
        otp.setCodeHash(passwordEncoder.encode("123456"));
        otp.setCreatedAt(java.time.LocalDateTime.parse("2026-11-16T06:59:00"));
        otp.setLastSentAt(otp.getCreatedAt());
        otp.setExpiresAt(java.time.LocalDateTime.parse("2026-11-16T07:04:00"));
        otp.setAttempts(1);
        when(userRepository.findByEmailIgnoreCase("owner@example.com")).thenReturn(Optional.of(user));
        when(otpRepository.findFirstByUser_IdAndPurposeOrderByCreatedAtDesc(9, "ACTIVATION"))
                .thenReturn(Optional.of(otp));

        OtpVerificationException error = assertThrows(OtpVerificationException.class,
                () -> accountAuthService.activate("owner@example.com", "000000"));

        assertEquals(2, otp.getAttempts());
        assertTrue(error.getMessage().contains("3 lần thử"));
        assertFalse(user.getActive());
    }

    @Test
    void validActivationOtpActivatesAccountAndIsMarkedUsed() {
        User user = new User();
        user.setId(10);
        user.setEmail("owner@example.com");
        user.setActive(false);
        AccountOtp otp = new AccountOtp();
        otp.setUser(user);
        otp.setPurpose("ACTIVATION");
        otp.setCodeHash(passwordEncoder.encode("654321"));
        otp.setCreatedAt(java.time.LocalDateTime.parse("2026-11-16T06:59:00"));
        otp.setLastSentAt(otp.getCreatedAt());
        otp.setExpiresAt(java.time.LocalDateTime.parse("2026-11-16T07:04:00"));
        otp.setAttempts(0);
        when(userRepository.findByEmailIgnoreCase("owner@example.com")).thenReturn(Optional.of(user));
        when(otpRepository.findFirstByUser_IdAndPurposeOrderByCreatedAtDesc(10, "ACTIVATION"))
                .thenReturn(Optional.of(otp));

        accountAuthService.activate("owner@example.com", "654321");

        assertTrue(user.getActive());
        assertEquals(java.time.LocalDateTime.parse("2026-11-16T07:00:00"), otp.getUsedAt());
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
