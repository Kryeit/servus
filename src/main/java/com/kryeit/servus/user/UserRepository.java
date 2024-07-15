package com.kryeit.servus.user;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Integer>
{
  List<User> findByVerifiedFalseAndCreatedAtBefore(LocalDateTime dateTime);
  Optional<User> findByUuid(String uuid);

}
