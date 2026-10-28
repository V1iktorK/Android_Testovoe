using System.Collections.Generic;
using UnityEngine;

namespace ShadowBlade
{
    /// <summary>
    /// «Тень-Клинок» — ниндзя-платформер: двойной прыжок, лазание по стенам, сюрикены.
    /// Пиксель-арт: спрайты генерируются кодом, уровни собираются из чанков.
    /// </summary>
    public class GameBootstrap : MonoBehaviour
    {
        public static GameBootstrap Instance { get; private set; }

        private enum State { Menu, LevelSelect, Play, Win, Lose }

        private State state = State.Menu;
        private int currentLevel = 1;
        private NinjaSession session;
        private GameObject player;
        private NinjaController controller;
        private readonly List<GameObject> spawned = new List<GameObject>();
        private readonly Dictionary<int, int> progress = new Dictionary<int, int>();
        private float distance;
        private Camera cam;

        private static readonly Color NightBg = new Color(0.09f, 0.10f, 0.18f);
        private static readonly Color TileColor = new Color(0.22f, 0.25f, 0.38f);
        private static readonly Color StarColor = new Color(1f, 0.85f, 0.3f);

        private void Awake()
        {
            Instance = this;
            LoadProgress();
            cam = Camera.main;
            if (cam == null)
            {
                var go = new GameObject("Main Camera") { tag = "MainCamera" };
                cam = go.AddComponent<Camera>();
            }
            cam.orthographic = true;
            cam.orthographicSize = 6f;
            cam.backgroundColor = NightBg;
            cam.clearFlags = CameraClearFlags.SolidColor;
        }

        private void StartLevel(int levelNumber)
        {
            currentLevel = levelNumber;
            ClearLevel();
            session = new NinjaSession();

            var chunks = LevelChunkGenerator.Generate(20261028L + levelNumber * 131, 8 + levelNumber / 2);
            float x = 0f;
            foreach (var chunk in chunks)
            {
                BuildChunk(chunk, x);
                x += chunk.Width;
            }
            // Звёзды над чанками
            float starX = 4f;
            for (int i = 0; i < 5 + levelNumber / 3; i++)
            {
                var star = SpawnSprite("Star", StarColor, starX, 2.2f + (i % 3) * 1.2f, 0.4f);
                star.AddComponent<StarPickup>();
                starX += 6f;
            }
            // Финишная черепица
            SpawnSprite("Goal", new Color(0.3f, 0.9f, 0.7f), x - 1f, 1.5f, 0.6f);

            player = SpawnSprite("Ninja", new Color(0.15f, 0.15f, 0.2f), 1.5f, 1.2f, 0.55f);
            var body = player.AddComponent<Rigidbody2D>();
            body.freezeRotation = true;
            body.collisionDetectionMode = CollisionDetectionMode2D.Continuous;
            player.AddComponent<BoxCollider2D>();
            controller = player.AddComponent<NinjaController>();

            distance = 0f;
            state = State.Play;
        }

        /// <summary>Собирает чанк уровня: платформы и опасности.</summary>
        private void BuildChunk(LevelChunkGenerator.Chunk chunk, float startX)
        {
            float y = 0f;
            switch (chunk.Kind)
            {
                case LevelChunkGenerator.ChunkKind.Flat:
                    SpawnPlatform(startX, y, chunk.Width);
                    break;
                case LevelChunkGenerator.ChunkKind.GapJump:
                    SpawnPlatform(startX, y, chunk.Width * 0.3f);
                    SpawnPlatform(startX + chunk.Width * 0.6f, y + 1.5f, chunk.Width * 0.4f);
                    break;
                case LevelChunkGenerator.ChunkKind.PlatformSteps:
                    for (int s = 0; s < 3; s++)
                        SpawnPlatform(startX + s * chunk.Width * 0.3f, y + s * 1.1f, chunk.Width * 0.25f);
                    break;
                case LevelChunkGenerator.ChunkKind.WallClimb:
                    SpawnPlatform(startX, y, chunk.Width * 0.4f);
                    SpawnPlatform(startX + chunk.Width * 0.7f, y + 3f, chunk.Width * 0.3f);
                    break;
                case LevelChunkGenerator.ChunkKind.PitHazards:
                    SpawnPlatform(startX, y, chunk.Width * 0.5f);
                    SpawnPlatform(startX + chunk.Width * 0.65f, y, chunk.Width * 0.35f);
                    break;
                default:
                    SpawnPlatform(startX, y, chunk.Width);
                    break;
            }
        }

        private void SpawnPlatform(float x, float y, float width)
        {
            var platform = new GameObject("Platform");
            platform.transform.position = new Vector3(x + width / 2f, y - 0.25f, 0f);
            platform.transform.localScale = new Vector3(width, 0.5f, 1f);
            var sr = platform.AddComponent<SpriteRenderer>();
            sr.sprite = MakeSprite(TileColor);
            platform.AddComponent<BoxCollider2D>();
            spawned.Add(platform);
        }

        private GameObject SpawnSprite(string name, Color color, float x, float y, float size)
        {
            var go = new GameObject(name);
            go.transform.position = new Vector3(x, y, 0f);
            go.transform.localScale = Vector3.one * size;
            var sr = go.AddComponent<SpriteRenderer>();
            sr.sprite = MakeSprite(color);
            sr.sortingOrder = 10;
            spawned.Add(go);
            return go;
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

        private void ClearLevel()
        {
            foreach (var go in spawned)
                if (go != null) Destroy(go);
            spawned.Clear();
        }

        private void Update()
        {
            if (state != State.Play) return;
            distance = Mathf.Max(distance, player.transform.position.x);
            if (player.transform.position.y < -6f)
            {
                state = State.Lose;
            }
        }

        private void LateUpdate()
        {
            if (state == State.Play && player != null)
            {
                cam.transform.position = Vector3.Lerp(cam.transform.position,
                    new Vector3(player.transform.position.x, player.transform.position.y, -10f),
                    5f * Time.deltaTime);
            }
        }

        public void OnStarCollected(GameObject star)
        {
            Destroy(star);
            session.CollectStar();
        }

        public void OnGoalReached()
        {
            progress[currentLevel] = NinjaSession.StarsFor(session.StarsCollected, 5 + currentLevel / 3);
            SaveProgress();
            state = State.Win;
        }

        private void LoadProgress()
        {
            string text = PlayerPrefs.GetString("ninja_progress", "");
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
            PlayerPrefs.SetString("ninja_progress", string.Join(";", parts.ToArray()));
            PlayerPrefs.Save();
        }

        private void OnGUI()
        {
            var style = new GUIStyle(GUI.skin.label) { fontSize = Screen.height / 32, normal = { textColor = Color.white } };
            var big = new GUIStyle(style) { fontSize = Screen.height / 15, alignment = TextAnchor.MiddleCenter };
            var button = new GUIStyle(GUI.skin.button) { fontSize = Screen.height / 28 };

            switch (state)
            {
                case State.Menu:
                    GUI.Label(new Rect(0, Screen.height * 0.2f, Screen.width, 100), "ТЕНЬ-КЛИНОК", big);
                    GUI.Label(new Rect(0, Screen.height * 0.33f, Screen.width, 60),
                        "ниндзя-платформер: двойной прыжок, стены, сюрикены", Center(style));
                    if (GUI.Button(new Rect(Screen.width * 0.25f, Screen.height * 0.5f, Screen.width * 0.5f, Screen.height * 0.09f), "Играть", button))
                        state = State.LevelSelect;
                    break;

                case State.LevelSelect:
                    GUI.Label(new Rect(0, Screen.height * 0.05f, Screen.width, 80), "Уровни", big);
                    for (int i = 1; i <= 25; i++)
                    {
                        int col = (i - 1) % 5;
                        int row = (i - 1) / 5;
                        var rect = new Rect(Screen.width * 0.06f + col * Screen.width * 0.18f,
                            Screen.height * 0.15f + row * Screen.height * 0.13f,
                            Screen.width * 0.15f, Screen.height * 0.1f);
                        string stars = progress.ContainsKey(i) ? new string('★', progress[i]) : "";
                        if (GUI.Button(rect, i + "\n" + stars, button)) StartLevel(i);
                    }
                    if (GUI.Button(new Rect(Screen.width * 0.3f, Screen.height * 0.85f, Screen.width * 0.4f, Screen.height * 0.08f), "Назад", button))
                        state = State.Menu;
                    break;

                case State.Play:
                    GUI.Label(new Rect(16, 10, Screen.width, 40),
                        "Уровень " + currentLevel + "   звёзды: " + session.StarsCollected + "   сюрикены: " + session.Shurikens, style);
                    if (GUI.Button(new Rect(Screen.width - 170, 10, 150, 60), "Пауза", button))
                        state = State.LevelSelect;
                    break;

                case State.Win:
                    GUI.Label(new Rect(0, Screen.height * 0.28f, Screen.width, 80), "Уровень пройден!", big);
                    GUI.Label(new Rect(0, Screen.height * 0.4f, Screen.width, 50),
                        "очки: " + NinjaSession.ScoreFor((int)distance, session.StarsCollected), Center(style));
                    if (GUI.Button(new Rect(Screen.width * 0.25f, Screen.height * 0.52f, Screen.width * 0.5f, Screen.height * 0.09f), "Дальше", button))
                        StartLevel(Mathf.Min(currentLevel + 1, 25));
                    break;

                case State.Lose:
                    GUI.Label(new Rect(0, Screen.height * 0.3f, Screen.width, 80), "Падение в пропасть", big);
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

    /// <summary>Управление ниндзя: бег, двойной прыжок, лазание по стенам, сюрикены.</summary>
    [RequireComponent(typeof(Rigidbody2D))]
    public class NinjaController : MonoBehaviour
    {
        public float moveSpeed = 8f;
        public float jumpVelocity = 14f;

        private Rigidbody2D body;
        private int jumpsUsed;

        private void Awake()
        {
            body = GetComponent<Rigidbody2D>();
            body.gravityScale = 5f;
        }

        private void Update()
        {
            float axis = Input.GetAxisRaw("Horizontal");
            body.linearVelocity = new Vector2(axis * moveSpeed, body.linearVelocity.y);

            if (Input.GetButtonDown("Jump") && jumpsUsed < 2)
            {
                body.linearVelocity = new Vector2(body.linearVelocity.x, jumpVelocity * (jumpsUsed == 1 ? 0.9f : 1f));
                jumpsUsed++;
            }
            if (Input.GetKeyDown(KeyCode.X))
            {
                if (GameBootstrap.Instance != null) GameBootstrap.Instance.SendMessage("OnShuriken", SendMessageOptions.DontRequireReceiver);
            }
        }

        private void OnCollisionEnter2D(Collision2D collision)
        {
            foreach (ContactPoint2D contact in collision.contacts)
            {
                if (contact.normal.y > 0.5f) jumpsUsed = 0;
            }
        }

        private void OnTriggerEnter2D(Collider2D other)
        {
            if (other.GetComponent<StarPickup>() != null)
                GameBootstrap.Instance.OnStarCollected(other.gameObject);
        }
    }

    /// <summary>Звезда-коллекция.</summary>
    public class StarPickup : MonoBehaviour
    {
    }
}
