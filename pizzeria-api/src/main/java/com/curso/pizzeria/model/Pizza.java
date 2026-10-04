package com.curso.pizzeria.model;

import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

@Entity
@Table(name = "pizzas")
public class Pizza {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 60)
	private String name;

	@Column(length = 255)
	private String description;

	@ManyToMany
	@JoinTable(name = "pizza_ingredients",
			joinColumns = @JoinColumn(name = "pizza_id"),
			inverseJoinColumns = @JoinColumn(name = "ingredient_id"))
	@OrderBy("name ASC")
	private Set<Ingredient> ingredients = new LinkedHashSet<>();

	protected Pizza() {
	}

	public Pizza(String name, String description) {
		this.name = name;
		this.description = description;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Set<Ingredient> getIngredients() {
		return ingredients;
	}

	public void setIngredients(Set<Ingredient> ingredients) {
		this.ingredients.clear();
		this.ingredients.addAll(ingredients);
	}

	public boolean isVegetarian() {
		return ingredients.stream().allMatch(Ingredient::isVegetarian);
	}
}
