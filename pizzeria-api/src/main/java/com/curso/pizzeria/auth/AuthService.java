package com.curso.pizzeria.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.curso.pizzeria.dto.TokenResponse;
import com.curso.pizzeria.exception.UnauthorizedException;

/**
 * Autenticación didáctica con tokens opacos en memoria.
 * <p>
 * No es un mecanismo para producción: sirve para practicar en Postman el flujo
 * login → token → peticiones protegidas → refresh.
 */
@Service
public class AuthService {

	private record Session(String username, Instant expiresAt) {
	}

	private final AuthProperties properties;
	private final Clock clock;
	private final Map<String, Session> accessTokens = new ConcurrentHashMap<>();
	private final Map<String, Session> refreshTokens = new ConcurrentHashMap<>();

	public AuthService(AuthProperties properties) {
		this.properties = properties;
		this.clock = Clock.systemUTC();
	}

	public TokenResponse login(String username, String password) {
		String expected = properties.users().get(username);
		if (expected == null || !expected.equals(password)) {
			throw new UnauthorizedException("Usuario o contraseña incorrectos");
		}
		return issueTokens(username);
	}

	public TokenResponse refresh(String refreshToken) {
		Session session = refreshTokens.remove(refreshToken);
		if (session == null || isExpired(session)) {
			throw new UnauthorizedException("Refresh token inválido o caducado");
		}
		return issueTokens(session.username());
	}

	public void logout(String accessToken) {
		accessTokens.remove(accessToken);
	}

	/** Devuelve el usuario asociado a un access token vigente. */
	public Optional<String> authenticate(String accessToken) {
		Session session = accessTokens.get(accessToken);
		if (session == null) {
			return Optional.empty();
		}
		if (isExpired(session)) {
			accessTokens.remove(accessToken);
			return Optional.empty();
		}
		return Optional.of(session.username());
	}

	public long secondsLeft(String accessToken) {
		Session session = accessTokens.get(accessToken);
		return session == null ? 0 : Math.max(0, Duration.between(clock.instant(), session.expiresAt()).toSeconds());
	}

	private TokenResponse issueTokens(String username) {
		Instant now = clock.instant();
		String access = UUID.randomUUID().toString();
		String refresh = UUID.randomUUID().toString();
		accessTokens.put(access, new Session(username, now.plus(properties.accessTokenTtl())));
		refreshTokens.put(refresh, new Session(username, now.plus(properties.refreshTokenTtl())));
		return new TokenResponse(access, refresh, "Bearer", properties.accessTokenTtl().toSeconds());
	}

	private boolean isExpired(Session session) {
		return !clock.instant().isBefore(session.expiresAt());
	}
}
