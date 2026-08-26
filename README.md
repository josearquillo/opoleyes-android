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

- **5 game modes**: Supervivencia, Contrarreloj, Repaso Express (5 preguntas, +50 XP bonus si 5/5), Mini Examen, Simulacro
- **7 ranks** with XP progression and mode/unlock progression
- **Daily missions** (1-3 depending on rank) with XP rewards
- **Power-ups**: Pista (50% points) and 50/50 (25% points) — unlimited usage with point penalties
- **Chest rewards** (Bronze/Silver/Gold) with XP and XP multiplier
- **Achievements** (30+) split into per-question (combo, firsts) and game-over (milestones)
- **Combo system** with overcharge charges for life/time recovery
- **Beginner-friendly**: first mistake forgiven at rank 0, combo forgiveness at ranks 0-1, 1 XP consolation per wrong answer at ranks 0-1
- **Debug mode** (long-press title in Help) with sandboxed progress
- **Lottie animations** for loading screen and chest openings

## Testing

```bash
# Unit tests (Robolectric)
./gradlew test

# Instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest
```

## Publicación en Google Play Store

### Configuración de firma (release)

El proyecto firma los builds de release con un keystore propio, configurado en
`app/build.gradle.kts` y cuyas credenciales se leen desde `keystore.properties`
(en la raíz, **no versionado**). Ya se ha generado uno por defecto:

- Keystore: `app/release-keystore.jks`
- Alias: `opoleyes-release`
- Contraseña (keystore y key): `Opoleyes2026!`

> **Importante:** guarda una copia de seguridad del `.jks` en un lugar seguro.
> Si lo pierdes no podrás actualizar la app en Play Store con el mismo paquete.

Para regenerar con tus propias credenciales:

```bash
keytool -genkeypair -v \
  -keystore app/release-keystore.jks -storetype PKCS12 \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias opoleyes-release \
  -storepass "TU_PASSWORD" -keypass "TU_PASSWORD" \
  -dname "CN=OpoLeyes, O=OpoLeyes, C=ES"
```

Y actualiza `keystore.properties` con los nuevos valores.

### Anuncios (AdMob)

En **release** los anuncios están **desactivados** (`BuildConfig.ADS_ENABLED = false`)
porque no hay ID real de AdMob. Usar el ID de test en producción hace que Google
rechace la app. Cuando tengas un ID real de AdMob:

1. Crea tu cuenta y app en https://admob.google.com/
2. En `app/build.gradle.kts`, cambia `ADS_ENABLED` a `true` en `release`
3. En `app/build.gradle.kts`, pon tu ID real en `manifestPlaceholders["admobAppId"]`
   (en el bloque `release`)
4. Sustituye los IDs de test en `AdBanner.kt` y `RewardedAdManager.kt` por los reales
5. Actualiza la política de privacidad si cambia el uso de datos

### Generar el AAB para Play Store

Play Store requiere el formato `.aab` (Android App Bundle), no `.apk`:

```bash
./gradlew :app:bundleRelease
```

El archivo se genera en `app/build/outputs/bundle/release/app-release.aab`.

> Recomendado: activa **Play App Signing** en Play Console (Te preguntará al
> subir el primer AAB). Google re-firma con su clave de distribución y tú
> conservas tu keystore para futuras actualizaciones.

### Antes de subir (checklist)

1. **Cuenta de desarrollador de Google Play** ($25, una sola vez):
   https://play.google.com/console/signup — requiere verificación de identidad.
2. **Política de privacidad**: hospeda `docs/privacy_policy.html` en una URL
   pública (GitHub Pages, tu web, etc.) y edita el email de contacto
   (`TU_EMAIL@example.com`). Necesitarás esa URL en Play Console.
3. **Store listing** (en Play Console):
   - Nombre de la app, descripción corta (80) y larga (4000)
   - Icono de la app 512×512 PNG (ya existe `ic_launcher_ol_v3`; exporta una
     versión 512×512)
   - Gráfico destacado 1024×500 PNG
   - Capturas de pantalla (mín. 2; idealmente teléfono 16:9 o 9:16)
   - Categoría, etiquetas, email de contacto
4. **App content** (cuestionarios obligatorios en Play Console):
   - **Data safety**: la app usa almacenamiento local (progreso) y el SDK de
     AdMob (cuando se active). Indica "No" a la recopilación de datos personales.
   - **Content rating**: completa el cuestionario (quiz/educación, sin contenido
     sensible) → suele salir "Todos los públicos".
   - **Target audience**: selecciona 13+ o "Todas las edades" según corresponda.
   - **Ads**: confirma que la app muestra anuncios (sí, vía AdMob).
5. **targetSdk**: el proyecto usa `targetSdk = 35` (requisito para apps nuevas
   desde agosto 2025).
6. **Versionado**: `versionCode = 1`, `versionName = "1.0"` para el primer
   release. En cada actualización sube `versionCode` (entero) y opcionalmente
   `versionName`.
7. **Prueba interna**: sube el AAB a la pista de "Pruebas internas" en Play
   Console y pruébalo en un dispositivo real antes de pasar a producción.

### Comandos útiles

```bash
# Build release AAB
./gradlew :app:bundleRelease

# Build release APK (solo para pruebas locales; Play Store usa AAB)
./gradlew :app:assembleRelease

# Limpiar y reconstruir
./gradlew clean :app:bundleRelease
```

