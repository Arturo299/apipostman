package com.curso.pizzeria.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.curso.pizzeria.model.Ingredient;

/**
 * Precio de una pizza = suma del coste de sus ingredientes + margen de beneficio (20 % por defecto).
 */
@Component
public class PriceCalculator {

	private final BigDecimal profitMargin;

	public PriceCalculator(@Value("${pizzeria.pricing.profit-margin:0.20}") BigDecimal profitMargin) {
		this.profitMargin = profitMargin;
	}

	public BigDecimal ingredientsCost(Collection<Ingredient> ingredients) {
		return ingredients.stream()
				.map(Ingredient::getCost)
				.reduce(BigDecimal.ZERO, BigDecimal::add)
				.setScale(2, RoundingMode.HALF_UP);
	}

	public BigDecimal price(Collection<Ingredient> ingredients) {
		return ingredientsCost(ingredients)
				.multiply(BigDecimal.ONE.add(profitMargin))
				.setScale(2, RoundingMode.HALF_UP);
	}

	public BigDecimal getProfitMargin() {
		return profitMargin;
	}
}
