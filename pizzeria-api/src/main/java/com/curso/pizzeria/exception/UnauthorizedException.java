package com.curso.pizzeria.exception;

/** Se traduce en 401 Unauthorized. */
public class UnauthorizedException extends RuntimeException {

	public UnauthorizedException(String message) {
		super(message);
	}
}
