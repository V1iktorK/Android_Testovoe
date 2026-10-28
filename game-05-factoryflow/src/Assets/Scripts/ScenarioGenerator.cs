using System.Collections.Generic;

namespace FactoryFlow
{
    /// <summary>Заказ цеха: продукт, темп поставки, срок, приоритет.</summary>
    public sealed class Order
    {
        public string Product;
        public float UnitsPerMinute;
        public float DeadlineMinutes;
        public int Priority;

        public float TotalUnits
        {
            get { return UnitsPerMinute * DeadlineMinutes; }
        }
    }

    /// <summary>
    /// Сценарий смены: список заказов, сгенерированных по сиду и уровню сложности.
    /// Ось C уникальности: генератор сценариев заказов с параметрами сложности.
    /// </summary>
    public sealed class Scenario
    {
        public List<Order> Orders = new List<Order>();
        public int Difficulty;
    }

    public static class ScenarioGenerator
    {
        private static readonly string[] Products =
        {
            "Шестерня", "Корпус", "Плата", "Сервопривод", "Редуктор", "Датчик", "Кабель-канал"
        };

        public static Scenario Generate(int seed, int difficulty)
        {
            var rng = new System.Random(seed);
            var scenario = new Scenario { Difficulty = difficulty };
            int orderCount = 2 + difficulty; // сложность -> больше заказов
            for (int i = 0; i < orderCount; i++)
            {
                float demand = 6f + difficulty * 3f + (float)rng.NextDouble() * 8f;
                float deadline = 6f + (float)rng.NextDouble() * 10f + difficulty * 1.5f;
                scenario.Orders.Add(new Order
                {
                    Product = Products[rng.Next(Products.Length)],
                    UnitsPerMinute = demand,
                    DeadlineMinutes = deadline,
                    Priority = 1 + rng.Next(3),
                });
            }
            return scenario;
        }
    }
}
