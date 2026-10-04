package com.curso.pizzeria.web;

import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.curso.pizzeria.dto.ErrorResponse;
import com.curso.pizzeria.exception.BusinessRuleException;
import com.curso.pizzeria.exception.ConflictException;
import com.curso.pizzeria.exception.NotFoundException;
import com.curso.pizzeria.exception.UnauthorizedException;

import jakarta.servlet.http.HttpServletRequest;

/** Todas las respuestas de error comparten el mismo formato JSON. */
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException ex, HttpServletRequest request) {
		List<ErrorResponse.FieldError> errors = ex.getBindingResult().getFieldErrors().stream()
				.map(e -> new ErrorResponse.FieldError(e.getField(), e.getDefaultMessage()))
				.toList();
		return build(HttpStatus.BAD_REQUEST, "La petición contiene datos no válidos", request, errors);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ResponseEntity<ErrorResponse> unreadable(HttpServletRequest request) {
		return build(HttpStatus.BAD_REQUEST, "El cuerpo de la petición no es un JSON válido", request, List.of());
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	ResponseEntity<ErrorResponse> typeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
		return build(HttpStatus.BAD_REQUEST, "Valor no válido para '" + ex.getName() + "'", request, List.of());
	}

	@ExceptionHandler(UnauthorizedException.class)
	ResponseEntity<ErrorResponse> unauthorized(UnauthorizedException ex, HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.header(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
				.body(body(HttpStatus.UNAUTHORIZED, ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler({ NotFoundException.class, NoResourceFoundException.class })
	ResponseEntity<ErrorResponse> notFound(Exception ex, HttpServletRequest request) {
		String message = ex instanceof NotFoundException ? ex.getMessage() : "Recurso no encontrado";
		return build(HttpStatus.NOT_FOUND, message, request, List.of());
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	ResponseEntity<ErrorResponse> methodNotAllowed(HttpRequestMethodNotSupportedException ex,
			HttpServletRequest request) {
		return build(HttpStatus.METHOD_NOT_ALLOWED, ex.getMessage(), request, List.of());
	}

	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	ResponseEntity<ErrorResponse> unsupportedMediaType(HttpMediaTypeNotSupportedException ex,
			HttpServletRequest request) {
		return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Content-Type no soportado: usa application/json", request,
				List.of());
	}

	@ExceptionHandler(ConflictException.class)
	ResponseEntity<ErrorResponse> conflict(ConflictException ex, HttpServletRequest request) {
		return build(HttpStatus.CONFLICT, ex.getMessage(), request, List.of());
	}

	@ExceptionHandler(BusinessRuleException.class)
	ResponseEntity<ErrorResponse> businessRule(BusinessRuleException ex, HttpServletRequest request) {
		return build(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage(), request, List.of());
	}

	private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, HttpServletRequest request,
			List<ErrorResponse.FieldError> errors) {
		return ResponseEntity.status(status).body(body(status, message, request, errors));
	}

	private ErrorResponse body(HttpStatus status, String message, HttpServletRequest request,
			List<ErrorResponse.FieldError> errors) {
		return new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), message,
				request.getRequestURI(), errors);
	}
}
