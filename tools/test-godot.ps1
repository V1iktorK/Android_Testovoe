# tools/test-godot.ps1 - headless-прогон GDScript-тестов Godot-проекта
# Тестовый раннер: src/tests/run_tests.gd (extends SceneTree, quit(code))
# Использование: tools\test-godot.ps1 -GameDir game-04-onebutton
param([Parameter(Mandatory=$true)][string]$GameDir)
. "$PSScriptRoot\env.ps1"
if (-not (Test-Path $GODOT_EXE)) { throw "Godot не найден: $GODOT_EXE" }
$proj = Join-Path $REPO_ROOT "$GameDir\src"
if (-not (Test-Path "$proj\project.godot")) { throw "Godot-проект не найден: $proj" }
Write-Host ">> Godot-тесты: $GameDir"
& $GODOT_EXE --headless --path $proj -s res://tests/run_tests.gd 2>&1 | Select-Object -Last 25
$code = $LASTEXITCODE
if ($code -ne 0) { Write-Host "!! Тесты упали: $GameDir (код $code)"; exit 1 }
Write-Host "OK тесты: $GameDir"
exit 0
