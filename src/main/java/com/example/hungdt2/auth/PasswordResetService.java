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
                                @Value("${otp.length:6}") int otpLength,
                                @Value("${otp.expirySeconds:60}") int otpExpirySeconds,
                                @Value("${otp.maxAttempts:3}") int otpMaxAttempts) {
        this.userRepository = userRepository;
        this.otpRepository = otpRepository;
        this.smsSender = smsSender;
        this.passwordEncoder = passwordEncoder;
        this.otpLength = otpLength;
        this.otpExpirySeconds = otpExpirySeconds;
        this.otpMaxAttempts = otpMaxAttempts;
    }

    @Transactional
    public void requestOtp(String phone) {
        Optional<UserEntity> opt = userRepository.findByPhone(phone);
        if (opt.isEmpty()) {
            // Do not reveal user existence — return success
            log.info("requestOtp: phone {} not found — returning success (no-op)", phone);
            return;
        }
        UserEntity user = opt.get();
        String code = generateNumericCode(otpLength);
        String hash = hash(code);
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(otpExpirySeconds);

        PasswordResetOtpEntity entity = new PasswordResetOtpEntity();
        entity.setUserId(user.getId());
        entity.setPhone(phone);
        entity.setOtpHash(hash);
        entity.setAttempts(0);
        entity.setUsed(false);
        entity.setCreatedAt(now);
        entity.setExpiresAt(expiresAt);
        otpRepository.save(entity);

        // send SMS (dev sender logs it)
        smsSender.sendSms(phone, "Your verification code: " + code);
        log.info("OTP generated for user {} phone={} expiresAt={}", user.getId(), phone, expiresAt);
    }

    @Transactional
    public String verifyOtp(String phone, String otp) {
        PasswordResetOtpEntity entity = otpRepository.findLatestByPhone(phone).orElseThrow(() -> new BadRequestException("OTP_EXPIRED", "OTP expired or not found"));
        if (entity.getUsed()) throw new BadRequestException("OTP_INVALID", "OTP already used or invalid");
        if (Instant.now().isAfter(entity.getExpiresAt())) throw new BadRequestException("OTP_EXPIRED", "OTP expired");

        int attempts = entity.getAttempts() + 1;
        entity.setAttempts(attempts);
        if (attempts >= otpMaxAttempts && !matchesHash(otp, entity.getOtpHash())) {
            entity.setUsed(true);
            otpRepository.save(entity);
            throw new BadRequestException("OTP_MAX_ATTEMPTS", "Max attempts exceeded");
        }

        if (!matchesHash(otp, entity.getOtpHash())) {
            otpRepository.save(entity);
            throw new BadRequestException("OTP_INVALID", "Invalid OTP");
        }

        // success: mark used and create reset token
        String resetToken = UUID.randomUUID().toString();
        entity.setUsed(true);
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
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
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

    private boolean matchesHash(String input, String hash) {
        return hash(input).equalsIgnoreCase(hash);
    }
}