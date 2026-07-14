package com.dbwb.platform.auth;

import com.dbwb.platform.auth.dto.AuthResponse;
import com.dbwb.platform.auth.dto.LoginRequest;
import com.dbwb.platform.auth.dto.RegisterRequest;
import com.dbwb.platform.common.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Public, unauthenticated endpoints - see SecurityConfig ("/api/auth/**" permitAll). */
@RestController
@org.springframework.web.bind.annotation.RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
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
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
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
}
