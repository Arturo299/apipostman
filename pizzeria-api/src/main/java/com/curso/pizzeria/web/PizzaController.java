package com.curso.pizzeria.web;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.curso.pizzeria.dto.PizzaRequest;
import com.curso.pizzeria.dto.PizzaResponse;
import com.curso.pizzeria.service.PizzaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/pizzas")
@Tag(name = "Pizzas")
public class PizzaController {

	private final PizzaService service;

	public PizzaController(PizzaService service) {
		this.service = service;
	}

	@GetMapping
	@Operation(summary = "Lista las pizzas con su precio calculado (ingredientes + 20 %)")
	public List<PizzaResponse> findAll(
			@RequestParam(required = false) String name,
			@RequestParam(required = false) Boolean vegetarian,
			@RequestParam(required = false) BigDecimal maxPrice) {
		return service.findAll(name, vegetarian, maxPrice);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Obtiene una pizza con el desglose de su precio")
	public PizzaResponse findById(@PathVariable Long id) {
		return service.findById(id);
	}

	@PostMapping
	@Operation(summary = "Crea una pizza a partir de ids de ingredientes",
			security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<PizzaResponse> create(@Valid @RequestBody PizzaRequest request) {
		PizzaResponse created = service.create(request);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{id}").buildAndExpand(created.id()).toUri();
		return ResponseEntity.created(location).body(created);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Modifica una pizza", security = @SecurityRequirement(name = "bearerAuth"))
	public PizzaResponse update(@PathVariable Long id, @Valid @RequestBody PizzaRequest request) {
		return service.update(id, request);
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Borra una pizza", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		service.delete(id);
		return ResponseEntity.noContent().build();
	}
}
