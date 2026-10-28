extends SceneTree
## Headless-раннер тестов: godot --headless --path <proj> -s res://tests/run_tests.gd
## Тестовые скрипты res://tests/test_*.gd: test_*-метод возвращает null или строку ошибки.

const TEST_DIR := "res://tests"


func _init() -> void:
	var failures := 0
	var total := 0

	var dir := DirAccess.open(TEST_DIR)
	if dir == null:
		printerr("Не удалось открыть каталог тестов: ", TEST_DIR)
		quit(1)
		return

	var files: Array[String] = []
	dir.list_dir_begin()
	var f := dir.get_next()
	while f != "":
		if not dir.current_is_dir() and f.begins_with("test_") and f.ends_with(".gd"):
			files.append(f)
		f = dir.get_next()
	dir.list_dir_end()
	files.sort()

	for file_name in files:
		var res = load(TEST_DIR + "/" + file_name)
		if res == null or not (res is GDScript) or not res.can_instantiate():
			failures += 1
			printerr("  FAIL  ", file_name, " -> скрипт не загрузился")
			continue
		var instance = res.new()
		if instance == null:
			failures += 1
			printerr("  FAIL  ", file_name, " -> не удалось создать экземпляр")
			continue
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
