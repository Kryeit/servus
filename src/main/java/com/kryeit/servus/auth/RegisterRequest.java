package com.kryeit.servus.auth;

import com.kryeit.servus.user.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {

  private String uuid;
  private String password;
  private Role role;
}
