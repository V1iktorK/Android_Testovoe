extends RefCounted
## Тесты генератора препятствий и правил раннера (одна кнопка, ретро-футуризм).

const RunnerLogicScript = preload("res://logic/runner_logic.gd")


func test_generate_is_deterministic() -> Variant:
	var a: Array = RunnerLogicScript.generate(42, 30, 0.5)
	var b: Array = RunnerLogicScript.generate(42, 30, 0.5)
	if a.size() != b.size():
		return "разное число препятствий при одном сиде"
	for i in a.size():
		if a[i]["x"] != b[i]["x"] or a[i]["kind"] != b[i]["kind"]:
			return "генератор недетерминирован на элементе %d" % i
	return null


func test_gaps_are_passable() -> Variant:
	for seed_value in [1, 7, 42, 123, 999]:
		var obstacles: Array = RunnerLogicScript.generate(seed_value, 50, 1.0)
		for o in obstacles:
			if o["gap_h"] < RunnerLogicScript.MIN_GAP:
				return "проход уже минимума: %s" % str(o)
			if o["gap_y"] < 0.0 or o["gap_y"] + o["gap_h"] > 1000.0:
				return "проход вне экрана: %s" % str(o)
	return null


func test_obstacles_are_spaced_for_reachability() -> Variant:
	var obstacles: Array = RunnerLogicScript.generate(5, 50, 1.0)
	for i in range(1, obstacles.size()):
		var gap: float = obstacles[i]["x"] - obstacles[i - 1]["x"]
		if gap < 380.0:
			return "препятствия слишком близко: %f" % gap
	return null


func test_speed_grows_monotonically() -> Variant:
	var prev := RunnerLogicScript.speed_at(0.0)
	for d in [500.0, 1000.0, 2000.0, 5000.0, 12000.0]:
		var s: float = RunnerLogicScript.speed_at(d)
		if s < prev:
			return "скорость уменьшилась на дистанции %f" % d
		prev = s
	if prev > RunnerLogicScript.MAX_SPEED:
		return "скорость превысила предел"
	return null


func test_collision_detection() -> Variant:
	var obstacles: Array = [
		{"x": 100.0, "kind": "gap_low", "gap_y": 300.0, "gap_h": 200.0}
	]
	# Корабль на позиции препятствия, внутри прохода — столкновения нет
	if RunnerLogicScript.collides(400.0, obstacles, 100.0):
		return "ложное столкновение внутри прохода"
	# Корабль на позиции препятствия, над проходом — столкновение
	if not RunnerLogicScript.collides(200.0, obstacles, 100.0):
		return "не обнаружено столкновение над плиткой"
	# Корабль далеко по X — столкновения нет
	if RunnerLogicScript.collides(200.0, obstacles, 500.0):
		return "ложное столкновение вдали от препятствия"
	return null


func test_scoring_and_stars() -> Variant:
	if RunnerLogicScript.score_for(1000.0) != 100:
		return "неверный скоринг за 1000"
	if RunnerLogicScript.stars_for(0.0) != 0:
		return "на старте не должно быть звёзд"
	if RunnerLogicScript.stars_for(4000.0) <= RunnerLogicScript.stars_for(1500.0):
		return "звёзды должны расти с дистанцией"
	if RunnerLogicScript.stars_for(1e9) != 12:
		return "звёзды ограничены 12"
	return null
