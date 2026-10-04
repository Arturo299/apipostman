package com.curso.pizzeria.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.curso.pizzeria.model.Ingredient;

public interface IngredientRepository extends JpaRepository<Ingredient, Long> {

	boolean existsByNameIgnoreCase(String name);

	boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

	List<Ingredient> findAllByOrderByNameAsc();

	List<Ingredient> findByVegetarianOrderByNameAsc(boolean vegetarian);
}
