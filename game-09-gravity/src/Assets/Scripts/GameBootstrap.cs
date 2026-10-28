using System.Collections.Generic;
using UnityEngine;

namespace GravityDrift
{
    /// <summary>
    /// «Гравити-Дрифт» — физическая головоломка: наклон телефона ведёт корабль сквозь
    /// гравитационные колодцы к стыковочному порту. Космос, туманности, звёзды.
    /// </summary>
    public class GameBootstrap : MonoBehaviour
    {
        public static GameBootstrap Instance { get; private set; }

        private enum State { Menu, LevelSelect, Play, Win, Lose }

        private State state = State.Menu;
        private int currentLevel = 1;
        private Level level;
        private Rigidbody2D ship;
        private Transform shipTransform;
        private float fuel;
        private int starsCollected;
        private List<Transform> starObjects = new List<Transform>();
        private readonly List<GameObject> spawned = new List<GameObject>();
        private readonly Dictionary<int, int> progress = new Dictionary<int, int>();
        private Camera cam;

        // Палитра туманности
        private static readonly Color SpaceBg = new Color(0.05f, 0.05f, 0.12f);
        private static readonly Color NebulaA = new Color(0.35f, 0.12f, 0.5f, 0.35f);
        private static readonly Color NebulaB = new Color(0.1f, 0.3f, 0.55f, 0.3f);

        private void Awake()
        {
            Instance = this;
            LoadProgress();
            SetupCamera();
            BuildNebula();
        }

        private void SetupCamera()
        {
            cam = Camera.main;
            if (cam == null)
            {
                var go = new GameObject("Main Camera") { tag = "MainCamera" };
                cam = go.AddComponent<Camera>();
            }
            cam.orthographic = true;
            cam.orthographicSize = 7f;
            cam.transform.position = new Vector3(0, 0, -10);
            cam.backgroundColor = SpaceBg;
            cam.clearFlags = CameraClearFlags.SolidColor;
        }

        /// <summary>Процедурная туманность: размытые цветные круги на фоне.</summary>
        private void BuildNebula()
        {
            for (int i = 0; i < 8; i++)
            {
                var nebula = new GameObject("Nebula" + i);
                nebula.transform.position = new Vector3(Random.Range(-9f, 9f), Random.Range(-7f, 7f), 5f);
                var sr = nebula.AddComponent<SpriteRenderer>();
                sr.sprite = MakeCircleSprite(Random.Range(2.5f, 6f));
                sr.color = i % 2 == 0 ? NebulaA : NebulaB;
                sr.sortingOrder = -10;
            }
            for (int i = 0; i < 40; i++)
            {
                var star = new GameObject("BgStar" + i);
                star.transform.position = new Vector3(Random.Range(-10f, 10f), Random.Range(-8f, 8f), 4f);
                star.transform.localScale = Vector3.one * Random.Range(0.03f, 0.09f);
                var sr = star.AddComponent<SpriteRenderer>();
                sr.sprite = MakeCircleSprite(1f);
                sr.color = new Color(1f, 1f, 1f, Random.Range(0.3f, 0.9f));
                sr.sortingOrder = -9;
            }
        }

        private static Sprite MakeCircleSprite(float radius)
        {
            int size = 64;
            var tex = new Texture2D(size, size, TextureFormat.RGBA32, false);
            for (int y = 0; y < size; y++)
                for (int x = 0; x < size; x++)
                {
                    float dx = x - size / 2f;
                    float dy = y - size / 2f;
                    float d = Mathf.Sqrt(dx * dx + dy * dy) / (size / 2f);
                    float alpha = Mathf.Clamp01(1f - d);
                    tex.SetPixel(x, y, new Color(1, 1, 1, alpha * alpha));
                }
            tex.Apply();
            return Sprite.Create(tex, new Rect(0, 0, size, size), new Vector2(0.5f, 0.5f), 32f);
        }

        private void StartLevel(int levelNumber)
        {
            currentLevel = levelNumber;
            ClearLevel();
            level = LevelGenerator.Generate(31337, levelNumber);

            // Колды
            foreach (var w in level.Wells)
            {
                var go = new GameObject("Well");
                go.transform.position = new Vector3(w.X, w.Y, 0);
                go.transform.localScale = Vector3.one * (1.2f + w.Mass / 90f);
                var sr = go.AddComponent<SpriteRenderer>();
                sr.sprite = MakeCircleSprite(1f);
                sr.color = new Color(0.15f, 0.1f, 0.3f, 0.95f);
                sr.sortingOrder = 1;
                spawned.Add(go);
            }

            // Звёзды (3 шт.) между стартом и целью
            starObjects.Clear();
            for (int i = 0; i < 3; i++)
            {
                var star = new GameObject("Star" + i);
                float t = (i + 1) / 4f;
                star.transform.position = new Vector3(
                    Mathf.Lerp(level.StartX, level.GoalX, t) + Random.Range(-2f, 2f),
                    Mathf.Lerp(level.StartY, level.GoalY, t) + Random.Range(-2f, 2f), 0);
                star.transform.localScale = Vector3.one * 0.45f;
                var sr = star.AddComponent<SpriteRenderer>();
                sr.sprite = MakeCircleSprite(1f);
                sr.color = new Color(1f, 0.85f, 0.3f);
                sr.sortingOrder = 2;
                starObjects.Add(star.transform);
                spawned.Add(star);
            }

            // Порт назначения
            var goal = new GameObject("Goal");
            goal.transform.position = new Vector3(level.GoalX, level.GoalY, 0);
            goal.transform.localScale = Vector3.one * 1.4f;
            var goalSr = goal.AddComponent<SpriteRenderer>();
            goalSr.sprite = MakeCircleSprite(1f);
            goalSr.color = new Color(0.2f, 0.9f, 0.7f, 0.55f);
            goalSr.sortingOrder = 0;
            spawned.Add(goal);

            // Корабль
            var shipGo = new GameObject("Ship");
            shipTransform = shipGo.transform;
            shipTransform.position = new Vector3(level.StartX, level.StartY, 0);
            shipTransform.localScale = Vector3.one * 0.6f;
            var shipSr = shipGo.AddComponent<SpriteRenderer>();
            shipSr.sprite = MakeCircleSprite(1f);
            shipSr.color = new Color(0.95f, 0.95f, 1f);
            shipSr.sortingOrder = 5;
            spawned.Add(shipGo);
            ship = shipGo.AddComponent<Rigidbody2D>();
            ship.gravityScale = 0f;
            ship.linearDamping = 0.15f;

            fuel = level.FuelSeconds;
            starsCollected = 0;
            state = State.Play;
        }

        private void ClearLevel()
        {
            foreach (var go in spawned)
                if (go != null) Destroy(go);
            spawned.Clear();
            starObjects.Clear();
        }

        private void FixedUpdate()
        {
            if (state != State.Play || ship == null) return;

            // Гравитация от колодцев
            Vector2 total = Vector2.zero;
            foreach (var w in level.Wells)
            {
                total += PhysicsMath.ForceVector(ship.transform.position.x, ship.transform.position.y,
                    w.X, w.Y, w.Mass);
            }

            // Управление: наклон (акселерометр) или тач-драг
            Vector2 control = ReadTilt();
            total += control * 22f;

            ship.AddForce(total, ForceMode2D.Force);

            // Топливо тратится только на управление
            if (control.sqrMagnitude > 0.01f) fuel -= Time.fixedDeltaTime;
            if (fuel <= 0f)
            {
                fuel = 0f;
                state = State.Lose;
            }

            // Сбор звёзд
            for (int i = 0; i < starObjects.Count; i++)
            {
                if (starObjects[i] == null) continue;
                if (Vector2.Distance(ship.transform.position, starObjects[i].position) < 0.7f)
                {
                    Destroy(starObjects[i].gameObject);
                    starObjects[i] = null;
                    starsCollected++;
                }
            }

            // Прибытие в порт
            if (Vector2.Distance(ship.transform.position, new Vector2(level.GoalX, level.GoalY)) < 1f)
            {
                progress[currentLevel] = starsCollected;
                SaveProgress();
                state = State.Win;
            }
        }

        private Vector2 ReadTilt()
        {
            // Наклон телефона
            Vector2 tilt = new Vector2(Input.acceleration.x, Input.acceleration.y);
            if (tilt.sqrMagnitude > 0.001f) return tilt * 2.2f;
            // Запасное управление (тач-драг) для эмулятора/редактора
            if (Input.GetMouseButton(0))
            {
                Vector3 mouse = cam.ScreenToWorldPoint(Input.mousePosition);
                Vector2 dir = (Vector2)mouse - (Vector2)ship.transform.position;
                return Vector2.ClampMagnitude(dir, 1f);
            }
            return Vector2.zero;
        }

        private void LateUpdate()
        {
            if (state == State.Play && shipTransform != null)
            {
                cam.transform.position = Vector3.Lerp(cam.transform.position,
                    new Vector3(shipTransform.position.x, shipTransform.position.y, -10f),
                    4f * Time.deltaTime);
            }
        }

        // ---------------- Прогресс ----------------

        private void LoadProgress()
        {
            string text = PlayerPrefs.GetString("gravity_progress", "");
            if (string.IsNullOrEmpty(text)) return;
            foreach (var entry in text.Split(';'))
            {
                var kv = entry.Split(':');
                if (kv.Length == 2) progress[int.Parse(kv[0])] = int.Parse(kv[1]);
            }
        }

        private void SaveProgress()
        {
            var parts = new List<string>();
            foreach (var kv in progress) parts.Add(kv.Key + ":" + kv.Value);
            PlayerPrefs.SetString("gravity_progress", string.Join(";", parts.ToArray()));
            PlayerPrefs.Save();
        }

        // ---------------- UI ----------------

        private void OnGUI()
        {
            var style = new GUIStyle(GUI.skin.label) { fontSize = Screen.height / 34, normal = { textColor = Color.white } };
            var big = new GUIStyle(style) { fontSize = Screen.height / 16, alignment = TextAnchor.MiddleCenter };
            var button = new GUIStyle(GUI.skin.button) { fontSize = Screen.height / 30 };

            switch (state)
            {
                case State.Menu:
                    GUI.Label(new Rect(0, Screen.height * 0.18f, Screen.width, 100), "ГРАВИТИ-ДРИФТ", big);
                    GUI.Label(new Rect(0, Screen.height * 0.32f, Screen.width, 60),
                        "наклон ведёт корабль сквозь гравитационные колодцы", Center(style));
                    GUI.Label(new Rect(0, Screen.height * 0.38f, Screen.width, 60),
                        "собери звёзды и сэкономь топливо", Center(style));
                    if (GUI.Button(new Rect(Screen.width * 0.25f, Screen.height * 0.52f, Screen.width * 0.5f, Screen.height * 0.09f), "Играть", button))
                        state = State.LevelSelect;
                    break;

                case State.LevelSelect:
                    GUI.Label(new Rect(0, Screen.height * 0.05f, Screen.width, 80), "Уровни", big);
                    for (int i = 1; i <= 30; i++)
                    {
                        int col = (i - 1) % 5;
                        int row = (i - 1) / 5;
                        var rect = new Rect(Screen.width * 0.06f + col * Screen.width * 0.18f,
                            Screen.height * 0.15f + row * Screen.height * 0.115f,
                            Screen.width * 0.15f, Screen.height * 0.09f);
                        string stars = progress.ContainsKey(i) ? new string('★', progress[i]) : "";
                        if (GUI.Button(rect, i + "\n" + stars, button)) StartLevel(i);
                    }
                    if (GUI.Button(new Rect(Screen.width * 0.3f, Screen.height * 0.88f, Screen.width * 0.4f, Screen.height * 0.08f), "Назад", button))
                        state = State.Menu;
                    break;

                case State.Play:
                    GUI.Label(new Rect(16, 10, Screen.width, 40),
                        "Уровень " + currentLevel + "   топливо: " + fuel.ToString("F1") + "   звёзды: " + starsCollected + "/3", style);
                    break;

                case State.Win:
                    GUI.Label(new Rect(0, Screen.height * 0.28f, Screen.width, 80), "Стыковка!", big);
                    GUI.Label(new Rect(0, Screen.height * 0.4f, Screen.width, 50),
                        "звёзды: " + starsCollected + "/3   очки: " + PhysicsMath.ScoreFor(starsCollected, fuel, level.FuelSeconds), Center(style));
                    if (GUI.Button(new Rect(Screen.width * 0.25f, Screen.height * 0.52f, Screen.width * 0.5f, Screen.height * 0.09f), "Дальше", button))
                        StartLevel(Mathf.Min(currentLevel + 1, 30));
                    break;

                case State.Lose:
                    GUI.Label(new Rect(0, Screen.height * 0.3f, Screen.width, 80), "Топливо кончилось", big);
                    if (GUI.Button(new Rect(Screen.width * 0.25f, Screen.height * 0.48f, Screen.width * 0.5f, Screen.height * 0.09f), "Ещё раз", button))
                        StartLevel(currentLevel);
                    if (GUI.Button(new Rect(Screen.width * 0.25f, Screen.height * 0.6f, Screen.width * 0.5f, Screen.height * 0.09f), "К уровням", button))
                        state = State.LevelSelect;
                    break;
            }
        }

        private static GUIStyle Center(GUIStyle style)
        {
            return new GUIStyle(style) { alignment = TextAnchor.MiddleCenter };
        }
    }
}
