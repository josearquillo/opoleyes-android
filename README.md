# OPOLEYES Android

Android migration of the OPOLEYES quiz game, built with Kotlin + Jetpack Compose.

## Project Structure

```
opoleyes-android/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── assets/
│       │   │   ├── data.json                 # Question bank (all laws)
│       │   │   ├── law_and_justice.json      # Lottie loading animation
│       │   │   ├── gift_bronze.json          # Lottie chest animations
│       │   │   ├── gift_silver.json
│       │   │   └── gift_gold.json
│       │   ├── java/com/opoleyes/
│       │   │   ├── OpoleyesApp.kt
│       │   │   ├── MainActivity.kt
│       │   │   ├── data/
│       │   │   │   ├── Constants.kt          # Ranks, achievements, unlocks, rewards
│       │   │   │   ├── Interfaces.kt         # Repository interfaces
│       │   │   │   ├── local/
│       │   │   │   │   ├── DataProvider.kt   # JSON loader, test/tema grouping
│       │   │   │   │   └── PreferencesManager.kt  # SharedPreferences, debug mode
│       │   │   │   ├── model/
│       │   │   │   │   └── Models.kt         # Question, Test, Rank, Mission, etc.
│       │   │   │   └── repository/
│       │   │   │       ├── GameRepository.kt
│       │   │   │       ├── MissionRepository.kt   # Daily missions, XP rewards
│       │   │   │       ├── ProgressRepository.kt  # XP, ranks, unlocks, achievements
│       │   │   │       └── StatsRepository.kt     # Per-question stats, law progress
│       │   │   ├── domain/
│       │   │   │   ├── AchievementChecker.kt  # Per-question + game-over checks
│       │   │   │   ├── ChestSystem.kt         # Post-game chest rewards
│       │   │   │   ├── ExamEngine.kt          # Mini Examen + Simulacro engine
│       │   │   │   └── GameEngine.kt          # Core game loop, power-ups, scoring
│       │   │   └── ui/
│       │   │       ├── components/
│       │   │       │   ├── GameComponents.kt  # HUD, buttons, cards, heart icons
│       │   │       │   └── GameEffects.kt     # Confetti, particles, animations
│       │   │       ├── navigation/
│       │   │       │   ├── NavGraph.kt
│       │   │       │   └── GameViewModel.kt   # Central state management
│       │   │       ├── screens/
│       │   │       │   ├── LoadingScreen.kt
│       │   │       │   ├── ErrorScreen.kt
│       │   │       │   ├── HomeScreen.kt
│       │   │       │   ├── ModeSelectScreen.kt
│       │   │       │   ├── TemaSelectScreen.kt
│       │   │       │   ├── GameScreen.kt
│       │   │       │   ├── GameOverScreen.kt  # Rank-up overlay, chest, results
│       │   │       │   ├── ExamScreen.kt
│       │   │       │   ├── ExamResultScreen.kt
│       │   │       │   ├── SimulacroIntroScreen.kt
│       │   │       │   ├── ProfileScreen.kt
│       │   │       │   └── HelpScreen.kt
│       │   │       └── theme/
│       │   │           ├── Theme.kt          # Color palette, scrims, confetti
│       │   │           ├── Type.kt
│       │   │           └── Shapes.kt
│       │   └── res/
│       │       ├── values/
│       │       │   ├── colors.xml
│       │       │   ├── strings.xml
│       │       │   └── themes.xml
│       │       └── drawable/
│       │           └── ic_logo_ol_v3.xml      # App logo (turquoise + gold)
│       ├── test/                              # Unit tests (Robolectric)
│       │   └── java/com/opoleyes/
│       │       ├── TestContextProvider.kt
│       │       ├── TestFakes.kt
│       │       ├── data/
│       │       │   ├── ConstantsTest.kt
│       │       │   ├── DataIntegrityTest.kt
│       │       │   └── model/ModelsTest.kt
│       │       ├── domain/
│       │       │   ├── GameEngineTest.kt
│       │       │   ├── GameEngineEdgeCaseTest.kt
│       │       │   ├── GameEngineTimerFlowTest.kt
│       │       │   └── PowerUpCombinationTest.kt
│       │       └── ui/screens/
│       │           └── GameScreenExhaustiveTest.kt
│       └── androidTest/                       # Instrumented tests
│           └── java/com/opoleyes/
│               └── ui/screens/
│                   ├── NavGraphTest.kt
│                   ├── ErrorScreenTest.kt
│                   ├── ExamResultScreenTest.kt
│                   ├── GameOverScreenTest.kt
│                   ├── HelpScreenTest.kt
│                   ├── LoadingScreenTest.kt
│                   ├── ProfileScreenTest.kt
│                   └── TemaSelectScreenTest.kt
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
- Jetpack Compose (BOM 2024.02.00)
- Material 3
- Navigation Compose
- SharedPreferences (via Gson serialization)
- Gson 2.10.1
- Lottie (dotlottie-android 0.15.0)
- Core SplashScreen
- Min SDK 24, Target SDK 34

## Features

- **5 game modes**: Supervivencia, Contrarreloj, Repaso Express, Mini Examen, Simulacro
- **7 ranks** with XP progression and mode/power-up unlocks
- **Daily missions** (1-3 depending on rank) with XP rewards
- **Power-ups**: Pista, Escudo, 50/50, x2 pts — received on rank-up and from chests
- **Chest rewards** (Bronze/Silver/Gold) with XP, power-ups, and XP multiplier
- **Achievements** (30+) split into per-question (combo, firsts) and game-over (milestones)
- **Combo system** with overcharge charges for life/time recovery
- **Debug mode** (long-press title in Help) with sandboxed progress
- **Lottie animations** for loading screen and chest openings

## Testing

```bash
# Unit tests (Robolectric)
./gradlew test

# Instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest
```
