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
$safeName = Split-Path $GameDir -Leaf
$res = Join-Path $art "test-results.xml"
$log = Join-Path $art "test.log"
Write-Host ">> Unity EditMode-тесты: $GameDir"
Start-Process -FilePath $UNITY_EXE -ArgumentList @(
    "-batchmode", "-nographics", "-projectPath", $proj,
    "-runTests", "-testPlatform", "EditMode", "-testResults", $res, "-logFile", $log
) | Out-Null
$deadline = (Get-Date).AddMinutes(60)
while ((Get-Date) -lt $deadline) {
    Start-Sleep -Seconds 10
    $active = Get-CimInstance Win32_Process -Filter "Name='Unity.exe'" |
        Where-Object { $_.CommandLine -like "*runTests*" -and $_.CommandLine -like "*$safeName*" }
    if (-not $active) { break }
}
if (-not (Test-Path $res)) {
    if (Test-Path $log) { Get-Content $log -Tail 25 | Write-Host }
    Write-Host "!! Тесты не дали результатов: $GameDir"; exit 1
}
[xml]$doc = Get-Content $res
$total = [int]$doc.'test-run'.total
$failed = [int]$doc.'test-run'.failed
Write-Host "Всего: $total, провалено: $failed"
if ($failed -gt 0) { Write-Host "!! Тесты упали: $GameDir"; exit 1 }
Write-Host "OK тесты: $GameDir"
exit 0
