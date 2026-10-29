# tools/make-dist.ps1 - сборка папки dist/ для загрузки на Google Диск:
#   dist/game-NN-название/ содержит APK и screenshot.png этой игры (структура
#   соответствует ссылкам из README), плюс ИНВЕНТАРЬ.md с контрольными суммами.
# Использование: tools\make-dist.ps1
. "$PSScriptRoot\env.ps1"
$ErrorActionPreference = "Continue"

$dist = Join-Path $REPO_ROOT "dist"
if (Test-Path $dist) { Remove-Item $dist -Recurse -Force }
New-Item -ItemType Directory -Force -Path $dist | Out-Null

$games = Get-ChildItem $REPO_ROOT -Directory -Filter "game-*" | Sort-Object Name
$lines = @()
$lines += "# Инвентарь сборок для Google Диска"
$lines += ""
$lines += "Сгенерировано: $(Get-Date -Format 'yyyy-MM-dd HH:mm'). Загрузить содержимое `dist/` в папку:"
$lines += "https://drive.google.com/drive/folders/1YZzw1tn-sIxi9mlgxwU7H5XNdUKCMvw7?usp=sharing"
$lines += ""
$lines += "| № | Папка | APK | Размер | SHA-256 (первые 16) | Метка |"
$lines += "|---|---|---|---|---|---|"

foreach ($g in $games) {
    $apk = Get-ChildItem "$($g.FullName)\artifacts" -Filter "*.apk" -ErrorAction SilentlyContinue | Select-Object -First 1
    $shot = Join-Path $g.FullName "screenshot.png"
    $folder = Join-Path $dist $g.Name
    New-Item -ItemType Directory -Force -Path $folder | Out-Null
    if ($apk) {
        Copy-Item $apk.FullName (Join-Path $folder $apk.Name) -Force
        $hash = (Get-FileHash $apk.FullName -Algorithm SHA256).Hash.ToLower()
        $size = [math]::Round($apk.Length / 1MB, 2)
        $badging = & "$UNITY_SDK\build-tools\36.0.0\aapt2.exe" dump badging $apk.FullName 2>&1 | Out-String
        $label = if ($badging -match "application-label:'([^']*)'") { $Matches[1] } else { "?" }
        $lines += "| $($g.Name.Substring(4,2)) | $($g.Name) | $($apk.Name) | $size MB | $($hash.Substring(0,16))… | $label |"
    } else {
        $lines += "| $($g.Name.Substring(4,2)) | $($g.Name) | НЕТ APK | - | - | - |"
    }
    if (Test-Path $shot) { Copy-Item $shot $folder -Force }
}

$lines += ""
$lines += "## Чек-лист загрузки"
$lines += "1. Создать в папке Диска подпапки с именами из таблицы и залить в каждую её APK + screenshot.png."
$lines += "2. Убедиться, что размеры файлов совпадают с таблицей (контрольные суммы SHA-256 — для проверки целостности)."
$lines += "3. Включить доступ «по ссылке — читатель» на общую папку."
$lines += "4. Сборки подписаны отладочными ключами из `game-NN/artifacts/release.keystore` (пароль android)."
$lines += "   Для публикации в Google Play создайте собственные ключи и переподпишите APK/AAB."
Set-Content -Path (Join-Path $dist "ИНВЕНТАРЬ.md") -Value ($lines -join "`n") -Encoding UTF8

"=== dist/ собран ==="
Get-ChildItem $dist | ForEach-Object {
    if ($_.PSIsContainer) {
        $apk = Get-ChildItem $_.FullName -Filter "*.apk" | Select-Object -First 1
        "  {0,-24} {1}" -f $_.Name, $(if ($apk) { [math]::Round($apk.Length/1MB,2).ToString() + " MB" } else { "ПУСТО" })
    } else {
        "  {0}" -f $_.Name
    }
}
$total = (Get-ChildItem $dist -Recurse -Filter "*.apk" | Measure-Object Length -Sum).Sum
"Итого APK: " + [math]::Round($total/1MB,1) + " MB"
