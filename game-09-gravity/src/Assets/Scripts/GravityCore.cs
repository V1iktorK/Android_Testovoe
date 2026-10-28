using System.Collections.Generic;
using UnityEngine;

namespace GravityDrift
{
    /// <summary>Гравитационный колодец: позиция и масса.</summary>
    public struct Well
    {
        public float X;
        public float Y;
        public float Mass;

        public Well(float x, float y, float mass)
        {
            X = x;
            Y = y;
            Mass = mass;
        }
    }

    /// <summary>Уровень: колодцы, старт, цель, запас топлива.</summary>
    public sealed class Level
    {
        public List<Well> Wells = new List<Well>();
        public float StartX, StartY;
        public float GoalX, GoalY;
        public float FuelSeconds;
    }

    /// <summary>
    /// Генератор уровней (ось C: параметрическая расстановка гравитационных колодцев).
    /// Колодцы расставляются по сиду, старт и цель — на краях поля, цель вне зоны колодцев.
    /// </summary>
    public static class LevelGenerator
    {
        public const float BOUNDS = 8f;
        public const float WELL_SAFE_RADIUS = 2.2f;

        public static Level Generate(int seed, int levelNumber)
        {
            var rng = new System.Random(seed * 7919 + levelNumber * 104729);
            var level = new Level();
            int wellCount = 1 + levelNumber / 2;
            for (int i = 0; i < wellCount; i++)
            {
                float x = (float)(rng.NextDouble() * 2 - 1) * (BOUNDS - 1f);
                float y = (float)(rng.NextDouble() * 2 - 1) * (BOUNDS - 1f);
                float mass = 40f + (float)rng.NextDouble() * 120f + levelNumber * 6f;
                level.Wells.Add(new Well(x, y, mass));
            }
            // Старт слева, цель справа — проверяем безопасность цели
            level.StartX = -BOUNDS + 1f;
            level.StartY = (float)(rng.NextDouble() * 2 - 1) * (BOUNDS - 2f);
            level.GoalX = BOUNDS - 1f;
            level.GoalY = (float)(rng.NextDouble() * 2 - 1) * (BOUNDS - 2f);
            for (int i = 0; i < level.Wells.Count; i++)
            {
                var w = level.Wells[i];
                float dx = level.GoalX - w.X;
                float dy = level.GoalY - w.Y;
                if (Mathf.Sqrt(dx * dx + dy * dy) < LevelGenerator.WELL_SAFE_RADIUS)
                {
                    // выталкиваем колодец из зоны цели
                    w.X -= dx * 1.5f;
                    w.Y -= dy * 1.5f;
                    level.Wells[i] = w;
                }
            }
            level.FuelSeconds = 6f + levelNumber * 0.6f;
            return level;
        }
    }

    /// <summary>Физика гравитации: обратные квадраты, векторы сил, скоринг.</summary>
    public static class PhysicsMath
    {
        public const float G = 12f;

        /// <summary>Модуль силы на расстоянии r от массы m (обратный квадрат). */
        public static float ForceAt(float r, float mass)
        {
            if (r < 0.2f) r = 0.2f;
            return G * mass / (r * r);
        }

        /// <summary>Вектор силы, действующей на корабль в точке (sx, sy) от колодца (wx, wy, mass). */
        public static Vector2 ForceVector(float sx, float sy, float wx, float wy, float mass)
        {
            float dx = wx - sx;
            float dy = wy - sy;
            float r = Mathf.Max(Mathf.Sqrt(dx * dx + dy * dy), 0.2f);
            float f = ForceAt(r, mass);
            return new Vector2(dx / r * f, dy / r * f);
        }

        /// <summary>Очки: собранные звёзды и остаток топлива. */
        public static int ScoreFor(int stars, float fuelLeft, float fuelTotal)
        {
            float fuelBonus = fuelTotal > 0 ? fuelLeft / fuelTotal : 0f;
            return stars * 500 + (int)(fuelBonus * 1000f);
        }
    }
}
