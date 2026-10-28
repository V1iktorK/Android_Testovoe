extends RefCounted
## Тесты логики осады: карта, башни, симуляция боя, улучшения.

const Siege := preload("res://logic/siege_logic.gd")


func test_map_is_deterministic() -> Variant:
	var a: Dictionary = Siege.generate_map(42, 0.5)
	var b: Dictionary = Siege.generate_map(42, 0.5)
	if a["path"].size() != b["path"].size():
		return "разная длина пути при одном сиде"
	if a["towers"].size() != b["towers"].size():
		return "разное число башен при одном сиде"
	return null


func test_map_connects_camp_to_fortress() -> Variant:
	for seed_value in [1, 5, 42, 99, 123]:
		var map: Dictionary = Siege.generate_map(seed_value, 1.0)
		if not Siege.validate_map(map):
			return "карта не прошла валидацию (seed=%d)" % seed_value
	return null


func test_towers_not_on_path() -> Variant:
	var map: Dictionary = Siege.generate_map(7, 1.0)
	for t in map["towers"]:
		if map["path"].has(t["pos"]):
			return "башня стоит на пути"
	return null


func test_difficulty_adds_towers() -> Variant:
	var easy: Dictionary = Siege.generate_map(3, 0.1)
	var hard: Dictionary = Siege.generate_map(3, 1.0)
	if hard["towers"].size() < easy["towers"].size():
		return "высокая сложность должна давать больше башен"
	return null


func test_simulate_wave_is_deterministic() -> Variant:
	var map: Dictionary = Siege.generate_map(11, 0.6)
	var army := [{"type": "warrior", "count": 10}]
	var d1: float = Siege.simulate_wave(map, army, 0.6)
	var d2: float = Siege.simulate_wave(map, army, 0.6)
	if d1 != d2:
		return "симуляция недетерминирована"
	return null


func test_bigger_army_deals_more_damage() -> Variant:
	var map: Dictionary = Siege.generate_map(5, 0.5)
	var small: float = Siege.simulate_wave(map, [{"type": "warrior", "count": 5}], 0.5)
	var big: float = Siege.simulate_wave(map, [{"type": "warrior", "count": 25}], 0.5)
	if big <= small:
		return "большая армия должна наносить больше урона"
	return null


func test_fortress_damage_clamps_at_zero() -> Variant:
	if Siege.fortress_damage(10.0, 50.0) != 0.0:
		return "прочность не может быть отрицательной"
	if Siege.fortress_damage(80.0, 30.0) != 50.0:
		return "неверный расчёт прочности"
	return null


func test_upgrades() -> Variant:
	if Siege.upgrade_cost(1) <= Siege.upgrade_cost(0):
		return "цена улучшения должна расти"
	if Siege.upgrade_multiplier(3) <= Siege.upgrade_multiplier(0):
		return "множитель должен расти"
	if Siege.upgrade_multiplier(0) != 1.0:
		return "без улучшений множитель равен 1"
	return null


func test_stars_by_wave_count() -> Variant:
	if Siege.stars_for_waves(2) != 3 or Siege.stars_for_waves(5) != 2 or Siege.stars_for_waves(9) != 1:
		return "неверные звёзды за волны"
	return null
