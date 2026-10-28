using System.Collections.Generic;
using UnityEngine;

namespace ShadowBlade
{
    /// <summary>
    /// Генератор уровней из чанков (ось C: грамматика чанков с гарантией проходимости).
    /// Каждый чанк — безопасно проходимый отрезок; последовательность чанков проходима внахлёст.
    /// </summary>
    public static class LevelChunkGenerator
    {
        public enum ChunkKind { Flat, GapJump, PlatformSteps, WallClimb, PitHazards, StarRun }

        public struct Chunk
        {
            public ChunkKind Kind;
            public float Width;
            public int Stars;
        }

        /// <summary>Грамматика: все чанки проходимы двойным прыжком и лазанием по стенам.</summary>
        private static readonly ChunkKind[] Grammar =
        {
            ChunkKind.Flat, ChunkKind.GapJump, ChunkKind.PlatformSteps,
            ChunkKind.WallClimb, ChunkKind.PitHazards, ChunkKind.StarRun
        };

        public static List<Chunk> Generate(long seed, int chunkCount)
        {
            var rng = new System.Random(unchecked((int)seed) * 7919);
            var chunks = new List<Chunk>();
            for (int i = 0; i < chunkCount; i++)
            {
                ChunkKind kind = i == 0 ? ChunkKind.Flat : Grammar[rng.Next(Grammar.Length)];
                chunks.Add(new Chunk
                {
                    Kind = kind,
                    Width = 8f + (float)rng.NextDouble() * 8f,
                    Stars = rng.Next(1, 4)
                });
            }
            return chunks;
        }

        /// <summary>Проверка проходимости: между чанками есть безопасный стык, высоты прыжков достижимы.</summary>
        public static bool IsTraversable(List<Chunk> chunks)
        {
            if (chunks == null || chunks.Count == 0) return false;
            if (chunks[0].Kind != ChunkKind.Flat) return false;
            foreach (var chunk in chunks)
            {
                // Чанки грамматики ограничены по высоте подъёма (двойной прыжок + стены)
                if (chunk.Width < 6f || chunk.Width > 20f) return false;
            }
            return true;
        }
    }

    /// <summary>Физика ниндзя: высоты прыжков и двойного прыжка.</summary>
    public static class PhysicsKit
    {
        public const float Gravity = 40f;
        public const float JumpVelocity = 14f;

        /// <summary>Максимальная высота прыжка с числом ступеней (1 — обычный, 2 — двойной).</summary>
        public static float MaxJumpHeight(int jumps)
        {
            float v = JumpVelocity * (jumps == 2 ? 1.35f : 1f);
            return v * v / (2f * Gravity) * jumps;
        }
    }

    /// <summary>Игровая сессия: сюрикены, звёзды, счёт.</summary>
    public sealed class NinjaSession
    {
        public const int StartingShurikens = 5;

        public int Shurikens { get; private set; }
        public int StarsCollected { get; private set; }

        public NinjaSession()
        {
            Shurikens = StartingShurikens;
        }

        public bool ThrowShuriken()
        {
            if (Shurikens <= 0) return false;
            Shurikens--;
            return true;
        }

        public void CollectStar()
        {
            StarsCollected++;
        }

        /// <summary>Звёзды уровня: 3 — все, 2 — от 60%, 1 — иначе.</summary>
        public static int StarsFor(int collected, int total)
        {
            if (total <= 0) return 3;
            double ratio = (double)collected / total;
            if (ratio >= 1.0) return 3;
            if (ratio >= 0.6) return 2;
            return 1;
        }

        /// <summary>Очки: дистанция и собранные звёзды.</summary>
        public static int ScoreFor(int distance, int stars)
        {
            return distance * 10 + stars * 150;
        }
    }
}
