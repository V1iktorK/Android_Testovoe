extends RefCounted
## Тесты чистой логики smoke-проекта.
## Каждый test_*-метод возвращает null при успехе или строку ошибки.

func test_add_works() -> Variant:
	if Logic.add(2, 3) != 5:
		return "add(2,3) должен быть 5"
	if Logic.add(-1, 1) != 0:
		return "add(-1,1) должен быть 0"
	return null
