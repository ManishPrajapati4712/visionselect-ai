package com.visionselect.backend.auth.service;

import com.visionselect.backend.auth.config.JwtProperties;
import com.visionselect.backend.auth.dto.AuthResponse;
import com.visionselect.backend.auth.dto.AuthUserDto;
import com.visionselect.backend.auth.dto.LoginRequest;
import com.visionselect.backend.auth.dto.RefreshRequest;
import com.visionselect.backend.auth.dto.RegisterRequest;
import com.visionselect.backend.auth.entity.RefreshToken;
import com.visionselect.backend.auth.entity.User;
import com.visionselect.backend.auth.entity.UserRole;
import com.visionselect.backend.auth.repository.RefreshTokenRepository;
import com.visionselect.backend.auth.repository.UserRepository;
import com.visionselect.backend.auth.security.AuthenticatedUser;
import com.visionselect.backend.common.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Authentication business logic: register, login, refresh, logout, and
 * current-user retrieval.
 *
 * <p>Security design:
 * <ul>
 *   <li>Passwords are hashed with BCrypt (via the {@link PasswordEncoder}
 *       bean declared in {@code SecurityConfig}).</li>
 *   <li>Refresh tokens are 32 random bytes encoded as Base64 URL-safe.
 *       Only their SHA-256 hex digest is stored in the database.</li>
 *   <li>Refresh token rotation: each {@code /refresh} call issues a new
 *       token, revokes the old one, and records the link via
 *       {@code replaced_by_token_id}.</li>
 *   <li>Logout revokes all active refresh tokens for the user.</li>
 * </ul>
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final JwtProperties jwtProperties;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       JwtService jwtService,
                       PasswordEncoder passwordEncoder,
                       JwtProperties jwtProperties) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.jwtProperties = jwtProperties;
    }

    // -----------------------------------------------------------------------
    // Registration
    // -----------------------------------------------------------------------

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyRegisteredException(request.email());
        }

        UserRole role = UserRole.valueOf(request.role()); // validated by @Pattern in DTO
        // RegisterRequest.displayName() is the API field name kept for frontend
        // compatibility; it maps to the full_name column via User.fullName.
        User user = new User(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.displayName(),   // stored as full_name in DB
                role
        );
        user = userRepository.save(user);

        return buildAuthResponse(user);
    }

    // -----------------------------------------------------------------------
    // Login
    // -----------------------------------------------------------------------

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        return buildAuthResponse(user);
    }

    // -----------------------------------------------------------------------
    // Refresh (with rotation)
    // -----------------------------------------------------------------------

    /**
     * Token rotation flow:
     * <ol>
     *   <li>Look up the old token by its SHA-256 hash.</li>
     *   <li>Validate it (not expired, not already revoked).</li>
     *   <li>Create and persist the new token first — we need its ID.</li>
     *   <li>Revoke the old token and set its {@code replaced_by_token_id}
     *       to the new token's ID (rotation audit trail).</li>
     *   <li>Persist the updated old token.</li>
     *   <li>Return a fresh access token + the raw new refresh token.</li>
     * </ol>
     *
     * <p>All DB writes happen in a single transaction so a partial failure
     * cannot leave the token table in an inconsistent state.
     */
    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        String tokenHash = sha256Hex(request.refreshToken());
        RefreshToken oldToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(InvalidOrExpiredRefreshTokenException::new);

        if (!oldToken.isValid()) {
            throw new InvalidOrExpiredRefreshTokenException();
        }

        User user = userRepository.findById(oldToken.getUserId())
                .orElseThrow(InvalidOrExpiredRefreshTokenException::new);

        // Step 1: persist the new token so we have its generated ID.
        String rawNewToken = generateRawRefreshToken();
        String newTokenHash = sha256Hex(rawNewToken);
        Instant newExpiresAt = Instant.now().plus(jwtProperties.refreshTokenExpirationDays(), ChronoUnit.DAYS);
        RefreshToken newToken = new RefreshToken(newTokenHash, user.getId(), newExpiresAt);
        newToken = refreshTokenRepository.save(newToken);

        // Step 2: revoke old token and record the rotation link.
        oldToken.revoke();                                  // sets revoked_at = now()
        oldToken.setReplacedByTokenId(newToken.getId());    // sets replaced_by_token_id
        refreshTokenRepository.save(oldToken);

        String accessToken = jwtService.generateAccessToken(user, UUID.randomUUID());
        return new AuthResponse(accessToken, rawNewToken, AuthUserDto.from(user));
    }

    // -----------------------------------------------------------------------
    // Logout
    // -----------------------------------------------------------------------

    @Transactional
    public void logout(AuthenticatedUser principal) {
        refreshTokenRepository.revokeAllByUserId(principal.userId());
    }

    // -----------------------------------------------------------------------
    // Get current user
    // -----------------------------------------------------------------------

    public AuthUserDto getMe(AuthenticatedUser principal) {
        User user = userRepository.findById(principal.userId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND",
                        "Authenticated user no longer exists") {
                });
        return AuthUserDto.from(user);
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /**
     * Creates a new refresh token and access token for the given user.
     * Used by register and login (no rotation needed — no old token to revoke).
     */
    private AuthResponse buildAuthResponse(User user) {
        UUID sessionId = UUID.randomUUID();
        String accessToken = jwtService.generateAccessToken(user, sessionId);

        String rawRefreshToken = generateRawRefreshToken();
        String tokenHash = sha256Hex(rawRefreshToken);
        Instant expiresAt = Instant.now().plus(jwtProperties.refreshTokenExpirationDays(), ChronoUnit.DAYS);

        RefreshToken refreshToken = new RefreshToken(tokenHash, user.getId(), expiresAt);
        refreshTokenRepository.save(refreshToken);

        return new AuthResponse(accessToken, rawRefreshToken, AuthUserDto.from(user));
    }

    private static String generateRawRefreshToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    // -----------------------------------------------------------------------
    // Domain exceptions — defined as inner classes so the package stays tidy
    // -----------------------------------------------------------------------

    public static class EmailAlreadyRegisteredException extends ApiException {
        public EmailAlreadyRegisteredException(String email) {
            super(HttpStatus.CONFLICT, "EMAIL_ALREADY_REGISTERED",
                    "An account with this email already exists", "email");
        }
    }

    public static class InvalidCredentialsException extends ApiException {
        public InvalidCredentialsException() {
            super(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS",
                    "Invalid email or password");
        }
    }

    public static class InvalidOrExpiredRefreshTokenException extends ApiException {
        public InvalidOrExpiredRefreshTokenException() {
            super(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN",
                    "Refresh token is invalid or has expired");
        }
    }
}
