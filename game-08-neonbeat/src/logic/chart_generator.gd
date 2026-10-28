class_name ChartGenerator
## Генератор нотных дорожек (ось C: генератор из BPM + процедурный синтез звука).
## Чистая логика без узлов сцены — тестируется headless-раннером.

const HIT_WINDOW_PERFECT := 0.055   # сек
const HIT_WINDOW_GOOD := 0.12       # сек
const LANES := 4


## Дорожка: массив нот { "time": float, "lane": int }. Детерминирована сидом и BPM.
static func generate(seed_value: int, bpm: float, difficulty: float, note_count: int) -> Array:
	var rng := RandomNumberGenerator.new()
	rng.seed = seed_value
	var notes: Array = []
	var beat := 60.0 / bpm
	var time := 2.0 # стартовая пауза
	for i in note_count:
		# Ритмическая грамматика: базовая доля, половинные и двойные ноты
		var pattern := rng.randf()
		if pattern < 0.15 * difficulty:
			time += beat * 0.5            # быстрая нота
		elif pattern > 0.9:
			time += beat * 2.0            # пауза
		else:
			time += beat
		var lane := rng.randi_range(0, LANES - 1)
		# На высокой сложности иногда две ноты одновременно
		notes.append({"time": time, "lane": lane})
		if difficulty > 0.5 and rng.randf() < 0.18 * difficulty:
			notes.append({"time": time, "lane": (lane + 2) % LANES})
	return notes


## Оценка нажатия по отклонению от идеального времени.
static func judge(delta: float) -> String:
	var d: float = absf(delta)
	if d <= HIT_WINDOW_PERFECT:
		return "perfect"
	if d <= HIT_WINDOW_GOOD:
		return "good"
	return "miss"


## Очки за ноту с учётом оценки и комбо (промах не даёт очков).
static func score_for(judgement: String, combo: int) -> int:
	var base: int = 0
	match judgement:
		"perfect":
			base = 100
		"good":
			base = 60
		_:
			return 0
	return base + mini(combo, 50) * 2


## Итоговая оценка по точности (доля perfect+good).
static func grade(accuracy: float) -> String:
	if accuracy >= 0.95:
		return "S"
	if accuracy >= 0.85:
		return "A"
	if accuracy >= 0.7:
		return "B"
	return "C"


## Скорость падения нот по сложности (для отрисовки).
static func note_speed(difficulty: float) -> float:
	return 320.0 + difficulty * 260.0
