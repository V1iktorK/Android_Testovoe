# tools/test-jvm.ps1 - компиляция и прогон JUnit-тестов (Java/Kotlin) без Gradle.
# Тесты лежат в game-NN/tests/**/*.java|kt и тестируют чистую логику без android.*.
# Использование: tools\test-jvm.ps1 -GameDir game-03-neonidle
param([Parameter(Mandatory=$true)][string]$GameDir)
. "$PSScriptRoot\env.ps1"
$ErrorActionPreference = "Continue"

$proj  = Join-Path $REPO_ROOT "$GameDir\src"
$tests = Join-Path $REPO_ROOT "$GameDir\tests"
if (-not (Test-Path $tests)) { throw "Нет каталога тестов: $tests" }

$JDK    = $UNITY_JDK
$AJAR   = "$UNITY_SDK\platforms\android-34\android.jar"
$M2     = "$TOOLS_DIR\m2"
$KT_CP  = "$M2\kotlin-compiler-embeddable.jar;$M2\kotlin-stdlib.jar;$M2\kotlinx-coroutines-core-jvm.jar;$M2\trove4j.jar;$M2\kotlin-script-runtime.jar;$M2\kotlin-reflect.jar;$M2\kotlin-daemon-embeddable.jar;$M2\annotations-13.0.jar"
$KT_LIB = "$M2\kotlin-stdlib.jar;$M2\kotlinx-coroutines-core-jvm.jar"
$JUNIT  = "$M2\junit-4.13.2.jar"
$HAM    = "$M2\hamcrest-core-1.3.jar"
foreach ($p in @($JUNIT, $HAM)) { if (-not (Test-Path $p)) { throw "Нет зависимости: $p" } }

$build   = Join-Path $proj "build-test"
if (Test-Path $build) { Remove-Item $build -Recurse -Force }
$classes = Join-Path $build "classes"
New-Item -ItemType Directory -Force -Path $classes | Out-Null

# --- 1. Компиляция игровой логики (src/java, src/kotlin) + тестов ---------
$javaSrc   = @(Get-ChildItem -Path (Join-Path $proj "java")   -Recurse -Filter "*.java" -ErrorAction SilentlyContinue)
$kotlinSrc = @(Get-ChildItem -Path (Join-Path $proj "kotlin") -Recurse -Filter "*.kt"   -ErrorAction SilentlyContinue)
$javaTests = @(Get-ChildItem -Path $tests -Recurse -Filter "*.java" -ErrorAction SilentlyContinue)
$ktTests   = @(Get-ChildItem -Path $tests -Recurse -Filter "*.kt"   -ErrorAction SilentlyContinue)
if (-not $javaTests -and -not $ktTests) { throw "Нет тестовых файлов в $tests" }

if ($kotlinSrc -or $ktTests) {
    Write-Host ">> kotlinc (логика + тесты)"
    $files = ($kotlinSrc + $ktTests | ForEach-Object { '"' + $_.FullName + '"' }) -join " "
    $argFile = Join-Path $build "kt-args.rsp"
    $ktArgs = "-no-stdlib -classpath `"$AJAR;$KT_LIB;$JUNIT;$HAM`" -d `"$classes`" $files" -replace '\\','/'
    [IO.File]::WriteAllText($argFile, $ktArgs, (New-Object Text.UTF8Encoding $false))
    & "$JDK\bin\java.exe" -cp "$KT_CP" org.jetbrains.kotlin.cli.jvm.K2JVMCompiler "@$argFile" 2>&1 | Select-Object -Last 8
    if ($LASTEXITCODE -ne 0) { throw "kotlinc упал (код $LASTEXITCODE)" }
}
if ($javaSrc -or $javaTests) {
    Write-Host ">> javac (логика + тесты)"
    $files = ($javaSrc + $javaTests | ForEach-Object { '"' + $_.FullName + '"' }) -join " "
    $argFile = Join-Path $build "javac-args.rsp"
    $jArgs = "-classpath `"$AJAR;$classes;$KT_LIB;$JUNIT;$HAM`" -d `"$classes`" -encoding UTF-8 $files" -replace '\\','/'
    [IO.File]::WriteAllText($argFile, $jArgs, ([Text.Encoding]::Default))
    & "$JDK\bin\javac.exe" "@$argFile" 2>&1 | Select-Object -Last 8
    if ($LASTEXITCODE -ne 0) { throw "javac упал (код $LASTEXITCODE)" }
}

# --- 2. Список тест-классов: *Test.class верхнего уровня ------------------
$testClasses = Get-ChildItem -Path $classes -Recurse -Filter "*Test.class" |
    Where-Object { $_.Name -notmatch '\$' } |
    ForEach-Object { ($_.FullName.Substring($classes.Length + 1) -replace '\\','/' -replace '\.class$','' -replace '/','.') }
if (-not $testClasses) { throw "Не найдено классов *Test в $classes" }

# --- 3. Прогон JUnitCore --------------------------------------------------
Write-Host ">> JUnit: $($testClasses -join ', ')"
& "$JDK\bin\java.exe" -cp "$classes;$KT_LIB;$JUNIT;$HAM;$AJAR" org.junit.runner.JUnitCore $testClasses
if ($LASTEXITCODE -ne 0) { Write-Host "!! Тесты упали: $GameDir"; exit 1 }
Write-Host "OK тесты: $GameDir"
exit 0
