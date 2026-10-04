package com.curso.pizzeria.exception;

/** Se traduce en 422 Unprocessable Content: la petición es sintácticamente válida pero no se puede procesar. */
public class BusinessRuleException extends RuntimeException {

	public BusinessRuleException(String message) {
		super(message);
	}
}
