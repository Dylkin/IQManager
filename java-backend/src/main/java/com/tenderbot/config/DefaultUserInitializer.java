package com.tenderbot.config;

import com.tenderbot.entity.User;
import com.tenderbot.entity.UserRole;
import com.tenderbot.entity.UserStatus;
import com.tenderbot.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DefaultUserInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DefaultUserInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DefaultUserInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        String defaultEmail = "pavel.dylkin@gmail.com";
        String defaultPassword = "00016346";

        if (userRepository.count() == 0) {
            User admin = new User();
            admin.setEmail(defaultEmail);
            admin.setPasswordHash(passwordEncoder.encode(defaultPassword));
            admin.setFullName("Администратор");
            admin.setRole(UserRole.ADMIN);
            admin.setStatus(UserStatus.ACTIVE);
            admin.setSystem(true);
            userRepository.save(admin);
            log.info("Default admin user created: {}", defaultEmail);
        } else {
            log.info("Users already exist, skipping default user creation");
        }
    }
}
