class_name SiegeLogic
## Логика осады: игрок атакует крепость (инверсия tower defense).
## Ось C уникальности: процедурная карта пути + пазл расстановки башен и детерминированная симуляция боя.

const MAP_W := 12
const MAP_H := 8

## Типы юнитов игрока: воин, катапульта, виверна.
const UNITS := {
	"warrior": {"hp": 60.0, "damage": 8.0, "speed": 1.2, "cost": 50},
	"catapult": {"hp": 40.0, "damage": 22.0, "speed": 0.7, "cost": 90},
	"wyvern": {"hp": 90.0, "damage": 14.0, "speed": 1.6, "cost": 140},
}


## Карта осады: путь от лагеря (0) до крепости (11), башни защитников.
## Возвращает { "path": Array[Vector2i], "towers": Array[Dictionary] }.
static func generate_map(seed_value: int, difficulty: float) -> Dictionary:
	var rng := RandomNumberGenerator.new()
	rng.seed = seed_value
	var path: Array[Vector2i] = []
	var x := 0
	var y := rng.randi_range(2, MAP_H - 3)
	path.append(Vector2i(x, y))
	while x < MAP_W - 1:
		var step := rng.randf()
		if step < 0.35 and y > 1:
			y -= 1
		elif step < 0.7 and y < MAP_H - 2:
			y += 1
		else:
			x += 1
		if path[-1] != Vector2i(x, y):
			path.append(Vector2i(x, y))

	# Башни: рядом с путём, но не на нём
	var towers: Array[Dictionary] = []
	var tower_count := 2 + int(difficulty * 4.0)
	var tries := 0
	while towers.size() < tower_count and tries < 200:
		tries += 1
		var tx := rng.randi_range(1, MAP_W - 2)
		var ty := rng.randi_range(1, MAP_H - 2)
		if path.has(Vector2i(tx, ty)):
			continue
		var near_path := false
		for p in path:
			if absi(p.x - tx) + absi(p.y - ty) <= 2:
				near_path = true
				break
		if near_path:
			towers.append({
				"pos": Vector2i(tx, ty),
				"range": 2.0 + rng.randf() * 1.5,
				"damage": 6.0 + difficulty * 8.0 + rng.randf() * 4.0,
			})
	return {"path": path, "towers": towers}


## Проверка: путь соединяет лагерь и крепость, башни не стоят на пути.
static func validate_map(map: Dictionary) -> bool:
	var path: Array = map["path"]
	if path.is_empty():
		return false
	if path[0].x != 0 or path[-1].x != MAP_W - 1:
		return false
	for t in map["towers"]:
		if path.has(t["pos"]):
			return false
	return true


## Детерминированная симуляция волны: кто сколько урона нанёс крепости.
## army: массив { "type": String, "count": int }. Возвращает урон по крепости (float).
static func simulate_wave(map: Dictionary, army: Array, difficulty: float) -> float:
	var rng := RandomNumberGenerator.new()
	rng.seed = int(map["path"][0].y) * 131 + army.size() * 17
	var total_damage := 0.0
	for squad in army:
		var stats: Dictionary = UNITS[squad["type"]]
		var survivors: float = float(squad["count"])
		for tower in map["towers"]:
			var exposure: float = tower["range"] / float(map["path"].size())
			var incoming: float = tower["damage"] * exposure * survivors * 0.12
			survivors = maxf(0.0, survivors - incoming / stats["hp"] * survivors)
		total_damage += survivors * stats["damage"]
	return total_damage


## Урон крепости за волну с учётом её прочности.
static func fortress_damage(remaining_hp: float, wave_damage: float) -> float:
	return maxf(0.0, remaining_hp - wave_damage)


## Стоимость улучшения юнита уровня level (квадратичный рост).
static func upgrade_cost(level: int) -> int:
	return 100 * (level + 1) * (level + 1)


## Множитель урона от уровня улучшения.
static func upgrade_multiplier(level: int) -> float:
	return 1.0 + level * 0.25


## Звёзды за прохождение уровня: меньше волн — больше звёзд.
static func stars_for_waves(waves_used: int) -> int:
	if waves_used <= 3:
		return 3
	if waves_used <= 5:
		return 2
	return 1
