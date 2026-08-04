# Plan de implementación: rangos, curva de dificultad y onboarding

## Objetivo

Re-diseñar el sistema de rangos y la experiencia de onboarding de OpoLeyes para que cada rango desbloquee una única mecánica y los nuevos usuarios progresen de 2 opciones / 5 corazones / sin power-ups hasta el modo Supervivencia completo (4 opciones / 3 corazones / todos los power-ups). El campo `difficulty` del `data.json` se integra en la selección y filtrado de preguntas.

---

## 1. Nuevo sistema de rangos

### 1.1 Tabla de rangos

Cada rango desbloquea **una sola cosa**.

| Rango | Nombre | XP | Desbloqueo principal | Supervivencia | Corazones | Power-ups iniciales (regalados al subir) | Dificultad máxima del pool |
|---|---|---|---|---|---|---|---|
| 0 | Novato | 0 | Supervivencia (2 opciones) | 2 opciones | 5 | Ninguno | 2 |
| 1 | Principiante | 200 | 3 opciones + Escudo + Doble Puntos | 3 opciones | 4 | Escudo ×2, Doble ×2 | 2 |
| 2 | Aprendiz | 800 | 4 opciones + 50/50 + Pista | 4 opciones | 3 | 50/50 ×2, Pista ×2 | 3 |
| 3 | Estudiante | 2.000 | Contrarreloj | 4 opciones | 3 | Todos (mantenidos) | 3 |
| 4 | Avanzado | 4.000 | 2 misiones diarias | 4 opciones | 3 | Todos | 4 |
| 5 | Experto | 7.000 | Repaso Express | 4 opciones | 3 | Todos | 4 |
| 6 | Veterano | 12.000 | 3 misiones diarias | 4 opciones | 3 | Todos | 5 |
| 7 | Maestro | 18.000 | Mini Examen | 4 opciones | 3 | Todos | 5 |
| 8 | Leyenda | 25.000 | Simulacro | 4 opciones | 3 | Todos | 5 |

### 1.2 Constantes en `data/Constants.kt`

```kotlin
val RANKS = listOf(
    Rank("Novato", "\uD83C\uDF31", 0, 0),
    Rank("Principiante", "\uD83C\uDF3F", 200, 1),
    Rank("Aprendiz", "\uD83D\uDCDA", 800, 2),
    Rank("Estudiante", "\uD83D\uDCDD", 2000, 3),
    Rank("Avanzado", "\uD83D\uDD25", 4000, 4),
    Rank("Experto", "\u2696\uFE0F", 7000, 5),
    Rank("Veterano", "\uD83C\uDFAF", 12000, 6),
    Rank("Maestro", "\uD83D\uDC51", 18000, 7),
    Rank("Leyenda", "\uD83D\uDC8E", 25000, 8),
)

val RANK_UNLOCKS = mapOf(
    3 to "\u23F1\uFE0F Contrarreloj",
    4 to "\uD83D\uDCCB 2 misiones diarias",
    5 to "\u26A1 Repaso Express",
    6 to "\uD83D\uDCCB 3 misiones diarias",
    7 to "\uD83D\uDCDD Mini Examen",
    8 to "\uD83C\uDFAF Simulacro",
)

// Regalos de power-ups al alcanzar el rango (se otorgan una sola vez).
val RANK_POWERUP_REWARDS = mapOf(
    1 to listOf("shield", "doubleScore"),
    2 to listOf("fiftyFifty", "hint"),
)

// Mecánicas por rango.
val MAX_OPTIONS_BY_RANK = mapOf(
    0 to 2, 1 to 3, 2 to 4, 3 to 4, 4 to 4,
    5 to 4, 6 to 4, 7 to 4, 8 to 4
)

val MAX_LIVES_BY_RANK = mapOf(
    0 to 5, 1 to 4, 2 to 3, 3 to 3, 4 to 3,
    5 to 3, 6 to 3, 7 to 3, 8 to 3
)

val MAX_DIFFICULTY_BY_RANK = mapOf(
    0 to 2, 1 to 2, 2 to 3, 3 to 3, 4 to 4,
    5 to 4, 6 to 5, 7 to 5, 8 to 5
)

// Power-ups disponibles por rango.
val AVAILABLE_POWERUPS_BY_RANK = mapOf(
    0 to listOf<String>(),
    1 to listOf("shield", "doubleScore"),
    2 to listOf("shield", "doubleScore", "fiftyFifty", "hint"),
    3 to listOf("shield", "doubleScore", "fiftyFifty", "hint"),
    4 to listOf("shield", "doubleScore", "fiftyFifty", "hint"),
    5 to listOf("shield", "doubleScore", "fiftyFifty", "hint"),
    6 to listOf("shield", "doubleScore", "fiftyFifty", "hint"),
    7 to listOf("shield", "doubleScore", "fiftyFifty", "hint"),
    8 to listOf("shield", "doubleScore", "fiftyFifty", "hint"),
)
```

### 1.3 Modos desbloqueables

- Survival: siempre.
- Timetrial: rango 3.
- Quick: rango 5.
- Exam: rango 7.
- Simulacro: rango 8.

Misiones diarias:
- Rango 0-3: 1 misión.
- Rango 4-5: 2 misiones.
- Rango 6-8: 3 misiones.

---

## 2. Curva de dificultad y onboarding

### 2.1 Fases de Supervivencia

#### Fase 1 — Rango 0 (Novato)
- 2 opciones por pregunta: la correcta + 1 distractor aleatorio.
- 5 corazones.
- Sin power-ups.
- Pool disponible: preguntas con `difficulty <= 2` (tope del rango). La selección dentro de la partida se rige por `sessionDifficultyCap` (ver 2.2/2.3), que parte en 1 y sube hasta 2.
- Selección: ordenadas por `difficulty` ascendente; las más fáciles primero.
- No se desbloquean logros de combo/precisión.

#### Fase 2 — Rango 1 (Principiante)
- 3 opciones por pregunta: la correcta + 2 distractores aleatorios.
- 4 corazones.
- Power-ups disponibles: Escudo y Doble Puntos.
- Al subir a este rango se regalan 2 cargas de Escudo y 2 de Doble Puntos.
- Pool filtrado: `difficulty <= 2`.
- 50/50 y Pista deshabilitados.

#### Fase 3 — Rango 2+ (Aprendiz en adelante)
- 4 opciones por pregunta.
- 3 corazones.
- Todos los power-ups disponibles.
- Al subir a rango 2 se regalan 2 cargas de 50/50 y 2 de Pista.
- Dificultad progresiva: `difficulty <= 3` en rango 2-3, `<= 4` en 4-5, `<= 5` en 6-8.
- Dentro de una partida, la dificultad máxima puede aumentar progresivamente cada 5 preguntas respondidas hasta el tope del rango.

### 2.2 Dificultad progresiva dentro de una partida

En `GameEngine` se introduce una variable `sessionDifficultyCap` que:
- Comienza en `1`.
- Cada 5 preguntas respondidas correctamente aumenta en 1, hasta `MAX_DIFFICULTY_BY_RANK[rankIndex]`.
- Al fallar, no baja.

Esto evita que un novato se estanque solo en `difficulty 1` y que un veterano empiece con preguntas difíciles.

### 2.3 Selección de preguntas

Para novatos y principiantes (rango 0-1):
- Filtrar `pool` donde `difficulty <= sessionDifficultyCap`.
- Ordenar por `difficulty` ascendente y servir en orden hasta agotar, luego reciclar manteniendo el orden.

Para aprendiz en adelante (rango 2+):
- Filtrar por `difficulty <= sessionDifficultyCap` y luego usar el sistema de pesos existente.
- El peso base se calcula como: `weight = (difficulty * 15) + 25`.
- Si hay estadísticas (`attempted >= 3`), el peso se ajusta con el histórico: `weight = max((100 * (1 - correct / attempted)) + (difficulty - 3) * 10, 5)`.

---

## 3. Cambios por archivo

### 3.1 `data/model/Models.kt`

Añadir `difficulty` a `QuestionEntry`:

```kotlin
data class QuestionEntry(
    val enunciado: String,
    val opciones: Map<String, String>,
    val correct: String,
    val weight: Int,
    val testId: String,
    val origId: String,
    val difficulty: Int = 3
)
```

### 3.2 `data/repository/GameRepository.kt`

- En `buildPoolFromTestData`: leer `q.difficulty` y pasarlo a `QuestionEntry`.
- Añadir `getFilteredAndWeightedPool(pool, rankIndex, stats)` que aplica filtros y pesos.
- `startQuickGame`: respetar rango para `maxOptions` y power-ups, manteniendo la lógica de fallos sin respuesta.

### 3.3 `domain/GameEngine.kt`

Añadir propiedades:

```kotlin
var rankIndex: Int = 0
var maxOptions: Int = 4
var maxLives: Int = 3
var maxDifficulty: Int = 5
var sessionDifficultyCap: Int = 1
var availablePowerUps: List<String> = listOf("shield", "doubleScore", "fiftyFifty", "hint")
```

Modificar `initGameStats()`:
- Leer `rankIndex` de `progressRepo.getRankIndex()`.
- Fijar `maxOptions`, `maxLives`, `maxDifficulty`, `availablePowerUps` con las tablas de `Constants`.
- `lives = maxLives`.
- `sessionDifficultyCap = 1`.

Modificar `nextQuestion()`:
- Filtrar `usePool` por `difficulty <= sessionDifficultyCap` y `difficulty <= maxDifficulty`.
- Si rango 0-1, ordenar por `difficulty` ascendente y tomar la primera no preguntada.
- Si rango 2+, aplicar peso combinado.
- Aumentar `sessionDifficultyCap` cada 5 preguntas respondidas.

Modificar `answer()`:
- Actualizar `sessionDifficultyCap` al contestar.

Modificar `activateFiftyFifty()`, `useHint()`, `activateShield()`, `activateDoubleScore()`:
- Verificar `availablePowerUps.contains("...")`.
- Verificar `maxOptions >= 4` para `fiftyFifty` y `hint`.

### 3.4 `domain/ExamEngine.kt`

- Leer `difficulty` en `QuestionEntry`.
- Mantener el peso existente; opcionalmente ordenar/pesar por dificultad en examen/simulacro.

### 3.5 `ui/screens/GameScreen.kt`

- Limitar las opciones visibles según `engine.maxOptions`:
  - Seleccionar siempre la opción correcta.
  - Elegir `maxOptions - 1` distractores aleatorios.
- Ocultar botones de power-ups no disponibles (`!engine.availablePowerUps.contains(...)`).
- Deshabilitar `50/50` y `Pista` cuando `maxOptions < 4`.

### 3.6 `ui/components/GameComponents.kt`

- `AnimatedHudBar`: cambiar `maxLives` fijo (3) por parámetro `maxLives: Int`.
- `HeartIcon` y layout de corazones: soportar 4 o 5 corazones sin desbordar.

### 3.7 `ui/navigation/GameViewModel.kt`

- Al iniciar partida: configurar `engine` según rango.
- Detectar subida de rango: comparar `lastKnownRankIndex` con `currentRankIndex`.
- Si sube, agregar los power-ups de `RANK_POWERUP_REWARDS[currentRankIndex]` a `prefs.getFreePowerUps()`.
- Mostrar `RankUpOverlay` con el nuevo desbloqueo.

### 3.8 `data/repository/ProgressRepository.kt`

- Actualizar `RANKS` y `RANK_UNLOCKS`.
- `getUnlocks()`: devolver misiones y modos según el nuevo rango.
- Añadir `getRankPowerUpGifts(rankIndex): List<String>`.

### 3.9 `data/Constants.kt`

Ver sección 1.2.

### 3.10 `data/repository/MissionRepository.kt`

- `generateDailyMissions`: respetar `missionCount` según rango.
- Misiones de combo/racha ajustan sus objetivos automáticamente porque la rampa de opciones las hace más fáciles.

### 3.11 `domain/AchievementChecker.kt`

- No desbloquear `perfect_game`, `sharpshooter` ni logros de combo (`combo5`, `combo10`, `combo15`, `combo20`, `combo25`) si `maxOptions < 4`. Con 2-3 opciones los combos son triviales/aleatorios y no deben contar como logro.
- Los logros de 100/500/1000 aciertos y leyes dominadas sí cuentan siempre.

### 3.12 `data/local/PreferencesManager.kt`

- Añadir `LAST_KNOWN_RANK_INDEX = "last_known_rank_index"`.
- `getLastKnownRankIndex()` y `setLastKnownRankIndex(index)`.
- Usar este valor para detectar subidas de rango y otorgar regalos una sola vez.

### 3.13 `ui/screens/ModeSelectScreen.kt`

- No requiere cambios de lógica, salvo que los `unlocks` ahora provienen del nuevo `getUnlocks()`.

### 3.14 `data/local/DataProvider.kt`

- No requiere cambios, ya que Gson parsea el campo `difficulty` automáticamente si existe.
- Asegurar que `TestData.Question` tenga `val difficulty: Int`.

### 3.15 `data/model/Models.kt` (clase Question)

Si aún no existe:

```kotlin
data class Question(
    val id: Int = 0,
    val test_id: String = "",
    val orig_id: Int = 0,
    val enunciado: String = "",
    val opciones: Map<String, String> = emptyMap(),
    val difficulty: Int = 3
)
```

---

## 4. Migración de usuarios existentes

### 4.1 Recálculo de rango

Al arrancar la app, el rango actual se recalcula con el nuevo `Constants.RANKS` a partir del XP guardado. Un usuario con XP 5000 pasará a ser rango 4 (Avanzado).

### 4.2 Regalo de power-ups por subida de rango

- Al iniciar sesión, comparar `lastKnownRankIndex` con `currentRankIndex`.
- Para cada rango nuevo alcanzado, agregar los power-ups de `RANK_POWERUP_REWARDS`.
- Actualizar `lastKnownRankIndex = currentRankIndex`.

### 4.3 Usuarios ya graduados

Un usuario en rango 8 no recibe regalos adicionales si ya estaba en 8. Empieza con todos los power-ups según `AVAILABLE_POWERUPS_BY_RANK`.

---

## 5. Flujo de una partida de Supervivencia

1. Usuario pulsa Supervivencia.
2. `GameViewModel` lee `rankIndex` y configura `engine.maxOptions`, `engine.maxLives`, `engine.maxDifficulty`, `engine.availablePowerUps`.
3. `GameEngine.initGameStats()` inicia `lives`, `sessionDifficultyCap = 1`, `askedIds.clear()`.
4. `GameRepository` construye el pool leyendo `difficulty` del JSON.
5. `GameEngine.nextQuestion()` filtra por dificultad y selecciona.
6. `GameScreen` muestra solo `maxOptions` opciones.
7. Al acertar/fallar, `GameEngine.answer()` actualiza `sessionDifficultyCap` si toca.
8. Al terminar, `MissionRepository` y `AchievementChecker` verifican progreso respetando `maxOptions`.

---

## 6. Orden de implementación recomendado

1. Actualizar `data/Constants.kt` (rangos, desbloqueos, tablas de mecánicas).
2. Actualizar `data/model/Models.kt` (`Question.difficulty`, `QuestionEntry.difficulty`).
3. Actualizar `data/repository/GameRepository.kt` para leer `difficulty`.
4. Implementar `GameEngine`: `initGameStats` con rango, `nextQuestion` filtrado/ponderado, power-ups disponibles.
5. Adaptar `ui/screens/GameScreen.kt` para `maxOptions` y ocultar power-ups no disponibles.
6. Adaptar `ui/components/GameComponents.kt` para `maxLives` variable.
7. Implementar regalos de power-ups en `GameViewModel` y `ProgressRepository`.
8. Actualizar `PreferencesManager` con `lastKnownRankIndex`.
9. Ajustar `MissionRepository` y `AchievementChecker`.
10. Probar flujos en rangos 0, 1, 2, 3, 8.

---

## 7. Preguntas pendientes de decisión

- ¿Mostrar un tutorial/toast al desbloquear cada mecánica (3 opciones, 4 opciones, Contrarreloj, etc.)?
- ¿En Contrarreloj se mantiene la rampa (corazones/power-ups por rango) o usa el modo completo directamente?
- ¿En Repaso Express se aplica `maxOptions` del rango o siempre 4 opciones?
- ¿El peso base `difficulty * 15 + 25` (rango 40-100) es correcto o se prefiere otra escala?
