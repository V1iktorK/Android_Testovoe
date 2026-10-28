# tools/build-javapk.ps1 - сборка APK для проектов Java/Kotlin БЕЗ Gradle:
#   javac / kotlinc -> d8 -> aapt2 link -> zipalign -> apksigner
# Стандартная структура проекта игры:
#   game-NN/src/AndroidManifest.xml
#   game-NN/src/java|kotlin/...   (исходники)
#   game-NN/src/res/...           (опционально: иконка и др. ресурсы)
# Использование: tools\build-javapk.ps1 -GameDir game-03-neonidle [-Release]
param(
    [Parameter(Mandatory=$true)][string]$GameDir,
    [string]$VersionCode = "1",
    [string]$VersionName = "1.0",
    [switch]$Release
)
. "$PSScriptRoot\env.ps1"
$ErrorActionPreference = "Continue"

$proj   = Join-Path $REPO_ROOT "$GameDir\src"
$manifest = Join-Path $proj "AndroidManifest.xml"
if (-not (Test-Path $manifest)) { throw "Нет AndroidManifest.xml в $proj" }

$JDK     = $UNITY_JDK
$BT      = "$UNITY_SDK\build-tools\36.0.0"
$AJAR    = "$UNITY_SDK\platforms\android-34\android.jar"
$M2      = "$TOOLS_DIR\m2"
$KT_CP   = "$M2\kotlin-compiler-embeddable.jar;$M2\kotlin-stdlib.jar;$M2\kotlinx-coroutines-core-jvm.jar;$M2\trove4j.jar;$M2\kotlin-script-runtime.jar;$M2\kotlin-reflect.jar;$M2\kotlin-daemon-embeddable.jar;$M2\annotations-13.0.jar"
$KT_LIB  = "$M2\kotlin-stdlib.jar;$M2\kotlinx-coroutines-core-jvm.jar"
foreach ($p in @("$BT\aapt2.exe","$BT\d8.bat","$BT\zipalign.exe","$BT\apksigner.bat","$AJAR")) {
    if (-not (Test-Path $p)) { throw "Не найден инструмент: $p" }
}

$build = Join-Path $proj "build"
if (Test-Path $build) { Remove-Item $build -Recurse -Force }
$classes = Join-Path $build "classes"
$dexdir  = Join-Path $build "dex"
New-Item -ItemType Directory -Force -Path $classes, $dexdir | Out-Null

# --- 1. Сборка исходников -------------------------------------------------
$javaSrc   = Get-ChildItem -Path (Join-Path $proj "java")   -Recurse -Filter "*.java" -ErrorAction SilentlyContinue
$kotlinSrc = Get-ChildItem -Path (Join-Path $proj "kotlin") -Recurse -Filter "*.kt"   -ErrorAction SilentlyContinue
if (-not $javaSrc -and -not $kotlinSrc) { throw "Нет исходников в $proj\java или $proj\kotlin" }

if ($kotlinSrc) {
    Write-Host ">> kotlinc: $($kotlinSrc.Count) файлов"
    $ktFiles = ($kotlinSrc | ForEach-Object { '"' + $_.FullName + '"' }) -join " "
    $ktArgs = "-no-stdlib -classpath `"$AJAR;$KT_LIB`" -d `"$classes`" $ktFiles"
    $ktArgs = $ktArgs -replace '\\','/'
    $argFile = Join-Path $build "kt-args.rsp"
    [IO.File]::WriteAllText($argFile, $ktArgs, (New-Object Text.UTF8Encoding $false))
    & "$JDK\bin\java.exe" -cp "$KT_CP" org.jetbrains.kotlin.cli.jvm.K2JVMCompiler "@$argFile" 2>&1 | Select-Object -Last 8
    if ($LASTEXITCODE -ne 0) { throw "kotlinc упал (код $LASTEXITCODE)" }
}
if ($javaSrc) {
    Write-Host ">> javac: $($javaSrc.Count) файлов"
    $jFiles = ($javaSrc | ForEach-Object { '"' + $_.FullName + '"' }) -join " "
    $jCp = "$AJAR;$classes;$KT_LIB"
    $argFile = Join-Path $build "javac-args.rsp"
    $jArgs = "-classpath `"$jCp`" -d `"$classes`" -encoding UTF-8 $jFiles" -replace '\\','/'
    [IO.File]::WriteAllText($argFile, $jArgs, ([Text.Encoding]::Default))
    & "$JDK\bin\javac.exe" "@$argFile" 2>&1 | Select-Object -Last 8
    if ($LASTEXITCODE -ne 0) { throw "javac упал (код $LASTEXITCODE)" }
}

# --- 2. DEX ---------------------------------------------------------------
Write-Host ">> d8"
$classesJar = Join-Path $build "classes.jar"
& "$JDK\bin\jar.exe" cf $classesJar -C $classes . 2>&1 | Out-Null
if ($LASTEXITCODE -ne 0) { throw "jar cf упал" }
$d8Inputs = @($classesJar)
if ($kotlinSrc) { $d8Inputs += ($KT_LIB -split ';') }
& "$JDK\bin\java.exe" -cp "$BT\lib\d8.jar" com.android.tools.r8.D8 --lib $AJAR --min-api 24 --output $dexdir $d8Inputs 2>&1 | Select-Object -Last 5
if ($LASTEXITCODE -ne 0) { throw "d8 упал (код $LASTEXITCODE)" }
if (-not (Test-Path (Join-Path $dexdir "classes.dex"))) { throw "classes.dex не создан" }

# --- 3. Ресурсы + манифест (aapt2 link) -----------------------------------
# aapt2 не работает с не-ASCII путями - компилируем ресурсы в ASCII-каталоге
$safeName = Split-Path $GameDir -Leaf
$unsigned = Join-Path $build "app-unsigned.apk"
$linkArgs = @("link", "-I", $AJAR, "--manifest", $manifest, "--min-sdk-version", "24",
              "--target-sdk-version", "34", "--version-code", $VersionCode, "--version-name", $VersionName,
              "-o", $unsigned)
$resDir = Join-Path $proj "res"
if (Test-Path $resDir) {
    $aaptWork = "C:\gamedev-work\res-$safeName"
    if (Test-Path $aaptWork) { Remove-Item $aaptWork -Recurse -Force }
    Copy-Item $resDir $aaptWork -Recurse
    $resZip = "$aaptWork\res.zip"
    & "$BT\aapt2.exe" compile --dir $aaptWork -o $resZip 2>&1 | Select-Object -Last 4
    if ($LASTEXITCODE -ne 0) { throw "aapt2 compile упал" }
    $linkArgs += $resZip
}
Write-Host ">> aapt2 link"
& "$BT\aapt2.exe" @linkArgs 2>&1 | Select-Object -Last 5
if ($LASTEXITCODE -ne 0 -or -not (Test-Path $unsigned)) { throw "aapt2 link упал (код $LASTEXITCODE)" }

# --- 4. Добавляем classes.dex в APK ---------------------------------------
& "$JDK\bin\jar.exe" uf $unsigned -C $dexdir classes.dex 2>&1 | Out-Null
if ($LASTEXITCODE -ne 0) { throw "jar uf упал" }

# --- 5. Выравнивание и подпись -------------------------------------------
$aligned = Join-Path $build "app-aligned.apk"
& "$BT\zipalign.exe" -f 4 $unsigned $aligned 2>&1 | Select-Object -Last 3
if ($LASTEXITCODE -ne 0) { throw "zipalign упал" }

$art = Join-Path $REPO_ROOT "$GameDir\artifacts"
New-Item -ItemType Directory -Force -Path $art | Out-Null
$ks = Join-Path $art "release.keystore"
if (-not (Test-Path $ks)) {
    Write-Host ">> keytool: генерация ключа подписи"
    & "$JDK\bin\keytool.exe" -genkeypair -keystore $ks -alias $safeName -keyalg RSA -keysize 2048 `
        -validity 10000 -storepass "android" -keypass "android" `
        -dname "CN=$safeName, OU=Portfolio, O=ATP-Mashinostroenie, C=RU" 2>&1 | Select-Object -Last 3
    if ($LASTEXITCODE -ne 0) { throw "keytool упал" }
}
$out = Join-Path $art "$safeName.apk"
& "$JDK\bin\java.exe" -cp "$BT\lib\apksigner.jar" com.android.apksigner.ApkSignerTool sign --ks $ks --ks-key-alias $safeName --ks-pass "pass:android" --key-pass "pass:android" --out $out $aligned 2>&1 | Select-Object -Last 4
if ($LASTEXITCODE -ne 0 -or -not (Test-Path $out)) { throw "apksigner упал (код $LASTEXITCODE)" }
& "$JDK\bin\java.exe" -cp "$BT\lib\apksigner.jar" com.android.apksigner.ApkSignerTool verify --print-certs $out 2>&1 | Select-Object -First 3 | Out-Null
Write-Host "OK APK: $out ($([math]::Round((Get-Item $out).Length/1MB,2)) MB)"
