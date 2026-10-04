-- Ingredientes: coste en euros
INSERT INTO ingredients (name, cost, vegetarian) VALUES
  ('Masa', 1.50, TRUE),
  ('Tomate', 0.80, TRUE),
  ('Mozzarella', 1.70, TRUE),
  ('Albahaca', 0.30, TRUE),
  ('Jamón york', 1.20, FALSE),
  ('Champiñones', 0.90, TRUE),
  ('Pepperoni', 1.60, FALSE),
  ('Cebolla', 0.40, TRUE),
  ('Pimiento', 0.60, TRUE),
  ('Aceitunas negras', 0.70, TRUE),
  ('Atún', 1.80, FALSE),
  ('Piña', 0.80, TRUE),
  ('Gorgonzola', 1.90, TRUE),
  ('Parmesano', 1.50, TRUE),
  ('Emmental', 1.30, TRUE);

INSERT INTO pizzas (name, description) VALUES
  ('Margarita', 'La clásica: tomate, mozzarella y albahaca fresca'),
  ('Prosciutto e funghi', 'Jamón york y champiñones'),
  ('Pepperoni', 'Tomate, mozzarella y pepperoni'),
  ('Quattro formaggi', 'Mozzarella, gorgonzola, parmesano y emmental'),
  ('Vegetal', 'Champiñones, cebolla, pimiento y aceitunas negras'),
  ('Hawaiana', 'Jamón york y piña'),
  ('Tonno', 'Atún y cebolla');

-- Relación pizza-ingrediente por nombre para no depender de los ids generados
INSERT INTO pizza_ingredients (pizza_id, ingredient_id)
SELECT p.id, i.id FROM pizzas p JOIN ingredients i ON
     (p.name = 'Margarita'           AND i.name IN ('Masa', 'Tomate', 'Mozzarella', 'Albahaca'))
  OR (p.name = 'Prosciutto e funghi' AND i.name IN ('Masa', 'Tomate', 'Mozzarella', 'Jamón york', 'Champiñones'))
  OR (p.name = 'Pepperoni'           AND i.name IN ('Masa', 'Tomate', 'Mozzarella', 'Pepperoni'))
  OR (p.name = 'Quattro formaggi'    AND i.name IN ('Masa', 'Tomate', 'Mozzarella', 'Gorgonzola', 'Parmesano', 'Emmental'))
  OR (p.name = 'Vegetal'             AND i.name IN ('Masa', 'Tomate', 'Mozzarella', 'Champiñones', 'Cebolla', 'Pimiento', 'Aceitunas negras'))
  OR (p.name = 'Hawaiana'            AND i.name IN ('Masa', 'Tomate', 'Mozzarella', 'Jamón york', 'Piña'))
  OR (p.name = 'Tonno'               AND i.name IN ('Masa', 'Tomate', 'Mozzarella', 'Atún', 'Cebolla'));
