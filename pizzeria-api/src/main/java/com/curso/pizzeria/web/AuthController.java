package com.curso.pizzeria.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.curso.pizzeria.auth.AuthInterceptor;
import com.curso.pizzeria.auth.AuthService;
import com.curso.pizzeria.dto.LoginRequest;
import com.curso.pizzeria.dto.RefreshRequest;
import com.curso.pizzeria.dto.TokenResponse;
import com.curso.pizzeria.dto.UserResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticación")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/login")
	@Operation(summary = "Devuelve un access token y un refresh token")
	public TokenResponse login(@Valid @RequestBody LoginRequest request) {
		return authService.login(request.username(), request.password());
	}

	@PostMapping("/refresh")
	@Operation(summary = "Canjea un refresh token por un par de tokens nuevo")
	public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
		return authService.refresh(request.refreshToken());
	}

	@GetMapping("/me")
	@Operation(summary = "Usuario del token actual", security = @SecurityRequirement(name = "bearerAuth"))
	public UserResponse me(@RequestAttribute(AuthInterceptor.USER_ATTRIBUTE) String user,
			@RequestAttribute(AuthInterceptor.TOKEN_ATTRIBUTE) String token) {
		return new UserResponse(user, authService.secondsLeft(token));
	}

	@PostMapping("/logout")
	@Operation(summary = "Invalida el access token actual", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<Void> logout(@RequestAttribute(AuthInterceptor.TOKEN_ATTRIBUTE) String token) {
		authService.logout(token);
		return ResponseEntity.noContent().build();
	}
}
