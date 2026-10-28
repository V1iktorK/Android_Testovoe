using System.Collections.Generic;

namespace FactoryFlow
{
    /// <summary>
    /// Производственная линия: станции, загрузка энергии, узкое место (bottleneck).
    /// Игрок распределяет ресурсы между станциями — кор-механика симулятора.
    /// </summary>
    public sealed class ProductionLine
    {
        public const float StationOutput = 1f;
        public const float StationEnergyCost = 10f;

        private readonly List<float> stationSpeeds = new List<float>();

        public ProductionLine(int stations)
        {
            for (int i = 0; i < stations; i++)
                stationSpeeds.Add(1f);
        }

        public int StationCount
        {
            get { return stationSpeeds.Count; }
        }

        /// <summary>Суммарная мощность линии (без учёта узких мест).</summary>
        public float OutputPerMinute
        {
            get
            {
                float sum = 0f;
                foreach (float speed in stationSpeeds) sum += speed;
                return sum * StationOutput;
            }
        }

        /// <summary>Фактическая пропускная способность ограничена самым медленным звеном.</summary>
        public float ThroughputPerMinute
        {
            get
            {
                float min = float.MaxValue;
                foreach (float speed in stationSpeeds)
                    if (speed < min) min = speed;
                return min * StationOutput;
            }
        }

        public void AddStation()
        {
            stationSpeeds.Add(1f);
        }

        public bool CanAddStation(float energyAvailable)
        {
            return energyAvailable >= StationEnergyCost;
        }

        public void SetStationSpeed(int index, float speed)
        {
            stationSpeeds[index] = speed;
        }

        public float StationSpeed(int index)
        {
            return stationSpeeds[index];
        }
    }

    /// <summary>Оценка выполнения заказа: объём и соблюдение сроков.</summary>
    public static class Fulfillment
    {
        public static int Score(int orderUnits, int delivered, float deadline, float elapsed)
        {
            if (orderUnits <= 0) return 0;
            double volume = (double)delivered / orderUnits;
            double timeFactor = elapsed <= deadline ? 1.25 : System.Math.Max(0.25, 1.0 - (elapsed - deadline) / deadline);
            return (int)System.Math.Round(1000.0 * volume * timeFactor);
        }
    }

    /// <summary>Симуляция смены: распределение выпуска по заказам.</summary>
    public sealed class FactorySim
    {
        private readonly ProductionLine line;
        private readonly Scenario scenario;
        private readonly Dictionary<Order, float> delivered = new Dictionary<Order, float>();
        private float elapsed;

        public int CompletedUnits { get; private set; }
        public int Score { get; private set; }

        public FactorySim(ProductionLine line, Scenario scenario)
        {
            this.line = line;
            this.scenario = scenario;
            foreach (var order in scenario.Orders)
                delivered[order] = 0f;
        }

        public void RunMinutes(float minutes)
        {
            float produced = line.ThroughputPerMinute * minutes;
            elapsed += minutes;
            // Раздаём выпуск заказам по приоритету
            var sorted = new List<Order>(scenario.Orders);
            sorted.Sort((a, b) => b.Priority.CompareTo(a.Priority));
            foreach (var order in sorted)
            {
                float need = order.TotalUnits - delivered[order];
                float give = System.Math.Min(need, produced);
                delivered[order] += give;
                produced -= give;
                if (produced <= 0f) break;
            }
            Recount();
        }

        private void Recount()
        {
            int total = 0;
            int score = 0;
            foreach (var order in scenario.Orders)
            {
                total += (int)delivered[order];
                score += Fulfillment.Score((int)order.TotalUnits, (int)delivered[order],
                    order.DeadlineMinutes, elapsed);
            }
            CompletedUnits = total;
            Score = score;
        }

        public float ProgressOf(Order order)
        {
            return delivered[order] / order.TotalUnits;
        }
    }
}
