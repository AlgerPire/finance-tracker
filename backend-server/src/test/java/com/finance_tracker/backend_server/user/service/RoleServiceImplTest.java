package com.finance_tracker.backend_server.user.service;

import com.finance_tracker.backend_server.user.entity.Role;
import com.finance_tracker.backend_server.user.entity.enumeration.ERole;
import com.finance_tracker.backend_server.user.repository.RoleRepository;
import com.finance_tracker.backend_server.user.service.impl.RoleServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoleServiceImpl")
class RoleServiceImplTest {

    @Mock RoleRepository roleRepository;

    @InjectMocks RoleServiceImpl service;

    private Role userRole;
    private Role adminRole;

    @BeforeEach
    void setUp() {
        userRole  = Role.builder().name(ERole.ROLE_USER).build();
        adminRole = Role.builder().name(ERole.ROLE_ADMIN).build();
    }

    // -----------------------------------------------------------------------
    // findByName
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("findByName")
    class FindByName {

        @Test
        @DisplayName("success — returns role when it exists in the database")
        void found() {
            when(roleRepository.findByName(ERole.ROLE_USER)).thenReturn(Optional.of(userRole));

            Role result = service.findByName(ERole.ROLE_USER);

            assertThat(result.getName()).isEqualTo(ERole.ROLE_USER);
        }

        @Test
        @DisplayName("throws IllegalArgumentException when role does not exist")
        void notFound() {
            when(roleRepository.findByName(ERole.ROLE_USER)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findByName(ERole.ROLE_USER))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ROLE_USER")
                    .hasMessageContaining("not found");
        }
    }

    // -----------------------------------------------------------------------
    // findAll
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("success — returns all roles from the repository")
        void success() {
            when(roleRepository.findAll()).thenReturn(List.of(userRole, adminRole));

            List<Role> result = service.findAll();

            assertThat(result).hasSize(2)
                    .extracting(Role::getName)
                    .containsExactlyInAnyOrder(ERole.ROLE_USER, ERole.ROLE_ADMIN);
        }

        @Test
        @DisplayName("returns empty list when no roles exist")
        void empty() {
            when(roleRepository.findAll()).thenReturn(List.of());

            List<Role> result = service.findAll();

            assertThat(result).isEmpty();
        }
    }

    // -----------------------------------------------------------------------
    // existsByName
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("existsByName")
    class ExistsByName {

        @Test
        @DisplayName("returns true when role exists")
        void exists() {
            when(roleRepository.findByName(ERole.ROLE_USER)).thenReturn(Optional.of(userRole));

            assertThat(service.existsByName(ERole.ROLE_USER)).isTrue();
        }

        @Test
        @DisplayName("returns false when role does not exist")
        void notExists() {
            when(roleRepository.findByName(ERole.ROLE_ADMIN)).thenReturn(Optional.empty());

            assertThat(service.existsByName(ERole.ROLE_ADMIN)).isFalse();
        }
    }

    // -----------------------------------------------------------------------
    // createRole
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("createRole")
    class CreateRole {

        @Test
        @DisplayName("success — saves role with correct name and returns it")
        void success() {
            when(roleRepository.save(any(Role.class))).thenAnswer(inv -> inv.getArgument(0));

            Role result = service.createRole(ERole.ROLE_USER);

            assertThat(result.getName()).isEqualTo(ERole.ROLE_USER);
            verify(roleRepository).save(argThat(r -> r.getName() == ERole.ROLE_USER));
        }
    }

    // -----------------------------------------------------------------------
    // initializeRoles
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("initializeRoles")
    class InitializeRoles {

        @Test
        @DisplayName("creates all roles when none exist yet")
        void createsAllRoles() {
            when(roleRepository.findByName(any(ERole.class))).thenReturn(Optional.empty());
            when(roleRepository.save(any(Role.class))).thenAnswer(inv -> inv.getArgument(0));

            service.initializeRoles();

            // one save per ERole value
            verify(roleRepository, times(ERole.values().length)).save(any(Role.class));
        }

        @Test
        @DisplayName("skips creation when all roles already exist")
        void skipsWhenAllExist() {
            when(roleRepository.findByName(ERole.ROLE_USER)).thenReturn(Optional.of(userRole));
            when(roleRepository.findByName(ERole.ROLE_ADMIN)).thenReturn(Optional.of(adminRole));

            service.initializeRoles();

            verify(roleRepository, never()).save(any());
        }

        @Test
        @DisplayName("creates only the roles that are missing")
        void createsOnlyMissingRoles() {
            // ROLE_USER already exists, ROLE_ADMIN is missing
            when(roleRepository.findByName(ERole.ROLE_USER)).thenReturn(Optional.of(userRole));
            when(roleRepository.findByName(ERole.ROLE_ADMIN)).thenReturn(Optional.empty());
            when(roleRepository.save(any(Role.class))).thenAnswer(inv -> inv.getArgument(0));

            service.initializeRoles();

            verify(roleRepository, times(1)).save(
                    argThat(r -> r.getName() == ERole.ROLE_ADMIN));
            verify(roleRepository, never()).save(
                    argThat(r -> r.getName() == ERole.ROLE_USER));
        }
    }
}