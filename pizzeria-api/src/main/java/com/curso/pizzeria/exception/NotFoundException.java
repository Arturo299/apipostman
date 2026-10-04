package com.curso.pizzeria.exception;

/** Se traduce en 404 Not Found. */
public class NotFoundException extends RuntimeException {

	public NotFoundException(String message) {
		super(message);
	}
}
