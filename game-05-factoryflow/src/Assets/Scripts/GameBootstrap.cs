using UnityEngine;

namespace FactoryFlow
{
    /// <summary>
    /// «Фабрика-Флоу» — симулятор распределения ресурсов в промышленном цехе (VR-тренажёр оператора).
    /// Low-poly 3D: конвейер и станции строятся из примитивов Unity, материалы — плоские цвета.
    /// </summary>
    public class GameBootstrap : MonoBehaviour
    {
        public static GameBootstrap Instance { get; private set; }

        private enum State { Menu, Play, Paused, Win, Lose }

        private State state = State.Menu;
        private ProductionLine line;
        private Scenario scenario;
        private FactorySim sim;
        private float shiftMinutes;      // сколько «минут смены» прошло
        private float energy = 25f;      // бюджет энергии
        private int currentLevel = 1;
        private float[] stationVisualSpeed;
        private Material[] stationMaterials;

        private static readonly Color FloorColor = new Color(0.22f, 0.24f, 0.28f);
        private static readonly Color BeltColor = new Color(0.16f, 0.17f, 0.2f);
        private static readonly Color StationColor = new Color(0.29f, 0.45f, 0.62f);
        private static readonly Color HotColor = new Color(0.36f, 0.85f, 0.55f);

        private void Awake()
        {
            Instance = this;
            BuildScene();
        }

        private void BuildScene()
        {
            // Пол цеха
            var floor = GameObject.CreatePrimitive(PrimitiveType.Plane);
            floor.name = "Floor";
            floor.transform.localScale = new Vector3(3f, 1f, 2f);
            floor.GetComponent<Renderer>().material.color = FloorColor;

            // Конвейерная лента
            var belt = GameObject.CreatePrimitive(PrimitiveType.Cube);
            belt.name = "Conveyor";
            belt.transform.position = new Vector3(0f, 0.25f, 0f);
            belt.transform.localScale = new Vector3(9f, 0.5f, 1.2f);
            belt.GetComponent<Renderer>().material.color = BeltColor;

            // Станции (4 шт.) над лентой
            stationMaterials = new Material[4];
            stationVisualSpeed = new float[4];
            for (int i = 0; i < 4; i++)
            {
                var station = GameObject.CreatePrimitive(PrimitiveType.Cube);
                station.name = "Station" + i;
                station.transform.position = new Vector3(-3.6f + i * 2.4f, 1.1f, 0f);
                station.transform.localScale = new Vector3(1.4f, 1.6f, 1.4f);
                var mat = station.GetComponent<Renderer>().material;
                mat.color = StationColor;
                stationMaterials[i] = mat;
                var lamp = GameObject.CreatePrimitive(PrimitiveType.Sphere);
                lamp.name = "Lamp" + i;
                lamp.transform.position = station.transform.position + new Vector3(0f, 1.1f, 0f);
                lamp.transform.localScale = Vector3.one * 0.35f;
                lamp.GetComponent<Renderer>().material.color = new Color(0.4f, 0.95f, 1f, 0.85f);
            }

            // Свет и камера (вид сверху-сбоку — как в VR-тренажёре)
            var lightGo = new GameObject("Sun");
            var light = lightGo.AddComponent<Light>();
            light.type = LightType.Directional;
            light.intensity = 1.15f;
            lightGo.transform.rotation = Quaternion.Euler(55f, -30f, 0f);

            var cam = Camera.main;
            if (cam == null)
            {
                var camGo = new GameObject("Main Camera") { tag = "MainCamera" };
                cam = camGo.AddComponent<Camera>();
            }
            cam.transform.position = new Vector3(0f, 7.2f, -7.6f);
            cam.transform.rotation = Quaternion.Euler(42f, 0f, 0f);
            cam.clearFlags = CameraClearFlags.SolidColor;
            cam.backgroundColor = new Color(0.13f, 0.15f, 0.19f);
        }

        private void StartShift(int level)
        {
            currentLevel = level;
            line = new ProductionLine(2);
            scenario = ScenarioGenerator.Generate(31337 + level * 101, 1 + (level - 1) / 4);
            sim = new FactorySim(line, scenario);
            shiftMinutes = 0f;
            energy = 25f + level * 5f;
            for (int i = 0; i < 4; i++)
            {
                stationVisualSpeed[i] = 1f;
                stationMaterials[i].color = StationColor;
            }
            state = State.Play;
        }

        private void Update()
        {
            if (state != State.Play) return;

            // Симуляция: секунда реального времени = 0.25 минуты смены
            float minutes = Time.deltaTime * 0.25f;
            shiftMinutes += minutes;
            sim.RunMinutes(minutes);
            energy += Time.deltaTime * 1.1f; // восстановление энергии

            if (shiftMinutes >= 6f)
            {
                state = sim.CompletedUnits > 0 ? State.Win : State.Lose;
            }

            HandleTouch();
        }

        private void HandleTouch()
        {
            if (Input.GetMouseButtonDown(0))
            {
                Ray ray = Camera.main.ScreenPointToRay(Input.mousePosition);
                RaycastHit hit;
                if (Physics.Raycast(ray, out hit))
                {
                    if (hit.collider.name.StartsWith("Station"))
                    {
                        int idx = int.Parse(hit.collider.name.Substring(7));
                        UpgradeStation(idx);
                    }
                }
            }
        }

        /// <summary>Инвестиция энергии в станцию: +скорость (кор-механика распределения ресурсов).</summary>
        public void UpgradeStation(int index)
        {
            if (state != State.Play) return;
            float cost = 8f + stationVisualSpeed[index] * 4f;
            if (energy < cost) return;
            energy -= cost;
            stationVisualSpeed[index] += 0.25f;
            line.SetStationSpeed(index, stationVisualSpeed[index]);
            stationMaterials[index].color = Color.Lerp(StationColor, HotColor,
                Mathf.Clamp01((stationVisualSpeed[index] - 1f) / 2.5f));
        }

        private void OnGUI()
        {
            var style = new GUIStyle(GUI.skin.label) { fontSize = Screen.height / 34, normal = { textColor = Color.white } };
            var big = new GUIStyle(style) { fontSize = Screen.height / 18, alignment = TextAnchor.MiddleCenter };
            var button = new GUIStyle(GUI.skin.button) { fontSize = Screen.height / 32 };

            switch (state)
            {
                case State.Menu:
                    GUI.Label(new Rect(0, Screen.height * 0.16f, Screen.width, 90), "ФАБРИКА-ФЛОУ", big);
                    GUI.Label(new Rect(0, Screen.height * 0.29f, Screen.width, 60),
                        "VR-тренажёр оператора: распределяй энергию между станциями", Center(style));
                    GUI.Label(new Rect(0, Screen.height * 0.35f, Screen.width, 60),
                        "тап по станции — инвестировать энергию и ускорить выпуск", Center(style));
                    if (GUI.Button(new Rect(Screen.width * 0.3f, Screen.height * 0.5f, Screen.width * 0.4f, Screen.height * 0.09f),
                        "Начать смену", button))
                        StartShift(1);
                    break;

                case State.Play:
                    GUI.Label(new Rect(16, 10, Screen.width, 40),
                        string.Format("Смена {0:F1} мин   энергия: {1:F0}   выпущено: {2}   счёт: {3}",
                            shiftMinutes, energy, sim.CompletedUnits, sim.Score), style);
                    float y = Screen.height * 0.12f;
                    foreach (var order in scenario.Orders)
                    {
                        GUI.Label(new Rect(16, y, Screen.width * 0.7f, 34),
                            string.Format("{0}  {1:F0} ед/мин  срок {2:F0} мин  приоритет {3}",
                                order.Product, order.UnitsPerMinute, order.DeadlineMinutes, order.Priority), style);
                        float progress = sim.ProgressOf(order);
                        GUI.Box(new Rect(Screen.width * 0.72f, y, Screen.width * 0.25f, 22f), "");
                        GUI.Box(new Rect(Screen.width * 0.72f, y, Screen.width * 0.25f * Mathf.Clamp01(progress), 22f), "");
                        y += 40f;
                    }
                    GUI.Label(new Rect(16, Screen.height - 60f, Screen.width, 40),
                        "тап по станции цеха — ускорить её (стоимость энергии растёт)", style);
                    break;

                case State.Win:
                    GUI.Label(new Rect(0, Screen.height * 0.3f, Screen.width, 80), "Смена выполнена!", big);
                    GUI.Label(new Rect(0, Screen.height * 0.42f, Screen.width, 50),
                        string.Format("выпущено {0} ед., счёт {1}", sim.CompletedUnits, sim.Score), Center(style));
                    if (GUI.Button(new Rect(Screen.width * 0.25f, Screen.height * 0.55f, Screen.width * 0.5f, Screen.height * 0.09f),
                        "Следующая смена", button))
                        StartShift(currentLevel + 1);
                    break;

                case State.Lose:
                    GUI.Label(new Rect(0, Screen.height * 0.3f, Screen.width, 80), "Смена сорвана", big);
                    if (GUI.Button(new Rect(Screen.width * 0.25f, Screen.height * 0.5f, Screen.width * 0.5f, Screen.height * 0.09f),
                        "Повторить", button))
                        StartShift(currentLevel);
                    break;

                case State.Paused:
                    break;
            }
        }

        private static GUIStyle Center(GUIStyle style)
        {
            return new GUIStyle(style) { alignment = TextAnchor.MiddleCenter };
        }
    }
}
