package com.curso.pizzeria.auth;

import java.time.Duration;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración de la autenticación de ejemplo.
 *
 * @param users usuarios válidos (usuario → contraseña)
 * @param accessTokenTtl vida del access token
 * @param refreshTokenTtl vida del refresh token
 */
@ConfigurationProperties(prefix = "pizzeria.auth")
public record AuthProperties(Map<String, String> users, Duration accessTokenTtl, Duration refreshTokenTtl) {
}
