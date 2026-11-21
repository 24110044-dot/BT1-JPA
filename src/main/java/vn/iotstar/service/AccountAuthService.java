package vn.iotstar.service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.iotstar.dto.RegisterForm;
import vn.iotstar.entity.AccountOtp;
import vn.iotstar.entity.User;
import vn.iotstar.exception.DuplicateUserFieldException;
import vn.iotstar.exception.EmailDeliveryException;
import vn.iotstar.exception.OtpVerificationException;
import vn.iotstar.exception.UserNotFoundException;
import vn.iotstar.repository.AccountOtpRepository;
import vn.iotstar.repository.UserRepository;

@Service
@Transactional
public class AccountAuthService {

    private static final String ACTIVATION = "ACTIVATION";
    private static final String PASSWORD_RESET = "PASSWORD_RESET";
    public static final String EMAIL_CHANGE = "EMAIL_CHANGE";
    private static final int OTP_ATTEMPT_LIMIT = 5;
    private static final int OTP_LIFETIME_MINUTES = 5;
    private static final int RESEND_WAIT_SECONDS = 60;
    private static final int EMAIL_CHANGE_SEND_LIMIT = 5;
    private static final int EMAIL_CHANGE_SEND_WINDOW_HOURS = 24;

    private final UserRepository userRepository;
    private final AccountOtpRepository otpRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    public AccountAuthService(UserRepository userRepository, AccountOtpRepository otpRepository,
            PasswordEncoder passwordEncoder, EmailService emailService, Clock clock) {
        this.userRepository = userRepository;
        this.otpRepository = otpRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.clock = clock;
    }

    public User register(RegisterForm form) {
        validatePassword(form.getPassword());
        String username = form.getUserName().trim();
        String email = form.getEmail().trim().toLowerCase(Locale.ROOT);
        String phone = form.getPhone().trim();
        if (userRepository.existsByUserNameIgnoreCase(username)) {
            throw new DuplicateUserFieldException("userName", "Username đã được sử dụng.");
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateUserFieldException("email", "Email đã được sử dụng.");
        }
        if (userRepository.existsByPhone(phone)) {
            throw new DuplicateUserFieldException("phone", "Số điện thoại đã được sử dụng.");
        }
        if (!form.getPassword().equals(form.getConfirmPassword())) {
            throw new IllegalArgumentException("Mật khẩu xác nhận không khớp.");
        }

        User user = new User();
        user.setUserName(username);
        user.setEmail(email);
        user.setFullName(form.getFullName().trim());
        user.setPhone(phone);
        user.setRoleid(5);
        user.setAvatar("/images/avatar.png");
        user.setPassWord(passwordEncoder.encode(form.getPassword()));
        user.setActive(false);
        User saved = userRepository.saveAndFlush(user);
        issueOtp(saved, ACTIVATION, "Kích hoạt tài khoản CategoryHub");
        return saved;
    }

    public void resendActivation(String email) {
        User user = userRepository.findByEmailIgnoreCase(normalizeEmail(email))
                .orElseThrow(() -> new OtpVerificationException("Không thể gửi mã. Hãy kiểm tra email đã nhập."));
        if (Boolean.TRUE.equals(user.getActive())) {
            throw new OtpVerificationException("Tài khoản này đã được kích hoạt.");
        }
        issueOtp(user, ACTIVATION, "Kích hoạt tài khoản CategoryHub");
    }

    @Transactional(noRollbackFor = OtpVerificationException.class)
    public void activate(String email, String code) {
        User user = userRepository.findByEmailIgnoreCase(normalizeEmail(email))
                .orElseThrow(() -> new OtpVerificationException("Email hoặc mã OTP không hợp lệ."));
        if (Boolean.TRUE.equals(user.getActive())) {
            throw new OtpVerificationException("Tài khoản này đã được kích hoạt.");
        }
        verifyOtp(user, ACTIVATION, code);
        user.setActive(true);
    }

    public void requestPasswordReset(String email) {
        Optional<User> candidate = userRepository.findByEmailIgnoreCase(normalizeEmail(email));
        if (candidate.isPresent() && Boolean.TRUE.equals(candidate.get().getActive())) {
            issueOtp(candidate.get(), PASSWORD_RESET, "Đặt lại mật khẩu CategoryHub");
        }
    }

    @Transactional(noRollbackFor = OtpVerificationException.class)
    public Integer verifyPasswordReset(String email, String code) {
        User user = userRepository.findByEmailIgnoreCase(normalizeEmail(email))
                .filter(candidate -> Boolean.TRUE.equals(candidate.getActive()))
                .orElseThrow(() -> new OtpVerificationException("Email hoặc mã OTP không hợp lệ."));
        verifyOtp(user, PASSWORD_RESET, code);
        return user.getId();
    }

    public void resetPassword(Integer userId, String password, String confirmPassword) {
        validatePassword(password);
        if (!password.equals(confirmPassword)) {
            throw new IllegalArgumentException("Mật khẩu xác nhận không khớp.");
        }
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new IllegalStateException("Tài khoản chưa được kích hoạt.");
        }
        user.setPassWord(passwordEncoder.encode(password));
    }

    public void sendEmailChangeOtp(Integer userId, String newEmail) {
        String email = normalizeEmail(newEmail);
        if (email.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập email mới.");
        }
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        if (email.equalsIgnoreCase(user.getEmail())) {
            throw new IllegalArgumentException("Email mới trùng email hiện tại.");
        }
        if (userRepository.existsByEmailIgnoreCaseAndIdNot(email, userId)) {
            throw new OtpVerificationException("Email mới đã được sử dụng bởi tài khoản khác.");
        }
        if (otpRepository.countByUser_IdAndPurposeAndCreatedAtAfter(
                userId, EMAIL_CHANGE, now().minusHours(EMAIL_CHANGE_SEND_WINDOW_HOURS))
                >= EMAIL_CHANGE_SEND_LIMIT) {
            throw new OtpVerificationException("Bạn đã gửi quá nhiều mã xác nhận. Hãy thử lại sau 24 giờ.");
        }
        issueEmailChangeOtp(user, email);
    }

    @Transactional(noRollbackFor = OtpVerificationException.class)
    public User verifyEmailChangeOtp(Integer userId, String code) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        AccountOtp otp = otpRepository.findFirstByUser_IdAndPurposeOrderByCreatedAtDesc(userId, EMAIL_CHANGE)
                .orElseThrow(() -> new OtpVerificationException("Không tìm thấy mã OTP. Hãy yêu cầu gửi mã mới."));

        LocalDateTime now = now();
        if (otp.getUsedAt() != null) {
            throw new OtpVerificationException("Mã OTP đã được sử dụng hoặc vô hiệu. Hãy yêu cầu mã mới.");
        }
        if (!otp.getExpiresAt().isAfter(now)) {
            otp.setUsedAt(now);
            throw new OtpVerificationException("Mã OTP đã hết hạn. Hãy yêu cầu gửi mã mới.");
        }
        if (otp.getAttempts() >= OTP_ATTEMPT_LIMIT) {
            otp.setUsedAt(now);
            throw new OtpVerificationException("Bạn đã nhập sai quá số lần cho phép. Hãy yêu cầu mã mới.");
        }

        if (code == null || code.trim().length() != 6 || !code.trim().matches("\\d{6}")
                || !passwordEncoder.matches(code.trim(), otp.getCodeHash())) {
            int attempts = otp.getAttempts() + 1;
            otp.setAttempts(attempts);
            if (attempts >= OTP_ATTEMPT_LIMIT) {
                otp.setUsedAt(now);
                throw new OtpVerificationException("Mã OTP không đúng. Bạn đã hết số lần thử; hãy yêu cầu mã mới.");
            }
            throw new OtpVerificationException("Mã OTP không đúng. Còn " + (OTP_ATTEMPT_LIMIT - attempts) + " lần thử.");
        }

        String targetEmail = otp.getTargetEmail();
        if (targetEmail == null || targetEmail.isBlank()) {
            otp.setUsedAt(now);
            throw new OtpVerificationException("Phiên xác nhận email không hợp lệ. Hãy yêu cầu mã mới.");
        }
        if (userRepository.existsByEmailIgnoreCaseAndIdNot(normalizeEmail(targetEmail), userId)) {
            otp.setUsedAt(now);
            throw new OtpVerificationException("Email mới vừa được tài khoản khác sử dụng.");
        }

        otp.setUsedAt(now);
        user.setEmail(normalizeEmail(targetEmail));
        otpRepository.delete(otp);
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public Optional<String> pendingEmailChange(Integer userId) {
        return otpRepository.findFirstByUser_IdAndPurposeOrderByCreatedAtDesc(userId, EMAIL_CHANGE)
                .filter(otp -> otp.getUsedAt() == null)
                .filter(otp -> otp.getExpiresAt().isAfter(now()))
                .map(AccountOtp::getTargetEmail);
    }

    private void issueEmailChangeOtp(User user, String newEmail) {
        LocalDateTime now = now();
        AccountOtp previous = otpRepository.findFirstByUser_IdAndPurposeOrderByCreatedAtDesc(user.getId(), EMAIL_CHANGE)
                .orElse(null);
        if (previous != null && previous.getLastSentAt() != null
                && previous.getLastSentAt().plusSeconds(RESEND_WAIT_SECONDS).isAfter(now)) {
            long secondsLeft = java.time.Duration.between(now, previous.getLastSentAt()
                    .plusSeconds(RESEND_WAIT_SECONDS)).toSeconds();
            throw new OtpVerificationException("Vui lòng đợi " + Math.max(1, secondsLeft)
                    + " giây trước khi gửi lại mã.");
        }
        if (previous != null && previous.getUsedAt() == null) {
            previous.setUsedAt(now);
        }

        String code = String.format(Locale.ROOT, "%06d", secureRandom.nextInt(1_000_000));
        AccountOtp otp = new AccountOtp();
        otp.setUser(user);
        otp.setPurpose(EMAIL_CHANGE);
        otp.setTargetEmail(normalizeEmail(newEmail));
        otp.setCodeHash(passwordEncoder.encode(code));
        otp.setCreatedAt(now);
        otp.setLastSentAt(now);
        otp.setExpiresAt(now.plusMinutes(OTP_LIFETIME_MINUTES));
        otp.setAttempts(0);
        otpRepository.save(otp);
        try {
            emailService.sendOtp(normalizeEmail(newEmail), "Xác nhận email mới trên CategoryHub",
                    code, "thay đổi email");
        } catch (EmailDeliveryException exception) {
            otp.setUsedAt(now());
            throw exception;
        }
    }

    private void issueOtp(User user, String purpose, String emailSubject) {
        LocalDateTime now = now();
        AccountOtp previous = otpRepository.findFirstByUser_IdAndPurposeOrderByCreatedAtDesc(user.getId(), purpose)
                .orElse(null);
        if (previous != null && previous.getLastSentAt() != null
                && previous.getLastSentAt().plusSeconds(RESEND_WAIT_SECONDS).isAfter(now)) {
            long secondsLeft = java.time.Duration.between(now, previous.getLastSentAt()
                    .plusSeconds(RESEND_WAIT_SECONDS)).toSeconds();
            throw new OtpVerificationException("Vui lòng đợi " + Math.max(1, secondsLeft)
                    + " giây trước khi gửi lại mã.");
        }
        if (previous != null && previous.getUsedAt() == null) {
            previous.setUsedAt(now);
        }

        String code = String.format(Locale.ROOT, "%06d", secureRandom.nextInt(1_000_000));
        AccountOtp otp = new AccountOtp();
        otp.setUser(user);
        otp.setPurpose(purpose);
        otp.setCodeHash(passwordEncoder.encode(code));
        otp.setCreatedAt(now);
        otp.setLastSentAt(now);
        otp.setExpiresAt(now.plusMinutes(OTP_LIFETIME_MINUTES));
        otp.setAttempts(0);
        otpRepository.save(otp);
        emailService.sendOtp(user.getEmail(), emailSubject, code,
                purpose.equals(ACTIVATION) ? "kích hoạt tài khoản" : "đặt lại mật khẩu");
    }

    private void verifyOtp(User user, String purpose, String code) {
        AccountOtp otp = otpRepository.findFirstByUser_IdAndPurposeOrderByCreatedAtDesc(user.getId(), purpose)
                .orElseThrow(() -> new OtpVerificationException("Không tìm thấy mã OTP. Hãy yêu cầu gửi mã mới."));
        LocalDateTime now = now();
        if (otp.getUsedAt() != null) {
            throw new OtpVerificationException("Mã OTP đã được sử dụng hoặc vô hiệu. Hãy yêu cầu mã mới.");
        }
        if (!otp.getExpiresAt().isAfter(now)) {
            otp.setUsedAt(now);
            throw new OtpVerificationException("Mã OTP đã hết hạn. Hãy yêu cầu gửi mã mới.");
        }
        if (otp.getAttempts() >= OTP_ATTEMPT_LIMIT) {
            otp.setUsedAt(now);
            throw new OtpVerificationException("Bạn đã nhập sai quá số lần cho phép. Hãy yêu cầu mã mới.");
        }
        if (code == null || !passwordEncoder.matches(code.trim(), otp.getCodeHash())) {
            int attempts = otp.getAttempts() + 1;
            otp.setAttempts(attempts);
            if (attempts >= OTP_ATTEMPT_LIMIT) {
                otp.setUsedAt(now);
                throw new OtpVerificationException("Mã OTP không đúng. Bạn đã hết số lần thử; hãy yêu cầu mã mới.");
            }
            throw new OtpVerificationException("Mã OTP không đúng. Còn " + (OTP_ATTEMPT_LIMIT - attempts) + " lần thử.");
        }
        otp.setUsedAt(now);
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 8 || password.length() > 72
                || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Mật khẩu phải từ 8 đến 72 ký tự và tối đa 72 byte UTF-8.");
        }
    }

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), clock.getZone());
    }
}
