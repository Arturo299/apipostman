package com.curso.pizzeria.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.curso.pizzeria.model.Ingredient;

class PriceCalculatorTest {

	private final PriceCalculator calculator = new PriceCalculator(new BigDecimal("0.20"));

	private static Ingredient ingredient(String cost) {
		return new Ingredient("x" + cost, new BigDecimal(cost), true);
	}

	@Test
	void priceIsIngredientsCostPlusTwentyPercent() {
		List<Ingredient> margarita = List.of(ingredient("1.50"), ingredient("0.80"), ingredient("1.70"),
				ingredient("0.30"));

		assertThat(calculator.ingredientsCost(margarita)).isEqualByComparingTo("4.30");
		assertThat(calculator.price(margarita)).isEqualByComparingTo("5.16");
	}

	@Test
	void priceIsRoundedToCents() {
		assertThat(calculator.price(List.of(ingredient("0.33")))).isEqualByComparingTo("0.40");
	}

	@Test
	void emptyPizzaCostsNothing() {
		assertThat(calculator.price(List.of())).isEqualByComparingTo("0.00");
	}
}
