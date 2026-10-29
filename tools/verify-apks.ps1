# tools/verify-apks.ps1 - инструментальная проверка всех собранных APK:
#   подпись (apksigner verify), метка/пакет/иконка/minSdk (aapt2 dump badging),
#   наличие classes.dex и нативных библиотек (jar tf).
# Использование: tools\verify-apks.ps1   (или -GameDir game-01-robolab для одной игры)
param([string]$GameDir = "")
. "$PSScriptRoot\env.ps1"
$ErrorActionPreference = "Continue"

$BT   = "$UNITY_SDK\build-tools\36.0.0"
$AJAR = "$UNITY_SDK\platforms\android-34\android.jar"
$JDK  = $UNITY_JDK

$games = if ($GameDir) { @($GameDir) } else {
    Get-ChildItem $REPO_ROOT -Directory -Filter "game-*" | Sort-Object Name | ForEach-Object { $_.Name }
}

$results = @()
foreach ($g in $games) {
    $apk = Get-ChildItem (Join-Path $REPO_ROOT "$g\artifacts") -Filter "*.apk" -ErrorAction SilentlyContinue | Select-Object -First 1
    if (-not $apk) {
        $results += [pscustomobject]@{ Game = $g; Apk = "НЕТ"; Sign = "-"; Label = "-"; Package = "-"; MinSdk = "-"; Dex = "-"; Verdict = "FAIL" }
        continue
    }

    # 1. Подпись (apksigner verify возвращает 0 и печатает сертификат подписи)
    $signOut = & "$JDK\bin\java.exe" -cp "$BT\lib\apksigner.jar" com.android.apksigner.ApkSignerTool verify --print-certs $apk.FullName 2>&1 | Out-String
    $signed = if ($LASTEXITCODE -eq 0 -and $signOut -match "Signer #1 certificate DN") { "OK" } else { "FAIL" }

    # 2. Метка/пакет/minSdk/иконка
    $badging = & "$BT\aapt2.exe" dump badging $apk.FullName 2>&1 | Out-String
    $label   = if ($badging -match "application-label:'([^']*)'") { $Matches[1] } else { "НЕТ" }
    $pkg     = if ($badging -match "package: name='([^']*)'") { $Matches[1] } else { "НЕТ" }
    $minSdk  = if ($badging -match "sdkVersion:'(\d+)'") { $Matches[1] } else { "НЕТ" }
    $iconOk  = if ($badging -match "application-icon-") { "OK" } else { "НЕТ" }

    # 3. classes.dex и .so
    $listing = & "$JDK\bin\jar.exe" tf $apk.FullName 2>&1 | Out-String
    $dex = if ($listing -match "classes\.dex") { "OK" } else { "FAIL" }

    $verdict = if ($signed -eq "OK" -and $dex -eq "OK" -and $pkg -ne "НЕТ") { "PASS" } else { "FAIL" }
    $results += [pscustomobject]@{
        Game = $g; Apk = [math]::Round($apk.Length / 1MB, 2).ToString() + " MB"
        Sign = $signed; Label = $label; Package = $pkg; MinSdk = $minSdk; Icon = $iconOk; Dex = $dex; Verdict = $verdict
    }
}

$results | Format-Table -AutoSize
$fail = @($results | Where-Object { $_.Verdict -ne "PASS" })
"=== ИТОГ: " + ($results.Count - $fail.Count) + "/" + $results.Count + " APK прошли проверку" + $(if ($fail.Count) { "; ПРОБЛЕМЫ: " + (($fail | ForEach-Object { $_.Game }) -join ', ') } else { "" })
if ($fail.Count) { exit 1 }
exit 0
