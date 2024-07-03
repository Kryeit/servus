package com.kryeit.servus;

import com.kryeit.servus.auth.AuthenticationService;
import com.kryeit.servus.auth.RegisterRequest;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import static com.kryeit.servus.user.Role.STAFF;

@SpringBootApplication(scanBasePackages = "com.kryeit.servus")
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class SecurityApplication {

	public static void main(String[] args) {
		SpringApplication.run(SecurityApplication.class, args);
	}

	@Bean
	public CommandLineRunner commandLineRunner(
			AuthenticationService service
	) {
		return args -> {
			var staff = RegisterRequest.builder()
					.uuid("test")
					.password("test")
					.role(STAFF)
					.build();
			System.out.println("Admin token: " + service.register(staff).getAccessToken());
		};
	}
}
