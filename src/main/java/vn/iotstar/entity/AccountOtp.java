package vn.iotstar.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "AccountOtps")
public class AccountOtp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "otpId")
    private Long otpId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "userId", nullable = false)
    private User user;

    @Column(name = "purpose", nullable = false, length = 30)
    private String purpose;

    @Column(name = "targetEmail", length = 254)
    private String targetEmail;

    @Column(name = "codeHash", nullable = false, length = 100)
    private String codeHash;

    @Column(name = "createdAt", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expiresAt", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "lastSentAt", nullable = false)
    private LocalDateTime lastSentAt;

    @Column(name = "attempts", nullable = false)
    private Integer attempts = 0;

    @Column(name = "usedAt")
    private LocalDateTime usedAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (attempts == null) {
            attempts = 0;
        }
    }

    public Long getOtpId() { return otpId; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    public String getTargetEmail() { return targetEmail; }
    public void setTargetEmail(String targetEmail) { this.targetEmail = targetEmail; }
    public String getCodeHash() { return codeHash; }
    public void setCodeHash(String codeHash) { this.codeHash = codeHash; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public LocalDateTime getLastSentAt() { return lastSentAt; }
    public void setLastSentAt(LocalDateTime lastSentAt) { this.lastSentAt = lastSentAt; }
    public Integer getAttempts() { return attempts; }
    public void setAttempts(Integer attempts) { this.attempts = attempts; }
    public LocalDateTime getUsedAt() { return usedAt; }
    public void setUsedAt(LocalDateTime usedAt) { this.usedAt = usedAt; }
}
