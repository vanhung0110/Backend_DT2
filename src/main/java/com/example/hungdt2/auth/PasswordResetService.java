package com.example.hungdt2.auth;

import com.example.hungdt2.auth.entity.PasswordResetOtpEntity;
import com.example.hungdt2.auth.repository.PasswordResetOtpRepository;
import com.example.hungdt2.exceptions.BadRequestException;
import com.example.hungdt2.exceptions.NotFoundException;
import com.example.hungdt2.sms.SmsSender;
import com.example.hungdt2.user.entity.UserEntity;
import com.example.hungdt2.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
public class PasswordResetService {
    private final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private final UserRepository userRepository;
    private final PasswordResetOtpRepository otpRepository;
    private final SmsSender smsSender;
    private final BCryptPasswordEncoder passwordEncoder;
    private final int otpLength;
    private final int otpExpirySeconds;
    private final int otpMaxAttempts;
    private final SecureRandom random = new SecureRandom();

    public PasswordResetService(UserRepository userRepository, PasswordResetOtpRepository otpRepository, SmsSender smsSender, BCryptPasswordEncoder passwordEncoder,
                                @Value("${otp.expirySeconds:60}") int otpExpirySeconds,
                                @Value("${otp.maxAttempts:3}") int otpMaxAttempts,
                                com.example.hungdt2.sms.TwilioVerifyService twilioVerifyService) {
        this.userRepository = userRepository;
        this.otpRepository = otpRepository;
        this.smsSender = smsSender;
        this.passwordEncoder = passwordEncoder;
        this.otpLength = 6; // no local generation; keep default for validation consistency
        this.otpExpirySeconds = otpExpirySeconds;
        this.otpMaxAttempts = otpMaxAttempts;
        this.twilioVerifyService = twilioVerifyService;
    }

    // Twilio Verify integration (required): delegate sending/verification to Twilio Verify service
    private final com.example.hungdt2.sms.TwilioVerifyService twilioVerifyService;

    @Transactional
    public void requestOtp(String phone) {
        String normalized = normalizePhone(phone);
        Optional<UserEntity> opt = userRepository.findByPhone(normalized);
        if (opt.isEmpty()) opt = userRepository.findByPhone(phone);
        if (opt.isEmpty()) {
            // Do not reveal user existence — return success
            log.info("requestOtp: phone {} not found — returning success (no-op)", normalized);
            return;
        }

        UserEntity user = opt.get();

        // Always delegate OTP sending to Twilio Verify using normalized E.164 phone
        twilioVerifyService.sendVerification(normalized);
        log.info("Delegated OTP send to Twilio Verify for user {} phone={}", user.getId(), normalized);
    }

    @Transactional
    public String verifyOtp(String phone, String otp) {
        String normalized = normalizePhone(phone);
        boolean ok = twilioVerifyService.checkVerification(normalized, otp);
        if (!ok) throw new BadRequestException("OTP_INVALID", "Invalid OTP");
        // try to find user by normalized phone, fallback to raw phone
        Optional<UserEntity> opt = userRepository.findByPhone(normalized);
        if (opt.isEmpty()) opt = userRepository.findByPhone(phone);
        if (opt.isEmpty()) throw new BadRequestException("USER_NOT_FOUND", "User not found");
        UserEntity user = opt.get();
        String resetToken = UUID.randomUUID().toString();
        Instant now = Instant.now();
        Instant tokenExpiry = now.plusSeconds(Math.max(300, otpExpirySeconds)); // default at least 5 minutes
        PasswordResetOtpEntity entity = new PasswordResetOtpEntity();
        entity.setUserId(user.getId());
        entity.setPhone(normalized);
        // Twilio-verified token record
        entity.setOtpHash(hash(resetToken));
        entity.setAttempts(0);
        entity.setUsed(true);
        entity.setCreatedAt(now);
        entity.setExpiresAt(tokenExpiry);
        entity.setResetToken(resetToken);
        otpRepository.save(entity);
        return resetToken;
    }

    @Transactional
    public void resetPassword(String resetToken, String newPassword) {
        PasswordResetOtpEntity entity = otpRepository.findByResetToken(resetToken).orElseThrow(() -> new BadRequestException("RESET_TOKEN_INVALID", "Invalid reset token"));
        if (entity.getUsed() == null || !entity.getUsed()) {
            // ensure token still valid by checking expiry
            if (Instant.now().isAfter(entity.getExpiresAt())) throw new BadRequestException("RESET_TOKEN_EXPIRED", "Reset token expired");
        }
        // find user
        UserEntity user = userRepository.findById(entity.getUserId()).orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));
        String encodedPassword = passwordEncoder.encode(newPassword);
        user.setPasswordHash(encodedPassword);
        userRepository.save(user);
        log.info("Password reset for userId={}, phone={}", entity.getUserId(), entity.getPhone());
        // mark otp record consumed
        entity.setUsed(true);
        entity.setResetToken(null);
        otpRepository.save(entity);
    }

    private String generateNumericCode(int length) {
        int max = (int) Math.pow(10, length);
        int code = random.nextInt(max - (max / 10)) + (max / 10); // ensure leading digits
        return String.format("%0" + length + "d", code);
    }

    private String hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] out = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(out);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String normalizePhone(String phone) {
        if (phone == null) return null;
        String p = phone.trim().replaceAll("\\s+", "");
        if (p.startsWith("+")) return p;
        if (p.startsWith("0")) return "+84" + p.substring(1);
        if (p.startsWith("84")) return "+" + p;
        return p;
    }
}