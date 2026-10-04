package com.curso.pizzeria.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.curso.pizzeria.dto.IngredientRequest;
import com.curso.pizzeria.dto.IngredientResponse;
import com.curso.pizzeria.exception.ConflictException;
import com.curso.pizzeria.exception.NotFoundException;
import com.curso.pizzeria.model.Ingredient;
import com.curso.pizzeria.repository.IngredientRepository;
import com.curso.pizzeria.repository.PizzaRepository;

@Service
@Transactional
public class IngredientService {

	private final IngredientRepository ingredients;
	private final PizzaRepository pizzas;

	public IngredientService(IngredientRepository ingredients, PizzaRepository pizzas) {
		this.ingredients = ingredients;
		this.pizzas = pizzas;
	}

	@Transactional(readOnly = true)
	public List<IngredientResponse> findAll(Boolean vegetarian) {
		List<Ingredient> result = vegetarian == null
				? ingredients.findAllByOrderByNameAsc()
				: ingredients.findByVegetarianOrderByNameAsc(vegetarian);
		return result.stream().map(IngredientResponse::from).toList();
	}

	@Transactional(readOnly = true)
	public IngredientResponse findById(Long id) {
		return IngredientResponse.from(get(id));
	}

	public IngredientResponse create(IngredientRequest request) {
		String name = request.name().trim();
		if (ingredients.existsByNameIgnoreCase(name)) {
			throw new ConflictException("Ya existe un ingrediente con el nombre '" + name + "'");
		}
		Ingredient saved = ingredients.save(new Ingredient(name, normalize(request.cost()), request.vegetarian()));
		return IngredientResponse.from(saved);
	}

	public IngredientResponse update(Long id, IngredientRequest request) {
		Ingredient ingredient = get(id);
		String name = request.name().trim();
		if (ingredients.existsByNameIgnoreCaseAndIdNot(name, id)) {
			throw new ConflictException("Ya existe un ingrediente con el nombre '" + name + "'");
		}
		ingredient.setName(name);
		ingredient.setCost(normalize(request.cost()));
		ingredient.setVegetarian(request.vegetarian());
		return IngredientResponse.from(ingredient);
	}

	public void delete(Long id) {
		Ingredient ingredient = get(id);
		if (pizzas.existsByIngredientsId(id)) {
			throw new ConflictException(
					"El ingrediente '" + ingredient.getName() + "' se usa en alguna pizza y no se puede borrar");
		}
		ingredients.delete(ingredient);
	}

	private static BigDecimal normalize(BigDecimal cost) {
		return cost.setScale(2, RoundingMode.HALF_UP);
	}

	Ingredient get(Long id) {
		return ingredients.findById(id)
				.orElseThrow(() -> new NotFoundException("No existe el ingrediente con id " + id));
	}
}
