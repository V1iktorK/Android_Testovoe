extends Node2D
## «Станция-Падение» — рогалик-шутер на космической станции: процедурные комнаты,
## лут, боссы, прокачка. Космо-индастри стиль: тёмный металл, неоновые аварийные огни.

const Dungeon := preload("res://logic/dungeon_gen.gd")

enum State { MENU, MAP, ROOM, UPGRADE, GAMEOVER, VICTORY }

const COLORS := {
	"bg": Color(0.015, 0.03, 0.05),
	"room": Color(0.05, 0.12, 0.16),
	"combat": Color(0.2, 0.95, 1.0),
	"treasure": Color(1.0, 0.82, 0.25),
	"shop": Color(0.3, 0.85, 0.95),
	"lab": Color(0.35, 1.0, 0.75),
	"boss": Color(1.0, 0.45, 0.85),
	"entrance": Color(0.6, 0.65, 0.75),
}

var state: int = State.MENU
var rooms: Array = []
var current_room := 0
var hero_power := 40.0
var weapon_power := 12.0
var hull := 100.0
var score := 0
var depth := 1
var log_text := ""
var best_score := 0


func _ready() -> void:
	var cfg := ConfigFile.new()
	if cfg.load("user://station_save.cfg") == OK:
		best_score = int(cfg.get_value("run", "best", 0))


func save_best() -> void:
	if score > best_score:
		best_score = score
	var cfg := ConfigFile.new()
	cfg.set_value("run", "best", best_score)
	cfg.save("user://station_save.cfg")


func start_run() -> void:
	depth = 1
	hero_power = 40.0
	weapon_power = 12.0
	hull = 100.0
	score = 0
	build_depth()


func build_depth() -> void:
	rooms = Dungeon.generate(20261028 + depth * 13, depth)
	current_room = 0
	state = State.MAP


func enter_room(index: int) -> void:
	current_room = index
	var room: Dictionary = rooms[index]
	match room["kind"]:
		"combat", "boss":
			var damage: int = Dungeon.simulate_fight(hero_power, weapon_power, index * 31 + depth)
			var taken: float = maxf(5.0, room["enemy_power"] - damage * 0.25)
			hull -= taken
			score += damage
			log_text = "Бой: нанёс %d, получил %d урона" % [damage, int(taken)]
			if hull <= 0.0:
				hull = 0.0
				save_best()
				state = State.GAMEOVER
				return
		"treasure":
			if not room["loot"].is_empty():
				weapon_power += 4.0 * room["loot"].size()
				score += 150
				log_text = "Найдено снаряжение: %s (оружие +%d)" % [str(room["loot"]), 4 * room["loot"].size()]
			else:
				log_text = "Пустая сокровищница..."
		"shop":
			log_text = "Торговец: обменял припасы на ремонт (+25 корпуса)"
			hull = minf(100.0, hull + 25.0)
			score += 50
		"lab":
			log_text = "Лаборатория: калибровка оружия (+3 к урону)"
			weapon_power += 3.0
			score += 80
		_:
			log_text = "Шлюз станции. Путь дальше."
	if index == rooms.size() - 1:
		score += 500 * depth
		depth += 1
		if depth > 5:
			save_best()
			state = State.VICTORY
		else:
			state = State.UPGRADE
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
			start_run()
		State.MAP:
			# сетка комнат 4xN
			var col := int(pos.x / (w / 4.0))
			var row := int((pos.y - h * 0.18) / (h * 0.22))
			var idx := row * 4 + col
			if idx >= 0 and idx < rooms.size():
				enter_room(idx)
		State.UPGRADE:
			if pos.y > h * 0.4 and pos.y < h * 0.55:
				hero_power += 15.0
			elif pos.y > h * 0.6 and pos.y < h * 0.75:
				weapon_power += 8.0
			build_depth()
		State.GAMEOVER, State.VICTORY:
			state = State.MENU
	queue_redraw()


func _draw() -> void:
	var size := get_viewport_rect().size
	draw_rect(Rect2(Vector2.ZERO, size), COLORS["bg"])
	var font := ThemeDB.fallback_font
	match state:
		State.MENU:
			draw_string(font, Vector2(size.x / 2, size.y * 0.36), "СТАНЦИЯ-ПАДЕНИЕ", HORIZONTAL_ALIGNMENT_CENTER, -1, 84, Color(0.25, 1.0, 1.0))
			draw_string(font, Vector2(size.x / 2, size.y * 0.46), "рогалик-шутер: процедурные комнаты станции", HORIZONTAL_ALIGNMENT_CENTER, -1, 34, Color(0.8, 0.85, 0.95))
			draw_string(font, Vector2(size.x / 2, size.y * 0.58), "тап — начать забег", HORIZONTAL_ALIGNMENT_CENTER, -1, 38, Color(1, 0.85, 0.4))
		State.MAP:
			draw_map(size, font)
		State.UPGRADE:
			draw_string(font, Vector2(size.x / 2, size.y * 0.28), "Глубина %d пройдена!" % (depth - 1), HORIZONTAL_ALIGNMENT_CENTER, -1, 64, Color(0.5, 0.9, 0.6))
			draw_string(font, Vector2(size.x / 2, size.y * 0.48), "Улучшить силу (+15)", HORIZONTAL_ALIGNMENT_CENTER, -1, 44, Color.WHITE)
			draw_string(font, Vector2(size.x / 2, size.y * 0.68), "Улучшить оружие (+8)", HORIZONTAL_ALIGNMENT_CENTER, -1, 44, Color.WHITE)
		State.GAMEOVER:
			draw_string(font, Vector2(size.x / 2, size.y * 0.4), "КОРПУС РАЗРУШЕН", HORIZONTAL_ALIGNMENT_CENTER, -1, 76, Color(1, 0.4, 0.4))
			draw_string(font, Vector2(size.x / 2, size.y * 0.52), "очки: %d   рекорд: %d" % [score, best_score], HORIZONTAL_ALIGNMENT_CENTER, -1, 40, Color(0.9, 0.9, 1))
		State.VICTORY:
			draw_string(font, Vector2(size.x / 2, size.y * 0.4), "СТАНЦИЯ СПАСЕНА!", HORIZONTAL_ALIGNMENT_CENTER, -1, 76, Color(1, 0.85, 0.3))
			draw_string(font, Vector2(size.x / 2, size.y * 0.52), "очки: %d   рекорд: %d" % [score, best_score], HORIZONTAL_ALIGNMENT_CENTER, -1, 40, Color(0.9, 0.9, 1))


func draw_map(size: Vector2, font: Font) -> void:
	draw_string(font, Vector2(size.x / 2, size.y * 0.1), "Глубина %d   корпус: %d   оружие: %d   очки: %d" % [depth, int(hull), int(weapon_power), score],
		HORIZONTAL_ALIGNMENT_CENTER, -1, 34, Color.WHITE)
	for i in rooms.size():
		var col := i % 4
		var row := i / 4
		var r := Rect2(col * size.x / 4.0 + 20, size.y * 0.18 + row * size.y * 0.22, size.x / 4.0 - 40, size.y * 0.18)
		var kind: String = rooms[i]["kind"]
		draw_rect(r, COLORS.get(kind, Color.GRAY))
		draw_rect(r, Color(0, 0, 0, 0.35))
		draw_string(font, Vector2(r.position.x + 18, r.position.y + r.size.y * 0.4), kind, HORIZONTAL_ALIGNMENT_LEFT, -1, 26, Color(0.25, 1.0, 1.0))
		draw_string(font, Vector2(r.position.x + 18, r.position.y + r.size.y * 0.72), "угроза %d" % int(rooms[i]["enemy_power"]), HORIZONTAL_ALIGNMENT_LEFT, -1, 22, Color(0.9, 0.9, 0.9))
	draw_string(font, Vector2(size.x / 2, size.y * 0.93), log_text, HORIZONTAL_ALIGNMENT_CENTER, -1, 30, Color(1, 0.85, 0.5))
