using NUnit.Framework;
using EnergyShield;

/// <summary>
/// Тесты волн и энергобюджета обороны электростанции.
/// Ось C уникальности: планировщик волн + балансировка бюджета энергии.
/// </summary>
public class WavePlannerTests
{
    [Test]
    public void Waves_AreDeterministic()
    {
        var a = WavePlanner.Generate(42, 5);
        var b = WavePlanner.Generate(42, 5);
        Assert.AreEqual(a.Count, b.Count);
        for (int i = 0; i < a.Count; i++)
            Assert.AreEqual(a[i].EnemyCount, b[i].EnemyCount);
    }

    [Test]
    public void Waves_GrowInStrength()
    {
        var waves = WavePlanner.Generate(7, 12);
        for (int i = 1; i < waves.Count; i++)
        {
            Assert.GreaterOrEqual(waves[i].TotalHp, waves[i - 1].TotalHp,
                "Волна " + (i + 1) + " не слабее предыдущей");
        }
    }

    [Test]
    public void BossWave_EveryFifth()
    {
        var waves = WavePlanner.Generate(5, 10);
        Assert.IsTrue(waves[4].IsBoss, "5-я волна — босс");
        Assert.IsFalse(waves[3].IsBoss, "4-я волна — обычная");
    }

    [Test]
    public void WaveCount_MatchesRequest()
    {
        var waves = WavePlanner.Generate(3, 7);
        Assert.AreEqual(7, waves.Count);
    }

    [Test]
    public void Tower_CostsAndDamage()
    {
        var tower = TowerCatalog.For(TowerCatalog.Kind.Pylon);
        Assert.Greater(tower.Damage, 0f);
        Assert.Greater(tower.Cost, 0);
        Assert.Greater(tower.Range, 0f);
    }

    [Test]
    public void EnergyBudget_LimitsTowers()
    {
        var grid = new DefenseGrid(100);
        Assert.IsTrue(grid.CanBuild(TowerCatalog.Kind.Pylon, 100), "Энергии хватает");
        Assert.IsTrue(grid.Build(TowerCatalog.Kind.Pylon, 0, 0));
        Assert.IsFalse(grid.CanBuild(TowerCatalog.Kind.Reactor, 10),
            "Мало энергии — строить нельзя");
    }

    [Test]
    public void TowersConsumeEnergy()
    {
        var grid = new DefenseGrid(100);
        grid.Build(TowerCatalog.Kind.Pylon, 0, 0);
        Assert.Less(grid.FreeEnergy, 100, "Башня потребляет энергию");
    }

    [Test]
    public void Upgrade_MultipliesDamage()
    {
        var grid = new DefenseGrid(500);
        grid.Build(TowerCatalog.Kind.Pylon, 0, 0);
        float before = grid.TowerDamage(0, 0);
        grid.Upgrade(0, 0);
        Assert.Greater(grid.TowerDamage(0, 0), before, "Апгрейд усиливает башню");
    }

    [Test]
    public void Kill_ScoreGrowsWithWave()
    {
        int early = DefenseGrid.ScoreForKill(1, false);
        int late = DefenseGrid.ScoreForKill(8, false);
        int boss = DefenseGrid.ScoreForKill(5, true);
        Assert.Greater(late, early);
        Assert.Greater(boss, early);
    }

    [Test]
    public void Build_OnOccupiedCell_Fails()
    {
        var grid = new DefenseGrid(100);
        Assert.IsTrue(grid.Build(TowerCatalog.Kind.Pylon, 1, 1));
        Assert.IsFalse(grid.Build(TowerCatalog.Kind.Pylon, 1, 1), "Занятая клетка");
    }
}
