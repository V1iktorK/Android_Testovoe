# tools/test-godot.ps1 - headless-прогон GDScript-тестов Godot-проекта
# Тестовый раннер: src/tests/run_tests.gd (extends SceneTree, quit(code))
# Использование: tools\test-godot.ps1 -GameDir game-04-onebutton
param([Parameter(Mandatory=$true)][string]$GameDir)
. "$PSScriptRoot\env.ps1"
$ErrorActionPreference = "Continue"
if (-not (Test-Path $GODOT_EXE)) { throw "Godot не найден: $GODOT_EXE" }
$proj = Join-Path $REPO_ROOT "$GameDir\src"
if (-not (Test-Path "$proj\project.godot")) { throw "Godot-проект не найден: $proj" }

# Импорт проекта: создаёт .godot и кеш глобальных классов (class_name)
Write-Host ">> Godot import: $GameDir"
$imp = & $GODOT_EXE --headless --path $proj --import 2>&1
if ($LASTEXITCODE -ne 0) { $imp | Select-Object -Last 10 | Write-Host }

Write-Host ">> Godot-тесты: $GameDir"
$out = & $GODOT_EXE --headless --path $proj -s "res://tests/run_tests.gd" 2>&1
$code = $LASTEXITCODE
$out | Select-Object -Last 30 | Write-Host
if ($code -ne 0) { Write-Host "!! Тесты упали: $GameDir (код $code)"; exit 1 }
Write-Host "OK тесты: $GameDir"
exit 0
