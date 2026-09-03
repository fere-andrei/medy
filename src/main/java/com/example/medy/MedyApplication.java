package com.example.medy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

// UserDetailsServiceAutoConfiguration excluded: auth is custom (JWT + our own
// User table), so Boot's default in-memory user/password fallback is unused
// noise, not a real credential.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class MedyApplication {

	public static void main(String[] args) {
		SpringApplication.run(MedyApplication.class, args);
	}

}
