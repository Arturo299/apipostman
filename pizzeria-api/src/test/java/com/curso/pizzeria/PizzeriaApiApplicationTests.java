package com.curso.pizzeria;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.curso.pizzeria.service.PizzaService;

@SpringBootTest
class PizzeriaApiApplicationTests {

	@Autowired
	PizzaService pizzaService;

	@Test
	void seedDataIsLoadedWithComputedPrices() {
		assertThat(pizzaService.findAll("Margarita", null, null))
				.singleElement()
				.satisfies(p -> {
					assertThat(p.ingredients()).hasSize(4);
					assertThat(p.price()).isEqualByComparingTo("5.16");
				});
	}
}
