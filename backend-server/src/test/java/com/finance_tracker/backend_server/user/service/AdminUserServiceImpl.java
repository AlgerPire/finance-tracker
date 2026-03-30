package com.finance_tracker.backend_server.user.service;

import com.finance_tracker.backend_server.user.dto.response.AdminUserResponse;
import com.finance_tracker.backend_server.user.dto.response.PagedUserResponse;
import com.finance_tracker.backend_server.user.entity.User;
import com.finance_tracker.backend_server.user.entity.enumeration.ERole;
import com.finance_tracker.backend_server.user.mapper.UserMapper;
import com.finance_tracker.backend_server.user.repository.UserRepository;
import com.finance_tracker.backend_server.user.service.impl.AdminUserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminUserServiceImpl")
class AdminUserServiceImplTest {

    @Mock UserRepository userRepository;
    @Mock UserMapper userMapper;

    @InjectMocks AdminUserServiceImpl service;

    private User user;
    private AdminUserResponse userResponse;

    @BeforeEach
    void setUp() {
        user = new User("john", "john@test.com", "encoded_pwd");
        user.setId(1L);

        userResponse = new AdminUserResponse(1L, "john", "john@test.com", Set.of("ROLE_USER"));
    }

    @Nested
    @DisplayName("listAllUsers")
    class ListAllUsers {

        @Test
        @DisplayName("success — returns paginated users with correct metadata")
        void success() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<User> page = new PageImpl<>(List.of(user), pageable, 1);

            when(userRepository.findAllWithRoles(eq(ERole.ROLE_USER), any(Pageable.class)))
                    .thenReturn(page);
            when(userMapper.toDto(user)).thenReturn(userResponse);

            PagedUserResponse result = service.listAllUsers(pageable);

            assertThat(result.content()).hasSize(1);
            assertThat(result.content().get(0).username()).isEqualTo("john");
            assertThat(result.totalElements()).isEqualTo(1);
            assertThat(result.page()).isZero();
            assertThat(result.first()).isTrue();
            assertThat(result.last()).isTrue();
        }

        @Test
        @DisplayName("success — returns empty content when no users with ROLE_USER exist")
        void emptyPage() {
            Pageable pageable = PageRequest.of(0, 20);
            when(userRepository.findAllWithRoles(eq(ERole.ROLE_USER), any(Pageable.class)))
                    .thenReturn(Page.empty(pageable));

            PagedUserResponse result = service.listAllUsers(pageable);

            assertThat(result.content()).isEmpty();
            assertThat(result.totalElements()).isZero();
        }

        @Test
        @DisplayName("always queries for ROLE_USER — never returns admin accounts")
        void alwaysFiltersRoleUser() {
            Pageable pageable = PageRequest.of(0, 20);
            when(userRepository.findAllWithRoles(eq(ERole.ROLE_USER), any(Pageable.class)))
                    .thenReturn(Page.empty());

            service.listAllUsers(pageable);

            verify(userRepository).findAllWithRoles(eq(ERole.ROLE_USER), any(Pageable.class));
        }

        @Test
        @DisplayName("clamps page size to MAX_PAGE_SIZE=100 when oversized request is given")
        void clampPageSize() {
            Pageable oversized = PageRequest.of(0, 500);
            when(userRepository.findAllWithRoles(eq(ERole.ROLE_USER), any(Pageable.class)))
                    .thenReturn(Page.empty());

            service.listAllUsers(oversized);

            verify(userRepository).findAllWithRoles(
                    eq(ERole.ROLE_USER),
                    argThat(p -> p.getPageSize() == 100));
        }

        @Test
        @DisplayName("applies default sort by username asc when pageable has no sort")
        void defaultSortApplied() {
            Pageable unsorted = PageRequest.of(0, 20, Sort.unsorted());
            when(userRepository.findAllWithRoles(eq(ERole.ROLE_USER), any(Pageable.class)))
                    .thenReturn(Page.empty());

            service.listAllUsers(unsorted);

            verify(userRepository).findAllWithRoles(
                    eq(ERole.ROLE_USER),
                    argThat(p -> {
                        Sort.Order order = p.getSort().getOrderFor("username");
                        return order != null && order.isAscending();
                    }));
        }

        @Test
        @DisplayName("preserves caller-provided sort when pageable has explicit sort")
        void customSortPreserved() {
            Sort customSort = Sort.by(Sort.Order.desc("username"));
            Pageable pageable = PageRequest.of(0, 20, customSort);
            when(userRepository.findAllWithRoles(eq(ERole.ROLE_USER), any(Pageable.class)))
                    .thenReturn(Page.empty());

            service.listAllUsers(pageable);

            verify(userRepository).findAllWithRoles(
                    eq(ERole.ROLE_USER),
                    argThat(p -> {
                        Sort.Order order = p.getSort().getOrderFor("username");
                        return order != null && order.isDescending();
                    }));
        }
    }
}
