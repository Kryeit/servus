package com.kryeit.servus.otp;

import com.kryeit.servus.otp.OtpService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/otp")
public class OtpController
{

  @Autowired
  private OtpService otpService;

  @PostMapping("/generate")
  public String generateOtp(@RequestParam String uuid)
  {
    return otpService.generateOtp(uuid);
  }

  @PostMapping("/verify")
  public boolean verifyOtp(@RequestParam String uuid, @RequestParam String otp)
  {
    return otpService.verifyOtp(uuid, otp);
  }
}
