extends RefCounted
## Тесты ритм-логики: генератор дорожек, окна попаданий, скоринг, оценки.

const Chart := preload("res://logic/chart_generator.gd")


func test_generate_is_deterministic() -> Variant:
	var a: Array = Chart.generate(42, 120.0, 0.5, 100)
	var b: Array = Chart.generate(42, 120.0, 0.5, 100)
	if a.size() != b.size():
		return "разное число нот при одном сиде"
	for i in a.size():
		if a[i]["time"] != b[i]["time"] or a[i]["lane"] != b[i]["lane"]:
			return "генератор недетерминирован на ноте %d" % i
	return null


func test_notes_are_sorted_by_time() -> Variant:
	var notes: Array = Chart.generate(7, 140.0, 0.8, 200)
	for i in range(1, notes.size()):
		if notes[i]["time"] < notes[i - 1]["time"]:
			return "ноты не отсортированы по времени"
	return null


func test_lanes_are_valid() -> Variant:
	var notes: Array = Chart.generate(3, 100.0, 1.0, 150)
	for n in notes:
		if n["lane"] < 0 or n["lane"] >= Chart.LANES:
			return "полоса вне диапазона: %s" % str(n)
	return null


func test_difficulty_increases_density() -> Variant:
	var easy: Array = Chart.generate(11, 120.0, 0.1, 0)
	var hard: Array = Chart.generate(11, 120.0, 1.0, 0)
	# при note_count=0 генерация всё равно строит по времени — проверяем время ноты по счёту
	var easy_chart: Array = Chart.generate(11, 120.0, 0.1, 120)
	var hard_chart: Array = Chart.generate(11, 120.0, 1.0, 120)
	var easy_span: float = easy_chart[-1]["time"]
	var hard_span: float = hard_chart[-1]["time"]
	if hard_span >= easy_span:
		return "высокая сложность должна давать более плотную дорожку"
	return null


func test_judge_windows() -> Variant:
	if Chart.judge(0.02) != "perfect":
		return "20 мс — perfect"
	if Chart.judge(0.09) != "good":
		return "90 мс — good"
	if Chart.judge(0.3) != "miss":
		return "300 мс — miss"
	if Chart.judge(-0.02) != "perfect":
		return "отрицательное отклонение тоже учитывается"
	return null


func test_score_grows_with_combo() -> Variant:
	var low: int = Chart.score_for("perfect", 5)
	var high: int = Chart.score_for("perfect", 25)
	if high <= low:
		return "комбо должно увеличивать очки"
	if Chart.score_for("miss", 10) != 0:
		return "промах не даёт очков"
	return null


func test_grades() -> Variant:
	if Chart.grade(0.98) != "S" or Chart.grade(0.9) != "A":
		return "неверные высокие оценки"
	if Chart.grade(0.75) != "B" or Chart.grade(0.4) != "C":
		return "неверные низкие оценки"
	return null


func test_note_speed_grows() -> Variant:
	if Chart.note_speed(0.0) >= Chart.note_speed(1.0):
		return "скорость нот должна расти со сложностью"
	return null
