using UnityEngine;

namespace RoboLab
{
    /// <summary>
    /// Процедурная пиксель-графика: текстуры робота, тайлов и батареек генерируются кодом.
    /// </summary>
    public static class PixelSprites
    {
        public static Sprite FromPixels(int w, int h, System.Func<int, int, Color> painter, int scale = 1)
        {
            var tex = new Texture2D(w, h, TextureFormat.RGBA32, false)
            {
                filterMode = FilterMode.Point,
                wrapMode = TextureWrapMode.Clamp
            };
            for (int y = 0; y < h; y++)
                for (int x = 0; x < w; x++)
                    tex.SetPixel(x, y, painter(x, y));
            tex.Apply();
            return Sprite.Create(tex, new Rect(0, 0, w, h), new Vector2(0.5f, 0.5f), 16f * scale);
        }

        /// <summary>Пиксельный робот 12x16: корпус, глаза-антенна, ножки.</summary>
        public static Sprite Robot()
        {
            return FromPixels(12, 16, (x, y) =>
            {
                if (y < 2 && (x == 2 || x == 3 || x == 8 || x == 9)) return new Color(0.2f, 0.75f, 0.95f); // ноги
                if (y < 6) return new Color(0.55f, 0.6f, 0.7f);            // корпус
                if (y < 13 && x > 1 && x < 10) return new Color(0.75f, 0.82f, 0.92f); // голова
                if (y >= 13) return (x == 5 || x == 6) ? new Color(0.95f, 0.85f, 0.3f) : new Color(0.75f, 0.82f, 0.92f);
                if (y >= 6 && (x == 0 || x == 11)) return new Color(0.4f, 0.45f, 0.55f); // руки
                if (y >= 8 && y <= 10 && (x == 3 || x == 4 || x == 7 || x == 8)) return new Color(0.15f, 0.85f, 0.9f); // глаза
                return new Color(0.75f, 0.82f, 0.92f);
            });
        }

        /// <summary>Тайл стены — металлическая плита с заклёпками.</summary>
        public static Sprite WallTile()
        {
            return FromPixels(16, 16, (x, y) =>
            {
                bool rivet = (x == 2 || x == 13) && (y == 2 || y == 13);
                if (rivet) return new Color(0.9f, 0.9f, 0.95f);
                if (x == 0 || y == 0) return new Color(0.25f, 0.3f, 0.42f);
                if (x == 15 || y == 15) return new Color(0.12f, 0.15f, 0.25f);
                return new Color(0.32f, 0.38f, 0.52f);
            });
        }

        /// <summary>Батарейка 8x12: жёлтый корпус с индикатором.</summary>
        public static Sprite Battery()
        {
            return FromPixels(8, 12, (x, y) =>
            {
                if (y == 11 && x > 2 && x < 5) return new Color(0.8f, 0.8f, 0.85f);
                if (y > 1 && y < 10 && x > 0 && x < 7) return new Color(0.20f, 0.95f, 0.45f);
                if (y > 3 && y < 7 && x > 1 && x < 6) return new Color(0.95f, 1f, 0.97f);
                return new Color(0.10f, 0.6f, 0.3f);
            });
        }

        /// <summary>Спрайт выхода — светящаяся дверь.</summary>
        public static Sprite ExitDoor()
        {
            return FromPixels(12, 20, (x, y) =>
            {
                bool border = x == 0 || x == 11 || y == 0 || y == 19;
                if (border) return new Color(0.25f, 0.7f, 0.95f);
                if (x > 3 && x < 8 && y > 6 && y < 14) return new Color(0.55f, 1f, 0.85f);
                return new Color(0.12f, 0.2f, 0.38f);
            });
        }

        /// <summary>Спрайт шипов.</summary>
        public static Sprite Spikes()
        {
            return FromPixels(16, 8, (x, y) =>
            {
                int peak = Mathf.Abs((x % 4) - 2);
                return y <= peak ? new Color(0.85f, 0.3f, 0.35f) : new Color(0.1f, 0.1f, 0.15f, 0f);
            });
        }

        /// <summary>Спрайт лазерного излучателя.</summary>
        public static Sprite Laser()
        {
            return FromPixels(10, 16, (x, y) =>
            {
                if (x > 3 && x < 7) return new Color(1f, 0.35f, 0.4f, 0.85f);
                return new Color(0.2f, 0.22f, 0.3f);
            });
        }

        /// <summary>Спрайт патрульного бота.</summary>
        public static Sprite PatrolBot()
        {
            return FromPixels(12, 12, (x, y) =>
            {
                if (y > 7 && (x == 2 || x == 3 || x == 8 || x == 9)) return new Color(0.3f, 0.3f, 0.38f);
                if ((x == 4 || x == 7) && y > 3 && y < 7) return new Color(1f, 0.3f, 0.35f);
                return new Color(0.55f, 0.28f, 0.32f);
            });
        }
    }
}
