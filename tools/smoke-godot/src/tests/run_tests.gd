extends SceneTree
## Headless-раннер тестов Godot: godot --headless --path <proj> -s res://tests/run_tests.gd
## Каждый тестовый скрипт в res://tests/ с именем test_*.gd предоставляет run(t: T) методы.
## Код выхода: 0 = все тесты прошли, 1 = есть ошибки.

const TEST_DIR := "res://tests"


func _init() -> void:
	var failures := 0
	var total := 0
	var dir := DirAccess.open(TEST_DIR)
	if dir == null:
		printerr("Не удалось открыть каталог тестов: ", TEST_DIR)
		quit(1)
		return
	var files := []
	dir.list_dir_begin()
	var f := dir.get_next()
	while f != "":
		if not dir.current_is_dir() and f.begins_with("test_") and f.ends_with(".gd"):
			files.append(f)
		f = dir.get_next()
	dir.list_dir_end()
	files.sort()

	for file_name in files:
		var script = load(TEST_DIR + "/" + file_name)
		if script == null:
			printerr("Не удалось загрузить: ", file_name)
			failures += 1
			continue
		var instance = script.new()
		for m in instance.get_method_list():
			var mname: String = m["name"]
			if not mname.begins_with("test_"):
				continue
			total += 1
			var err = instance.call(mname)
			if err == null:
				print("  PASS  ", file_name, "::", mname)
			else:
				failures += 1
				printerr("  FAIL  ", file_name, "::", mname, " -> ", err)

	print("=== Godot tests: всего %d, провалено %d ===" % [total, failures])
	quit(1 if failures > 0 else 0)
