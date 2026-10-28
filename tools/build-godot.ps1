# tools/build-godot.ps1 - headless-экспорт Godot-проекта в APK
# Использование: tools\build-godot.ps1 -GameDir game-04-onebutton
param(
    [Parameter(Mandatory=$true)][string]$GameDir,
    [string]$Preset = "Android"
)
. "$PSScriptRoot\env.ps1"
if (-not (Test-Path $GODOT_EXE)) { throw "Godot не найден: $GODOT_EXE" }
if (-not (Test-Path $GODOT_TPL)) { throw "Шаблоны экспорта Godot не найдены: $GODOT_TPL" }
$proj = Join-Path $REPO_ROOT "$GameDir\src"
if (-not (Test-Path "$proj\project.godot")) { throw "Godot-проект не найден: $proj" }

# Настройки Android-экспорта Godot (SDK из Unity-модуля)
$editorSettings = "$env:APPDATA\Godot\editor_settings-4.tres"
if (-not (Test-Path $editorSettings)) {
    New-Item -ItemType Directory -Force -Path "$env:APPDATA\Godot" | Out-Null
    Set-Content -Path $editorSettings -Value "[gd_resource type=\"EditorSettings\" load_steps=2 format=3]`n`n[resource]`nexport/android/android_sdk_path = `"$($UNITY_SDK -replace '\\','/')`"`nexport/android/debug_keystore = `"$($env:APPDATA -replace '\\','/')/Godot/debug.keystore`"`n" -Encoding UTF8
}
$art = Join-Path $REPO_ROOT "$GameDir\artifacts"
New-Item -ItemType Directory -Force -Path $art | Out-Null
$out = Join-Path $art "$GameDir.apk"
Write-Host ">> Godot export: $GameDir -> $out"
& $GODOT_EXE --headless --path $proj --export-release $Preset $out 2>&1 | Select-Object -Last 15
if ($LASTEXITCODE -ne 0 -or -not (Test-Path $out)) { throw "Godot-экспорт не удался (код $LASTEXITCODE)" }
Write-Host "OK APK: $out ($([math]::Round((Get-Item $out).Length/1MB,1)) MB)"
