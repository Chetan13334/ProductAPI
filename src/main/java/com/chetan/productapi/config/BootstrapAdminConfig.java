package com.chetan.productapi.config;

import com.chetan.productapi.entity.Role;
import com.chetan.productapi.entity.User;
import com.chetan.productapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class BootstrapAdminConfig {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap.admin-username}")
    private String adminUsername;

    @Value("${app.bootstrap.admin-password}")
    private String adminPassword;

    @Bean
    public CommandLineRunner bootstrapAdmin() {
        return args -> {
            if (!userRepository.existsByUsername(adminUsername)) {
                User admin = User.builder()
                        .username(adminUsername)
                        .password(passwordEncoder.encode(adminPassword))
                        .role(Role.ROLE_ADMIN)
                        .build();
                userRepository.save(admin);
            }
        };
    }
}
