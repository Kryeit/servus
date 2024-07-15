package com.kryeit.servus.otp;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OtpRepository extends JpaRepository<Otp, Long>
{
  List<Otp> findByUserUuid(String uuid);
  void deleteByUserUuid(String uuid);
  List<Otp> findByExpiryDateBefore(LocalDateTime dateTime);
}