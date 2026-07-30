# Cambios pendientes

## BUGS

### 1. Pantalla de carga de juegos: bloquear selección durante carga ✅
- Al elegir un modo de juego, mientras se cargan las preguntas se pueden pulsar otros modos.
- **Fix**: Deshabilitar los botones de selección de modo mientras `isLoading` sea `true`. Añadir overlay o `enabled = false` en los botones.

## CAMBIOS EN LA FUNCIONALIDAD

### 2. Escudo se activa como cualquier otro power-up ✅
- Actualmente el escudo funciona de forma pasiva/automática.
- Cambiar para que el escudo se active manualmente pulsando su botón, igual que 50/50, hint y x2.
- Consumir un charge al activar. El efecto (protección contra fallo) se aplica en la siguiente respuesta incorrecta.
- Al activar el escudo, se impide el uso de cualquier otro powerup. Es mutuamente excluyente como todos. 

### 3. Misiones diarias: "jugar en supervivencia" se completa sin esfuerzo ✅
- La misión se completa solo por jugar, aunque se pierdan las 3 vidas inmediatamente.
- **Fix**: Cambiar el criterio de la misión para requerir un mínimo de aciertos (ej: "Acerta 5 preguntas en supervivencia") o un mínimo de preguntas respondidas correctamente.

## VISUAL

### 4. Pantalla de carga inicial: eliminar icono de balanza animado ✅
- Quitar el icono de la balanza que se mueve en la pantalla de carga/splash.
- Dejar solo un spinner o texto de carga simple.

### 5. Combo: quitar fuego de arriba, dejar solo la barra inferior ✅
- Eliminar el indicador de fuego superior que muestra el número de respuestas correctas consecutivas.
- Mantener únicamente la barra de combo inferior.

### 6. Configurar Examen: quitar iconos de números de preguntas ✅
- En la pantalla de configuración de examen, quitar los iconos (rayo, corazón, copa) junto a las opciones de número de preguntas.
- Dejar solo los números como texto.

### 7. Reducir alto mínimo de la tarjeta de pregunta ✅
- Algunas preguntas tienen 1-2 líneas y la tarjeta no se adapta al contenido.
- Reducir el `minHeight` de la tarjeta de pregunta para aprovechar mejor el espacio.

### 8. Diferenciar pregunta de respuestas visualmente
- Poner el texto de la pregunta en **negrita**.
- Cambiar el color de fondo de la tarjeta de pregunta para que resalte respecto a las respuestas.

### 9. Iconos de cofres: rediseñar o eliminar
- Los iconos actuales de cofres son visualmente pobres.
- Opción A: Rediseñar con iconos atractivos.
- Opción B: Usar icono de regalo/caja.
- Opción C: Eliminar icono y solo mostrar texto de puntos extra.

### 10. Actualizar texto de ayuda
- Actualizar la pantalla/texto de ayuda con todos los cambios anteriores:
  - Escudo ahora se activa manualmente.
  - Modo QUICK da x1.5 puntos.
  - Combo solo muestra barra inferior.
  - Cualquier otro cambio que afecte al comportamiento visible.
