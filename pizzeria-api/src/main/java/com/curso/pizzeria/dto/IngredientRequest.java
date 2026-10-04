package com.curso.pizzeria.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record IngredientRequest(
		@NotBlank @Size(max = 60) String name,
		@NotNull @DecimalMin(value = "0.01") @DecimalMax(value = "100.00") @Digits(integer = 6, fraction = 2) BigDecimal cost,
		@NotNull Boolean vegetarian) {
}
