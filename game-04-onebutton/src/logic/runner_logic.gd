class_name RunnerLogic
## Чистая логика раннера одной кнопкой: генератор препятствий, физика, скоринг.
## (Ось C уникальности: грамматика паттернов препятствий — комбинаторная генерация.)

const MIN_GAP := 150.0          # минимальная высота прохода, достижимая для корабля
const BASE_SPEED := 320.0
const MAX_SPEED := 900.0
const GRAVITY := 2200.0
const IMPULSE := -820.0
const SHIP_RADIUS := 22.0

## Словарь паттернов: каждый задаёт форму препятствия на отрезке трассы.
const PATTERNS := ["gap_low", "gap_high", "spikes", "double_gap", "mine", "tunnel"]


## Детерминированная генерация препятствий: seed + словарь паттернов.
## Каждое препятствие: { "x": float, "kind": String, "gap_y": float, "gap_h": float }
static func generate(seed_value: int, count: int, difficulty: float) -> Array:
	var rng := RandomNumberGenerator.new()
	rng.seed = seed_value
	var obstacles: Array = []
	var x := 900.0
	for i in count:
		# Грамматика: на низкой сложности только простые паттерны
		var pool: Array = PATTERNS.slice(0, 2)
		if difficulty > 0.3:
			pool = PATTERNS.slice(0, 4)
		if difficulty > 0.6:
			pool = PATTERNS
		var kind: String = pool[rng.randi_range(0, pool.size() - 1)]
		var gap_h := rng.randf_range(MIN_GAP, MIN_GAP + 220.0 - 160.0 * difficulty)
		gap_h = maxf(gap_h, MIN_GAP)
		var gap_y := rng.randf_range(140.0, 860.0 - gap_h)
		obstacles.append({
			"x": x,
			"kind": kind,
			"gap_y": gap_y,
			"gap_h": gap_h,
		})
		# Шаг между препятствиями растёт с высотой прохода (проходимость)
		x += 420.0 + gap_h * 0.6 + rng.randf_range(0.0, 120.0)
	return obstacles


## Скорость корабля по дистанции: плавный рост до предела.
static func speed_at(distance: float) -> float:
	return minf(BASE_SPEED + distance * 0.045, MAX_SPEED)


## Прыжок: вертикальный импульс (одна кнопка).
static func impulse() -> float:
	return IMPULSE


static func gravity() -> float:
	return GRAVITY


## Проверка столкновения: корабль — круг в точке (ship_x = distance), препятствие — плитки с проходом.
static func collides(ship_y: float, obstacles: Array, distance: float) -> bool:
	for o in obstacles:
		var dx: float = o["x"] - distance
		if absf(dx) > 60.0:
			continue
		var gap_y: float = o["gap_y"]
		var gap_h: float = o["gap_h"]
		var in_gap := ship_y - SHIP_RADIUS > gap_y and ship_y + SHIP_RADIUS < gap_y + gap_h
		if not in_gap:
			return true
	return false


## Очки: дистанция + бонус за скорость.
static func score_for(distance: float) -> int:
	return int(distance / 10.0)


## Награда (звёзды) за дистанцию — открывают новые цвета корабля.
static func stars_for(distance: float) -> int:
	var stars := int(distance / 1500.0)
	return clampi(stars, 0, 12)
