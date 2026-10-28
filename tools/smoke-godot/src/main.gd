extends Node2D

## Точка входа smoke-проекта Godot.

func _ready() -> void:
	print("Godot smoke: OK, logic=", Logic.add(2, 3))
