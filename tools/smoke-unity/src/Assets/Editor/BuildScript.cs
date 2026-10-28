using System;
using System.IO;
using UnityEditor;
using UnityEditor.Build.Reporting;
using UnityEditor.SceneManagement;
using UnityEngine;

/// <summary>
/// Скрипт batchmode-сборки Android APK.
/// Запуск: Unity -batchmode -nographics -quit -projectPath ... -executeMethod BuildScript.BuildAndroid
///         -buildPath out.apk -appId com.example.game -productName "Game" -keystorePath key.jks
///         -keystorePass android -keyAlias game -keyPass android [-portrait]
/// </summary>
public static class BuildScript
{
    private static string GetArg(string[] args, string name)
    {
        for (int i = 0; i < args.Length - 1; i++)
            if (args[i] == name) return args[i + 1];
        return null;
    }

    public static void BuildAndroid()
    {
        string[] args = Environment.GetCommandLineArgs();
        string buildPath = GetArg(args, "-buildPath") ?? "Builds/game.apk";
        string appId = GetArg(args, "-appId") ?? "com.portfolio.game";
        string productName = GetArg(args, "-productName") ?? "Game";
        string keystorePath = GetArg(args, "-keystorePath");
        string keystorePass = GetArg(args, "-keystorePass") ?? "android";
        string keyAlias = GetArg(args, "-keyAlias") ?? productName;
        string keyPass = GetArg(args, "-keyPass") ?? "android";
        bool portrait = Array.IndexOf(args, "-portrait") >= 0;
        bool il2cpp = Array.IndexOf(args, "-il2cpp") >= 0;

        // --- Настройки игрока ---
        PlayerSettings.productName = productName;
        PlayerSettings.SetApplicationIdentifier(BuildTargetGroup.Android, appId);
        PlayerSettings.bundleVersion = "1.0";
        PlayerSettings.Android.bundleVersionCode = 1;
        PlayerSettings.Android.minSdkVersion = AndroidSdkVersions.AndroidApiLevel26;
        PlayerSettings.Android.targetSdkVersion = AndroidSdkVersions.AndroidApiLevelAuto;
        PlayerSettings.SetScriptingBackend(BuildTargetGroup.Android,
            il2cpp ? ScriptingImplementation.IL2CPP : ScriptingImplementation.Mono2x);
        PlayerSettings.Android.targetArchitectures = AndroidArchitecture.ARM64 | AndroidArchitecture.ARMv7;
        PlayerSettings.defaultInterfaceOrientation = portrait
            ? UIOrientation.Portrait
            : UIOrientation.LandscapeLeft;
        PlayerSettings.fullScreenMode = FullScreenMode.ExclusiveFullScreen;

        if (!string.IsNullOrEmpty(keystorePath) && File.Exists(keystorePath))
        {
            PlayerSettings.Android.useCustomKeystore = true;
            PlayerSettings.Android.keystoreName = keystorePath;
            PlayerSettings.Android.keystorePass = keystorePass;
            PlayerSettings.Android.keyaliasName = keyAlias;
            PlayerSettings.Android.keyaliasPass = keyPass;
        }

        // --- Сцена с точкой входа ---
        var scene = EditorSceneManager.NewScene(NewSceneSetup.EmptyScene, NewSceneMode.Single);
        var go = new GameObject("GameBootstrap");
        go.AddComponent<GameBootstrap>();
        Directory.CreateDirectory("Assets/Scenes");
        EditorSceneManager.SaveScene(scene, "Assets/Scenes/Main.unity");
        EditorBuildSettings.scenes = new[] { new EditorBuildSettingsScene("Assets/Scenes/Main.unity", true) };

        // --- Сборка ---
        var opts = new BuildPlayerOptions
        {
            scenes = new[] { "Assets/Scenes/Main.unity" },
            locationPathName = buildPath,
            target = BuildTarget.Android,
            options = BuildOptions.None
        };
        BuildReport report = BuildPipeline.BuildPlayer(opts);
        if (report.summary.result != BuildResult.Succeeded)
            throw new Exception("Сборка Android не удалась: " + report.summary.result);
        Debug.Log("[BuildScript] APK готов: " + buildPath + " (" + report.summary.totalSize + " байт)");
    }
}
