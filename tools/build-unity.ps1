# tools/build-unity.ps1 - batchmode-сборка Unity-проекта в APK
# В проекте должен быть Assets/Editor/BuildScript.cs с методом BuildScript.BuildAndroid
# Использование: tools\build-unity.ps1 -GameDir game-01-robolab [-AppId com.portfolio.robolab] [-ProductName "RoboLab"] [-Portrait] [-Il2cpp]
param(
    [Parameter(Mandatory=$true)][string]$GameDir,
    [string]$AppId = "com.portfolio.game",
    [string]$ProductName = "Game",
    [switch]$Portrait,
    [switch]$Il2cpp
)
. "$PSScriptRoot\env.ps1"
$ErrorActionPreference = "Continue"
if (-not (Test-Path $UNITY_EXE)) { throw "Unity не найден: $UNITY_EXE" }
$proj = Join-Path $REPO_ROOT "$GameDir\src"
if (-not (Test-Path "$proj\ProjectSettings\ProjectVersion.txt")) { throw "Unity-проект не найден: $proj" }

$art = Join-Path $REPO_ROOT "$GameDir\artifacts"
New-Item -ItemType Directory -Force -Path $art | Out-Null
$safeName = Split-Path $GameDir -Leaf
$out = Join-Path $art "$safeName.apk"
$log = Join-Path $art "build.log"

# Ключ подписи
$ks = Join-Path $art "release.keystore"
if (-not (Test-Path $ks)) {
    Write-Host ">> keytool: генерация ключа подписи"
    & "$UNITY_JDK\bin\keytool.exe" -genkeypair -keystore $ks -alias $safeName -keyalg RSA -keysize 2048 `
        -validity 10000 -storepass "android" -keypass "android" `
        -dname "CN=$safeName, OU=Portfolio, O=ATP-Mashinostroenie, C=RU" 2>&1 | Select-Object -Last 2
}

$extra = @()
if ($Portrait) { $extra += "-portrait" }
if ($Il2cpp) { $extra += "-il2cpp" }

# Unity Android Tools не работают с не-ASCII путями: собираем в ASCII-копии
$work = "C:\gamedev-work\$safeName"
New-Item -ItemType Directory -Force -Path $work | Out-Null
robocopy $proj $work /MIR /XD build build-test artifacts Library Temp Logs UserSettings /NFL /NDL /NJH /NJS | Out-Null
if ($LASTEXITCODE -ge 8) { throw "robocopy не удался (код $LASTEXITCODE)" }

$uargs = @(
    "-batchmode", "-nographics", "-quit",
    "-projectPath", $work,
    "-executeMethod", "BuildScript.BuildAndroid",
    "-buildTarget", "Android",
    "-buildPath", "$work\$safeName.apk",
    "-appId", $AppId,
    "-productName", $ProductName,
    "-keystorePath", "$work\release.keystore",
    "-keystorePass", "android",
    "-keyAlias", $safeName,
    "-keyPass", "android",
    "-logFile", $log
) + $extra

# Ключ подписи кладём в ASCII-рабочую копию
if (Test-Path $ks) { Copy-Item $ks "$work\release.keystore" -Force }

Write-Host ">> Unity build: $GameDir -> $out"
$sw = [Diagnostics.Stopwatch]::StartNew()
Start-Process -FilePath $UNITY_EXE -ArgumentList $uargs | Out-Null

# Unity перезапускается внутри - дожидаемся завершения ВСЕХ процессов этого проекта
$deadline = (Get-Date).AddMinutes(90)
while ((Get-Date) -lt $deadline) {
    Start-Sleep -Seconds 15
    $active = Get-CimInstance Win32_Process -Filter "Name='Unity.exe'" |
        Where-Object { $_.CommandLine -like "*gamedev-work*$safeName*" }
    if (-not $active) { break }
}
$sw.Stop()
$ok = Test-Path "$work\$safeName.apk"
if (-not $ok) {
    if (Test-Path $log) { Get-Content $log -Tail 30 | Write-Host }
    throw "Unity-сборка не удалась ($($sw.Elapsed))"
}
Copy-Item "$work\$safeName.apk" $out -Force
Write-Host "OK APK: $out ($([math]::Round((Get-Item $out).Length/1MB,1)) MB, $($sw.Elapsed))"
