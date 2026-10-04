package com.curso.pizzeria.exception;

/** Se traduce en 409 Conflict (nombre duplicado, recurso en uso...). */
public class ConflictException extends RuntimeException {

	public ConflictException(String message) {
		super(message);
	}
}
