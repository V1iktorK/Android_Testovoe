extends Node2D
## «Импульс-раннер» — аркада одной кнопкой (тап/пробел = прыжок).
## Ретро-футуризм: неоновый закат, перспективная сетка, светящиеся препятствия.

const RunnerLogic := preload("res://logic/runner_logic.gd")

enum State { MENU, PLAY, GAMEOVER }

const SKY_TOP := Color(0.09, 0.05, 0.24)
const SKY_BOTTOM := Color(0.85, 0.28, 0.45)
const SUN_COLOR := Color(1.0, 0.72, 0.25)
const GRID_COLOR := Color(0.25, 0.85, 0.95, 0.55)
const NEON_A := Color(0.2, 0.95, 0.95)
const NEON_B := Color(1.0, 0.35, 0.75)
const SHIP_COLOR := Color(1.0, 0.95, 0.6)

var state: int = State.MENU
var distance := 0.0
var ship_y := 360.0
var ship_vy := 0.0
var obstacles: Array = []
var generated_count := 0
var best_score := 0
var stars := 0
var flash := 0.0
var time_passed := 0.0

var save_path := "user://impulse_save.cfg"


func _ready() -> void:
	load_save()
	obstacles = RunnerLogic.generate(20261028, 40, 0.0)
	generated_count = 40


func load_save() -> void:
	var cfg := ConfigFile.new()
	if cfg.load(save_path) == OK:
		best_score = int(cfg.get_value("run", "best", 0))
		stars = int(cfg.get_value("run", "stars", 0))


func save_best() -> void:
	var cfg := ConfigFile.new()
	cfg.set_value("run", "best", maxi(best_score, RunnerLogic.score_for(distance)))
	cfg.set_value("run", "stars", stars)
	cfg.save(save_path)


func _input(event: InputEvent) -> void:
	if event.is_action_pressed("impulse"):
		match state:
			State.MENU:
				state = State.PLAY
				distance = 0.0
				ship_y = 360.0
				ship_vy = 0.0
				obstacles = RunnerLogic.generate(20261028, 40, 0.0)
				generated_count = 40
			State.PLAY:
				ship_vy = RunnerLogic.impulse()
			State.GAMEOVER:
				state = State.MENU
		queue_redraw()


func _process(delta: float) -> void:
	time_passed += delta
	flash = maxf(0.0, flash - delta * 2.0)
	if state == State.PLAY:
		var speed: float = RunnerLogic.speed_at(distance)
		distance += speed * delta
		ship_vy += RunnerLogic.gravity() * delta
		ship_y += ship_vy * delta
		if ship_y < 60.0:
			ship_y = 60.0
			ship_vy = 0.0
		if ship_y > 820.0:
			ship_y = 820.0
			ship_vy = 0.0
		if RunnerLogic.collides(ship_y, obstacles, distance):
			state = State.GAMEOVER
			flash = 1.0
			stars += RunnerLogic.stars_for(distance)
			save_best()
		# Догенерация препятствий: сложность растёт с дистанцией
		var difficulty := clampf(distance / 20000.0, 0.0, 1.0)
		if generated_count < 400 and obstacles.size() > 0 and obstacles[-1]["x"] - distance < 3000.0:
			var more: Array = RunnerLogic.generate(20261028 + generated_count, 20, difficulty)
			for o in more:
				obstacles.append(o)
			generated_count += 20
	queue_redraw()


func _draw() -> void:
	# Небо-градиент
	for i in 24:
		var t := i / 23.0
		draw_rect(Rect2(0, t * 420, 1280, 20), SKY_TOP.lerp(SKY_BOTTOM, t))
	# Солнце со штрихами
	draw_circle(Vector2(640, 420), 170, SUN_COLOR)
	for i in 6:
		draw_rect(Rect2(470, 300 + i * 42, 340, 14), SKY_TOP)
	# Перспективная сетка (ретро)
	var horizon := 420.0
	for i in 14:
		var x := -640.0 + i * 160.0 - fmod(distance * 0.6, 160.0)
		draw_line(Vector2(640, horizon), Vector2(x * 3.0 + 640, 720), GRID_COLOR, 2.0)
	for i in 8:
		var yy := horizon + pow(1.28, i) * 34.0
		draw_line(Vector2(0, yy), Vector2(1280, yy), GRID_COLOR, 2.0)
	# Препятствия
	for o in obstacles:
		var sx: float = o["x"] - distance + 640.0
		if sx < -200.0 or sx > 1500.0:
			continue
		var gap_y: float = o["gap_y"]
		var gap_h: float = o["gap_h"]
		var color := NEON_A if int(o["x"]) % 2 == 0 else NEON_B
		draw_rect(Rect2(sx - 40, 60, 80, gap_y - 60), color)
		draw_rect(Rect2(sx - 40, gap_y + gap_h, 80, 720 - gap_y - gap_h), color)
		draw_rect(Rect2(sx - 40, gap_y - 10, 80, 10), Color(1, 1, 1, 0.8))
		draw_rect(Rect2(sx - 40, gap_y + gap_h, 80, 10), Color(1, 1, 1, 0.8))
	# Корабль (треугольник со светящимся следом)
	var ship_pos := Vector2(640, ship_y)
	for i in 7:
		var a := 0.22 - i * 0.03
		draw_circle(ship_pos - Vector2(30 + i * 26, 0), 26 - i * 3, Color(1.0, 0.6, 0.3, a))
	draw_colored_polygon(PackedVector2Array([
		ship_pos + Vector2(34, 0), ship_pos + Vector2(-26, -22), ship_pos + Vector2(-26, 22)
	]), SHIP_COLOR)
	draw_circle(ship_pos + Vector2(2, 0), 9, Color(0.2, 0.4, 0.9))

	# Вспышка при столкновении
	if flash > 0.0:
		draw_rect(Rect2(0, 0, 1280, 720), Color(1, 0.3, 0.3, flash * 0.5))

	# Сканлайны ретро-ЭЛТ
	for sy in range(0, 720, 6):
		draw_rect(Rect2(0, sy, 1280, 2), Color(0, 0, 0, 0.18))
	# HUD
	var font := ThemeDB.fallback_font
	var score := RunnerLogic.score_for(distance)
	draw_string(font, Vector2(40, 80), "ДИСТАНЦИЯ %d" % score, HORIZONTAL_ALIGNMENT_LEFT, -1, 42, Color.WHITE)
	draw_string(font, Vector2(1240 - 380, 80), "РЕКОРД %d" % best_score, HORIZONTAL_ALIGNMENT_LEFT, -1, 36, Color(1, 0.85, 0.5))
	draw_string(font, Vector2(40, 690), "★ %d" % stars, HORIZONTAL_ALIGNMENT_LEFT, -1, 40, Color(1, 0.85, 0.5))

	match state:
		State.MENU:
			draw_rect(Rect2(0, 0, 1280, 720), Color(0, 0, 0, 0.45))
			draw_string(font, Vector2(640, 300), "ИМПУЛЬС-РАННЕР", HORIZONTAL_ALIGNMENT_CENTER, -1, 84, Color.WHITE)
			draw_string(font, Vector2(640, 380), "тап или пробел — прыжок", HORIZONTAL_ALIGNMENT_CENTER, -1, 40, Color(0.9, 0.9, 1))
			draw_string(font, Vector2(640, 440), "уклоняйся от барьеров и ставь рекорд", HORIZONTAL_ALIGNMENT_CENTER, -1, 32, Color(0.75, 0.8, 1))
		State.GAMEOVER:
			draw_rect(Rect2(0, 0, 1280, 720), Color(0, 0, 0, 0.5))
			draw_string(font, Vector2(640, 320), "ПОЛЁТ ЗАВЕРШЁН", HORIZONTAL_ALIGNMENT_CENTER, -1, 72, Color.WHITE)
			draw_string(font, Vector2(640, 400), "дистанция: %d   рекорд: %d" % [score, best_score], HORIZONTAL_ALIGNMENT_CENTER, -1, 42, Color(1, 0.85, 0.5))
			draw_string(font, Vector2(640, 460), "тап — в меню", HORIZONTAL_ALIGNMENT_CENTER, -1, 34, Color(0.85, 0.9, 1))
