class_name DungeonGen
## Генератор графа комнат космической станции (ось C: граф комнат с лутом и боссами).
## Чистая логика без узлов сцены — тестируется headless-раннером.

const ROOM_KINDS := ["entrance", "combat", "treasure", "shop", "lab", "boss"]
const WEAPONS := ["лазер", "плазма", "рельса", "импульс"]


## Генерация станции: массив комнат { kind, enemy_power, loot, connections }.
## Количество комнат растёт с глубиной; первая — вход, последняя — босс.
static func generate(seed_value: int, depth: int) -> Array:
	var rng := RandomNumberGenerator.new()
	rng.seed = seed_value * 7919 + depth * 104729
	var count := 3 + depth
	var rooms: Array = []
	for i in count:
		var kind := "combat"
		if i == 0:
			kind = "entrance"
		elif i == count - 1:
			kind = "boss"
		else:
			var roll := rng.randf()
			if roll < 0.25:
				kind = "treasure"
			elif roll < 0.4:
				kind = "shop"
			elif roll < 0.55:
				kind = "lab"
		var power: float = (10.0 + depth * 6.0) * (1.0 + i * 0.25)
		if kind == "boss":
			power *= 2.5
		var loot: Array = []
		if kind == "treasure":
			var loot_count := rng.randi_range(1, 3)
			for l in loot_count:
				loot.append(WEAPONS[rng.randi_range(0, WEAPONS.size() - 1)])
		rooms.append({
			"kind": kind,
			"enemy_power": power,
			"loot": loot,
			"connections": [],
		})
	# Связи: комната соединена со следующей + случайные перемычки
	for i in count - 1:
		rooms[i]["connections"].append(i + 1)
		rooms[i + 1]["connections"].append(i)
	for i in count:
		if rng.randf() < 0.3:
			var j := rng.randi_range(0, count - 1)
			if j != i and not rooms[i]["connections"].has(j):
				rooms[i]["connections"].append(j)
				rooms[j]["connections"].append(i)
	return rooms


## Связность графа комнат (обход в ширину из входа).
static func is_connected_graph(rooms: Array) -> bool:
	if rooms.is_empty():
		return false
	var seen := {}
	var queue := [0]
	seen[0] = true
	while not queue.is_empty():
		var cur: int = queue.pop_front()
		for nb in rooms[cur]["connections"]:
			if not seen.has(nb):
				seen[nb] = true
				queue.append(nb)
	return seen.size() == rooms.size()


## Детерминированная симуляция боя: урон, нанесённый героем.
static func simulate_fight(hero_power: float, weapon_power: float, seed_value: int) -> int:
	var rng := RandomNumberGenerator.new()
	rng.seed = seed_value
	var roll := rng.randf_range(0.5, 1.5)
	return int((hero_power + weapon_power) * roll)


## Стоимость прокачки уровня level (квадратичный рост).
static func upgrade_cost(level: int) -> int:
	return 120 * level * level
