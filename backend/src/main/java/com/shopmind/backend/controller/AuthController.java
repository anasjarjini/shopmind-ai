package com.shopmind.backend.controller;

import com.shopmind.backend.dto.*;
import com.shopmind.backend.entity.RefreshCookie;
import com.shopmind.backend.entity.User;
import com.shopmind.backend.exception.UnauthorizedException;
import com.shopmind.backend.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private  RefreshCookie refreshCookie;


    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request,
                                                 HttpServletRequest http,
                                                 HttpServletResponse res) {
        AuthResult result = authService.register(request, http.getHeader("User-Agent"));
        refreshCookie.set(res, result.refreshToken());
        return ResponseEntity.status(HttpStatus.CREATED).body(result.response());
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletRequest http,
                                              HttpServletResponse res) {
        AuthResult result = authService.login(request, http.getHeader("User-Agent"));
        refreshCookie.set(res, result.refreshToken());
        return ResponseEntity.ok(result.response());
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@CookieValue(name = RefreshCookie.NAME, required = false) String token,
                                HttpServletRequest http, HttpServletResponse res) {
        try {
            AuthResult result = authService.refresh(token, http.getHeader("User-Agent"));
            refreshCookie.set(res, result.refreshToken());
            return result.response();
        } catch (UnauthorizedException e) {
            refreshCookie.clear(res);
            throw e;
        }
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@CookieValue(name = RefreshCookie.NAME, required = false) String token,
                       @AuthenticationPrincipal User user,
                       HttpServletResponse res) {
        authService.logout(token, user);
        refreshCookie.clear(res);
    }

    @PostMapping("/logout-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logoutAll(@AuthenticationPrincipal User user, HttpServletResponse res) {
        if (user == null) {
            throw new UnauthorizedException("Non authentifié");
        }
        authService.logoutAll(user.getId());
        refreshCookie.clear(res);
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(@AuthenticationPrincipal User user) {
        if (user == null) {
            throw new UnauthorizedException("Non authentifié");
        }
        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "fullName", user.getFullName(),
                "email", user.getEmail(),
                "role", user.getRole().name()
        ));
    }
}
