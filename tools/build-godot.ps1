# tools/build-godot.ps1 - headless-экспорт Godot-проекта в APK
# Особенности окружения: Godot хранит временные файлы и настройки в профиле пользователя;
# minizip внутри Godot не умеет работать с не-ASCII путями, поэтому APPDATA/LOCALAPPDATA
# перенаправляются в C:\gamedev-work (ASCII), туда же складываются шаблоны и keystore.
# Использование: tools\build-godot.ps1 -GameDir game-04-onebutton
param(
    [Parameter(Mandatory=$true)][string]$GameDir,
    [string]$Preset = "Android"
)
. "$PSScriptRoot\env.ps1"
$ErrorActionPreference = "Continue"
if (-not (Test-Path $GODOT_EXE)) { throw "Godot не найден: $GODOT_EXE" }

$GD_APPDATA = "C:\gamedev-work\appdata"
$GD_LOCAL   = "C:\gamedev-work\localappdata"
foreach ($d in @("$GD_APPDATA\Godot\keystores", "$GD_LOCAL\Godot")) {
    if (-not (Test-Path $d)) { New-Item -ItemType Directory -Force -Path $d | Out-Null }
}

# Шаблоны экспорта (1 ГБ) - в ASCII-каталог, если ещё не перенесены
$tplDst = "$GD_APPDATA\Godot\export_templates\4.3.stable"
if (-not (Test-Path "$tplDst\android_release.apk")) {
    New-Item -ItemType Directory -Force -Path $tplDst | Out-Null
    if (Test-Path $GODOT_TPL) {
        Write-Host ">> Перенос шаблонов экспорта Godot..."
        Move-Item "$GODOT_TPL\*" $tplDst -Force
    } else {
        throw "Шаблоны экспорта Godot не найдены: $GODOT_TPL"
    }
}

# Debug keystore для подписи
$ks = "$GD_APPDATA\Godot\keystores\debug.keystore"
if (-not (Test-Path $ks)) {
    Write-Host ">> keytool: debug keystore для Godot"
    & "$UNITY_JDK\bin\keytool.exe" -genkeypair -keystore $ks -alias androiddebugkey -keyalg RSA -keysize 2048 `
        -validity 10000 -storepass "android" -keypass "android" `
        -dname "CN=Android Debug, O=Android, C=US" 2>&1 | Select-Object -Last 1
}

# Настройки редактора Godot: пути к JDK/SDK (файл версионный: editor_settings-4.3.tres)
$es = "$GD_APPDATA\Godot\editor_settings-4.3.tres"
if (-not (Test-Path $es)) {
    # первая инициализация: создаём настройки запуском редактора
    $env:LOCALAPPDATA = $GD_LOCAL; $env:APPDATA = $GD_APPDATA
    & $GODOT_EXE --headless --editor --quit --path (Join-Path $REPO_ROOT "$GameDir\src") 2>&1 | Out-Null
}
$esText = [IO.File]::ReadAllText($es)
if ($esText -notmatch 'export/android/java_sdk_path = "C:/Program') {
    $jdkUrl  = ($UNITY_JDK -replace '\\','/')
    $sdkUrl  = ($UNITY_SDK -replace '\\','/')
    $ksUrl   = ($ks -replace '\\','/')
    $esText = $esText -replace 'export/android/java_sdk_path = "[^"]*"', ('export/android/java_sdk_path = "' + $jdkUrl + '"')
    $esText = $esText -replace 'export/android/android_sdk_path = "[^"]*"', ('export/android/android_sdk_path = "' + $sdkUrl + '"')
    $esText = $esText -replace 'export/android/debug_keystore = "[^"]*"', ('export/android/debug_keystore = "' + $ksUrl + '"')
    [IO.File]::WriteAllText($es, $esText)
}

$proj = Join-Path $REPO_ROOT "$GameDir\src"
if (-not (Test-Path "$proj\project.godot")) { throw "Godot-проект не найден: $proj" }
$art = Join-Path $REPO_ROOT "$GameDir\artifacts"
New-Item -ItemType Directory -Force -Path $art | Out-Null
$safeName = Split-Path $GameDir -Leaf
$out = Join-Path $art "$safeName.apk"

# ASCII-путь для результата (Godot-инструменты подписи чувствительны к кодировке)
$asciiOut = "C:\gamedev-work\$safeName.apk"

$env:LOCALAPPDATA = $GD_LOCAL
$env:APPDATA = $GD_APPDATA
Remove-Item "$GD_LOCAL\Godot\tmpexport-*" -Force -ErrorAction SilentlyContinue

# Импорт ресурсов
Write-Host ">> Godot import: $GameDir"
& $GODOT_EXE --headless --path $proj --import 2>&1 | Select-Object -Last 3

$ok = $false
for ($attempt = 1; $attempt -le 3 -and -not $ok; $attempt++) {
    Write-Host ">> Godot export (попытка $attempt): $GameDir -> $out"
    $log = Join-Path $art "build.log"
    cmd /c "`"$GODOT_EXE`" --headless --path `"$proj`" --export-release $Preset `"$asciiOut`" > `"$log`" 2>&1"
    if ((Test-Path $asciiOut) -and ((Get-Item $asciiOut).Length -gt 1MB)) { $ok = $true }
    else {
        # флаки minizip: даём антивирусу и системе время освободить временный файл
        Start-Sleep -Seconds (30 * $attempt)
        Remove-Item "$GD_LOCAL\Godot\tmpexport-*" -Force -ErrorAction SilentlyContinue
        Remove-Item $asciiOut -Force -ErrorAction SilentlyContinue
    }
}
if (-not $ok) {
    Get-Content $log -Tail 20 | Write-Host
    throw "Godot-экспорт не удался после 3 попыток"
}
Copy-Item $asciiOut $out -Force
Write-Host "OK APK: $out ($([math]::Round((Get-Item $out).Length/1MB,1)) MB)"
