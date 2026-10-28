using NUnit.Framework;
using RoboLab;

/// <summary>Тесты прогрессии и сессии: звёзды, жизни, счёт, правила сохранения.</summary>
public class ProgressionTests
{
    [Test]
    public void Stars_AllCollected_IsThree()
    {
        Assert.AreEqual(3, GameSession.StarsFor(5, 5));
    }

    [Test]
    public void Stars_Majority_IsTwo()
    {
        Assert.AreEqual(2, GameSession.StarsFor(3, 5));
    }

    [Test]
    public void Stars_Minority_IsOne()
    {
        Assert.AreEqual(1, GameSession.StarsFor(1, 5));
        Assert.AreEqual(1, GameSession.StarsFor(0, 5));
    }

    [Test]
    public void Score_BatteriesAndTime_AffectScore()
    {
        int baseScore = GameSession.ScoreFor(0, 30);
        int batteryScore = GameSession.ScoreFor(5, 30);
        int fastScore = GameSession.ScoreFor(0, 10);
        Assert.Greater(batteryScore, baseScore);
        Assert.Greater(fastScore, baseScore, "Быстрое прохождение даёт больше очков");
    }

    [Test]
    public void Lives_LoseLifeOnHazard()
    {
        var session = new GameSession(3);
        session.Hit();
        Assert.AreEqual(2, session.Lives);
        session.Hit();
        session.Hit();
        Assert.IsFalse(session.IsAlive);
    }

    [Test]
    public void LevelCatalog_HasTwentyLevels()
    {
        Assert.AreEqual(20, LevelCatalog.LEVEL_COUNT);
    }

    [Test]
    public void LevelCatalog_LevelsAreUnique()
    {
        for (int i = 1; i <= LevelCatalog.LEVEL_COUNT; i++)
        {
            for (int j = i + 1; j <= LevelCatalog.LEVEL_COUNT; j++)
            {
                Assert.AreNotEqual(LevelCatalog.Map(i), LevelCatalog.Map(j),
                    $"Карты уровней {i} и {j} совпадают");
            }
        }
    }

    [Test]
    public void UnlockRule_NextLevelOpensAfterWin()
    {
        var progress = new ProgressState();
        Assert.IsFalse(progress.IsUnlocked(2), "Уровень 2 закрыт до победы на 1");
        progress.Complete(1, 3);
        Assert.IsTrue(progress.IsUnlocked(2));
        Assert.IsFalse(progress.IsUnlocked(3));
    }
}
