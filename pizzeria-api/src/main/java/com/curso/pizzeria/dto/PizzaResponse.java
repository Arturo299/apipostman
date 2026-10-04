package com.curso.pizzeria.dto;

import java.math.BigDecimal;
import java.util.List;

public record PizzaResponse(
		Long id,
		String name,
		String description,
		boolean vegetarian,
		List<IngredientResponse> ingredients,
		BigDecimal ingredientsCost,
		BigDecimal profitMargin,
		BigDecimal price) {
}
