using NUnit.Framework;
using RoboLab;

/// <summary>
/// Тесты разбора уровня и генерации батареек.
/// Ось C уникальности: авторские карты в текстовом формате + рандомизация батареек по сиду.
/// </summary>
public class LevelMapTests
{
    [Test]
    public void Parse_ValidMap_HasCorrectSize()
    {
        var map = LevelMap.Parse(LevelCatalog.Map(1));
        Assert.AreEqual(LevelCatalog.WIDTH, map.Width);
        Assert.AreEqual(LevelCatalog.HEIGHT, map.Height);
    }

    [Test]
    public void Parse_ValidMap_HasSpawn()
    {
        for (int level = 1; level <= LevelCatalog.LEVEL_COUNT; level++)
        {
            var map = LevelMap.Parse(LevelCatalog.Map(level));
            Assert.IsNotNull(map.Spawn, $"Уровень {level}: нет точки старта");
        }
    }

    [Test]
    public void Parse_ValidMap_HasExit()
    {
        for (int level = 1; level <= LevelCatalog.LEVEL_COUNT; level++)
        {
            var map = LevelMap.Parse(LevelCatalog.Map(level));
            Assert.IsTrue(map.Exits.Count > 0, $"Уровень {level}: нет выхода");
        }
    }

    [Test]
    public void Parse_BorderTiles_AreWalls()
    {
        for (int level = 1; level <= LevelCatalog.LEVEL_COUNT; level++)
        {
            var map = LevelMap.Parse(LevelCatalog.Map(level));
            for (int x = 0; x < map.Width; x++)
            {
                Assert.IsTrue(map.IsWall(x, 0), $"Уровень {level}: пол не из стен");
                Assert.IsTrue(map.IsWall(x, map.Height - 1), $"Уровень {level}: потолок не из стен");
            }
        }
    }

    [Test]
    public void Parse_RaggedMap_Throws()
    {
        string[] rows = { "#####", "#S.E#", "###" };
        Assert.Throws<System.ArgumentException>(() => LevelMap.Parse(rows));
    }

    [Test]
    public void PlaceBatteries_IsDeterministic()
    {
        var map = LevelMap.Parse(LevelCatalog.Map(3));
        var a = LevelGenerator.PlaceBatteries(map, 42, 5);
        var b = LevelGenerator.PlaceBatteries(map, 42, 5);
        CollectionAssert.AreEqual(a, b);
    }

    [Test]
    public void PlaceBatteries_OnFreeTilesOnly()
    {
        var map = LevelMap.Parse(LevelCatalog.Map(5));
        var spots = LevelGenerator.PlaceBatteries(map, 7, 6);
        Assert.AreEqual(6, spots.Count);
        foreach (var p in spots)
        {
            Assert.IsFalse(map.IsWall(p.Key, p.Value), "Батарейка в стене");
            Assert.AreNotEqual(map.Spawn, p, "Батарейка на точке старта");
        }
    }

    [Test]
    public void PlaceBatteries_DifferentSeeds_DifferentLayouts()
    {
        var map = LevelMap.Parse(LevelCatalog.Map(8));
        var a = LevelGenerator.PlaceBatteries(map, 1, 5);
        var b = LevelGenerator.PlaceBatteries(map, 2, 5);
        CollectionAssert.AreNotEqual(a, b);
    }
}
