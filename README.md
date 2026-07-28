# OPOLEYES Android

Android migration of the OPOLEYES quiz game, built with Kotlin + Jetpack Compose.

## Project Structure

```
opoleyes-android/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── assets/
│       │   └── data.json
│       ├── java/com/opoleyes/
│       │   ├── OpoleyesApp.kt
│       │   ├── MainActivity.kt
│       │   ├── data/
│       │   │   ├── Constants.kt
│       │   │   ├── local/
│       │   │   │   ├── DataProvider.kt
│       │   │   │   └── PreferencesManager.kt
│       │   │   ├── model/
│       │   │   │   └── Models.kt
│       │   │   └── repository/
│       │   │       ├── GameRepository.kt
│       │   │       ├── MissionRepository.kt
│       │   │       ├── ProgressRepository.kt
│       │   │       └── StatsRepository.kt
│       │   ├── domain/
│       │   │   ├── AchievementChecker.kt
│       │   │   ├── ChestSystem.kt
│       │   │   └── GameEngine.kt
│       │   └── ui/
│       │       ├── components/
│       │       │   └── GameComponents.kt
│       │       ├── navigation/
│       │       │   ├── NavGraph.kt
│       │       │   ├── GameViewModel.kt
│       │       │   └── TrainingViewModel.kt
│       │       ├── screens/
│       │       │   ├── LoadingScreen.kt
│       │       │   ├── ErrorScreen.kt
│       │       │   ├── HomeScreen.kt
│       │       │   ├── ModeSelectScreen.kt
│       │       │   ├── TemaSelectScreen.kt
│       │       │   ├── GameScreen.kt
│       │       │   ├── GameOverScreen.kt
│       │       │   ├── ProfileScreen.kt
│       │       │   ├── HelpScreen.kt
│       │       │   ├── TrainSelectScreen.kt
│       │       │   ├── TrainListScreen.kt
│       │       │   ├── TestBrowserScreen.kt
│       │       │   ├── FlagReviewScreen.kt
│       │       │   ├── WrongReviewScreen.kt
│       │       │   └── ResultsScreen.kt
│       │       └── theme/
│       │           ├── Theme.kt
│       │           ├── Type.kt
│       │           └── Shapes.kt
│       └── res/
│           ├── values/
│           │   ├── colors.xml
│           │   └── themes.xml
│           └── mipmap-anydpi-v26/
│               ├── ic_launcher.xml
│               └── ic_launcher_round.xml
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── gradle/wrapper/
    └── gradle-wrapper.properties
```

## How to Open

1. Open Android Studio
2. File → Open → Select `opoleyes-android` folder
3. Let Gradle sync complete
4. Run on emulator or device

## Tech Stack

- Kotlin 1.9.22
- Jetpack Compose (BOM 2024.01.00)
- Material 3
- Navigation Compose
- DataStore Preferences
- Gson
- Google Mobile Ads SDK
- Min SDK 24, Target SDK 34
