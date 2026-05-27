package com.example.demo.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.validation.ValidationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	// 1. Handles Resource Missing Errors (404)
	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<Map<String, String>> handleResourceNotFound(ResourceNotFoundException ex) {
		Map<String, String> error = new HashMap<>();
		error.put("error", ex.getMessage());
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
	}

	// 2. Handles Infrastructure Missing Errors (404)
	@ExceptionHandler(InfrastructureNotFoundException.class)
	public ResponseEntity<Map<String, String>> handleInfrastructureNotFound(InfrastructureNotFoundException ex) {
		Map<String, String> error = new HashMap<>();
		error.put("error", ex.getMessage());
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
	}

	// 3. ✅ FIXES YOUR CURRENT ISSUE: Handles Business Rules & Status Logic Errors
	// (400)
	@ExceptionHandler(ValidationException.class)
	public ResponseEntity<Map<String, String>> handleBusinessValidation(ValidationException ex) {
		Map<String, String> error = new HashMap<>();
		error.put("error", ex.getMessage());
		return ResponseEntity.badRequest().body(error);
	}

	// 4. Handles DTO Input Validation Fields like @NotNull, @Min, @Size (400)
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, String>> handleDtoValidation(MethodArgumentNotValidException ex) {
		Map<String, String> errors = new HashMap<>();
		ex.getBindingResult().getFieldErrors()
				.forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
		return ResponseEntity.badRequest().body(errors);
	}

	// 5. Handles Broken/Malformed JSON & Bad Enum Strings (400)
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<Map<String, String>> handleJsonParseException(HttpMessageNotReadableException ex) {
		Map<String, String> errorResponse = new HashMap<>();

		if (ex.getCause() instanceof InvalidFormatException) {
			InvalidFormatException ife = (InvalidFormatException) ex.getCause();
			String fieldName = !ife.getPath().isEmpty() ? ife.getPath().get(0).getFieldName() : "field";

			if (ife.getTargetType().isEnum()) {
				Object[] enumConstants = ife.getTargetType().getEnumConstants();
				errorResponse.put(fieldName,
						"Invalid value. Accepted values are: " + java.util.Arrays.toString(enumConstants));
				return ResponseEntity.badRequest().body(errorResponse);
			}
		}

		errorResponse.put("error", "Malformed or unreadable JSON request body");
		return ResponseEntity.badRequest().body(errorResponse);
	}

	// 6. Global Catch-All Fallback for unexpected failures (500)
	@ExceptionHandler(Exception.class)
	public ResponseEntity<Map<String, String>> handleGlobalFallback(Exception ex) {
		Map<String, String> error = new HashMap<>();
		error.put("error", "An unexpected server error occurred: " + ex.getMessage());
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
	}
}