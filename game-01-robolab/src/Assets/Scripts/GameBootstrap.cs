using System.Collections.Generic;
using UnityEngine;

namespace RoboLab
{
    /// <summary>
    /// Точка входа и игровой процесс «Робо-Лаборатория»: меню, уровни, пауза, победа.
    /// Пиксель-арт лаборатории роботов — вся графика процедурная (PixelSprites).
    /// </summary>
    public class GameBootstrap : MonoBehaviour
    {
        public static GameBootstrap Instance { get; private set; }

        private enum State { Menu, LevelSelect, Play, Pause, Win, Lose }

        private State state = State.Menu;
        private int currentLevel = 1;
        private LevelMap map;
        private Transform levelRoot;
        private GameObject player;
        private PlayerController controller;
        private List<GameObject> batteries;
        private GameSession session;
        private float playSeconds;
        private int batteriesOnLevel;
        private readonly List<Vector2> collected = new List<Vector2>();
        private ProgressState progress = new ProgressState();
        private Camera cam;

        // Процедурный звук
        private AudioClip jumpClip, batteryClip, hitClip, winClip;
        private AudioSource audioSource;

        // Сенсорное управление
        private int moveFinger = -1;
        private float moveAxis;

        private void Awake()
        {
            Instance = this;
            cam = Camera.main;
            if (cam == null)
            {
                var camGo = new GameObject("Main Camera");
                camGo.tag = "MainCamera";
                cam = camGo.AddComponent<Camera>();
            }
            cam.orthographic = true;
            cam.orthographicSize = 6f;
            cam.backgroundColor = new Color(0.07f, 0.09f, 0.16f);
            cam.clearFlags = CameraClearFlags.SolidColor;

            audioSource = gameObject.AddComponent<AudioSource>();
            jumpClip = MakeClip(520, 0.09f);
            batteryClip = MakeClip(880, 0.12f);
            hitClip = MakeClip(180, 0.18f);
            winClip = MakeClip(660, 0.3f);

            progress = ProgressState.Deserialize(PlayerPrefs.GetString("robolab_progress", ""));
        }

        private AudioClip MakeClip(float freq, float seconds)
        {
            int rate = 22050;
            int n = (int)(rate * seconds);
            float[] samples = new float[n];
            for (int i = 0; i < n; i++)
            {
                float env = 1f - (float)i / n;
                samples[i] = Mathf.Sin(2f * Mathf.PI * freq * i / rate) * 0.25f * env;
            }
            var clip = AudioClip.Create("tone", n, 1, rate, false);
            clip.SetData(samples, 0);
            return clip;
        }

        private void PlaySound(AudioClip clip)
        {
            audioSource.PlayOneShot(clip);
        }

        // ---------------- Уровни ----------------

        private void StartLevel(int level)
        {
            currentLevel = level;
            if (levelRoot != null) Destroy(levelRoot.gameObject);
            map = LevelMap.Parse(LevelCatalog.Map(level));
            var spots = LevelGenerator.PlaceBatteries(map, LevelCatalog.SeedFor(level), LevelCatalog.BatteriesFor(level));
            GameObject playerObj;
            LevelBuilder.Build(map, spots, transform, out playerObj, out batteries);
            levelRoot = GameObject.Find("LevelRoot").transform;
            player = playerObj;
            controller = player.GetComponent<PlayerController>();
            batteriesOnLevel = spots.Count;
            session = new GameSession(LevelCatalog.LivesFor(level), batteriesOnLevel);
            collected.Clear();
            playSeconds = 0f;
            state = State.Play;
            SnapCamera();
        }

        private void SnapCamera()
        {
            Vector3 p = player.transform.position;
            cam.transform.position = new Vector3(p.x, p.y, -10f);
        }

        private void LateUpdate()
        {
            if (state == State.Play && player != null)
            {
                Vector3 p = player.transform.position;
                cam.transform.position = Vector3.Lerp(
                    cam.transform.position,
                    new Vector3(p.x, p.y, -10f),
                    6f * Time.deltaTime);
            }
        }

        private void Update()
        {
            if (state != State.Play) return;
            playSeconds += Time.deltaTime;

            float axis = Input.GetAxisRaw("Horizontal");
            if (Mathf.Abs(moveAxis) > 0.01f) axis = moveAxis;
            controller.Move(axis);

            if (Input.GetButtonDown("Jump")) controller.Jump();
            if (Input.GetKeyDown(KeyCode.W) || Input.GetKeyDown(KeyCode.UpArrow)) controller.Jump();

            HandleTouch();
        }

        private void HandleTouch()
        {
            for (int i = 0; i < Input.touchCount; i++)
            {
                Touch t = Input.GetTouch(i);
                if (t.phase == TouchPhase.Began)
                {
                    if (t.position.x < Screen.width * 0.35f)
                    {
                        moveFinger = t.fingerId;
                        moveAxis = t.position.x < Screen.width * 0.18f ? -1f : 1f;
                    }
                    else
                    {
                        controller.Jump();
                    }
                }
                else if (t.fingerId == moveFinger && (t.phase == TouchPhase.Ended || t.phase == TouchPhase.Canceled))
                {
                    moveFinger = -1;
                    moveAxis = 0f;
                }
            }
        }

        // ---------------- События ----------------

        public void OnPlayerHit()
        {
            if (state != State.Play) return;
            session.Hit();
            PlaySound(hitClip);
            if (!session.IsAlive)
            {
                state = State.Lose;
            }
            else
            {
                player.transform.position = new Vector3(map.SpawnX + 0.5f, map.SpawnY + 0.5f, 0f);
                var body = player.GetComponent<Rigidbody2D>();
                body.linearVelocity = Vector2.zero;
            }
        }

        public void OnBatteryCollected(GameObject batteryObject)
        {
            if (state != State.Play) return;
            collected.Add(batteryObject.transform.position);
            Destroy(batteryObject);
            batteries.Remove(batteryObject);
            session.CollectBattery();
            PlaySound(batteryClip);
        }

        public void OnExitReached()
        {
            if (state != State.Play) return;
            int stars = GameSession.StarsFor(session.BatteriesCollected, batteriesOnLevel);
            progress.Complete(currentLevel, stars);
            PlayerPrefs.SetString("robolab_progress", progress.Serialize());
            PlayerPrefs.Save();
            PlaySound(winClip);
            state = State.Win;
        }

        // ---------------- UI ----------------

        private void OnGUI()
        {
            var style = new GUIStyle(GUI.skin.label) { fontSize = Screen.height / 28, normal = { textColor = Color.white } };
            var big = new GUIStyle(style) { fontSize = Screen.height / 16 };
            var button = new GUIStyle(GUI.skin.button) { fontSize = Screen.height / 30 };

            switch (state)
            {
                case State.Menu:
                    GUI.Label(new Rect(0, Screen.height * 0.18f, Screen.width, 100), "РОБО-ЛАБОРАТОРИЯ", Center(big));
                    GUI.Label(new Rect(0, Screen.height * 0.30f, Screen.width, 60), "платформер про роботов: двойной прыжок и батарейки", Center(style));
                    if (GUI.Button(new Rect(Screen.width * 0.25f, Screen.height * 0.45f, Screen.width * 0.5f, Screen.height * 0.09f), "Играть", button))
                        state = State.LevelSelect;
                    GUI.Label(new Rect(0, Screen.height * 0.6f, Screen.width, 60), "всего звёзд: " + progress.TotalStars(), Center(style));
                    break;

                case State.LevelSelect:
                    GUI.Label(new Rect(0, Screen.height * 0.05f, Screen.width, 80), "Уровни", Center(big));
                    for (int i = 1; i <= LevelCatalog.LEVEL_COUNT; i++)
                    {
                        int col = (i - 1) % 5;
                        int row = (i - 1) / 5;
                        var rect = new Rect(Screen.width * 0.06f + col * Screen.width * 0.18f,
                            Screen.height * 0.16f + row * Screen.height * 0.12f,
                            Screen.width * 0.15f, Screen.height * 0.09f);
                        bool unlocked = progress.IsUnlocked(i);
                        GUI.enabled = unlocked;
                        string label = i + (unlocked ? "\n" + new string('*', progress.StarsFor(i)) : "\nзакрыт");
                        if (GUI.Button(rect, label, button))
                            StartLevel(i);
                        GUI.enabled = true;
                    }
                    if (GUI.Button(new Rect(Screen.width * 0.3f, Screen.height * 0.86f, Screen.width * 0.4f, Screen.height * 0.08f), "Назад", button))
                        state = State.Menu;
                    break;

                case State.Play:
                    GUI.Label(new Rect(20, 10, Screen.width, 50), "Уровень " + currentLevel + "   батарейки: " + session.BatteriesCollected + "/" + batteriesOnLevel + "   жизни: " + session.Lives, style);
                    if (GUI.Button(new Rect(Screen.width - 160, 10, 140, 60), "Пауза", button))
                        state = State.Pause;
                    break;

                case State.Pause:
                    GUI.Label(new Rect(0, Screen.height * 0.3f, Screen.width, 80), "Пауза", Center(big));
                    if (GUI.Button(new Rect(Screen.width * 0.25f, Screen.height * 0.45f, Screen.width * 0.5f, Screen.height * 0.09f), "Продолжить", button))
                        state = State.Play;
                    if (GUI.Button(new Rect(Screen.width * 0.25f, Screen.height * 0.57f, Screen.width * 0.5f, Screen.height * 0.09f), "В меню", button))
                        state = State.LevelSelect;
                    break;

                case State.Win:
                    int stars = GameSession.StarsFor(session.BatteriesCollected, batteriesOnLevel);
                    GUI.Label(new Rect(0, Screen.height * 0.22f, Screen.width, 80), "Уровень пройден!", Center(big));
                    GUI.Label(new Rect(0, Screen.height * 0.34f, Screen.width, 60), new string('★', stars) + "   очки: " + GameSession.ScoreFor(session.BatteriesCollected, (int)playSeconds), Center(style));
                    if (GUI.Button(new Rect(Screen.width * 0.25f, Screen.height * 0.5f, Screen.width * 0.5f, Screen.height * 0.09f),
                        currentLevel < LevelCatalog.LEVEL_COUNT ? "Следующий уровень" : "К уровням", button))
                    {
                        if (currentLevel < LevelCatalog.LEVEL_COUNT) StartLevel(currentLevel + 1);
                        else state = State.LevelSelect;
                    }
                    break;

                case State.Lose:
                    GUI.Label(new Rect(0, Screen.height * 0.3f, Screen.width, 80), "Робот сломан!", Center(big));
                    if (GUI.Button(new Rect(Screen.width * 0.25f, Screen.height * 0.45f, Screen.width * 0.5f, Screen.height * 0.09f), "Ещё раз", button))
                        StartLevel(currentLevel);
                    if (GUI.Button(new Rect(Screen.width * 0.25f, Screen.height * 0.57f, Screen.width * 0.5f, Screen.height * 0.09f), "К уровням", button))
                        state = State.LevelSelect;
                    break;
            }
        }

        private static GUIStyle Center(GUIStyle style)
        {
            var s = new GUIStyle(style) { alignment = TextAnchor.MiddleCenter };
            return s;
        }
    }
}
