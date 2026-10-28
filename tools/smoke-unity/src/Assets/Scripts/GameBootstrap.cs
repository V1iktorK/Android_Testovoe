using UnityEngine;

/// <summary>Точка входа игры: создаётся сценым скриптом при сборке.</summary>
public class GameBootstrap : MonoBehaviour
{
    private void OnGUI()
    {
        GUI.Label(new Rect(20, 20, 600, 40), "Unity smoke: OK");
    }
}
