using NUnit.Framework;
using UnityEngine;
using GravityDrift;

/// <summary>
/// Тесты генератора уровней физической головоломки.
/// Ось C уникальности: параметрическая расстановка гравитационных колодцев по сиду.
/// </summary>
public class GravityLevelTests
{
    [Test]
    public void Generate_IsDeterministic()
    {
        var a = LevelGenerator.Generate(42, 3);
        var b = LevelGenerator.Generate(42, 3);
        Assert.AreEqual(a.Wells.Count, b.Wells.Count);
        for (int i = 0; i < a.Wells.Count; i++)
        {
            Assert.AreEqual(a.Wells[i].X, b.Wells[i].X);
            Assert.AreEqual(a.Wells[i].Y, b.Wells[i].Y);
            Assert.AreEqual(a.Wells[i].Mass, b.Wells[i].Mass);
        }
    }

    [Test]
    public void Generate_WellCountGrowsWithLevel()
    {
        var easy = LevelGenerator.Generate(7, 1);
        var hard = LevelGenerator.Generate(7, 10);
        Assert.GreaterOrEqual(hard.Wells.Count, easy.Wells.Count);
    }

    [Test]
    public void Generate_WellsAreInsideBounds()
    {
        var level = LevelGenerator.Generate(5, 8);
        foreach (var w in level.Wells)
        {
            Assert.IsTrue(w.X >= -LevelGenerator.BOUNDS && w.X <= LevelGenerator.BOUNDS);
            Assert.IsTrue(w.Y >= -LevelGenerator.BOUNDS && w.Y <= LevelGenerator.BOUNDS);
            Assert.Greater(w.Mass, 0f);
        }
    }

    [Test]
    public void Generate_StartAndGoalSeparated()
    {
        for (int seed = 1; seed <= 15; seed++)
        {
            var level = LevelGenerator.Generate(seed, 4);
            float dx = level.GoalX - level.StartX;
            float dy = level.GoalY - level.StartY;
            Assert.Greater(Mathf.Sqrt(dx * dx + dy * dy), 2f,
                "Старт и цель должны быть разнесены (seed=" + seed + ")");
        }
    }

    [Test]
    public void Generate_GoalNotInsideWell()
    {
        var level = LevelGenerator.Generate(9, 6);
        foreach (var w in level.Wells)
        {
            float dx = level.GoalX - w.X;
            float dy = level.GoalY - w.Y;
            Assert.Greater(Mathf.Sqrt(dx * dx + dy * dy), LevelGenerator.WELL_SAFE_RADIUS,
                "Цель не должна быть внутри колодца");
        }
    }

    [Test]
    public void GravityForce_InverseSquare()
    {
        float f1 = PhysicsMath.ForceAt(1f, 10f);
        float f2 = PhysicsMath.ForceAt(2f, 10f);
        Assert.AreEqual(f1 / 4f, f2, 0.001f);
    }

    [Test]
    public void GravityForce_DirectionTowardWell()
    {
        var v = PhysicsMath.ForceVector(0f, 0f, 3f, 4f, 5f);
        Assert.Greater(v.x, 0f, "Сила тянет к колодцу вправо (колодец в x=3)");
        Assert.Greater(v.y, 0f, "Сила тянет к колодцу вверх (колодец в y=4)");
    }

    [Test]
    public void Fuel_BudgetScalesWithLevel()
    {
        var easy = LevelGenerator.Generate(1, 1);
        var hard = LevelGenerator.Generate(1, 12);
        Assert.GreaterOrEqual(hard.FuelSeconds, easy.FuelSeconds);
    }

    [Test]
    public void Score_RewardsFuelAndStars()
    {
        int good = PhysicsMath.ScoreFor(3, 50f, 100f);
        int perfect = PhysicsMath.ScoreFor(3, 100f, 100f);
        Assert.Greater(perfect, good);
    }
}
