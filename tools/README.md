# Инструменты сборки и тестирования

Все скрипты — PowerShell (запускать с `-ExecutionPolicy Bypass`), рабочая директория — корень репозитория.

## Основные команды

```powershell
# Полная регрессия: автотесты всех 20 игр (JUnit + GDScript + NUnit)
tools\run-all-tests.ps1

# Тесты одной игры (стек определяется автоматически по её конвейеру)
tools\test-jvm.ps1   -GameDir game-03-neonidle     # Java/Kotlin: JUnit
tools\test-godot.ps1 -GameDir game-04-onebutton    # Godot: headless GDScript
tools\test-unity.ps1 -GameDir game-01-robolab      # Unity: EditMode NUnit

# Сборка APK одной игры
tools\build-javapk.ps1 -GameDir game-03-neonidle   # javac/kotlinc -> d8 -> aapt2 -> apksigner
tools\build-godot.ps1  -GameDir game-04-onebutton  # headless-экспорт Godot
tools\build-unity.ps1  -GameDir game-01-robolab -AppId com.portfolio.robolab -ProductName "RoboLab"

# Генерация screenshot.png и иконки игры (офлайн, из assets-gen)
tools\test-screenshot.ps1 -GameDir game-03-neonidle [-Width 1080 -Height 1920]

# Контроль качества релиза
tools\verify-apks.ps1   # подпись, метка, пакет, minSdk, иконка, dex всех APK
tools\make-dist.ps1     # сборка dist/ для Google Диска (APK+скриншоты+SHA-256)
```

## Важные особенности окружения (учтены в скриптах)

- **Не-ASCII пути**: Unity Android Tools, CMake/NDK, `aapt2 --dir` и minizip в Godot не работают
  с кириллицей — Unity-проекты собираются в ASCII-копии `C:\gamedev-work\<игра>`,
  Godot-экспорт идёт с перенаправленными APPDATA/LOCALAPPDATA на `C:\gamedev-work\`.
- **JDK/SDK**: используются из Android-модуля Unity (`tools/env.ps1` задаёт JAVA_HOME/ANDROID_HOME);
  Gradle не требуется — конвейер Java/Kotlin работает напрямую через d8/aapt2/apksigner.
- **Kotlin**: компилятор `kotlin-compiler-embeddable` + зависимости в `%LOCALAPPDATA%\gamedev-tools\m2`.
- **Godot**: настройки Android-экспорта — в `editor_settings-4.3.tres` (версионное имя файла!),
  шаблоны экспорта 4.3.stable в `C:\gamedev-work\appdata\Godot\export_templates`.
- **PowerShell**: скрипты в UTF-8 с BOM; argfile для javac — в ANSI, для kotlinc — в UTF-8.
- **Unity**: сборка перезапускает Unity — ожидание ведётся по процессам проекта;
  minSdk 26, Mono-бэкенд (флаг `-Il2cpp` переключает на IL2CPP).

## Структура игры

```
game-NN-название/
├── README.md          # описание, управление, сборка, ссылка на Google Диск
├── src/               # полный исходный проект
├── tests/             # автотесты логики (для Unity — src/Assets/Tests/EditMode)
├── assets-gen/        # генерация screenshot.png и иконки
├── screenshot.png
└── artifacts/         # APK + release.keystore (в git не коммитится)
```
