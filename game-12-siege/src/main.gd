extends Node2D
## «Тёмная осада» — инверсия tower defense: игрок штурмует крепость тёмного королевства.
## Тёмное фэнтези: факелы, тёмные шпили башен, колонна войск.

const Siege := preload("res://logic/siege_logic.gd")

enum State { MENU, MAP_SELECT, DEPLOY, BATTLE, RESULT }

const COLORS := {
	"path": Color(0.28, 0.22, 0.16),
	"grass": Color(0.10, 0.13, 0.10),
	"tower": Color(0.20, 0.14, 0.28),
	"fortress": Color(0.35, 0.12, 0.12),
	"camp": Color(0.16, 0.28, 0.18),
	"warrior": Color(0.85, 0.65, 0.35),
	"catapult": Color(0.6, 0.45, 0.3),
	"wyvern": Color(0.65, 0.3, 0.75),
}

var state: int = State.MENU
var level := 1
var map: Dictionary = {}
var fortress_hp := 300.0
var fortress_max := 300.0
var gold := 200
var waves_used := 0
var wave_limit := 6
var army := {"warrior": 0, "catapult": 0, "wyvern": 0}
var upgrade_levels := {"warrior": 0, "catapult": 0, "wyvern": 0}
var battle_time := 0.0
var battle_damage := 0.0
var battle_result := ""
var best_stars := {}

func _ready() -> void:
	load_save()


func load_save() -> void:
	var cfg := ConfigFile.new()
	if cfg.load("user://siege_save.cfg") == OK:
		for i in range(1, 21):
			best_stars[i] = int(cfg.get_value("stars", "l" + str(i), 0))


func save_best(stars: int) -> void:
	if stars > int(best_stars.get(level, 0)):
		best_stars[level] = stars
	var cfg := ConfigFile.new()
	for k in best_stars:
		cfg.set_value("stars", "l" + str(k), best_stars[k])
	cfg.save("user://siege_save.cfg")


func start_level(l: int) -> void:
	level = l
	var difficulty := clampf(l / 20.0, 0.1, 1.0)
	map = Siege.generate_map(20261028 + l * 7, difficulty)
	fortress_max = 220.0 + l * 45.0
	fortress_hp = fortress_max
	gold = 200 + l * 30
	waves_used = 0
	army = {"warrior": 0, "catapult": 0, "wyvern": 0}
	state = State.DEPLOY


func launch_wave() -> void:
	if army["warrior"] + army["catapult"] + army["wyvern"] == 0:
		return
	var performance := fortress_hp / fortress_max
	var damage: float = Siege.simulate_wave(map, [
		{"type": "warrior", "count": army["warrior"]},
		{"type": "catapult", "count": army["catapult"]},
		{"type": "wyvern", "count": army["wyvern"]},
	], 1.0 - performance)
	# улучшения множат урон
	var mult := 1.0
	for t in army:
		mult += (Siege.upgrade_multiplier(upgrade_levels[t]) - 1.0) * army[t] / 10.0
	battle_damage = damage * mult
	battle_time = 0.0
	waves_used += 1
	state = State.BATTLE


func _process(delta: float) -> void:
	if state != State.BATTLE:
		return
	battle_time += delta * 0.35
	if battle_time >= 1.0:
		fortress_hp = Siege.fortress_damage(fortress_hp, battle_damage)
		gold += int(battle_damage * 0.4)
		army = {"warrior": 0, "catapult": 0, "wyvern": 0}
		if fortress_hp <= 0.0:
			battle_result = "Крепость пала!"
			save_best(Siege.stars_for_waves(waves_used))
			state = State.RESULT
		elif waves_used >= wave_limit:
			battle_result = "Волны закончились"
			state = State.RESULT
		else:
			state = State.DEPLOY
	queue_redraw()


func _input(event: InputEvent) -> void:
	if event is InputEventMouseButton and event.pressed:
		_handle_click(event.position)
	elif event is InputEventScreenTouch and event.pressed:
		_handle_click(event.position)


func _handle_click(pos: Vector2) -> void:
	var w := get_viewport_rect().size.x
	var h := get_viewport_rect().size.y
	match state:
		State.MENU:
			state = State.MAP_SELECT
		State.MAP_SELECT:
			var idx := int((pos.x - w * 0.05) / (w * 0.15)) + 1 + int((pos.y - h * 0.15) / (h * 0.16)) * 6
			if idx >= 1 and idx <= 20:
				start_level(idx)
		State.DEPLOY:
			# Кнопки найма внизу
			if pos.y > h * 0.82:
				var col := int(pos.x / (w / 4.0))
				match col:
					0:
						if gold >= Siege.UNITS["warrior"]["cost"]:
							gold -= Siege.UNITS["warrior"]["cost"]
							army["warrior"] += 1
					1:
						if gold >= Siege.UNITS["catapult"]["cost"]:
							gold -= Siege.UNITS["catapult"]["cost"]
							army["catapult"] += 1
					2:
						if gold >= Siege.UNITS["wyvern"]["cost"]:
							gold -= Siege.UNITS["wyvern"]["cost"]
							army["wyvern"] += 1
					3:
						launch_wave()
			# Улучшения вверху
			elif pos.y < h * 0.14:
				var col2 := int(pos.x / (w / 3.0))
				var unit: String = ["warrior", "catapult", "wyvern"][clampi(col2, 0, 2)]
				var cost: int = Siege.upgrade_cost(upgrade_levels[unit])
				if gold >= cost:
					gold -= cost
					upgrade_levels[unit] += 1
		State.RESULT:
			state = State.MAP_SELECT
	queue_redraw()


func _draw() -> void:
	var size := get_viewport_rect().size
	draw_rect(Rect2(Vector2.ZERO, size), COLORS["grass"])
	var font := ThemeDB.fallback_font
	match state:
		State.MENU:
			draw_string(font, Vector2(size.x / 2, size.y * 0.35), "ТЁМНАЯ ОСАДА", HORIZONTAL_ALIGNMENT_CENTER, -1, 88, Color(0.9, 0.35, 0.3))
			draw_string(font, Vector2(size.x / 2, size.y * 0.46), "ты — атакующая сторона: штурмуй крепость тьмы", HORIZONTAL_ALIGNMENT_CENTER, -1, 34, Color(0.8, 0.75, 0.7))
			draw_string(font, Vector2(size.x / 2, size.y * 0.58), "тап — выбрать уровень", HORIZONTAL_ALIGNMENT_CENTER, -1, 36, Color(1, 0.75, 0.4))
		State.MAP_SELECT:
			draw_string(font, Vector2(size.x / 2, size.y * 0.08), "Выбор уровня", HORIZONTAL_ALIGNMENT_CENTER, -1, 60, Color.WHITE)
			for i in range(1, 21):
				var col := (i - 1) % 6
				var row := (i - 1) / 6
				var r := Rect2(size.x * 0.05 + col * size.x * 0.15, size.y * 0.15 + row * size.y * 0.16, size.x * 0.13, size.y * 0.12)
				draw_rect(r, Color(0.22, 0.12, 0.12))
				draw_string(font, Vector2(r.position.x + r.size.x * 0.25, r.position.y + r.size.y * 0.55), str(i), HORIZONTAL_ALIGNMENT_LEFT, -1, 40, Color.WHITE)
				var stars := int(best_stars.get(i, 0))
				draw_string(font, Vector2(r.position.x + r.size.x * 0.2, r.position.y + r.size.y * 0.85), "*".repeat(stars), HORIZONTAL_ALIGNMENT_LEFT, -1, 28, Color(1, 0.85, 0.4))
		State.DEPLOY, State.BATTLE, State.RESULT:
			draw_map(size, font)


func draw_map(size: Vector2, font: Font) -> void:
	var cell := size.x / float(Siege.MAP_W)
	# путь
	for p in map["path"]:
		draw_rect(Rect2(p.x * cell, size.y * 0.18 + p.y * cell * 0.7, cell, cell * 0.7), COLORS["path"])
	# башни
	for t in map["towers"]:
		var tp: Vector2i = t["pos"]
		draw_rect(Rect2(tp.x * cell, size.y * 0.18 + tp.y * cell * 0.7, cell, cell * 0.7), COLORS["tower"])
		draw_circle(Vector2(tp.x * cell + cell / 2, size.y * 0.18 + tp.y * cell * 0.35), cell * 0.22, Color(0.5, 0.2, 0.6))
	# лагерь и крепость
	draw_rect(Rect2(0, size.y * 0.18, cell, size.y * 0.7), COLORS["camp"])
	draw_rect(Rect2((Siege.MAP_W - 1) * cell, size.y * 0.18, cell, size.y * 0.7), COLORS["fortress"])

	# колонна войск (анимация марша)
	if state == State.BATTLE:
		var total: int = army["warrior"] + army["catapult"] + army["wyvern"]
		for i in range(mini(total, 12)):
			var tt := clampf(battle_time - i * 0.05, 0.0, 1.0)
			var px := lerpf(cell * 0.5, (Siege.MAP_W - 1.5) * cell, tt)
			var py: float = size.y * 0.18 + map["path"][int(tt * (map["path"].size() - 1))].y * cell * 0.7
			draw_circle(Vector2(px, py), cell * 0.18, COLORS["warrior"])

	# HUD
	draw_string(font, Vector2(size.x * 0.02, size.y * 0.08), "Уровень %d   волна %d/%d" % [level, waves_used + 1, wave_limit], HORIZONTAL_ALIGNMENT_LEFT, -1, 36, Color.WHITE)
	draw_string(font, Vector2(size.x * 0.55, size.y * 0.08), "казна: %d золота" % gold, HORIZONTAL_ALIGNMENT_LEFT, -1, 34, Color(1, 0.85, 0.4))
	# полоса прочности крепости
	draw_rect(Rect2(size.x * 0.02, size.y * 0.11, size.x * 0.45, 26), Color(0.3, 0.1, 0.1))
	draw_rect(Rect2(size.x * 0.02, size.y * 0.11, size.x * 0.45 * clampf(fortress_hp / fortress_max, 0, 1), 26), Color(0.85, 0.25, 0.2))

	if state == State.DEPLOY:
		var units := ["warrior", "catapult", "wyvern"]
		var names := ["Воин", "Катапульта", "Виверна"]
		for i in 3:
			var x := size.x * 0.02 + i * size.x * 0.25
			draw_rect(Rect2(x, size.y * 0.84, size.x * 0.23, size.y * 0.12), Color(0.16, 0.12, 0.2))
			draw_string(font, Vector2(x + 12, size.y * 0.88), "%s  x%d" % [names[i], army[units[i]]], HORIZONTAL_ALIGNMENT_LEFT, -1, 30, COLORS[units[i]])
			draw_string(font, Vector2(x + 12, size.y * 0.93), "%d зол. · ур. %d" % [Siege.UNITS[units[i]]["cost"], upgrade_levels[units[i]]], HORIZONTAL_ALIGNMENT_LEFT, -1, 24, Color(0.8, 0.75, 0.7))
		draw_rect(Rect2(size.x * 0.77, size.y * 0.84, size.x * 0.21, size.y * 0.12), Color(0.6, 0.2, 0.15))
		draw_string(font, Vector2(size.x * 0.79, size.y * 0.92), "В АТАКУ!", HORIZONTAL_ALIGNMENT_LEFT, -1, 36, Color.WHITE)
		draw_string(font, Vector2(size.x * 0.02, size.y * 0.02), "улучшения: тап вверху по колонке юнита", HORIZONTAL_ALIGNMENT_LEFT, -1, 22, Color(0.65, 0.6, 0.6))
	elif state == State.RESULT:
		draw_rect(Rect2(0, 0, size.x, size.y), Color(0, 0, 0, 0.55))
		draw_string(font, Vector2(size.x / 2, size.y * 0.4), battle_result, HORIZONTAL_ALIGNMENT_CENTER, -1, 72, Color(1, 0.5, 0.4))
		var stars := Siege.stars_for_waves(waves_used)
		draw_string(font, Vector2(size.x / 2, size.y * 0.5), "*".repeat(stars), HORIZONTAL_ALIGNMENT_CENTER, -1, 64, Color(1, 0.85, 0.4))
		draw_string(font, Vector2(size.x / 2, size.y * 0.6), "тап — к уровням", HORIZONTAL_ALIGNMENT_CENTER, -1, 36, Color.WHITE)
