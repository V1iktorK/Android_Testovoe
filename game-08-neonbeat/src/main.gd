extends Node2D
## «Неон-Бит» — ритм-игра: ноты падают по 4 полосам, тапы в такт.
## Неоновый музыкальный стиль + процедурный синтез саундтрека (AudioStreamWAV).

const Chart := preload("res://logic/chart_generator.gd")

enum State { MENU, TRACK_SELECT, PLAY, RESULTS }

const LANE_COLORS := [Color(0.2, 0.95, 0.95), Color(1.0, 0.35, 0.75), Color(0.55, 0.9, 0.3), Color(0.95, 0.8, 0.25)]
const BG := Color(0.05, 0.04, 0.12)

# Треки: BPM и сложность
const TRACKS := [
	{"name": "Разгон", "bpm": 96.0, "diff": 0.15},
	{"name": "Пульс", "bpm": 108.0, "diff": 0.3},
	{"name": "Контур", "bpm": 118.0, "diff": 0.45},
	{"name": "Импульс", "bpm": 126.0, "diff": 0.55},
	{"name": "Синтез", "bpm": 132.0, "diff": 0.65},
	{"name": "Перегрузка", "bpm": 140.0, "diff": 0.75},
	{"name": "Каскад", "bpm": 148.0, "diff": 0.85},
	{"name": "Всплеск", "bpm": 156.0, "diff": 0.92},
	{"name": "Новатор", "bpm": 164.0, "diff": 0.97},
	{"name": "Мастер-бит", "bpm": 172.0, "diff": 1.0},
]

var state: int = State.MENU
var track_index := 0
var notes: Array = []
var note_states: Array = []   # 0 активна, 1 perfect, 2 good, 3 miss
var song_time := 0.0
var score := 0
var combo := 0
var max_combo := 0
var hit_count := 0
var total_judged := 0
var results_grade := ""
var best_scores: Dictionary = {}
var feedback := ""
var feedback_time := 0.0

var player: AudioStreamPlayer

func _ready() -> void:
	load_save()
	player = AudioStreamPlayer.new()
	add_child(player)


func load_save() -> void:
	var cfg := ConfigFile.new()
	if cfg.load("user://neonbeat_save.cfg") == OK:
		for i in TRACKS.size():
			best_scores[i] = int(cfg.get_value("best", "track" + str(i), 0))


func save_best() -> void:
	var cfg := ConfigFile.new()
	for i in TRACKS.size():
		cfg.set_value("best", "track" + str(i), best_scores.get(i, 0))
	cfg.save("user://neonbeat_save.cfg")


# ---------------- Процедурная музыка: PCM16 под темп трека ----------------

func build_music(track: Dictionary, duration: float) -> AudioStreamWAV:
	var rate := 22050
	var samples := int(duration * rate)
	var data := PackedByteArray()
	data.resize(samples * 2)
	var beat: float = 60.0 / float(track["bpm"])
	var bass_notes := [55.0, 65.41, 73.42, 82.41, 98.0]
	for i in samples:
		var t := float(i) / rate
		var sample := 0.0
		# кик: низкочастотный удар каждую долю
		var beat_pos := fmod(t, beat) / beat
		sample += 0.42 * sin(TAU * 62.0 * t) * exp(-beat_pos * 9.0)
		# хэт на восьмых
		var half_pos := fmod(t, beat * 0.5) / (beat * 0.5)
		sample += 0.1 * (randf() * 2.0 - 1.0) * exp(-half_pos * 22.0)
		# бас: синус по прогрессии
		var bar := int(t / (beat * 4.0)) % bass_notes.size()
		sample += 0.22 * sin(TAU * bass_notes[bar] * t)
		var v := int(clampf(sample, -1.0, 1.0) * 32767.0)
		data.encode_s16(i * 2, v)
	var wav := AudioStreamWAV.new()
	wav.format = AudioStreamWAV.FORMAT_16_BITS
	wav.mix_rate = rate
	wav.stereo = false
	wav.data = data
	return wav


func start_track(index: int) -> void:
	track_index = index
	var track: Dictionary = TRACKS[index]
	notes = Chart.generate(20261028 + index * 101, track["bpm"], track["diff"], 120 + index * 20)
	note_states = []
	note_states.resize(notes.size())
	note_states.fill(0)
	song_time = 0.0
	score = 0
	combo = 0
	max_combo = 0
	hit_count = 0
	total_judged = 0
	state = State.PLAY
	var duration: float = notes[-1]["time"] + 3.0
	player.stream = build_music(track, duration)
	player.play()


func _process(delta: float) -> void:
	if state != State.PLAY:
		return
	song_time += delta
	feedback_time = maxf(0.0, feedback_time - delta)
	# Автоматические промахи для прошедших нот
	for i in notes.size():
		if note_states[i] != 0:
			continue
		var diff: float = song_time - notes[i]["time"]
		if diff > Chart.HIT_WINDOW_GOOD:
			note_states[i] = 3
			combo = 0
			total_judged += 1
			feedback = "МИМО"
			feedback_time = 0.35
	# Конец трека
	if song_time > notes[-1]["time"] + 2.0:
		finish_track()
	queue_redraw()


func tap_lane(lane: int) -> void:
	if state != State.PLAY:
		return
	var best_index := -1
	var best_diff := 10.0
	for i in notes.size():
		if note_states[i] != 0 or notes[i]["lane"] != lane:
			continue
		var diff: float = absf(song_time - notes[i]["time"])
		if diff < best_diff:
			best_diff = diff
			best_index = i
		if notes[i]["time"] > song_time + 0.4:
			break
	if best_index < 0:
		combo = 0
		return
	var judgement: String = Chart.judge(song_time - notes[best_index]["time"])
	match judgement:
		"perfect":
			note_states[best_index] = 1
			hit_count += 1
		"good":
			note_states[best_index] = 2
			hit_count += 1
		_:
			note_states[best_index] = 3
			combo = 0
	total_judged += 1
	score += Chart.score_for(judgement, combo)
	if judgement != "miss":
		combo += 1
		max_combo = maxi(max_combo, combo)
	feedback = judgement.to_upper()
	feedback_time = 0.35


func finish_track() -> void:
	var accuracy := 0.0
	if total_judged > 0:
		accuracy = float(hit_count) / float(total_judged)
	results_grade = Chart.grade(accuracy)
	if score > best_scores.get(track_index, 0):
		best_scores[track_index] = score
	save_best()
	state = State.RESULTS
	player.stop()


func _input(event: InputEvent) -> void:
	if event is InputEventScreenTouch and event.pressed:
		handle_touch(event.position)
	elif event is InputEventMouseButton and event.pressed:
		handle_touch(event.position)
	elif event is InputEventKey and event.pressed and not event.echo:
		match event.keycode:
			KEY_D:
				tap_lane(0)
			KEY_F:
				tap_lane(1)
			KEY_J:
				tap_lane(2)
			KEY_K:
				tap_lane(3)


func handle_touch(pos: Vector2) -> void:
	match state:
		State.MENU:
			state = State.TRACK_SELECT
		State.TRACK_SELECT:
			var idx := int((pos.y - 174.0) / 92.0)
			if idx >= 0 and idx < TRACKS.size():
				start_track(idx)
		State.PLAY:
			var lane := int(pos.x / (get_viewport_rect().size.x / 4.0))
			tap_lane(clampi(lane, 0, 3))
		State.RESULTS:
			state = State.TRACK_SELECT


func _draw() -> void:
	draw_rect(Rect2(0, 0, 1280, 1280), BG)
	var font := ThemeDB.fallback_font
	match state:
		State.MENU:
			draw_string(font, Vector2(360, 340), "НЕОН-БИТ", HORIZONTAL_ALIGNMENT_CENTER, -1, 96, Color.WHITE)
			draw_string(font, Vector2(360, 420), "ритм и неон: тапай в такт", HORIZONTAL_ALIGNMENT_CENTER, -1, 36, Color(0.7, 0.8, 1))
			draw_string(font, Vector2(360, 560), "тап — выбрать трек", HORIZONTAL_ALIGNMENT_CENTER, -1, 40, Color(0.4, 0.95, 0.95))
		State.TRACK_SELECT:
			draw_string(font, Vector2(360, 130), "Треки", HORIZONTAL_ALIGNMENT_CENTER, -1, 64, Color.WHITE)
			for i in TRACKS.size():
				var y := 230 + i * 92
				var best: int = best_scores.get(i, 0)
				var color := Color(0.16, 0.2, 0.34) if i != track_index else Color(0.25, 0.32, 0.55)
				draw_rect(Rect2(90, y - 56, 540, 76), color)
				draw_string(font, Vector2(120, y - 6), "%d. %s  (%d BPM)" % [i + 1, TRACKS[i]["name"], int(TRACKS[i]["bpm"])],
					HORIZONTAL_ALIGNMENT_LEFT, -1, 38, Color.WHITE)
				draw_string(font, Vector2(600, y - 6), "рекорд %d" % best, HORIZONTAL_ALIGNMENT_RIGHT, -1, 30, Color(1, 0.85, 0.5))
		State.PLAY:
			draw_playfield(font)
		State.RESULTS:
			draw_string(font, Vector2(360, 320), "РЕЗУЛЬТАТ", HORIZONTAL_ALIGNMENT_CENTER, -1, 72, Color.WHITE)
			draw_string(font, Vector2(360, 470), results_grade, HORIZONTAL_ALIGNMENT_CENTER, -1, 180, grade_color())
			draw_string(font, Vector2(360, 580), "очков: %d   макс. комбо: %d" % [score, max_combo],
				HORIZONTAL_ALIGNMENT_CENTER, -1, 42, Color(0.85, 0.9, 1))
			draw_string(font, Vector2(360, 700), "тап — к трекам", HORIZONTAL_ALIGNMENT_CENTER, -1, 38, Color(0.4, 0.95, 0.95))


func grade_color() -> Color:
	match results_grade:
		"S":
			return Color(1.0, 0.85, 0.25)
		"A":
			return Color(0.4, 0.95, 0.6)
		"B":
			return Color(0.4, 0.75, 1.0)
	return Color(0.8, 0.5, 0.5)


func draw_playfield(font: Font) -> void:
	var lane_w := 320.0
	var hit_y := 1040.0
	# полосы
	for l in 4:
		draw_rect(Rect2(l * lane_w + 12, 0, lane_w - 24, 1280), Color(0.08, 0.07, 0.2))
		draw_rect(Rect2(l * lane_w + 12, hit_y - 10, lane_w - 24, 20), LANE_COLORS[l])
	# ноты
	for i in notes.size():
		if note_states[i] == 1 or note_states[i] == 2:
			continue
		var note_y: float = hit_y - (notes[i]["time"] - song_time) * Chart.note_speed(TRACKS[track_index]["diff"])
		if note_y < -120 or note_y > 1360:
			continue
		var c: Color = LANE_COLORS[notes[i]["lane"]]
		if note_states[i] == 3:
			c = c.darkened(0.7)
		draw_rect(Rect2(notes[i]["lane"] * lane_w + 34, note_y - 34, lane_w - 68, 68), c)
	# HUD
	draw_string(font, Vector2(40, 90), "очки: %d" % score, HORIZONTAL_ALIGNMENT_LEFT, -1, 44, Color.WHITE)
	draw_string(font, Vector2(680, 90), "комбо: %d" % combo, HORIZONTAL_ALIGNMENT_RIGHT, -1, 44, Color(1, 0.85, 0.5))
	if feedback_time > 0.0:
		draw_string(font, Vector2(360, 760), feedback, HORIZONTAL_ALIGNMENT_CENTER, -1, 72,
			Color(1, 1, 1, feedback_time * 2.5))
