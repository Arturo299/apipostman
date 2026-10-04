package com.curso.pizzeria.service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.curso.pizzeria.dto.IngredientResponse;
import com.curso.pizzeria.dto.PizzaRequest;
import com.curso.pizzeria.dto.PizzaResponse;
import com.curso.pizzeria.exception.BusinessRuleException;
import com.curso.pizzeria.exception.ConflictException;
import com.curso.pizzeria.exception.NotFoundException;
import com.curso.pizzeria.model.Ingredient;
import com.curso.pizzeria.model.Pizza;
import com.curso.pizzeria.repository.IngredientRepository;
import com.curso.pizzeria.repository.PizzaRepository;

@Service
@Transactional
public class PizzaService {

	private final PizzaRepository pizzas;
	private final IngredientRepository ingredients;
	private final PriceCalculator priceCalculator;

	public PizzaService(PizzaRepository pizzas, IngredientRepository ingredients, PriceCalculator priceCalculator) {
		this.pizzas = pizzas;
		this.ingredients = ingredients;
		this.priceCalculator = priceCalculator;
	}

	@Transactional(readOnly = true)
	public List<PizzaResponse> findAll(String name, Boolean vegetarian, BigDecimal maxPrice) {
		List<Pizza> result = name == null || name.isBlank()
				? pizzas.findAllByOrderByNameAsc()
				: pizzas.findByNameContainingIgnoreCaseOrderByNameAsc(name.trim());
		return result.stream()
				.filter(p -> vegetarian == null || p.isVegetarian() == vegetarian)
				.map(this::toResponse)
				.filter(p -> maxPrice == null || p.price().compareTo(maxPrice) <= 0)
				.toList();
	}

	@Transactional(readOnly = true)
	public PizzaResponse findById(Long id) {
		return toResponse(get(id));
	}

	public PizzaResponse create(PizzaRequest request) {
		String name = request.name().trim();
		if (pizzas.existsByNameIgnoreCase(name)) {
			throw new ConflictException("Ya existe una pizza con el nombre '" + name + "'");
		}
		Pizza pizza = new Pizza(name, request.description());
		pizza.setIngredients(resolveIngredients(request.ingredientIds()));
		return toResponse(pizzas.save(pizza));
	}

	public PizzaResponse update(Long id, PizzaRequest request) {
		Pizza pizza = get(id);
		String name = request.name().trim();
		if (pizzas.existsByNameIgnoreCaseAndIdNot(name, id)) {
			throw new ConflictException("Ya existe una pizza con el nombre '" + name + "'");
		}
		pizza.setName(name);
		pizza.setDescription(request.description());
		pizza.setIngredients(resolveIngredients(request.ingredientIds()));
		return toResponse(pizza);
	}

	public void delete(Long id) {
		pizzas.delete(get(id));
	}

	private Pizza get(Long id) {
		return pizzas.findById(id).orElseThrow(() -> new NotFoundException("No existe la pizza con id " + id));
	}

	private Set<Ingredient> resolveIngredients(Set<Long> ids) {
		List<Ingredient> found = ingredients.findAllById(ids);
		if (found.size() != ids.size()) {
			Set<Long> missing = new TreeSet<>(ids);
			found.forEach(i -> missing.remove(i.getId()));
			throw new BusinessRuleException("Ingredientes inexistentes: " + missing);
		}
		return new LinkedHashSet<>(found);
	}

	private PizzaResponse toResponse(Pizza pizza) {
		List<IngredientResponse> ingredientList = pizza.getIngredients().stream()
				.sorted(Comparator.comparing(Ingredient::getName))
				.map(IngredientResponse::from)
				.toList();
		return new PizzaResponse(
				pizza.getId(),
				pizza.getName(),
				pizza.getDescription(),
				pizza.isVegetarian(),
				ingredientList,
				priceCalculator.ingredientsCost(pizza.getIngredients()),
				priceCalculator.getProfitMargin(),
				priceCalculator.price(pizza.getIngredients()));
	}
}
