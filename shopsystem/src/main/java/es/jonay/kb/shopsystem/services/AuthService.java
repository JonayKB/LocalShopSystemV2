package es.jonay.kb.shopsystem.services;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.jonay.kb.shopsystem.api.dto.LoginRequest;
import es.jonay.kb.shopsystem.api.dto.RefreshRequest;
import es.jonay.kb.shopsystem.api.dto.TokenResponse;
import es.jonay.kb.shopsystem.api.exceptions.InvalidCredentialsException;
import es.jonay.kb.shopsystem.controller.AuthController;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/auth")
@Tag(name = "AUTH", description = "Login, refresco y cierre de sesión")
public class AuthService {

    private final AuthController authService;

    public AuthService(AuthController authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public TokenResponse login(@RequestBody LoginRequest request) {
        return authService.login(request.email(), request.password());
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@RequestBody RefreshRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody(required = false) RefreshRequest request) {
        authService.logout(request == null ? null : request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<String> invalid(InvalidCredentialsException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
    }
}
