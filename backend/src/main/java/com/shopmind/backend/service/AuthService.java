package com.shopmind.backend.service;

import com.shopmind.backend.dto.AuthResponse;
import com.shopmind.backend.dto.AuthResult;
import com.shopmind.backend.dto.LoginRequest;
import com.shopmind.backend.dto.RegisterRequest;
import com.shopmind.backend.entity.Role;
import com.shopmind.backend.entity.User;
import com.shopmind.backend.exception.UnauthorizedException;
import com.shopmind.backend.exception.UserAlreadyExistsException;
import com.shopmind.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private RefreshTokenService refreshTokens;

    @Autowired
    private AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest request) {
        return register(request, null).response();
    }

    public AuthResult register(RegisterRequest request, String userAgent) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException(request.getEmail());
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .build();

        userRepository.save(user);

        return issue(user, userAgent);
    }

    public AuthResponse login(LoginRequest request) {
        return login(request, null).response();
    }

    public AuthResult login(LoginRequest request, String userAgent) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Utilisateur non trouvé"));

        return issue(user, userAgent);
    }

    @Transactional(noRollbackFor = UnauthorizedException.class)
    public AuthResult refresh(String rawRefreshToken, String userAgent) {
        UUID userId = refreshTokens.consume(rawRefreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User no longer exists"));
        return issue(user, userAgent);
    }

    public void logout(String rawRefreshToken) {
        logout(rawRefreshToken, null);
    }

    @Transactional
    public void logout(String rawRefreshToken, User user) {
        UUID userId = null;
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            userId = refreshTokens.revoke(rawRefreshToken);
        }
        if (user != null) {
            userId = user.getId();
        }
        if (userId != null) {
            userRepository.findById(userId).ifPresent(u -> {
                u.setTokenInvalidBefore(java.time.Instant.now());
                userRepository.save(u);
            });
        }
    }

    @Transactional
    public void logoutAll(UUID userId) {
        refreshTokens.revokeAll(userId);
        if (userId != null) {
            userRepository.findById(userId).ifPresent(u -> {
                u.setTokenInvalidBefore(java.time.Instant.now());
                userRepository.save(u);
            });
        }
    }

    private AuthResult issue(User user, String userAgent) {
        String access = jwtService.generateToken(user);
        String refresh = refreshTokens.create(user.getId(), userAgent);
        return new AuthResult(
                new AuthResponse(access, user.getEmail(), user.getFullName(), user.getRole().name()),
                refresh);
    }
}
