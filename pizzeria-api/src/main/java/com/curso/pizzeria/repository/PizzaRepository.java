package com.curso.pizzeria.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.curso.pizzeria.model.Pizza;

public interface PizzaRepository extends JpaRepository<Pizza, Long> {

	boolean existsByNameIgnoreCase(String name);

	boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

	boolean existsByIngredientsId(Long ingredientId);

	List<Pizza> findAllByOrderByNameAsc();

	List<Pizza> findByNameContainingIgnoreCaseOrderByNameAsc(String name);
}
