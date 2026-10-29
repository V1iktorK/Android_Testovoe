# tools/run-all-tests.ps1 - полный прогон автотестов всех 20 игр одной командой.
# JVM-игры: JUnit; Godot: headless GDScript; Unity: EditMode NUnit.
# Использование: tools\run-all-tests.ps1 [-Filter game-0]  — регрессия перед коммитом/сдачей.
param([string]$Filter = "")
. "$PSScriptRoot\env.ps1"
$ErrorActionPreference = "Continue"

$jvm   = @("game-02-pathlink","game-03-neonidle","game-06-runner","game-07-geomatch","game-10-petclicker",
           "game-11-drone","game-13-lasergrid","game-14-wasteland","game-15-gearworks","game-17-bubble","game-19-farm")
$godot = @("game-04-onebutton","game-08-neonbeat","game-12-siege","game-20-station")
$unity = @("game-01-robolab","game-05-factoryflow","game-09-gravity","game-16-ninja","game-18-powergrid")

$plan = @()
foreach ($g in $jvm)   { $plan += @($g, "test-jvm.ps1") }
foreach ($g in $godot) { $plan += @($g, "test-godot.ps1") }
foreach ($g in $unity) { $plan += @($g, "test-unity.ps1") }

$failed = @()
$total = 0
for ($i = 0; $i -lt $plan.Count; $i += 2) {
    $g = $plan[$i]
    $script = $plan[$i + 1]
    if ($Filter -and $g -notlike "*$Filter*") { continue }
    $total++
    $sw = [Diagnostics.Stopwatch]::StartNew()
    powershell -ExecutionPolicy Bypass -File "$PSScriptRoot\$script" -GameDir $g | Out-Null
    $ok = ($LASTEXITCODE -eq 0)
    $sw.Stop()
    if (-not $ok) { $failed += $g }
    "{0}  {1,-22}  {2}  ({3:n0} с)" -f $(if ($ok) { "PASS" } else { "FAIL" }), $g, $script, $sw.Elapsed.TotalSeconds
}

""
"=== ИТОГ: " + ($total - $failed.Count) + "/" + $total + " игр зелёные" + $(if ($failed.Count) { "; ПРОВАЛЫ: " + ($failed -join ', ') } else { "" })
if ($failed.Count) { exit 1 }
exit 0
