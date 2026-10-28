# tools/test-unity.ps1 - прогон Unity EditMode-тестов (Unity Test Framework)
# Тесты лежат в src/Assets/Tests/EditMode
# Использование: tools\test-unity.ps1 -GameDir game-01-robolab
param([Parameter(Mandatory=$true)][string]$GameDir)
. "$PSScriptRoot\env.ps1"
if (-not (Test-Path $UNITY_EXE)) { throw "Unity не найден: $UNITY_EXE" }
$proj = Join-Path $REPO_ROOT "$GameDir\src"
if (-not (Test-Path "$proj\ProjectSettings\ProjectVersion.txt")) { throw "Unity-проект не найден: $proj" }
$art = Join-Path $REPO_ROOT "$GameDir\artifacts"
New-Item -ItemType Directory -Force -Path $art | Out-Null
$res = Join-Path $art "test-results.xml"
$log = Join-Path $art "test.log"
Write-Host ">> Unity EditMode-тесты: $GameDir"
& $UNITY_EXE -batchmode -nographics -projectPath $proj -runTests -testPlatform EditMode -testResults "$res" -logFile "$log"
$code = $LASTEXITCODE
if ($code -ne 0) {
    Get-Content $log -Tail 25 | Write-Host
    Write-Host "!! Тесты упали: $GameDir (код $code)"; exit 1
}
Write-Host "OK тесты: $GameDir"
exit 0
