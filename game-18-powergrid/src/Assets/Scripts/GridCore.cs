using System.Collections.Generic;
using UnityEngine;

namespace EnergyShield
{
    /// <summary>Волна атаки на электростанцию.</summary>
    public struct Wave
    {
        public int EnemyCount;
        public float EnemyHp;
        public float EnemySpeed;
        public bool IsBoss;

        public float TotalHp
        {
            get { return EnemyCount * EnemyHp; }
        }
    }

    /// <summary>
    /// Планировщик волн (ось C: планировщик волн + балансировка бюджета энергии).
    /// Суммарная прочность волны монотонно растёт: босс — один крепкий враг, обычная волна — группа.
    /// </summary>
    public static class WavePlanner
    {
        public static List<Wave> Generate(int seed, int waveCount)
        {
            var rng = new System.Random(seed * 7919 + waveCount * 104729);
            var waves = new List<Wave>();
            for (int i = 0; i < waveCount; i++)
            {
                bool boss = (i + 1) % 5 == 0;
                double totalHp = 200.0 * System.Math.Pow(1.3, i);
                int count = boss ? 1 + i / 5 : 4 + i + rng.Next(0, 3);
                waves.Add(new Wave
                {
                    EnemyCount = count,
                    EnemyHp = (float)(totalHp / count),
                    EnemySpeed = (boss ? 0.6f : 1.2f) + (float)rng.NextDouble() * 0.4f,
                    IsBoss = boss
                });
            }
            return waves;
        }
    }

    /// <summary>Виды башен обороны.</summary>
    public static class TowerCatalog
    {
        public enum Kind { Pylon, Turbine, Capacitor, Reactor }

        public struct Tower
        {
            public int Cost;
            public float Damage;
            public float Range;
            public float FireRate;
            public int EnergyUse;
        }

        public static Tower For(Kind kind)
        {
            switch (kind)
            {
                case Kind.Pylon:
                    return new Tower { Cost = 50, Damage = 8f, Range = 3f, FireRate = 1.2f, EnergyUse = 10 };
                case Kind.Turbine:
                    return new Tower { Cost = 90, Damage = 16f, Range = 2.5f, FireRate = 0.9f, EnergyUse = 20 };
                case Kind.Capacitor:
                    return new Tower { Cost = 130, Damage = 30f, Range = 4f, FireRate = 0.6f, EnergyUse = 35 };
                default:
                    return new Tower { Cost = 220, Damage = 60f, Range = 3.5f, FireRate = 0.5f, EnergyUse = 60 };
            }
        }
    }

    /// <summary>Сетка обороны: размещение башен и энергобюджет станции.</summary>
    public sealed class DefenseGrid
    {
        private readonly Dictionary<long, int> towerLevels = new Dictionary<long, int>();
        private readonly Dictionary<long, TowerCatalog.Kind> towers = new Dictionary<long, TowerCatalog.Kind>();
        private int energyTotal;

        public DefenseGrid(int energy)
        {
            energyTotal = energy;
        }

        public int FreeEnergy
        {
            get
            {
                int used = 0;
                foreach (var kv in towers) used += TowerCatalog.For(kv.Value).EnergyUse;
                return energyTotal - used;
            }
        }

        private static long Key(int x, int y)
        {
            return ((long)x << 32) | (uint)y;
        }

        public bool CanBuild(TowerCatalog.Kind kind, int energyAvailable)
        {
            var t = TowerCatalog.For(kind);
            return energyAvailable >= t.EnergyUse;
        }

        public bool Build(TowerCatalog.Kind kind, int x, int y)
        {
            long key = Key(x, y);
            if (towers.ContainsKey(key)) return false;
            if (!CanBuild(kind, FreeEnergy)) return false;
            towers[key] = kind;
            towerLevels[key] = 1;
            return true;
        }

        public void Upgrade(int x, int y)
        {
            long key = Key(x, y);
            if (towerLevels.ContainsKey(key)) towerLevels[key]++;
        }

        /// <summary>Урон башни с учётом уровня (×1.5 за уровень).</summary>
        public float TowerDamage(int x, int y)
        {
            long key = Key(x, y);
            if (!towers.ContainsKey(key)) return 0f;
            var t = TowerCatalog.For(towers[key]);
            return t.Damage * Mathf.Pow(1.5f, towerLevels[key] - 1);
        }

        /// <summary>Очки за убийство: растут с волной, боссы дороже.</summary>
        public static int ScoreForKill(int wave, bool boss)
        {
            return wave * 25 + (boss ? 500 : 10);
        }
    }
}
