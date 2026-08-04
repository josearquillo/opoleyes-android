# Investigación para la integración de niveles de dificultad en OpoLeyes

## Contexto

Una vez que `data.json` tenga el campo `difficulty: 1-5` en cada pregunta, hay que integrarlo en la app. Este documento lista todo lo que hay que investigar y decidir antes de escribir código, para no romper sistemas existentes.

---

## 1. Detección de usuario novato

### Qué investigar
- ¿Cómo saber si un usuario es novato? Actualmente `StatsRepository.getTotalCorrect()` y `getTotalWrong()` dan el total acumulado.
- ¿Qué umbral usar? Propuesta inicial: `totalCorrect + totalWrong < 30` = novato.
- ¿El umbral debería ser por partidas jugadas en vez de preguntas respondidas? `progressRepo.getGamesPlayed()` ya existe.
- ¿Qué pasa si un usuario borra datos o reinstala? Vuelve a ser novato — ¿es eso deseable?

### Archivos relevantes
- `StatsRepository.kt` — `getTotalCorrect()`, `getTotalWrong()`
- `ProgressRepository.kt` — `getGamesPlayed()`, `getRankIndex()`
- `GameEngine.kt` — `startXP`, `startRankIndex` (ya se captura estado inicial)

### Decisión pendiente
- Definir la función `isNovice(): Boolean` y dónde vivirá (¿`GameEngine`? ¿`GameViewModel`? ¿`ProgressRepository`?)

---

## 2. Selección de preguntas por dificultad

### Qué investigar
- `GameEngine.nextQuestion()` (línea 149) selecciona preguntas por peso (`weight`). ¿Cómo insertar lógica de dificultad sin romper el sistema de pesos?
- Si el usuario es novato, ¿ordenar el pool por `difficulty` ascendente y servir en orden? ¿O usar `difficulty` como peso inverso (las fáciles tienen más probabilidad de salir)?
- ¿Qué pasa cuando se agotan las preguntas fáciles del pool? ¿Pasa a las siguientes en orden de dificultad?
- El pool ya se reutiliza cuando se agotan las preguntas (`askedIds.clear()`). ¿Cómo interactúa la dificultad con este reciclaje?
- ¿La selección por dificultad aplica solo a Supervivencia, o también a Contrarreloj y Repaso Express?

### Archivos relevantes
- `GameEngine.kt:149-180` — `nextQuestion()`, selección por peso
- `GameRepository.kt:15-35` — `buildPoolFromTestData()`, asignación de `weight`
- `GameRepository.kt:52-87` — `startQuickGame()`, lógica específica de Quick

### Decisión pendiente
- Definir el algoritmo de selección para novatos:
  - **Opción A:** ordenar pool por `difficulty` asc, servir en orden (determinista)
  - **Opción B:** `weight = (6 - difficulty) * 20` para novatos (probabilístico, las fáciles salen más pero no siempre)
  - **Opción C:** híbrido — primeras 10 preguntas en orden de dificultad, resto por peso

---

## 3. Rampa de opciones (2 → 3 → 4)

### Qué investigar
- `GameScreen.kt:279-320` muestra las opciones. `presentLetters` se filtra por `q.opciones[it] != null`. ¿Cómo reducir a 2/3 opciones sin que el `fiftyFifty` o el `hint` se rompan?
- Si muestro solo 2 opciones (correcta + 1 incorrecta aleatoria), ¿qué pasa con el `hint`? El hint elimina 1 incorrecta — si solo hay 2, el hint revela la respuesta directamente.
- ¿Qué pasa con el `fiftyFifty`? Elimina 2 incorrectas — si solo hay 2 opciones, no tiene sentido.
- ¿La rampa cuenta aciertos de la sesión actual o aciertos totales? Si es totales, un usuario que ya tiene 30 aciertos nunca vería la rampa.
- ¿La rampa aplica solo a Supervivencia o a todos los modos?
- ¿Qué ocurre si el usuario falla durante la rampa? ¿Baja de nivel (de 3 opciones a 2)?

### Archivos relevantes
- `GameScreen.kt:279-320` — renderizado de opciones, `presentLetters`, `shuffledLetters`
- `GameEngine.kt` — `fiftyFiftyActive`, `fiftyFiftyRemoved`, `hintActive`, `hintRemoved`
- `GameViewModel.kt` — `activateFiftyFifty()`, `useHint()`

### Decisión pendiente
- Definir los tramos de la rampa y si los power-ups se deshabilitan durante los tramos bajos
- Decidir si la rampa es por sesión o por stats totales

---

## 4. Vidas adicionales para novatos

### Qué investigar
- `GameEngine.initGameStats()` (línea 79) fija `lives = 3` para Supervivencia. ¿Cómo dar 5 vidas a un novato sin afectar a usuarios normales?
- `GameEngine.answer()` (rama WRONG, línea 248) resta una vida. ¿Debería haber un "período de gracia" donde las primeras N preguntas no resten vida?
- ¿Cómo se muestra el número de vidas en la UI? `GameComponents.kt` renderiza los corazones — ¿se renderizan 5 corazones sin problema?
- ¿El HUD (`LivesDisplay`) tiene layout fijo para 3 corazones o es dinámico?

### Archivos relevantes
- `GameEngine.kt:79-114` — `initGameStats()`, asignación de vidas
- `GameEngine.kt:248-256` — rama WRONG, resta de vida
- `GameComponents.kt:200-380` — `LivesDisplay`, `HeartIcon`, renderizado de corazones

### Decisión pendiente
- ¿5 vidas fijas para novatos, o 3 vidas + período de gracia (no pierde en las primeras 5 preguntas)?
- ¿Cómo se gradúa el número de vidas? ¿Pasa de 5 a 3 de golpe o gradualmente (5 → 4 → 3)?

---

## 5. Sistema híbrido de pesos (difficulty + stats)

### Qué investigar
- `GameRepository.buildPoolFromTestData()` (línea 21-25) calcula `weight` basándose en stats: `weight = 100 * (1 - correct/attempted)`. ¿Cómo combinar esto con `difficulty`?
- Propuesta: `weight = (difficulty * 20) + ajuste_por_stats`. Pero ¿qué fórmula exacta? ¿Es aditivo o multiplicativo?
- ¿El `difficulty` afecta al peso desde el principio, o solo cuando no hay stats suficientes (`attempted < 3`)?
- ¿Qué pasa cuando un usuario ha respondido una pregunta 10 veces? ¿El `difficulty` sigue influyendo o los stats reales lo eclipsan?
- El modo Quick (`startQuickGame`) tiene su propia lógica de pool (wrongPool, unansweredPool, correctPool). ¿Cómo interactúa `difficulty` con esta separación?

### Archivos relevantes
- `GameRepository.kt:15-35` — `buildPoolFromTestData()`, cálculo de `weight`
- `GameRepository.kt:52-87` — `startQuickGame()`, lógica de pools separados
- `StatsRepository.kt:31-38` — `getWeight()`, cálculo de peso por stats
- `GameEngine.kt:149-171` — `nextQuestion()`, selección por peso

### Decisión pendiente
- Definir la fórmula exacta del peso híbrido
- Decidir si `difficulty` influye siempre o solo cuando no hay stats suficientes

---

## 6. Impacto en modos de juego existentes

### Qué investigar
- **Supervivencia:** es el modo principal donde aplicaría la dificultad + rampa. ¿Cambio `startTemaGame` y `startAllLawsGame`?
- **Contrarreloj:** ¿debería tener dificultad para novatos? El timer añade presión — quizás no aplicar rampa aquí.
- **Repaso Express:** ya tiene su propia lógica de pool (prioriza fallos). ¿La dificultad cambia algo?
- **Examen:** modo serio, sin rampa ni ayudas. ¿La dificultad solo afecta al orden de preguntas?
- **Simulacro:** idem, modo serio. ¿Sin dificultad?

### Archivos relevantes
- `GameEngine.kt:116-147` — `startQuickGame()`, `startTemaGame()`, `startAllLawsGame()`
- `ExamEngine.kt` — `loadExam()`, `loadSimulacro()`
- `ModeSelectScreen.kt:47-51` — definición de modos y desbloqueo

### Decisión pendiente
- ¿La dificultad + rampa aplica solo a Supervivencia, o también a otros modos?
- ¿El Examen/Simulacro usan `difficulty` para ordenar preguntas aunque sin rampa de opciones?

---

## 7. Impacto en XP y puntuación

### Qué investigar
- `GameEngine.kt:209` calcula XP por acierto: `progressRepo.addXP(pts * xpMultiplier)` donde `pts = 10 * combo`. ¿Debería la XP depender de la dificultad? ¿Acertar una pregunta dificultad 5 da más XP que una dificultad 1?
- Si un novato acierta preguntas fáciles (difficulty 1), ¿gana la misma XP que un usuario normal acertando preguntas difíciles? ¿Es justo?
- ¿La puntuación (score) debería reflejar la dificultad? Actualmente `pts = 10 * combo` (o `15 * combo` en Quick).
- ¿El `xpFromCorrect` del breakdown que implementamos se ve afectado?

### Archivos relevantes
- `GameEngine.kt:205-209` — cálculo de `pts` y `addXP`
- `GameViewModel.kt` — `buildGameXpBreakdown()`, `xpFromCorrect`

### Decisión pendiente
- ¿La XP por acierto incluye un multiplicador por dificultad? (ej: `pts = 10 * combo * difficulty / 3`)
- ¿O la dificultad solo afecta al orden de preguntas, no a la recompensa?

---

## 8. Impacto en misiones diarias

### Qué investigar
- `MissionRepository.checkOnGameOver()` verifica misiones basadas en `maxCombo`, `totalAnswered`, `correctCount`, `score`. Si un novato juega con 2 opciones, ¿sus aciertos cuentan igual para misiones?
- La misión "Llega a combo x10" es más fácil con 2 opciones (50% de probabilidad al azar). ¿Es un problema?
- ¿Deberían las misiones de novatos ser distintas o más fáciles?

### Archivos relevantes
- `MissionRepository.kt:200-224` — `checkOnGameOver()`
- `MissionRepository.kt:30-169` — `generateDailyMissions()`, generación según rango

### Decisión pendiente
- ¿Las misiones cuentan igual durante la rampa de novato, o se ajustan?
- ¿Se generan misiones más fáciles para novatos?

---

## 9. Impacto en logros (achievements)

### Qué investigar
- `AchievementChecker.kt` verifica logros como "combo x10", "combo x20", "100% acierto (mín 10 preguntas)". Con 2 opciones, estos logros son trivialmente fáciles.
- ¿El logro "Diana perfecta: 100% acierto mín 10 preguntas" debería requerir modo normal (4 opciones)?

### Archivos relevantes
- `AchievementChecker.kt` — `checkPerQuestion()`, `checkGameOver()`
- `Constants.kt:17-47` — lista de logros

### Decisión pendiente
- ¿Los logros se deshabilitan durante la rampa de novato, o se mantienen?
- ¿Se añade un flag `earnedInNoviceMode` para distinguir?

---

## 10. UI: indicación visual de dificultad

### Qué investigar
- ¿Debería la app mostrar la dificultad de la pregunta actual al usuario? (ej: un indicador "Fácil" / "Media" / "Difícil" junto a la pregunta)
- ¿O la dificultad es invisible para el usuario (solo afecta al orden, sin indicación visual)?
- Si se muestra, ¿dónde? ¿En la tarjeta de pregunta? ¿Como color del borde?
- ¿Podría ser contraproducente mostrar "Fácil" y que el usuario falle (se siente tonto)?

### Archivos relevantes
- `GameScreen.kt:242-251` — tarjeta de pregunta
- `Theme.kt` — colores disponibles

### Decisión pendiente
- ¿Mostrar o no la dificultad al usuario?
- Si se muestra, ¿cómo y dónde?

---

## 11. Graduación del modo novato

### Qué investigar
- ¿Cuándo deja un usuario de ser novato? Propuesta: `totalCorrect + totalWrong >= 30`.
- ¿La transición es instantánea (de 2 opciones a 4) o gradual (2 → 3 → 4)?
- ¿Se informa al usuario de que ha "graduado"? ¿Un toast o popup?
- ¿Puede un usuario volver al modo novato? (ej: si borra stats)

### Archivos relevantes
- `StatsRepository.kt:78-84` — `getTotalCorrect()`, `getTotalWrong()`
- `GameEngine.kt:79-95` — `initGameStats()`, donde se decidiría el modo

### Decisión pendiente
- Definir el umbral exacto de graduación
- Definir si la transición es instantánea o gradual
- Decidir si se informa al usuario

---

## 12. Persistencia y configuración

### Qué investigar
- ¿El modo novato es automático (detectado por stats) o configurable (el usuario elige)?
- ¿Debería haber un toggle en ajustes "Modo iniciación" que el usuario pueda activar/desactivar manualmente?
- ¿Cómo se guarda la preferencia? `PreferencesManager` ya guarda settings.
- ¿El `difficulty` del JSON se lee una sola vez al cargar el pool, o se relee?

### Archivos relevantes
- `PreferencesManager.kt` — persistencia de settings
- `DataProvider.kt` — carga del JSON
- `GameRepository.kt` — construcción del pool

### Decisión pendiente
- ¿Automático, manual, o ambos?
- ¿Se añade un toggle en la UI de ajustes?

---

## Resumen de decisiones pendientes

| # | Tema | Decisión principal |
|---|---|---|
| 1 | Detección novato | Umbral y función `isNovice()` |
| 2 | Selección por dificultad | Algoritmo (orden, peso inverso, o híbrido) |
| 3 | Rampa de opciones | Tramos, interacción con power-ups, por sesión o total |
| 4 | Vidas adicionales | 5 fijas, o 3 + período de gracia |
| 5 | Pesos híbridos | Fórmula exacta de `weight` con difficulty + stats |
| 6 | Modos afectados | Solo Supervivencia, o también otros |
| 7 | XP y puntuación | ¿Difficulty multiplica XP? |
| 8 | Misiones | ¿Cuentan igual durante rampa? |
| 9 | Logros | ¿Se deshabilitan durante rampa? |
| 10 | UI de dificultad | ¿Mostrar al usuario o invisible? |
| 11 | Graduación | Umbral, transición, aviso al usuario |
| 12 | Persistencia | Automático, manual, o ambos |

---

## Orden recomendado de investigación

1. **Decisión 1 + 11** (detección de novato y graduación) — define quién es novato y cuándo deja de serlo
2. **Decisión 2 + 5** (selección y pesos) — cómo se usan los `difficulty` en el engine
3. **Decisión 3 + 4** (rampa y vidas) — la experiencia del novato durante la partida
4. **Decisión 6** (modos afectados) — alcance
5. **Decisiones 7, 8, 9** (XP, misiones, logros) — impacto en sistemas de recompensa
6. **Decisión 10 + 12** (UI y persistencia) — presentación y configuración

Cada decisión debería tomarse tras leer el código relevante y entender las interacciones. No todas tienen la misma prioridad — las decisiones 1-4 son las que definen la experiencia del novato; el resto son refinamientos.
