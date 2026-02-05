package com.devopsapi.devops.config;

import com.devopsapi.devops.model.User;
import com.devopsapi.devops.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.Arrays;

@Configuration
public class DatabaseSeeder {

    @Bean
    CommandLineRunner initDatabase(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            userRepository.findByUsername("smit").ifPresentOrElse(
                user -> {
                    System.out.println("Updating 'smit' user password and roles...");
                    user.setPassword(passwordEncoder.encode("Admin123!"));
                    user.getRoles().add("ROLE_ADMIN");
                    user.getRoles().add("ROLE_USER");
                    userRepository.save(user);
                },
                () -> {
                    System.out.println("Creating default admin 'smit'...");
                    User admin = new User("smit", passwordEncoder.encode("Admin123!"));
                    admin.getRoles().add("ROLE_ADMIN");
                    admin.getRoles().add("ROLE_USER");
                    userRepository.save(admin);
                }
            );
        };
    }
}
