using NUnit.Framework;
using ShadowBlade;

/// <summary>
/// Тесты генератора уровней ниндзя-платформера.
/// Ось C уникальности: чанковый генератор с гарантией проходимости.
/// </summary>
public class NinjaLevelTests
{
    [Test]
    public void Generate_IsDeterministic()
    {
        var a = LevelChunkGenerator.Generate(42, 8);
        var b = LevelChunkGenerator.Generate(42, 8);
        Assert.AreEqual(a.Count, b.Count);
        for (int i = 0; i < a.Count; i++)
            Assert.AreEqual(a[i].Kind, b[i].Kind);
    }

    [Test]
    public void Generate_ChunksAreFromGrammar()
    {
        var level = LevelChunkGenerator.Generate(7, 15);
        foreach (var chunk in level)
        {
            Assert.IsTrue(System.Enum.IsDefined(typeof(LevelChunkGenerator.ChunkKind), chunk.Kind),
                "Неизвестный чанк: " + chunk.Kind);
        }
    }

    [Test]
    public void Generate_LevelIsTraversable()
    {
        for (long seed = 1; seed <= 15; seed++)
        {
            var level = LevelChunkGenerator.Generate(seed, 12);
            Assert.IsTrue(LevelChunkGenerator.IsTraversable(level),
                "Уровень непроходим (seed=" + seed + ")");
        }
    }

    [Test]
    public void Generate_LongerRequest_MoreChunks()
    {
        var shortLevel = LevelChunkGenerator.Generate(3, 6);
        var longLevel = LevelChunkGenerator.Generate(3, 18);
        Assert.Greater(longLevel.Count, shortLevel.Count);
    }

    [Test]
    public void Generate_StartsWithSafeChunk()
    {
        var level = LevelChunkGenerator.Generate(11, 10);
        Assert.AreEqual(LevelChunkGenerator.ChunkKind.Flat, level[0].Kind,
            "Уровень начинается с ровной площадки");
    }

    [Test]
    public void Stars_ByCollectedStars()
    {
        Assert.AreEqual(3, NinjaSession.StarsFor(5, 5));
        Assert.AreEqual(2, NinjaSession.StarsFor(3, 5));
        Assert.AreEqual(1, NinjaSession.StarsFor(1, 5));
    }

    [Test]
    public void Jump_ReachesDoubleJumpHeight()
    {
        float single = PhysicsKit.MaxJumpHeight(1);
        float dbl = PhysicsKit.MaxJumpHeight(2);
        Assert.Greater(dbl, single * 1.5f, "Двойной прыжок заметно выше");
    }

    [Test]
    public void Shuriken_AmmoIsLimited()
    {
        var session = new NinjaSession();
        Assert.AreEqual(NinjaSession.StartingShurikens, session.Shurikens);
        Assert.IsTrue(session.ThrowShuriken());
        Assert.AreEqual(NinjaSession.StartingShurikens - 1, session.Shurikens);
    }

    [Test]
    public void Shuriken_CannotThrowWithoutAmmo()
    {
        var session = new NinjaSession();
        for (int i = 0; i < NinjaSession.StartingShurikens; i++) session.ThrowShuriken();
        Assert.IsFalse(session.ThrowShuriken(), "Без сюрикенов бросок невозможен");
    }

    [Test]
    public void Score_RewardsDistanceAndStars()
    {
        int baseScore = NinjaSession.ScoreFor(100, 0);
        int withStars = NinjaSession.ScoreFor(100, 3);
        Assert.Greater(withStars, baseScore);
    }
}
