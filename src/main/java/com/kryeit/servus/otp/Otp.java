package com.kryeit.servus.otp;

import com.kryeit.servus.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "otp")
public class Otp {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String code;

  private LocalDateTime creationDate;

  private LocalDateTime expiryDate;

  @ManyToOne
  @JoinColumn(name = "user_id", nullable = false)
  private User user;
}
