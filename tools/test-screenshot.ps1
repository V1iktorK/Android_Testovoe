# tools/test-screenshot.ps1 - генерация screenshot.png для Java/Kotlin игр
# В game-NN/assets-gen/ лежит ScreenshotGen.java|kt с методом main, который пишет PNG.
# Использование: tools\test-screenshot.ps1 -GameDir game-03-neonidle
param(
    [Parameter(Mandatory=$true)][string]$GameDir,
    [int]$Width = 1080,
    [int]$Height = 1920
)
. "$PSScriptRoot\env.ps1"
$ErrorActionPreference = "Continue"

$proj   = Join-Path $REPO_ROOT "$GameDir\src"
$gen    = Join-Path $REPO_ROOT "$GameDir\assets-gen"
$outDir = Join-Path $REPO_ROOT "$GameDir"
$JDK    = $UNITY_JDK
$AJAR   = "$UNITY_SDK\platforms\android-34\android.jar"
$M2     = "$TOOLS_DIR\m2"
$KT_LIB = "$M2\kotlin-stdlib.jar;$M2\kotlinx-coroutines-core-jvm.jar"
$KT_CP  = "$M2\kotlin-compiler-embeddable.jar;$KT_LIB;$M2\trove4j.jar;$M2\kotlin-script-runtime.jar;$M2\kotlin-reflect.jar;$M2\kotlin-daemon-embeddable.jar;$M2\annotations-13.0.jar"

if (-not (Test-Path $gen)) { throw "Нет каталога assets-gen: $gen" }
$build = Join-Path $proj "build-shot"
if (Test-Path $build) { Remove-Item $build -Recurse -Force }
$classes = Join-Path $build "classes"
New-Item -ItemType Directory -Force -Path $classes | Out-Null

$javaSrc   = @(Get-ChildItem -Path (Join-Path $proj "java")   -Recurse -Filter "*.java" -ErrorAction SilentlyContinue)
$kotlinSrc = @(Get-ChildItem -Path (Join-Path $proj "kotlin") -Recurse -Filter "*.kt"   -ErrorAction SilentlyContinue)
$javaGen   = @(Get-ChildItem -Path $gen -Recurse -Filter "*.java" -ErrorAction SilentlyContinue)
$ktGen     = @(Get-ChildItem -Path $gen -Recurse -Filter "*.kt"   -ErrorAction SilentlyContinue)

if ($kotlinSrc -or $ktGen) {
    $files = ($kotlinSrc + $ktGen | ForEach-Object { '"' + $_.FullName + '"' }) -join " "
    $argFile = Join-Path $build "kt-args.rsp"
    $ktArgs = "-no-stdlib -classpath `"$AJAR;$KT_LIB`" -d `"$classes`" $files" -replace '\\','/'
    [IO.File]::WriteAllText($argFile, $ktArgs, (New-Object Text.UTF8Encoding $false))
    & "$JDK\bin\java.exe" -cp "$KT_CP" org.jetbrains.kotlin.cli.jvm.K2JVMCompiler "@$argFile" 2>&1 | Select-Object -Last 5
    if ($LASTEXITCODE -ne 0) { throw "kotlinc (screenshot) упал" }
}
if ($javaSrc -or $javaGen) {
    $files = ($javaSrc + $javaGen | ForEach-Object { '"' + $_.FullName + '"' }) -join " "
    $argFile = Join-Path $build "javac-args.rsp"
    $jArgs = "-classpath `"$AJAR;$classes;$KT_LIB`" -d `"$classes`" -encoding UTF-8 $files" -replace '\\','/'
    [IO.File]::WriteAllText($argFile, $jArgs, ([Text.Encoding]::Default))
    & "$JDK\bin\javac.exe" "@$argFile" 2>&1 | Select-Object -Last 5
    if ($LASTEXITCODE -ne 0) { throw "javac (screenshot) упал" }
}

# Ищем класс-генератор: *Gen.class с методом main (имя из assets-gen)
$genClass = Get-ChildItem -Path $classes -Recurse -Filter "*Gen.class" |
    Where-Object { $_.Name -notmatch '\$' } | Select-Object -First 1
if (-not $genClass) { throw "Не найден класс *Gen в $classes" }
$fqcn = ($genClass.FullName.Substring($classes.Length + 1) -replace '\\','/' -replace '\.class$','' -replace '/','.')

Write-Host ">> ScreenshotGen: $fqcn"
$shot = Join-Path $outDir "screenshot.png"
Push-Location $outDir
& "$JDK\bin\java.exe" "-Djava.awt.headless=true" -cp "$classes;$KT_LIB" $fqcn "$shot" $Width $Height 2>&1 | Select-Object -Last 5
Pop-Location
if ($LASTEXITCODE -ne 0 -or -not (Test-Path $shot)) { throw "Генерация скриншота не удалась" }
Write-Host "OK screenshot: $shot ($([math]::Round((Get-Item $shot).Length/1KB,0)) KB)"
