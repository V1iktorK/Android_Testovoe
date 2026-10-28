using System.Collections.Generic;

namespace RoboLab
{
    /// <summary>Игровая сессия уровня: жизни, счёт, звёзды.</summary>
    public sealed class GameSession
    {
        public int Lives { get; private set; }
        public int BatteriesCollected { get; private set; }
        public readonly int BatteriesTotal;

        public GameSession(int lives)
        {
            Lives = lives;
            BatteriesTotal = 0;
        }

        public GameSession(int lives, int batteriesTotal)
        {
            Lives = lives;
            BatteriesTotal = batteriesTotal;
        }

        public bool IsAlive
        {
            get { return Lives > 0; }
        }

        public void Hit()
        {
            if (Lives > 0) Lives--;
        }

        public void CollectBattery()
        {
            BatteriesCollected++;
        }

        /// <summary>Звёзды: 3 — все батарейки, 2 — от 60%, 1 — от 30%.</summary>
        public static int StarsFor(int collected, int total)
        {
            if (total <= 0) return 3;
            double ratio = (double)collected / total;
            if (ratio >= 1.0) return 3;
            if (ratio >= 0.6) return 2;
            return 1;
        }

        /// <summary>Очки: батарейки и скорость прохождения.</summary>
        public static int ScoreFor(int batteries, int seconds)
        {
            int timeBonus = System.Math.Max(0, 300 - seconds) * 10;
            return batteries * 150 + timeBonus;
        }

        public int CurrentScore(int seconds)
        {
            return ScoreFor(BatteriesCollected, seconds);
        }
    }

    /// <summary>Прогресс игрока: открытые уровни и звёзды.</summary>
    public sealed class ProgressState
    {
        private readonly Dictionary<int, int> stars = new Dictionary<int, int>();

        public void Complete(int level, int starCount)
        {
            int old;
            stars.TryGetValue(level, out old);
            if (starCount > old) stars[level] = starCount;
        }

        public int StarsFor(int level)
        {
            int s;
            stars.TryGetValue(level, out s);
            return s;
        }

        public bool IsUnlocked(int level)
        {
            return level == 1 || StarsFor(level - 1) > 0;
        }

        public int TotalStars()
        {
            int total = 0;
            foreach (var kv in stars) total += kv.Value;
            return total;
        }

        public string Serialize()
        {
            var parts = new List<string>();
            foreach (var kv in stars) parts.Add(kv.Key + ":" + kv.Value);
            return string.Join(";", parts.ToArray());
        }

        public static ProgressState Deserialize(string text)
        {
            var p = new ProgressState();
            if (string.IsNullOrEmpty(text)) return p;
            foreach (var entry in text.Split(';'))
            {
                var kv = entry.Split(':');
                if (kv.Length == 2)
                {
                    int level, starCount;
                    if (int.TryParse(kv[0], out level) && int.TryParse(kv[1], out starCount))
                        p.Complete(level, starCount);
                }
            }
            return p;
        }
    }
}
