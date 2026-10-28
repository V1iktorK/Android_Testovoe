extends RefCounted
## Тесты чистой логики smoke-проекта.
## ВАЖНО: class_name виден только после импорта проекта (godot --headless --import).

const LogicScript = preload("res://logic.gd")


func test_add_works() -> Variant:
	if LogicScript.add(2, 3) != 5:
		return "add(2,3) должен быть 5"
	if LogicScript.add(-1, 1) != 0:
		return "add(-1,1) должен быть 0"
	return null
