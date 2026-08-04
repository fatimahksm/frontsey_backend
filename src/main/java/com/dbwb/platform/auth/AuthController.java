package com.dbwb.platform.auth;

import com.dbwb.platform.auth.dto.AuthResponse;
import com.dbwb.platform.auth.dto.LoginRequest;
import com.dbwb.platform.auth.dto.RegisterRequest;
import com.dbwb.platform.common.config.JwtProperties;
import com.dbwb.platform.common.dto.ApiResponse;
import com.dbwb.platform.common.exception.BusinessRuleViolationException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;

/** Public, unauthenticated endpoints - see SecurityConfig ("/api/auth/**" permitAll). */
@RestController
@org.springframework.web.bind.annotation.RequestMapping("/api/auth")
public class AuthController {

    /** BR-AUTH-007: httpOnly so XSS can't read a long-lived credential; scoped to /api/auth so it's only ever sent back to refresh/logout. */
    private static final String REFRESH_COOKIE_NAME = "refreshToken";

    private final AuthService authService;
    private final JwtProperties jwtProperties;

    public AuthController(AuthService authService, JwtProperties jwtProperties) {
        this.authService = authService;
        this.jwtProperties = jwtProperties;
    }

    @PostMapping("/register")
    public ApiResponse<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ApiResponse.ok(null, "Registration successful. Please check your email to verify your account.");
    }

    @PostMapping("/verify-email")
    public ApiResponse<Void> verifyEmail(@RequestParam String token) {
        authService.verifyEmail(token);
        return ApiResponse.ok(null, "Email verified successfully.");
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        AuthService.AuthResult result = authService.login(request);
        setRefreshCookie(response, result.refreshToken(), result.refreshTokenExpiresAt());
        return ApiResponse.ok(result.response());
    }

    /** BR-AUTH-007: silently exchanges the httpOnly refresh cookie for a new access token, without the user re-entering credentials. */
    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(HttpServletRequest request, HttpServletResponse response) {
        String rawRefreshToken = readRefreshCookie(request)
                .orElseThrow(() -> new BusinessRuleViolationException("Invalid session. Please log in again."));

        AuthService.AuthResult result = authService.refresh(rawRefreshToken);
        setRefreshCookie(response, result.refreshToken(), result.refreshTokenExpiresAt());
        return ApiResponse.ok(result.response());
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        readRefreshCookie(request).ifPresent(authService::logout);
        clearRefreshCookie(response);
        return ApiResponse.ok(null, "Logged out.");
    }

    @PostMapping("/password-reset/request")
    public ApiResponse<Void> requestPasswordReset(@RequestParam String email) {
        authService.requestPasswordReset(email);
        return ApiResponse.ok(null, "If this email is registered, a reset link has been sent.");
    }

    @PostMapping("/password-reset/confirm")
    public ApiResponse<Void> confirmPasswordReset(@RequestParam String token, @RequestParam String newPassword) {
        authService.resetPassword(token, newPassword);
        return ApiResponse.ok(null, "Password reset successfully.");
    }

    private Optional<String> readRefreshCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return Optional.empty();
        return Arrays.stream(cookies)
                .filter(c -> REFRESH_COOKIE_NAME.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }

    private void setRefreshCookie(HttpServletResponse response, String rawToken, Instant expiresAt) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, rawToken)
                .httpOnly(true)
                .secure(jwtProperties.isRefreshCookieSecure())
                .sameSite("Lax")
                .path("/api/auth")
                .maxAge(Duration.between(Instant.now(), expiresAt))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(jwtProperties.isRefreshCookieSecure())
                .sameSite("Lax")
                .path("/api/auth")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
