# OpoLeyes

App de estudio gamificada para preparar oposiciones de justicia.
Kotlin + Jetpack Compose. Sin anuncios. 100% offline.

## Características

- **5 modos de juego**: Supervivencia, Contrarreloj, Repaso Express, Mini Examen y Simulacro (100 preguntas, 100 min, penalización oficial)
- **Sistema de rangos y XP**: progresa desde Novato hasta Leyenda
- **Misiones diarias** y **logros** desbloqueables
- **Cofres** con recompensas al finalizar partidas
- **Power-ups**: 50/50, pista, congelar tiempo, etc.
- **Combos** y bonus por racha de aciertos
- **Estadísticas** por ley y por pregunta
- **Modo debug** oculto (solo en builds de debug, no en release)

## Instalar

Descarga el APK desde [releases](https://github.com/josearquillo/opoleyes-android/releases)
e instálalo en tu dispositivo Android (mínimo Android 7.0 / API 24).

Al abrir el APK, Android pedirá permiso para instalar desde "orígenes
desconocidos" → acepta y listo.

## Compilar

Requisitos: Android Studio + JDK 17.

```bash
gradlew.bat :app:assembleDebug      # Debug APK
gradlew.bat :app:assembleRelease    # Release APK firmado
```

El APK se genera en `app/build/outputs/apk/release/app-release.apk`.

## Tests

```bash
gradlew.bat test                    # Unit tests
gradlew.bat connectedAndroidTest    # Instrumented tests (requiere dispositivo/emulador)
```

## Info técnica

- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 35 (Android 15)
- **Java/Kotlin**: 17
- **Sin permisos de red**: la app no necesita INTERNET ni ACCESS_NETWORK_STATE
- **Sin anuncios ni trackers**: no incluye ningún SDK de publicidad ni analítica
- **Firma release**: configurada via `keystore.properties` (no versionado)

## Política de privacidad

Disponible en [docs/privacy_policy.html](docs/privacy_policy.html).

## Licencia

MIT
