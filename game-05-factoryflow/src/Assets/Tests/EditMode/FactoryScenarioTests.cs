using NUnit.Framework;
using FactoryFlow;

/// <summary>
/// Тесты симулятора цеха: балансировка конвейера, заказы, распределение ресурсов.
/// Ось C уникальности: генератор сценариев заказов с параметрами сложности.
/// </summary>
public class FactoryScenarioTests
{
    [Test]
    public void Scenario_IsDeterministic_ForSeed()
    {
        var a = ScenarioGenerator.Generate(42, 3);
        var b = ScenarioGenerator.Generate(42, 3);
        Assert.AreEqual(a.Orders.Count, b.Orders.Count);
        for (int i = 0; i < a.Orders.Count; i++)
            Assert.AreEqual(a.Orders[i].Product, b.Orders[i].Product);
    }

    [Test]
    public void Scenario_HarderDifficulty_MoreOrders()
    {
        var easy = ScenarioGenerator.Generate(7, 1);
        var hard = ScenarioGenerator.Generate(7, 5);
        Assert.GreaterOrEqual(hard.Orders.Count, easy.Orders.Count);
    }

    [Test]
    public void Scenario_OrdersHaveValidDemand()
    {
        var s = ScenarioGenerator.Generate(11, 4);
        foreach (var order in s.Orders)
        {
            Assert.Greater(order.UnitsPerMinute, 0f);
            Assert.Greater(order.DeadlineMinutes, 0f);
            Assert.GreaterOrEqual(order.Priority, 1);
        }
    }

    [Test]
    public void Line_ProductionScalesWithStations()
    {
        var line = new ProductionLine(2);
        float before = line.OutputPerMinute;
        line.AddStation();
        // Линейный рост выпуска: 2 -> 3 станции = ×1.5
        Assert.AreEqual(before * 1.5f, line.OutputPerMinute, 0.001f);
    }

    [Test]
    public void Line_Bottleneck_LimitsThroughput()
    {
        var line = new ProductionLine(3);
        line.SetStationSpeed(0, 10f);
        line.SetStationSpeed(1, 4f);
        line.SetStationSpeed(2, 10f);
        Assert.AreEqual(4f, line.ThroughputPerMinute, 0.001f);
    }

    [Test]
    public void Line_EnergyBudget_LimitsStations()
    {
        var line = new ProductionLine(1);
        Assert.IsFalse(line.CanAddStation(energyAvailable: 5f), "Не хватает энергии на станцию");
        Assert.IsTrue(line.CanAddStation(energyAvailable: 15f));
    }

    [Test]
    public void Fulfillment_Scoring_RewardsOnTime()
    {
        int early = Fulfillment.Score(orderUnits: 100, delivered: 100, deadline: 10f, elapsed: 8f);
        int late = Fulfillment.Score(orderUnits: 100, delivered: 100, deadline: 10f, elapsed: 12f);
        Assert.Greater(early, late);
    }

    [Test]
    public void Fulfillment_PartialDelivery_ScoresLess()
    {
        int full = Fulfillment.Score(100, 100, 10f, 8f);
        int half = Fulfillment.Score(100, 50, 10f, 8f);
        Assert.Greater(full, half);
    }

    [Test]
    public void Shift_Simulation_UpdatesStocks()
    {
        var sim = new FactorySim(new ProductionLine(2), ScenarioGenerator.Generate(1, 2));
        sim.RunMinutes(10);
        Assert.Greater(sim.CompletedUnits, 0);
    }
}
