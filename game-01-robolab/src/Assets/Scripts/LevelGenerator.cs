using System.Collections.Generic;

namespace RoboLab
{
    /// <summary>
    /// Генерация батареек (ось C: рандомизация позиций по сиду уровня поверх авторской карты).
    /// </summary>
    public static class LevelGenerator
    {
        public static List<KeyValuePair<int, int>> PlaceBatteries(LevelMap map, int seed, int count)
        {
            var rng = new System.Random(seed);
            var free = new List<KeyValuePair<int, int>>();
            for (int y = 0; y < map.Height; y++)
                for (int x = 0; x < map.Width; x++)
                    if (map.IsFree(x, y))
                        free.Add(new KeyValuePair<int, int>(x, y));

            var result = new List<KeyValuePair<int, int>>();
            for (int i = 0; i < count && free.Count > 0; i++)
            {
                int idx = rng.Next(free.Count);
                result.Add(free[idx]);
                free.RemoveAt(idx);
            }
            return result;
        }
    }
}
