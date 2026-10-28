using System.Collections.Generic;
using UnityEngine;

namespace EnergyShield
{
    /// <summary>
    /// «Энерго-Щит» — tower defense защиты электростанции: враги идут по коридору цеха,
    /// башни тратят энергию станции. Промышленный стиль: сталь, трубы, предупреждающие полосы.
    /// Сквозная тема: промышленная автоматизация и энергобюджет.
    /// </summary>
    public class GameBootstrap : MonoBehaviour
    {
        public static GameBootstrap Instance { get; private set; }

        private enum State { Menu, Play, Building, Win, Lose }

        private State state = State.Menu;
        private int currentLevel = 1;
        private List<Wave> waves;
        private int waveIndex;
        private DefenseGrid grid;
        private float plantHp = 100f;
        private int score;
        private readonly List<GameObject> enemies = new List<GameObject>();
        private readonly List<Vector2> path = new List<Vector2>();
        private float spawnTimer;
        private int spawnedThisWave;
        private TowerCatalog.Kind buildKind = TowerCatalog.Kind.Pylon;
        private Camera cam;

        private static readonly Color FloorColor = new Color(0.16f, 0.17f, 0.2f);
        private static readonly Color PathColor = new Color(0.28f, 0.26f, 0.22f);
        private static readonly Color TowerColor = new Color(0.35f, 0.55f, 0.7f);
        private static readonly Color EnemyColor = new Color(0.85f, 0.35f, 0.3f);

        private void Awake()
        {
            Instance = this;
            cam = Camera.main;
            if (cam == null)
            {
                var go = new GameObject("Main Camera") { tag = "MainCamera" };
                cam = go.AddComponent<Camera>();
            }
            cam.orthographic = true;
            cam.orthographicSize = 7f;
            cam.backgroundColor = new Color(0.1f, 0.11f, 0.13f);
            cam.clearFlags = CameraClearFlags.SolidColor;
            BuildScene();
        }

        /// <summary>Промышленный цех: пол, коридор атаки, электростанция.</summary>
        private void BuildScene()
        {
            var floor = new GameObject("Floor");
            floor.transform.position = new Vector3(0, 0, 1);
            floor.transform.localScale = new Vector3(20, 12, 1);
            floor.AddComponent<SpriteRenderer>().sprite = MakeSprite(FloorColor);

            // Коридор врагов: изгибающийся путь
            path.Clear();
            path.Add(new Vector2(-9f, 3f));
            path.Add(new Vector2(-3f, 3f));
            path.Add(new Vector2(-3f, -2f));
            path.Add(new Vector2(3f, -2f));
            path.Add(new Vector2(3f, 3f));
            path.Add(new Vector2(8f, 3f));

            for (int i = 0; i < path.Count - 1; i++)
            {
                var segment = new GameObject("Path");
                Vector2 mid = (path[i] + path[i + 1]) / 2f;
                segment.transform.position = new Vector3(mid.x, mid.y, 0.5f);
                segment.transform.localScale = new Vector3(
                    Mathf.Abs(path[i + 1].x - path[i].x) + 1f,
                    Mathf.Abs(path[i + 1].y - path[i].y) + 1f, 1f);
                segment.AddComponent<SpriteRenderer>().sprite = MakeSprite(PathColor);
            }

            // Электростанция в конце пути
            var plant = new GameObject("Plant");
            plant.transform.position = new Vector3(8f, 3f, -0.5f);
            plant.transform.localScale = Vector3.one * 2.2f;
            plant.AddComponent<SpriteRenderer>().sprite = MakeSprite(new Color(0.9f, 0.75f, 0.25f));
        }

        private static Sprite MakeSprite(Color color)
        {
            var tex = new Texture2D(8, 8, TextureFormat.RGBA32, false) { filterMode = FilterMode.Point };
            for (int y = 0; y < 8; y++)
                for (int x = 0; x < 8; x++)
                    tex.SetPixel(x, y, color);
            tex.Apply();
            return Sprite.Create(tex, new Rect(0, 0, 8, 8), new Vector2(0.5f, 0.5f), 8f);
        }

        private void StartLevel(int levelNumber)
        {
            currentLevel = levelNumber;
            ClearLevel();
            waves = WavePlanner.Generate(20261028 + levelNumber, 6 + levelNumber / 3);
            waveIndex = 0;
            grid = new DefenseGrid(100 + levelNumber * 25);
            plantHp = 100f;
            score = 0;
            spawnedThisWave = 0;
            spawnTimer = 1.5f;
            state = State.Play;
        }

        private void ClearLevel()
        {
            foreach (var e in enemies) if (e != null) Destroy(e);
            enemies.Clear();
            foreach (var t in GameObject.FindGameObjectsWithTag("Untagged"))
                if (t.name.StartsWith("Tower")) Destroy(t);
        }

        private void Update()
        {
            if (state != State.Play) return;

            // Спавн врагов текущей волны
            if (waveIndex < waves.Count)
            {
                Wave wave = waves[waveIndex];
                if (spawnedThisWave < wave.EnemyCount)
                {
                    spawnTimer -= Time.deltaTime;
                    if (spawnTimer <= 0f)
                    {
                        SpawnEnemy(wave);
                        spawnedThisWave++;
                        spawnTimer = 1.2f / wave.EnemySpeed;
                    }
                }
                else if (enemies.Count == 0)
                {
                    waveIndex++;
                    spawnedThisWave = 0;
                    if (waveIndex >= waves.Count)
                    {
                        state = State.Win;
                    }
                }
            }

            // Движение врагов по пути
            for (int i = enemies.Count - 1; i >= 0; i--)
            {
                var e = enemies[i];
                if (e == null) { enemies.RemoveAt(i); continue; }
                MoveAlongPath(e);
                if (e.GetComponent<Enemy>().ReachedEnd)
                {
                    plantHp -= 10f;
                    Destroy(e);
                    enemies.RemoveAt(i);
                    if (plantHp <= 0f) state = State.Lose;
                }
            }

            // Огонь башен
            FireTowers();
        }

        private void SpawnEnemy(Wave wave)
        {
            var e = new GameObject("Enemy");
            e.transform.position = new Vector3(path[0].x, path[0].y, -0.3f);
            e.transform.localScale = Vector3.one * (wave.IsBoss ? 1.6f : 0.8f);
            e.AddComponent<SpriteRenderer>().sprite = MakeSprite(wave.IsBoss ? new Color(0.5f, 0.1f, 0.6f) : EnemyColor);
            var enemy = e.AddComponent<Enemy>();
            enemy.Init(wave.EnemyHp, wave.EnemySpeed, wave.IsBoss);
            enemies.Add(e);
        }

        private void MoveAlongPath(GameObject e)
        {
            var enemy = e.GetComponent<Enemy>();
            if (enemy.ReachedEnd) return;
            int seg = enemy.PathIndex;
            if (seg >= path.Count - 1) { enemy.ReachedEnd = true; return; }
            Vector2 target = path[seg + 1];
            e.transform.position = Vector3.MoveTowards(e.transform.position,
                new Vector3(target.x, target.y, e.transform.position.z),
                enemy.Speed * Time.deltaTime * 2.2f);
            if (Vector2.Distance(e.transform.position, target) < 0.1f) enemy.PathIndex++;
        }

        private void FireTowers()
        {
            foreach (var t in GameObject.FindGameObjectsWithTag("Untagged"))
            {
                if (!t.name.StartsWith("Tower")) continue;
                var parts = t.name.Split('_');
                int tx = int.Parse(parts[1]);
                int ty = int.Parse(parts[2]);
                float damage = grid.TowerDamage(tx, ty);
                float range = TowerCatalog.For(TowerCatalog.Kind.Pylon).Range;

                GameObject best = null;
                float bestDist = range;
                foreach (var e in enemies)
                {
                    if (e == null) continue;
                    float d = Vector2.Distance(t.transform.position, e.transform.position);
                    if (d < bestDist)
                    {
                        bestDist = d;
                        best = e;
                    }
                }
                if (best != null)
                {
                    var enemy = best.GetComponent<Enemy>();
                    enemy.Hp -= damage * Time.deltaTime;
                    if (enemy.Hp <= 0f)
                    {
                        score += DefenseGrid.ScoreForKill(waveIndex + 1, enemy.IsBoss);
                        enemies.Remove(best);
                        Destroy(best);
                    }
                }
            }
        }

        // ---------------- Ввод: строительство ----------------

        private void OnGUI()
        {
            var style = new GUIStyle(GUI.skin.label) { fontSize = Screen.height / 34, normal = { textColor = Color.white } };
            var big = new GUIStyle(style) { fontSize = Screen.height / 16, alignment = TextAnchor.MiddleCenter };
            var button = new GUIStyle(GUI.skin.button) { fontSize = Screen.height / 30 };

            switch (state)
            {
                case State.Menu:
                    GUI.Label(new Rect(0, Screen.height * 0.2f, Screen.width, 100), "ЭНЕРГО-ЩИТ", big);
                    GUI.Label(new Rect(0, Screen.height * 0.33f, Screen.width, 60),
                        "оборона электростанции: башни тратят энергию станции", Center(style));
                    GUI.Label(new Rect(0, Screen.height * 0.39f, Screen.width, 60),
                        "разумно распределяй бюджет между пилонами и реакторами", Center(style));
                    if (GUI.Button(new Rect(Screen.width * 0.25f, Screen.height * 0.52f, Screen.width * 0.5f, Screen.height * 0.09f), "В бой", button))
                        StartLevel(1);
                    break;

                case State.Play:
                    GUI.Label(new Rect(16, 10, Screen.width, 40),
                        "Волна " + (waveIndex + 1) + "/" + waves.Count + "   станция: " + plantHp + "   счёт: " + score
                        + "   энергия: " + grid.FreeEnergy, style);
                    if (GUI.Button(new Rect(Screen.width * 0.02f, Screen.height * 0.88f, Screen.width * 0.3f, Screen.height * 0.08f),
                        "Пилон (50⚡)", button)) buildKind = TowerCatalog.Kind.Pylon;
                    if (GUI.Button(new Rect(Screen.width * 0.35f, Screen.height * 0.88f, Screen.width * 0.3f, Screen.height * 0.08f),
                        "Турбина (90⚡)", button)) buildKind = TowerCatalog.Kind.Turbine;
                    if (GUI.Button(new Rect(Screen.width * 0.68f, Screen.height * 0.88f, Screen.width * 0.3f, Screen.height * 0.08f),
                        "Реактор (220⚡)", button)) buildKind = TowerCatalog.Kind.Reactor;
                    break;

                case State.Win:
                    GUI.Label(new Rect(0, Screen.height * 0.3f, Screen.width, 80), "Станция спасена!", big);
                    GUI.Label(new Rect(0, Screen.height * 0.42f, Screen.width, 50), "очки: " + score, Center(style));
                    if (GUI.Button(new Rect(Screen.width * 0.25f, Screen.height * 0.55f, Screen.width * 0.5f, Screen.height * 0.09f), "Дальше", button))
                        StartLevel(Mathf.Min(currentLevel + 1, 20));
                    break;

                case State.Lose:
                    GUI.Label(new Rect(0, Screen.height * 0.3f, Screen.width, 80), "Станция разрушена", big);
                    if (GUI.Button(new Rect(Screen.width * 0.25f, Screen.height * 0.48f, Screen.width * 0.5f, Screen.height * 0.09f), "Ещё раз", button))
                        StartLevel(currentLevel);
                    break;
            }
        }

        private State StartLevelGuard()
        {
            StartLevel(1);
            return State.Play;
        }

        private void OnMouseDownPlace(Vector2 world)
        {
            int gx = Mathf.RoundToInt(world.x);
            int gy = Mathf.RoundToInt(world.y);
            if (grid.Build(buildKind, gx, gy))
            {
                var tower = new GameObject("Tower_" + gx + "_" + gy);
                tower.transform.position = new Vector3(gx, gy, -0.4f);
                tower.transform.localScale = Vector3.one * 0.85f;
                tower.AddComponent<SpriteRenderer>().sprite = MakeSprite(TowerColor);
            }
        }

        private void UpdateInput()
        {
            if (state != State.Play) return;
            if (Input.GetMouseButtonDown(0))
            {
                Vector2 world = cam.ScreenToWorldPoint(Input.mousePosition);
                OnMouseDownPlace(world);
            }
        }

        private void LateUpdate()
        {
            UpdateInput();
        }

        private static GUIStyle Center(GUIStyle style)
        {
            return new GUIStyle(style) { alignment = TextAnchor.MiddleCenter };
        }
    }

    /// <summary>Враг: здоровье, скорость, путь.</summary>
    public class Enemy : MonoBehaviour
    {
        public float Hp;
        public float Speed;
        public bool IsBoss;
        public int PathIndex;
        public bool ReachedEnd;

        public void Init(float hp, float speed, bool boss)
        {
            Hp = hp;
            Speed = speed;
            IsBoss = boss;
        }
    }
}
