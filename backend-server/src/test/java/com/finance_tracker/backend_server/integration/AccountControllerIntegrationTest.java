package com.finance_tracker.backend_server.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance_tracker.backend_server.account.dto.request.CreateAccountRequest;
import com.finance_tracker.backend_server.account.entity.Account;
import com.finance_tracker.backend_server.account.entity.enumeration.AccountType;
import com.finance_tracker.backend_server.account.entity.enumeration.CurrencyType;
import com.finance_tracker.backend_server.account.repository.AccountRepository;
import com.finance_tracker.backend_server.security.jwt.JwtUtils;
import com.finance_tracker.backend_server.user.entity.Role;
import com.finance_tracker.backend_server.user.entity.User;
import com.finance_tracker.backend_server.user.entity.enumeration.ERole;
import com.finance_tracker.backend_server.user.repository.RoleRepository;
import com.finance_tracker.backend_server.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@DisplayName("POST /api/accounts — createAccount (Integration)")
class AccountControllerIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired WebApplicationContext context;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired UserRepository userRepository;
    @Autowired RoleRepository roleRepository;
    @Autowired AccountRepository accountRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtUtils jwtUtils;

    private MockMvc mockMvc;
    private String userJwt;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        accountRepository.deleteAll();
        userRepository.deleteAll();

        Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                .orElseThrow(() -> new IllegalStateException("ROLE_USER not found in DB"));

        User testUser = new User();
        testUser.setEmail("test@example.com");
        testUser.setUsername("testuser");
        testUser.setPassword(passwordEncoder.encode("password"));
        testUser.setRoles(Set.of(userRole));
        userRepository.save(testUser);

        userJwt = jwtUtils.generateTokenFromUsername(testUser.getUsername());
    }

    @Nested
    @DisplayName("given valid input")
    class HappyPath {

        @Test
        @DisplayName("returns 201 and persists the account in the database")
        void createAccount_validRequest_returns201AndPersists() throws Exception {
            CreateAccountRequest request =
                    new CreateAccountRequest(AccountType.SAVINGS, new BigDecimal("500.00"), CurrencyType.EUR);

            mockMvc.perform(post("/api/accounts")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + userJwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNumber())
                    .andExpect(jsonPath("$.accountIdentification").isNotEmpty())
                    .andExpect(jsonPath("$.accountType").value("SAVINGS"))
                    .andExpect(jsonPath("$.currencyType").value("EUR"))
                    .andExpect(jsonPath("$.balance").value(500.00))
                    .andExpect(jsonPath("$.active").value(true));

            List<Account> accounts = accountRepository.findAll();
            assertThat(accounts).hasSize(1);
            assertThat(accounts.get(0).getAccountType()).isEqualTo(AccountType.SAVINGS);
            assertThat(accounts.get(0).getCurrencyType()).isEqualTo(CurrencyType.EUR);
            assertThat(accounts.get(0).getBalance()).isEqualByComparingTo("500.00");
        }

        @Test
        @DisplayName("returns 201 with zero initial balance")
        void createAccount_zeroBalance_returns201() throws Exception {
            CreateAccountRequest request =
                    new CreateAccountRequest(AccountType.SAVINGS, BigDecimal.ZERO, CurrencyType.USD);

            mockMvc.perform(post("/api/accounts")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + userJwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.balance").value(0.00));
        }
    }

    @Nested
    @DisplayName("given invalid input")
    class ValidationErrors {

        @Test
        @DisplayName("returns 400 when body is empty")
        void createAccount_emptyBody_returns400() throws Exception {
            mockMvc.perform(post("/api/accounts")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + userJwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 400 when balance is negative")
        void createAccount_negativeBalance_returns400() throws Exception {
            CreateAccountRequest request =
                    new CreateAccountRequest(AccountType.SAVINGS, new BigDecimal("-1.00"), CurrencyType.EUR);

            mockMvc.perform(post("/api/accounts")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + userJwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 400 when balance has more than 2 decimal places")
        void createAccount_tooManyDecimalPlaces_returns400() throws Exception {
            CreateAccountRequest request =
                    new CreateAccountRequest(AccountType.SAVINGS, new BigDecimal("100.999"), CurrencyType.EUR);

            mockMvc.perform(post("/api/accounts")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + userJwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("given a duplicate account")
    class DuplicateAccount {

        @Test
        @DisplayName("returns 409 when user already has the same type + currency")
        void createAccount_duplicate_returns409() throws Exception {
            CreateAccountRequest request =
                    new CreateAccountRequest(AccountType.SAVINGS, new BigDecimal("100.00"), CurrencyType.EUR);

            mockMvc.perform(post("/api/accounts")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + userJwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());

            mockMvc.perform(post("/api/accounts")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + userJwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("allows same type with different currency")
        void createAccount_sameTypeDifferentCurrency_returns201() throws Exception {
            CreateAccountRequest eur =
                    new CreateAccountRequest(AccountType.SAVINGS, new BigDecimal("100.00"), CurrencyType.EUR);
            CreateAccountRequest usd =
                    new CreateAccountRequest(AccountType.SAVINGS, new BigDecimal("100.00"), CurrencyType.USD);

            mockMvc.perform(post("/api/accounts")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + userJwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(eur)))
                    .andExpect(status().isCreated());

            mockMvc.perform(post("/api/accounts")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + userJwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(usd)))
                    .andExpect(status().isCreated());
        }
    }

    @Nested
    @DisplayName("given no or invalid authentication")
    class Security {

        @Test
        @DisplayName("returns 401 when no JWT is present")
        void createAccount_noToken_returns401() throws Exception {
            CreateAccountRequest request =
                    new CreateAccountRequest(AccountType.SAVINGS, new BigDecimal("100.00"), CurrencyType.EUR);

            mockMvc.perform(post("/api/accounts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("returns 401 when JWT is expired or tampered")
        void createAccount_invalidToken_returns401() throws Exception {
            CreateAccountRequest request =
                    new CreateAccountRequest(AccountType.SAVINGS, new BigDecimal("100.00"), CurrencyType.EUR);

            mockMvc.perform(post("/api/accounts")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer invalid.token.here")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }
}