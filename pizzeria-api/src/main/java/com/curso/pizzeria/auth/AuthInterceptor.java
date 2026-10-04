package com.curso.pizzeria.auth;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.curso.pizzeria.exception.UnauthorizedException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Las lecturas (GET) son públicas; crear, modificar y borrar exige
 * la cabecera {@code Authorization: Bearer <accessToken>}.
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

	public static final String USER_ATTRIBUTE = "pizzeria.user";
	public static final String TOKEN_ATTRIBUTE = "pizzeria.token";
	private static final String BEARER = "Bearer ";

	private final AuthService authService;

	public AuthInterceptor(AuthService authService) {
		this.authService = authService;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		boolean protectedRequest = !HttpMethod.GET.matches(request.getMethod())
				&& !HttpMethod.OPTIONS.matches(request.getMethod())
				|| request.getRequestURI().endsWith("/auth/me");
		if (!protectedRequest) {
			return true;
		}
		String header = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (header == null || !header.startsWith(BEARER)) {
			throw new UnauthorizedException("Falta la cabecera Authorization: Bearer <token>");
		}
		String token = header.substring(BEARER.length()).trim();
		String user = authService.authenticate(token)
				.orElseThrow(() -> new UnauthorizedException("Token inválido o caducado"));
		request.setAttribute(USER_ATTRIBUTE, user);
		request.setAttribute(TOKEN_ATTRIBUTE, token);
		return true;
	}
}
