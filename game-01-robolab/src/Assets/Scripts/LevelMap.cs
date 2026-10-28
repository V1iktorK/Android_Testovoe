using System;
using System.Collections.Generic;

namespace RoboLab
{
    /// <summary>
    /// Уровень в текстовом формате: # стена, . пусто, S старт, E выход,
    /// ^ шипы, L лазер, P патрульный бот, b батарейка.
    /// </summary>
    public sealed class LevelMap
    {
        public int Width { get; private set; }
        public int Height { get; private set; }
        public int SpawnX { get; private set; }
        public int SpawnY { get; private set; }
        public List<KeyValuePair<int, int>> Exits { get; private set; }
        public List<KeyValuePair<int, int>> Spikes { get; private set; }
        public List<KeyValuePair<int, int>> Lasers { get; private set; }
        public List<KeyValuePair<int, int>> Bots { get; private set; }
        public List<KeyValuePair<int, int>> FixedBatteries { get; private set; }

        private string[] rows;

        public KeyValuePair<int, int> Spawn
        {
            get { return new KeyValuePair<int, int>(SpawnX, SpawnY); }
        }

        public static LevelMap Parse(string[] rows)
        {
            if (rows == null || rows.Length < 3)
                throw new ArgumentException("Слишком мало строк в карте");
            int width = rows[0].Length;
            foreach (var row in rows)
                if (row.Length != width)
                    throw new ArgumentException("Карта не прямоугольная");

            var map = new LevelMap();
            map.rows = (string[])rows.Clone();
            map.Width = width;
            map.Height = rows.Length;
            map.Exits = new List<KeyValuePair<int, int>>();
            map.Spikes = new List<KeyValuePair<int, int>>();
            map.Lasers = new List<KeyValuePair<int, int>>();
            map.Bots = new List<KeyValuePair<int, int>>();
            map.FixedBatteries = new List<KeyValuePair<int, int>>();
            int spawnCount = 0;

            for (int y = 0; y < map.Height; y++)
            {
                for (int x = 0; x < map.Width; x++)
                {
                    switch (rows[y][x])
                    {
                        case '#':
                        case '.':
                            break;
                        case 'S':
                            spawnCount++;
                            map.SpawnX = x;
                            map.SpawnY = y;
                            break;
                        case 'E':
                            map.Exits.Add(new KeyValuePair<int, int>(x, y));
                            break;
                        case '^':
                            map.Spikes.Add(new KeyValuePair<int, int>(x, y));
                            break;
                        case 'L':
                            map.Lasers.Add(new KeyValuePair<int, int>(x, y));
                            break;
                        case 'P':
                            map.Bots.Add(new KeyValuePair<int, int>(x, y));
                            break;
                        case 'b':
                            map.FixedBatteries.Add(new KeyValuePair<int, int>(x, y));
                            break;
                        default:
                            throw new ArgumentException("Неизвестный символ: " + rows[y][x]);
                    }
                }
            }
            if (spawnCount != 1)
                throw new ArgumentException("На карте должна быть ровно одна точка старта");
            if (map.Exits.Count == 0)
                throw new ArgumentException("На карте должен быть хотя бы один выход");
            return map;
        }

        public bool IsWall(int x, int y)
        {
            if (x < 0 || y < 0 || x >= Width || y >= Height) return true;
            return rows[y][x] == '#';
        }

        public bool IsHazard(int x, int y)
        {
            if (x < 0 || y < 0 || x >= Width || y >= Height) return false;
            char c = rows[y][x];
            return c == '^' || c == 'L';
        }

        public bool IsExit(int x, int y)
        {
            foreach (var e in Exits)
                if (e.Key == x && e.Value == y) return true;
            return false;
        }

        /// <summary>Пустая клетка, пригодная для размещения батарейки.</summary>
        public bool IsFree(int x, int y)
        {
            if (x < 0 || y < 0 || x >= Width || y >= Height) return false;
            char c = rows[y][x];
            return c == '.' && !(x == SpawnX && y == SpawnY);
        }
    }
}
