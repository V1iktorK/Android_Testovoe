# tools/env.ps1 - общие пути и переменные окружения для всех стеков
$Global:REPO_ROOT   = Split-Path -Parent $PSScriptRoot
$Global:UNITY_VER   = "6000.5.6f1"
$Global:UNITY_ROOT  = "C:\Program Files\Unity\Hub\Editor\$UNITY_VER\Editor"
$Global:UNITY_EXE   = "$UNITY_ROOT\Unity.exe"
$Global:UNITY_JDK   = "$UNITY_ROOT\Data\PlaybackEngines\AndroidPlayer\OpenJDK"
$Global:UNITY_SDK   = "$UNITY_ROOT\Data\PlaybackEngines\AndroidPlayer\SDK"
$Global:TOOLS_DIR   = Join-Path ([Environment]::GetFolderPath('LocalApplicationData')) "gamedev-tools"
$Global:GRADLE_BIN  = "$TOOLS_DIR\gradle-8.10\bin\gradle.bat"
$Global:GODOT_EXE   = "$TOOLS_DIR\godot\Godot_v4.3-stable_win64_console.exe"
$Global:GODOT_TPL   = "$env:APPDATA\Godot\export_templates\4.3.stable"

# JAVA_HOME / ANDROID_HOME для Gradle-сборок Java и Kotlin
$env:JAVA_HOME    = $UNITY_JDK
$env:ANDROID_HOME = $UNITY_SDK
$env:ANDROID_SDK_ROOT = $UNITY_SDK
# ВАЖНО: CMake/NDK/Gradle не работают с не-ASCII путями - все кеши вне профиля пользователя
$env:GRADLE_USER_HOME = "C:\gamedev-work\gradle-home"
$env:TEMP = "C:\gamedev-work\temp"
$env:TMP  = "C:\gamedev-work\temp"
foreach ($d in @($env:GRADLE_USER_HOME, $env:TEMP)) {
    if (-not (Test-Path $d)) { New-Item -ItemType Directory -Force -Path $d | Out-Null }
}
if (-not (Test-Path $env:ANDROID_HOME)) { throw "Android SDK не найден: $env:ANDROID_HOME" }
if (-not (Test-Path "$env:JAVA_HOME\bin\java.exe")) { throw "JDK не найден: $env:JAVA_HOME" }
