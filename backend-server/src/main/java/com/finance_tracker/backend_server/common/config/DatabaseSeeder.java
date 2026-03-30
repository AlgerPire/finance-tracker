package com.finance_tracker.backend_server.common.config;

import com.finance_tracker.backend_server.user.entity.Role;
import com.finance_tracker.backend_server.user.entity.User;
import com.finance_tracker.backend_server.user.entity.enumeration.ERole;
import com.finance_tracker.backend_server.user.repository.UserRepository;
import com.finance_tracker.backend_server.user.service.RoleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Database seeder component for initializing required data on application startup.
 * <p>
 * This component runs during application startup and ensures that all required
 * database records (such as roles) are present. It uses Spring's {@link CommandLineRunner}
 * interface to execute initialization logic after the application context is loaded.
 * </p>
 * <p>
 * The {@link Order} annotation ensures this seeder runs before other CommandLineRunners
 * that might depend on the seeded data.
 * </p>
 *
 * @author Finance Tracker Team
 * @version 1.0
 * @since 2026
 */
@Component
@Order(1)
public class DatabaseSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseSeeder.class);

    private final RoleService roleService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminSeedProperties adminProps;

    /**
     * Constructs a DatabaseSeeder with the required services.
     *
     * @param roleService the service for role management
     */
    @Autowired
    public DatabaseSeeder(RoleService roleService, UserRepository userRepository, PasswordEncoder passwordEncoder, AdminSeedProperties adminProps) {
        this.roleService = roleService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminProps = adminProps;
    }

    /**
     * Executes the database seeding logic on application startup.
     * <p>
     * This method initializes all default roles defined in the {@code ERole} enum.
     * If roles already exist, they are not recreated.
     * </p>
     *
     * @param args command line arguments (not used)
     */
    @Override
    public void run(String... args) {
        logger.info("Starting database seeding...");
        try {
            seedRoles();
            seedAdminUser();
            logger.info("Database seeding completed successfully!");
        } catch (Exception e) {
            logger.error("Error during database seeding: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to seed database", e);
        }
    }

    /**
     * Seeds the default roles into the database.
     * <p>
     * Delegates to {@link RoleService#initializeRoles()} which creates
     * all roles from the {@code ERole} enum if they don't exist.
     * </p>
     */
    private void seedRoles() {
        logger.debug("Seeding roles...");
        roleService.initializeRoles();
    }

    /**
     * Creates the admin user if it doesn't exist.
     */
    private void seedAdminUser() {
        if (userRepository.existsByUsername(adminProps.getUsername())) {
            logger.debug("Admin user already exists, skipping.");
            return;
        }
        Role adminRole = roleService.findByName(ERole.ROLE_ADMIN);
        User admin = new User(
                adminProps.getUsername(),
                adminProps.getEmail(),
                passwordEncoder.encode(adminProps.getPassword())
        );
        admin.setRoles(Set.of(adminRole));
        userRepository.save(admin);
        logger.info("Admin user '{}' created successfully.", adminProps.getUsername());
    }
}