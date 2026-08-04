# Plan de etiquetado de dificultad para las preguntas de OpoLeyes

## Objetivo

Asignar a cada una de las 7.087 preguntas del `data.json` un campo `difficulty` con valor 1-5, razonando la dificultad desde la perspectiva de un opositor de Justicia. Este campo será usado por la app para servir preguntas más fáciles primero a los usuarios novatos, mejorando su experiencia de onboarding y reduciendo la frustración inicial.

## Estado actual

- **Archivo:** `app/src/main/assets/data.json`
- **Tamaño:** ~4,3 MB
- **Tests:** 66
- **Preguntas totales:** 7.087
- **Estructura actual de cada pregunta:**
  ```json
  {
    "id": 1,
    "test_id": "JUSTICIA__Examenes por temas__Tema_N01",
    "orig_id": 1,
    "enunciado": "La Soberanía Nacional reside en:",
    "opciones": {
      "A": "La Monarquía Parlamentaria",
      "B": "Las Cortes Generales",
      "C": "El Gobierno de la Nación",
      "D": "El pueblo español"
    }
  }
  ```
- **Respuestas correctas:** en el campo `answers` de cada test, separado de las preguntas:
  ```json
  {
    "id": 1,
    "correct": "D"
  }
  ```

## Estructura objetivo

Cada pregunta debe quedar así:

```json
{
  "id": 1,
  "test_id": "JUSTICIA__Examenes por temas__Tema_N01",
  "orig_id": 1,
  "enunciado": "La Soberanía Nacional reside en:",
  "opciones": {
    "A": "La Monarquía Parlamentaria",
    "B": "Las Cortes Generales",
    "C": "El Gobierno de la Nación",
    "D": "El pueblo español"
  },
  "difficulty": 1
}
```

El campo `difficulty` es un entero de 1 a 5. El resto del JSON debe quedar idéntico al original — mismo orden, mismos campos, mismos valores. Solo se añade `difficulty`.

---

## Escala de dificultad

| Nivel | Nombre | Criterio | Ejemplo |
|---|---|---|---|
| **1** | Muy fácil | Se acierta con conocimiento general o asociación directa reconocible. El enunciado es prácticamente la definición literal del concepto. Los distractores son claramente incorrectos o figuras distintas. | "La Soberanía Nacional reside en: el pueblo español" (art. 1.2 CE) |
| **2** | Fácil | Se acierta con conocimiento básico del temario. Al menos 2 distractores se eliminan fácilmente por lógica o por corresponder a figuras/órganos distintos. La respuesta correcta es deducible con razonamiento institucional básico. | "Los presupuestos de las Cortes Generales son aprobados por: Las Cortes Generales" |
| **3** | Media | Requiere conocimiento específico del tema. La duda real está entre 2 opciones. Los distractores restantes son eliminables pero la distinción final exige saber el concepto o la sistemática. | "El derecho de huelga: derecho fundamental Sección 1ª vs Sección 2ª" |
| **4** | Difícil | Las opciones son casi idénticas, diferenciándose en una sola palabra o frase. Requiere memorización literal del artículo o norma. No se puede deducir sin conocer el texto exacto. | "Art. 71: inviolabilidad por opiniones 'en el ejercicio de sus funciones' vs 'durante su mandato'" |
| **5** | Muy difícil | Requiere distinguir matices jurisprudenciales, reformas legislativas recientes, excepciones muy específicas, o plazos numéricos exactos que la mayoría de opositores fallan. La diferencia entre opciones es mínima y solo se conoce con estudio profundo. | "Plazo exacto para recurso de apelación contra auto de sobreseimiento conforme al art. 790.1 LECrim" |

### Criterios adicionales para la valoración

Al razonar la dificultad de cada pregunta, considerar:

1. **Distinguibilidad de la respuesta correcta:** ¿qué tan diferente es la opción correcta de los distractores? Si todas las opciones son plausibles, la dificultad sube.

2. **Conocimiento requerido:** ¿es conocimiento general (concepto básico de Constitución), conocimiento del temario (artículo específico), o conocimiento detallado (matiz jurisprudencial, plazo exacto, reforma reciente)?

3. **Trampas habituales:** ¿hay opciones diseñadas para confundir al opositor? ¿hay negaciones ("señale cuál NO es correcto")? ¿hay opciones que son casi idénticas entre sí?

4. **Especificidad:** ¿la pregunta pide un concepto amplio (fácil de retener) o un detalle concreto (plazo, artículo exacto, número)?

5. **Distractores:** ¿son figuras claramente distintas (fácil de eliminar) o son variaciones del mismo concepto (difícil de distinguir)?

### Lo que NO debe influir en la valoración

- **Longitud del enunciado:** una pregunta corta no es necesariamente fácil ni una larga es necesariamente difícil.
- **Longitud de las opciones:** no es un indicador fiable.
- **Número de tema:** el tema 1 no es siempre más fácil que el tema 30. Cada pregunta se valora individualmente.
- **Patrones automáticos:** no se aplican reglas como "si contiene 'no' es más difícil". El razonamiento es cualitativo, pregunta por pregunta.

---

## Proceso de ejecución

### Fase 1: Preparación

1. **Backup del data.json original:**
   ```
   copiar app/src/main/assets/data.json a data.json.backup
   ```

2. **Script de división (`split_questions.py`):**
   - Lee `data.json`
   - Extrae todas las preguntas de todos los tests, manteniendo la referencia a qué test pertenecen y su índice dentro del test
   - Para cada pregunta, junta el enunciado, las 4 opciones y la respuesta correcta (extraída del campo `answers` del test correspondiente)
   - Divide las 7.087 preguntas en lotes de ~500 preguntas
   - Escribe cada lote en un archivo `batch_XX.json` dentro de una carpeta `batches/`
   - Genera un archivo `batches/index.json` con el mapeo: `{ lote, test_id, índice_pregunta, id_pregunta }` para poder reconstruir el JSON final

   **Formato de cada lote (`batch_01.json`):**
   ```json
   [
     {
       "batch": 1,
       "test_index": 0,
       "question_index": 0,
       "id": 1,
       "test_id": "JUSTICIA__Examenes por temas__Tema_N01",
       "orig_id": 1,
       "enunciado": "La Soberanía Nacional reside en:",
       "opciones": {
         "A": "La Monarquía Parlamentaria",
         "B": "Las Cortes Generales",
         "C": "El Gobierno de la Nación",
         "D": "El pueblo español"
       },
       "correct": "D"
     },
     ...
   ]
   ```

3. **Verificación:**
   - Confirmar que la suma de preguntas en todos los lotes = 7.087
   - Confirmar que cada pregunta tiene su respuesta correcta adjunta
   - Confirmar que el mapeo `index.json` cubre todas las preguntas

### Fase 2: Procesamiento con subagentes

4. **Lanzar subagentes en paralelo:**
   - Un subagent por lote (~14 subagentes para 7.087 preguntas / 500 por lote)
   - Cada subagent es una IA que lee su lote, razona la dificultad de cada pregunta y escribe el resultado

5. **Prompt estándar para cada subagent:**
   ```
   Eres un opositor experimentado a la Administración de Justicia en España.
   Tu tarea es asignar un nivel de dificultad (1-5) a cada pregunta del archivo
   batch_XX.json.

   Para cada pregunta:
   1. Lee el enunciado, las 4 opciones y la respuesta correcta
   2. Razona: ¿qué concepto jurídico evalúa? ¿qué tan distinguible es la
      respuesta correcta de los distractores? ¿requiere memorización literal
      o se puede deducir? ¿hay trampas con negaciones o opciones casi idénticas?
   3. Asigna dificultad 1-5 según la escala:

      1 = Muy fácil: conocimiento general o asociación directa reconocible
      2 = Fácil: conocimiento básico, 2+ distractores eliminables fácil
      3 = Media: conocimiento específico, duda entre 2 opciones
      4 = Difícil: opciones casi idénticas, requiere memorización literal
      5 = Muy difícil: matices jurisprudenciales, plazos exactos, excepciones

   No uses la longitud del enunciado ni patrones automáticos. Razona cualitativamente.

   Escribe el resultado en batch_XX_result.json con este formato:
   [
     {
       "id": 1,
       "test_id": "JUSTICIA__Examenes por temas__Tema_N01",
       "orig_id": 1,
       "difficulty": 1
     },
     ...
   ]

   Solo incluye id, test_id, orig_id y difficulty. No modifiques ningún otro campo.
   ```

6. **Salida de cada subagent:**
   - Archivo `batch_XX_result.json` con `{ id, test_id, orig_id, difficulty }` por pregunta
   - Un lote de 500 preguntas genera ~500 entradas

### Fase 3: Reconstrucción

7. **Script de reconstrucción (`merge_difficulty.py`):**
   - Lee el `data.json` original
   - Lee todos los `batch_XX_result.json`
   - Construye un mapa: `{ (test_id, orig_id) -> difficulty }`
   - Para cada pregunta en el `data.json` original, añade el campo `difficulty` basándose en el mapa
   - Si una pregunta no tiene dificultad asignada (no debería ocurrir), asigna `difficulty: 3` por defecto y la marca en un log
   - Escribe el resultado en `data.json` (sobreescribiendo el original, que ya tiene backup)
   - Mantiene el resto del JSON idéntico: mismo orden de tests, mismo orden de preguntas, mismos campos, mismos valores. Solo se añade `difficulty` a cada pregunta

8. **Verificación final:**
   - Contar preguntas con `difficulty` asignada → debe ser 7.087
   - Distribución de dificultades (debe ser razonable, no todas iguales):
     ```
     python verify_difficulty.py
     # Salida esperada:
     # Difficulty 1: ~800-1200 preguntas
     # Difficulty 2: ~1800-2500 preguntas
     # Difficulty 3: ~2000-2800 preguntas
     # Difficulty 4: ~1200-1800 preguntas
     # Difficulty 5: ~400-800 preguntas
     ```
   - Verificar que el JSON es válido: `python -c "import json; json.load(open('data.json'))"`
   - Verificar que el tamaño del archivo es similar al original + ~30KB (7.087 × ~4 bytes por `"difficulty": N,`)

### Fase 4: Limpieza

9. **Eliminar archivos temporales:**
   - `batches/` (carpeta con lotes y resultados)
   - `split_questions.py`, `merge_difficulty.py`, `verify_difficulty.py`
   - `data.json.backup` (solo tras verificar que todo está correcto)

10. **Commit:**
    ```
    git add app/src/main/assets/data.json
    git commit -m "Add difficulty field (1-5) to all 7087 questions"
    ```

---

## Integración en la app (fase posterior, separada)

Una vez el `data.json` tiene el campo `difficulty`, la integración en la app es independiente:

### 1. Modelo de datos (`Models.kt`)
```kotlin
data class QuestionEntry(
    val enunciado: String,
    val opciones: Map<String, String>,
    val correct: String,
    val weight: Int,
    val testId: String,
    val origId: String,
    val difficulty: Int = 3  // nuevo campo, default 3 = media
)
```

### 2. Lectura del JSON (`GameRepository.kt`)
En `buildPoolFromTestData`, leer el campo `difficulty` del JSON y pasarlo al `QuestionEntry`.

### 3. Selección de preguntas para novatos (`GameEngine.kt`)
En `nextQuestion()`, si el usuario es novato (`totalCorrect + totalWrong < 30`):
- Ordenar el pool por `difficulty` ascendente
- Servir las preguntas más fáciles primero
- El sistema de pesos adaptativo sigue funcionando por encima: una pregunta fácil que se falla mucho sube su peso

### 4. Rampa de opciones (UI, `GameScreen.kt`)
Independiente del `difficulty`, pero complementario:
- Novato (0-5 aciertos): mostrar 2 opciones (correcta + 1 aleatoria incorrecta)
- Intermedio (5-15): mostrar 3 opciones
- Avanzado (15-30): mostrar 4 opciones
- Graduado (30+): modo normal

### 5. Sistema híbrido de pesos
La fórmula final del peso de una pregunta combina dificultad semilla + stats reales:
```
weight = (difficulty * 20) + ajuste_por_stats
```
- Pregunta dificultad 1: peso base 20
- Pregunta dificultad 5: peso base 100
- Los stats reales (aciertos/fallos) ajustan el peso por encima de la base

Con el tiempo, los datos reales de usuarios superan a la semilla, pero desde el primer día el novato recibe preguntas fáciles.

---

## Consideraciones

### Precisión esperada
- **Constitución y temas básicos:** ~85-90% de precisión
- **Derecho Administrativo, Civil, Laboral:** ~75-80%
- **Derecho Penal, Procesal, legislación específica (LOPJ, LECrim):** ~70-75%
- **Global estimado:** ~75-80%

Esta precisión es suficiente porque:
1. El sistema actual es 0% (todas las preguntas con peso 50 = aleatorio)
2. El sistema adaptativo refina con datos reales de uso
3. La rampa de opciones garantiza que el novato acierte aunque la valoración sea imperfecta
4. Solo afecta a novatos en sus primeras partidas; después el sistema adaptativo toma el relevo

### Inconsistencia entre subagentes
Cada subagent puede aplicar criterios ligeramente distintos. Para mitigar:
- El prompt estándar define la escala con ejemplos concretos
- La escala 1-5 es suficientemente granular para tolerar variaciones menores
- Un 2 de un subagent y un 3 de otro no rompen el sistema — ambas son "fácil/media"

### Preguntas sin respuesta correcta
Si una pregunta no tiene su respuesta en el campo `answers` del test, no se puede razonar la dificultad. En ese caso:
- Asignar `difficulty: 3` por defecto
- Marcarla en un log para revisión manual posterior
- El script de división debe detectar esto y reportar cuántas preguntas carecen de respuesta

### Preservación del JSON original
El `data.json` resultante debe ser idéntico al original excepto por el campo `difficulty` añadido a cada pregunta. Para verificar:
- Mismo número de tests (66)
- Mismo número de preguntas (7.087)
- Mismo orden de tests y preguntas
- Mismos campos en cada pregunta (id, test_id, orig_id, enunciado, opciones) + difficulty
- Mismas respuestas en el campo `answers` de cada test
- JSON válido

---

## Archivos a crear

| Archivo | Propósito | Destino final |
|---|---|---|
| `split_questions.py` | Divide data.json en lotes | Eliminar tras merge |
| `batches/batch_XX.json` | Lotes de preguntas para procesar | Eliminar tras merge |
| `batches/batch_XX_result.json` | Resultados de cada subagent | Eliminar tras merge |
| `batches/index.json` | Mapeo para reconstrucción | Eliminar tras merge |
| `merge_difficulty.py` | Junta resultados en data.json | Eliminar tras merge |
| `verify_difficulty.py` | Verifica distribución y validez | Eliminar tras merge |
| `data.json.backup` | Backup del original | Eliminar tras verificar |

## Archivo modificado

| Archivo | Cambio |
|---|---|
| `app/src/main/assets/data.json` | Añadir `difficulty: 1-5` a cada pregunta |

---

## Resumen del flujo

```
data.json (original, 7087 preguntas sin difficulty)
        │
        ▼
split_questions.py
        │
        ├── batches/batch_01.json (500 preguntas + respuestas correctas)
        ├── batches/batch_02.json (500 preguntas + respuestas correctas)
        ├── ...
        └── batches/batch_15.json (87 preguntas + respuestas correctas)
        │
        ▼
14 subagentes en paralelo (cada uno razona su lote)
        │
        ├── batches/batch_01_result.json (500 difficulties)
        ├── batches/batch_02_result.json (500 difficulties)
        ├── ...
        └── batches/batch_15_result.json (87 difficulties)
        │
        ▼
merge_difficulty.py
        │
        ▼
data.json (actualizado, 7087 preguntas CON difficulty 1-5)
        │
        ▼
verify_difficulty.py
        │
        ▼
Commit + limpieza de archivos temporales
```
