using UnityEngine;

namespace RoboLab
{
    /// <summary>
    /// Строит уровень из карты: тайлы стен, опасности, батарейки, выход, патрули.
    /// </summary>
    public static class LevelBuilder
    {
        public const string TAG_HAZARD = "Player"; // триггеры обрабатываются через компоненты

        public static Transform Build(LevelMap map, System.Collections.Generic.List<System.Collections.Generic.KeyValuePair<int, int>> batterySpots,
            Transform parent, out GameObject player, out System.Collections.Generic.List<GameObject> batteries)
        {
            var root = new GameObject("LevelRoot").transform;
            root.SetParent(parent, false);

            Sprite wall = PixelSprites.WallTile();
            Sprite spike = PixelSprites.Spikes();
            Sprite laser = PixelSprites.Laser();
            Sprite door = PixelSprites.ExitDoor();
            Sprite batterySpr = PixelSprites.Battery();
            Sprite botSpr = PixelSprites.PatrolBot();

            for (int y = 0; y < map.Height; y++)
            {
                for (int x = 0; x < map.Width; x++)
                {
                    if (map.IsWall(x, y))
                    {
                        var tile = CreateSpriteObject("Wall", wall, x, y, root);
                        tile.AddComponent<BoxCollider2D>();
                    }
                }
            }

            foreach (var p in map.Spikes)
            {
                var obj = CreateSpriteObject("Spikes", spike, p.Key, p.Value, root);
                var box = obj.AddComponent<BoxCollider2D>();
                box.isTrigger = true;
                obj.AddComponent<Hazard>();
            }

            foreach (var p in map.Lasers)
            {
                var obj = CreateSpriteObject("Laser", laser, p.Key, p.Value, root);
                var box = obj.AddComponent<BoxCollider2D>();
                box.isTrigger = true;
                obj.AddComponent<Hazard>();
            }

            foreach (var p in map.Bots)
            {
                var obj = CreateSpriteObject("PatrolBot", botSpr, p.Key, p.Value, root);
                var box = obj.AddComponent<BoxCollider2D>();
                box.isTrigger = true;
                obj.AddComponent<Hazard>();
                obj.AddComponent<Patrol>().Init(p.Key - 2.5f, p.Key + 2.5f);
            }

            batteries = new System.Collections.Generic.List<GameObject>();
            foreach (var spot in batterySpots)
            {
                var obj = CreateSpriteObject("Battery", batterySpr, spot.Key, spot.Value, root);
                var box = obj.AddComponent<BoxCollider2D>();
                box.isTrigger = true;
                obj.AddComponent<BatteryPickup>();
                batteries.Add(obj);
            }

            foreach (var p in map.Exits)
            {
                var obj = CreateSpriteObject("Exit", door, p.Key, p.Value, root);
                var box = obj.AddComponent<BoxCollider2D>();
                box.isTrigger = true;
                obj.AddComponent<ExitTrigger>();
            }

            player = CreateSpriteObject("Robot", PixelSprites.Robot(), map.SpawnX, map.SpawnY, root);
            var body = player.AddComponent<Rigidbody2D>();
            body.bodyType = RigidbodyType2D.Dynamic;
            body.constraints = RigidbodyConstraints2D.FreezeRotation;
            var playerBox = player.AddComponent<BoxCollider2D>();
            playerBox.size = new Vector2(0.7f, 0.95f);
            player.AddComponent<PlayerController>();

            return root;
        }

        private static GameObject CreateSpriteObject(string name, Sprite sprite, int x, int y, Transform parent)
        {
            var obj = new GameObject(name);
            obj.transform.SetParent(parent, false);
            obj.transform.position = new Vector3(x + 0.5f, y + 0.5f, 0f);
            var renderer = obj.AddComponent<SpriteRenderer>();
            renderer.sprite = sprite;
            renderer.sortingOrder = name == "Robot" ? 10 : 5;
            return obj;
        }
    }

    /// <summary>Опасность: касание отнимает жизнь.</summary>
    public class Hazard : MonoBehaviour
    {
        private void OnTriggerEnter2D(Collider2D other)
        {
            if (other.GetComponent<PlayerController>() != null)
                GameBootstrap.Instance.OnPlayerHit();
        }
    }

    /// <summary>Сбор батарейки.</summary>
    public class BatteryPickup : MonoBehaviour
    {
        private void OnTriggerEnter2D(Collider2D other)
        {
            if (other.GetComponent<PlayerController>() != null)
                GameBootstrap.Instance.OnBatteryCollected(gameObject);
        }
    }

    /// <summary>Выход из уровня.</summary>
    public class ExitTrigger : MonoBehaviour
    {
        private void OnTriggerEnter2D(Collider2D other)
        {
            if (other.GetComponent<PlayerController>() != null)
                GameBootstrap.Instance.OnExitReached();
        }
    }

    /// <summary>Патрульный бот: движение влево-вправо.</summary>
    public class Patrol : MonoBehaviour
    {
        private float minX, maxX, speed = 1.8f;

        public void Init(float minX, float maxX)
        {
            this.minX = minX;
            this.maxX = maxX;
        }

        private void Update()
        {
            float x = transform.position.x + speed * Time.deltaTime;
            if (x > maxX || x < minX) speed = -speed;
            transform.position = new Vector3(Mathf.Clamp(x, minX, maxX), transform.position.y, 0f);
        }
    }
}
