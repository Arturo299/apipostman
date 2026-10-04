package com.curso.pizzeria.web;

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

import com.curso.pizzeria.dto.IngredientRequest;
import com.curso.pizzeria.dto.IngredientResponse;
import com.curso.pizzeria.service.IngredientService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/ingredients")
@Tag(name = "Ingredientes")
public class IngredientController {

	private final IngredientService service;

	public IngredientController(IngredientService service) {
		this.service = service;
	}

	@GetMapping
	@Operation(summary = "Lista los ingredientes, opcionalmente filtrados por vegetariano")
	public List<IngredientResponse> findAll(@RequestParam(required = false) Boolean vegetarian) {
		return service.findAll(vegetarian);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Obtiene un ingrediente")
	public IngredientResponse findById(@PathVariable Long id) {
		return service.findById(id);
	}

	@PostMapping
	@Operation(summary = "Crea un ingrediente", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<IngredientResponse> create(@Valid @RequestBody IngredientRequest request) {
		IngredientResponse created = service.create(request);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{id}").buildAndExpand(created.id()).toUri();
		return ResponseEntity.created(location).body(created);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Modifica un ingrediente", security = @SecurityRequirement(name = "bearerAuth"))
	public IngredientResponse update(@PathVariable Long id, @Valid @RequestBody IngredientRequest request) {
		return service.update(id, request);
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Borra un ingrediente que no se use en ninguna pizza",
			security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		service.delete(id);
		return ResponseEntity.noContent().build();
	}
}
