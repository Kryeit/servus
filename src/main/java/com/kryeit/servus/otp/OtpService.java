package com.kryeit.servus.otp;

import com.kryeit.servus.user.UserRepository;
import com.kryeit.servus.user.User;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
public class OtpService
{

  private static final Logger logger = LoggerFactory.getLogger(OtpService.class);

  @Autowired
  private OtpRepository otpRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  private final Random random = new Random();

  private static final int OTP_EXPIRY_MINUTES = 5; // OTP expiry time

  public String generateOtp(String uuid)
  {
    User user = userRepository.findByUuid(uuid)
                              .orElseThrow(() -> new RuntimeException("User not found"));
    String otp = String.format("%06d", random.nextInt(1000000));
    Otp otpEntity = Otp.builder()
                       .code(passwordEncoder.encode(otp))
                       .creationDate(LocalDateTime.now())
                       .expiryDate(LocalDateTime.now()
                                                .plusMinutes(OTP_EXPIRY_MINUTES))
                       .user(user)
                       .build();
    otpRepository.save(otpEntity);
    logger.info("Generated OTP for user {}: {}", uuid, otp); // Log the OTP for debugging
    return otp;
  }

  public boolean verifyOtp(String uuid, String code)
  {
    User user = userRepository.findByUuid(uuid)
                              .orElse(null);
    if (user == null)
    {
      logger.error("User with UUID {} not found", uuid);
      return false;
    }

    List<Otp> otpList = otpRepository.findByUserUuid(user.getUuid());
    for (Otp otp : otpList)
    {
      if (otp.getExpiryDate() == null || LocalDateTime.now()
                                                      .isAfter(otp.getExpiryDate()))
      {
        otpRepository.delete(otp); // Remove expired OTP
        logger.info("Expired OTP deleted for user {}", uuid);
        continue;
      }
      if (passwordEncoder.matches(code, otp.getCode()))
      {
        user.setVerified(true);
        userRepository.save(user);
        otpRepository.delete(otp);
        logger.info("OTP verified successfully for user {}", uuid);
        return true;
      }
    }

    logger.error("OTP does not match or is expired for user {}", uuid);
    return false;
  }

  @Scheduled(fixedRate = 86400000)  // 24 hours
  public void deleteExpiredOtpsAndUsers()
  {
    LocalDateTime threeDaysAgo = LocalDateTime.now()
                                              .minusDays(3);
    List<User> unverifiedUsers = userRepository.findByVerifiedFalseAndCreatedAtBefore(threeDaysAgo);
    unverifiedUsers.forEach(user -> {
      userRepository.delete(user);
      logger.info("Deleted unverified user {}", user.getUuid());
    });

    LocalDateTime now = LocalDateTime.now();
    List<Otp> expiredOtps = otpRepository.findByExpiryDateBefore(now);
    expiredOtps.forEach(otp -> {
      otpRepository.delete(otp);
      logger.info("Deleted expired OTP with id {}", otp.getId());
    });
  }
}
