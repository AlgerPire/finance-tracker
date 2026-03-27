package com.finance_tracker.backend_server.user.service.impl;

import com.finance_tracker.backend_server.user.entity.Role;
import com.finance_tracker.backend_server.user.entity.enumeration.ERole;
import com.finance_tracker.backend_server.user.repository.RoleRepository;
import com.finance_tracker.backend_server.user.service.RoleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/**
 * Implementation of the {@link com.finance_tracker.backend_server.user.service.RoleService} interface.
 * <p>
 * This service handles all role-related business logic including
 * role retrieval, creation, and initialization of default roles.
 * </p>
 *
 * @author FullSecurity Team
 * @version 1.0
 * @since 2024
 */
@Service
@Transactional
public class RoleServiceImpl implements RoleService {

    private static final Logger logger = LoggerFactory.getLogger(RoleServiceImpl.class);

    private final RoleRepository roleRepository;

    /**
     * Constructs a RoleServiceImpl with the required repository.
     *
     * @param roleRepository repository for role data access
     */
    public RoleServiceImpl(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public Role findByName(ERole roleName) {
        return roleRepository.findByName(roleName)
                .orElseThrow(() -> {
                    logger.error("Role not found: {}", roleName);
                    return new IllegalArgumentException("Error: Role " + roleName + " is not found.");
                });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<Role> findAll() {
        return roleRepository.findAll();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public boolean existsByName(ERole roleName) {
        return roleRepository.findByName(roleName).isPresent();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initializeRoles() {
        logger.info("Initializing default roles...");

        Arrays.stream(ERole.values()).forEach(role -> {
            if (!existsByName(role)) {
                createRole(role);
                logger.info("Created role: {}", role);
            } else {
                logger.debug("Role already exists: {}", role);
            }
        });

        logger.info("Role initialization completed. Total roles: {}", roleRepository.count());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Role createRole(ERole roleName) {
        Role role = Role.builder()
                .name(roleName)
                .build();

        Role savedRole = roleRepository.save(role);
        logger.debug("Created new role: {}", roleName);

        return savedRole;
    }
}
