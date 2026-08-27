# Guia: Cuestionarios "App content" en Play Console

Google Play exige completar varios cuestionarios sobre el contenido y los datos
de la app antes de poder publicar. Esta guia te dice exactamente que responder
para OpoLeyes.

(Play Console → Selecciona tu app → App content)

---

## 1. Privacy Policy (Politica de privacidad)

- URL: https://josearquillo.github.io/opoleyes-privacy/
- (Ya la tienes lista)

---

## 2. Data safety (Seguridad de los datos)

Pantalla: "Data safety" → Start

### Pregunta 1: "Does your app collect or share any of the required user data types?"
- **NO** (la app no recopila datos personales; el progreso se guarda localmente)

### Pregunta 2: "Is all of the user data collected by your app encrypted in transit?"
- (Si marcaste NO arriba, esta no aparece)

### Pregunta 3: "Do you provide a way for users to request that their data is deleted?"
- (Si marcaste NO arriba, esta no aparece)

> NOTA: Aunque la app integra el SDK de AdMob, en la v1.0 los anuncios estan
> desactivados (ADS_ENABLED=false). Por eso puedes decir que no recopila datos.
> Si en el futuro activas anuncios reales, tendras que actualizar este cuestionario
> indicando que el SDK de AdMob recopila identificadores publicitarios e IP.

### Resumen final que veras:
- "No data collected" / "No data shared"
- (Review y Submit)

---

## 3. Content rating (Clasificacion de contenido)

Pantalla: "Content rating" → Start questionnaire

- **App category**: Educational / Quiz game (segun te aparezca)
- Responde el cuestionario IARC:

| Pregunta | Respuesta |
|----------|-----------|
| Does your app contain cartoon violence? | No |
| Does your app contain realistic violence? | No |
| Does your app contain sexual content? | No |
| Does your app contain nudity? | No |
| Does your app contain profanity or crude humor? | No |
| Does your app contain fear/horror? | No |
| Does your app contain drugs, tobacco or alcohol references? | No |
| Does your app contain gambling? | No |
| Does your app contain user-generated content (chat, sharing)? | No |
| Does your app contain in-app purchases? | No |
| Does your app show ads? | Yes (AdMob) |
| Are the ads contextual or personalized? | Contextual (mas seguro) |
| Does your app collect personal info for ads? | No |
| Is your app directed to children under 13? | No |

Resultado esperado: **"Everyone" / "Todos los publicos"** (PEGI 3 / ESRB E)

---

## 4. Target audience (Publico objetivo)

Pantalla: "Target audience" → Start

- **Target age group**: 13 and under? NO → selecciona **"13 - 17"** o **"18 and over"**
  (Recomendado: **"18 and over"** porque es un examen de oposiciones, aunque la app
  no tiene contenido inapropiado. Si prefieres "All ages" tambien es valido).
- **Is your app directed to children?** No
- (Google te puede pedir que anadas una politica de privacidad especifica para
  ninos si dices que va dirigida a menores; por eso es mas simple decir 18+)

---

## 5. Ads (Anuncios)

Pantalla: "Ads" → Start

- **Does your app contain ads?** Yes
- **What ad SDKs do you use?** Google AdMob
- **Do you use contextual or personalized ads?** Contextual
- **Do you collect device identifiers for ads?** Yes (AdMob lo hace, aunque en
  la v1.0 este desactivado, el SDK esta integrado)

> Si te resulta mas simple, puedes marcar "No" en "Does your app contain ads?"
> ya que en la v1.0 no se muestran anuncios. Pero si planeas activarlos pronto,
> es mejor decir "Yes" desde el principio para no tener que volver a pasar el
> cuestionario.

---

## 6. Government apps / Financial features / Privacy shortcuts

Estas secciones suelen aparecer pero para OpoLeyes:

- **Government apps**: No es una app gubernamental → Skip / "Not applicable"
- **Financial features**: No tiene funcionalidades financieras → Skip
- **Privacy shortcuts**: No aplica → Skip

---

## Resumen del estado de "App content"

| Seccion | Estado |
|---------|--------|
| Privacy Policy | ✅ URL lista |
| Data safety | ✅ "No data collected" |
| Content rating | ✅ "Everyone" |
| Target audience | ✅ 18+ |
| Ads | ✅ AdMob (contextual) |
