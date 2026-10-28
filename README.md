# Портфолио: 20 Android-игр (тестовое задание)

Сквозная тема: **робототехника, VR, промышленная автоматизация** — направление 15.03.04
«Автоматизация технологических процессов в машиностроении». Тема раскрыта в играх
**01 (робо-лаборатория), 05 (VR-тренажёр цеха), 11 (дрон на автозаводе), 18 (защита
электростанции), 20 (автоматизированная космостанция)**.

Каждая игра уникальна по трём осям: **A** — жанр и кор-механика, **B** — визуальный стиль
и сеттинг, **C** — способ генерации контента. Реестр уникальности — в `REGISTRY.md`.

## Стек технологий (по требованию: Unity, Godot, Java, Kotlin — без веба)

| Стек | Игры |
|---|---|
| Unity 6 (C#) | 01, 05, 09, 16, 18 |
| Godot 4.3 (GDScript) | 04, 08, 12, 20 |
| Java 17 (Android SDK) | 03, 07, 13, 15, 17, 19 |
| Kotlin 2.0 (Android SDK) | 02, 06, 10, 11, 14 |

## Каталог игр

| № | Папка | Игра | Жанр | Технология |
|---|---|---|---|---|
| 01 | game-01-robolab | Робо-Лаборатория ★ | платформер | Unity |
| 02 | game-02-pathlink | Соедини пути | головоломка | Kotlin |
| 03 | game-03-neonidle | Неоновое ядро | idle-кликер | Java |
| 04 | game-04-onebutton | Импульс-раннер | аркада одной кнопкой | Godot |
| 05 | game-05-factoryflow | Фабрика-Флоу ★ | симулятор цеха (VR-тренажёр) | Unity |
| 06 | game-06-runner | Мультяшный раннер | раннер | Kotlin |
| 07 | game-07-geomatch | Гео-Матч | match-3 | Java |
| 08 | game-08-neonbeat | Неон-Бит | ритм-игра | Godot |
| 09 | game-09-gravity | Гравити-Дрифт | физ. головоломка (наклон) | Unity |
| 10 | game-10-petclicker | Пушистый кликер | кликер | Kotlin |
| 11 | game-11-drone | Дрон-Патруль ★ | шутер сверху | Kotlin |
| 12 | game-12-siege | Тёмная осада | TD с инверсией ролей | Godot |
| 13 | game-13-lasergrid | Луч-88 | головоломка с лучом | Java |
| 14 | game-14-wasteland | Пепел | выживание | Kotlin |
| 15 | game-15-gearworks | Механозавр | пазл механизма | Java |
| 16 | game-16-ninja | Тень-Клинок | платформер | Unity |
| 17 | game-17-bubble | Пузырьки глубин | bubble shooter | Java |
| 18 | game-18-powergrid | Энерго-Щит ★ | tower defense | Unity |
| 19 | game-19-farm | Уютная ферма | ферма | Java |
| 20 | game-20-station | Станция-Падение ★ | рогалик-шутер | Godot |

★ — сквозная тема портфолио.

## Структура каждой игры
- `README.md` — описание, механика, управление, сборка, ссылка на Google Диск;
- `src/` — полный исходный проект игры;
- `tests/` (или `src/Assets/Tests` для Unity) — автотесты логики, написанные ДО реализации;
- `assets-gen/` — офлайн-генерация screenshot.png и иконки;
- `screenshot.png` — скриншот игры;
- `artifacts/` — локальные APK и ключи подписи (в git не коммитятся).

## Сборка и тесты
Инструменты в `tools/` (PowerShell):
```powershell
# JVM-игры (Java/Kotlin): тесты и APK без Gradle (javac/kotlinc -> d8 -> aapt2 -> apksigner)
tools\test-jvm.ps1 -GameDir game-03-neonidle
tools\build-javapk.ps1 -GameDir game-03-neonidle

# Godot: headless-тесты и экспорт APK
tools\test-godot.ps1 -GameDir game-04-onebutton
tools\build-godot.ps1 -GameDir game-04-onebutton

# Unity: EditMode-тесты и batchmode-сборка
tools\test-unity.ps1 -GameDir game-01-robolab
tools\build-unity.ps1 -GameDir game-01-robolab -AppId com.portfolio.robolab -ProductName "RoboLab"
```

## Сборки на Google Диск
APK всех игр подготовлены в `game-NN/artifacts/`. Папка для загрузки:
https://drive.google.com/drive/folders/1YZzw1tn-sIxi9mlgxwU7H5XNdUKCMvw7?usp=sharing

## План и отчётность
- `PLAN.md` — согласованный план работ и внесённые в него правки;
- `REGISTRY.md` — реестр игр с осями уникальности и статусами тестов/сборок.
