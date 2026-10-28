extends RefCounted
## Тесты рогалика: генератор графа комнат, лут, бой, прокачка.
## Ось C уникальности: генератор графа комнат с лутом и боссами.

const Dungeon := preload("res://logic/dungeon_gen.gd")


func test_dungeon_is_deterministic() -> Variant:
	var a: Array = Dungeon.generate(42, 3)
	var b: Array = Dungeon.generate(42, 3)
	if a.size() != b.size():
		return "разное число комнат"
	for i in a.size():
		if a[i]["kind"] != b[i]["kind"]:
			return "генератор недетерминирован"
	return null


func test_rooms_form_connected_graph() -> Variant:
	for seed_value in [1, 9, 42, 77]:
		var rooms: Array = Dungeon.generate(seed_value, 4)
		if not Dungeon.is_connected_graph(rooms):
			return "граф комнат несвязен (seed=%d)" % seed_value
	return null


func test_first_room_is_entrance() -> Variant:
	var rooms: Array = Dungeon.generate(5, 2)
	if rooms[0]["kind"] != "entrance":
		return "первая комната — вход"
	return null


func test_last_room_is_boss() -> Variant:
	var rooms: Array = Dungeon.generate(5, 4)
	if rooms[-1]["kind"] != "boss":
		return "последняя комната — босс"
	return null


func test_loot_in_treasure_rooms() -> Variant:
	var rooms: Array = Dungeon.generate(11, 5)
	var found := false
	for r in rooms:
		if r["kind"] == "treasure" and not r["loot"].is_empty():
			found = true
	if not found:
		return "сокровищницы должны давать лут"
	return null


func test_difficulty_grows_with_depth() -> Variant:
	var easy: Array = Dungeon.generate(3, 1)
	var hard: Array = Dungeon.generate(3, 6)
	var easy_power: float = easy[-1]["enemy_power"]
	var hard_power: float = hard[-1]["enemy_power"]
	if hard_power <= easy_power:
		return "глубже — опаснее"
	return null


func test_combat_deterministic() -> Variant:
	var d1: int = Dungeon.simulate_fight(50.0, 20.0, 123)
	var d2: int = Dungeon.simulate_fight(50.0, 20.0, 123)
	if d1 != d2:
		return "бой недетерминирован"
	return null


func test_stronger_hero_wins_more() -> Variant:
	var weak: int = Dungeon.simulate_fight(30.0, 10.0, 7)
	var strong: int = Dungeon.simulate_fight(90.0, 30.0, 7)
	if strong <= weak:
		return "сильный герой должен побеждать чаще"
	return null


func test_upgrade_cost_grows() -> Variant:
	if Dungeon.upgrade_cost(2) <= Dungeon.upgrade_cost(1):
		return "цена прокачки должна расти"
	return null


func test_room_count_matches_depth() -> Variant:
	var rooms: Array = Dungeon.generate(2, 8)
	if rooms.size() < 8:
		return "число комнат растёт с глубиной"
	return null
