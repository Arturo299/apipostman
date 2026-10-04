package com.curso.pizzeria.dto;

import java.math.BigDecimal;

import com.curso.pizzeria.model.Ingredient;

public record IngredientResponse(Long id, String name, BigDecimal cost, boolean vegetarian) {

	public static IngredientResponse from(Ingredient ingredient) {
		return new IngredientResponse(ingredient.getId(), ingredient.getName(), ingredient.getCost(),
				ingredient.isVegetarian());
	}
}
