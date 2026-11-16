package vn.iotstar.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import vn.iotstar.exception.EmailDeliveryException;

@Service
public class EmailService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String fromAddress;
    private final String configuredHost;

    public EmailService(ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${app.mail.from:}") String fromAddress,
            @Value("${spring.mail.host:}") String configuredHost) {
        this.mailSenderProvider = mailSenderProvider;
        this.fromAddress = fromAddress;
        this.configuredHost = configuredHost;
    }

    public void sendOtp(String recipient, String subject, String otp, String purpose) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (configuredHost.isBlank() || fromAddress.isBlank() || mailSender == null) {
            throw new EmailDeliveryException("Email chưa được cấu hình. Hãy thiết lập MAIL_HOST, MAIL_USERNAME, "
                    + "MAIL_PASSWORD và MAIL_FROM rồi thử lại.");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(recipient);
        message.setSubject(subject);
        message.setText("Mã OTP " + purpose + " của bạn là: " + otp
                + "\nMã có hiệu lực trong 5 phút và chỉ sử dụng được một lần."
                + "\nNếu bạn không yêu cầu thao tác này, hãy bỏ qua email.");
        try {
            mailSender.send(message);
        } catch (MailException exception) {
            throw new EmailDeliveryException("Không gửi được email OTP. Hãy kiểm tra cấu hình SMTP.", exception);
        }
    }
}
