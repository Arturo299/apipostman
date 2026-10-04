package com.curso.pizzeria.dto;

public record TokenResponse(String accessToken, String refreshToken, String tokenType, long expiresIn) {
}
